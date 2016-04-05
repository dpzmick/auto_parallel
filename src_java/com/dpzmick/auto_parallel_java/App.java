package com.dpzmick.auto_parallel_java;

import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

class FibTask extends RecursiveTask<Integer> {
    private int grain_;
    private int n_;

    public FibTask(int n, int grain) {
        grain_ = grain;
        n_     = n;
    }

    static Integer serialCompute(int n) {
        if (n == 0 || n == 1)
            return 1;

        return serialCompute(n - 1) + serialCompute(n - 2);
    }

    @Override
    public Integer compute() {
        if (n_ == 0 || n_ == 1) {
            return 1;
        }

        if (n_ <= grain_) {
            return serialCompute(n_);
        }

        FibTask f1 = new FibTask(n_ - 1, grain_);
        FibTask f2 = new FibTask(n_ - 2, grain_);
        f1.fork();

        Integer this_thread = f2.compute();
        Integer other_thread = f1.join();

        return this_thread + other_thread;
    }
}


public class App {
    public static class Benchmark extends SimpleBenchmark {
        private int n;

        @Override
        protected void setUp() throws Exception {
            n = Integer.parseInt(System.getenv("BIG_FIB"));
        }

        public void timeFibFJ(int reps) {
            for (int i = 0; i < reps; i++) {
                ForkJoinPool pool = new ForkJoinPool();
                FibTask f = new FibTask(n, 35);
                Integer res = pool.invoke(f);
                System.out.println(res);
            }
        }

        public void timeFibSerial(int reps) {
            for (int i = 0; i < reps; i++) {
                System.out.println(FibTask.serialCompute(n));
            }
        }

    }

    public static void main(String[] args) {
        Runner.main(Benchmark.class, new String[0]);
        // System.out.println("hello world");
        // ForkJoinPool pool = new ForkJoinPool();
        // int n = Integer.parseInt(System.getenv("BIG_FIB"));
        // FibTask f = new FibTask(n, 31);
        // Integer res = pool.invoke(f);
        // System.out.println(res);
    }
}

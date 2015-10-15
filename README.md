# auto_parallel

    (def r1 (slow-function 100 #(rand-int 100)))
    (def r2 (slow-function 500 #(rand-int 100)))

    (time (+ (r2) (+ (r1) (r1))))
    "Elapsed time: 710 msecs"

    (time (par1 (+ (r2) (+ (r1) (r1)))))
    "Elapsed time: 503 msecs"

    (time (par2 (+ (r2) (+ (r1) (r1)))))
    "Elapsed time: 504 msecs"

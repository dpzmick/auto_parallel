# coding=utf8
from __future__ import print_function
import sys
from sys import stderr
from os import listdir
import os.path

# converts a directory of output files into a csv file with timings for each run
# output csv should be in form:
# benchmark_spec,cores,date,mean_execution_time

# the filename is the benchmark spec
# the log contains the number of cores
# the execution time needs to be pulled out of the log per spec

# "quick and dirty wins the race" - definitely not me

def usage():
    print("dir2csv.py output_dir", file=stderr)

def to_micros(b, unit):
    if unit == "µs":
        return b

    raise Exception("fixmepls")

def handle_single_spec(trial_dir, spec_name):
    print(trial_dir, spec_name, file=stderr)

    lines = None
    with open(os.path.join(trial_dir, spec_name)) as f:
        lines = f.readlines()

    if lines == None:
        print("lines was none for spec", trial_dir, spec_name, file=stderr)
        return None

    lines = map(lambda l: l.strip(), lines)

    mean_time_line = filter(lambda l: l.startswith("Execution time mean"), lines)
    mean_time_v = mean_time_line[0].split(":")[1].split()
    mean_time_base = float(mean_time_v[0])
    mean_time_unit = mean_time_v[1]

    mean_time = to_micros(mean_time_base, mean_time_unit)

    return (spec_name, mean_time)


def handle_trial(input_dir, trial_name):
    path = os.path.join(input_dir, trial_name)
    trial_date = trial_name.split('-')[1]

    # open the log, make sure we finished the run
    # if we did, get all the stuff we need from the log
    lines = None
    with open(os.path.join(path, 'log')) as log_f:
        lines = log_f.readlines()

    if lines == None:
        print("didn't get any lines in the log for", path, file=stderr)
        return []

    lines = map(lambda l: l.strip(), lines)

    if "finished test" not in lines:
        print("didn't finish the test:", path, file=stderr)
        print("skipping", file=stderr)
        return []

    cpus_line = filter(lambda l: l.startswith('num_cpus'), lines)
    num_cpus = int(cpus_line[0].split(":")[1])

    date = int(lines[2])

    children_data = []
    for f in listdir(path):
        if f == 'log' or f == 'env':
            continue

        children_data.append(handle_single_spec(path, f))

    my_data = []
    for name, time in children_data:
        my_data.append( (name, num_cpus, date, time) )
    return my_data

if __name__ == "__main__":
    if len(sys.argv) != 2:
        usage();
        sys.exit()

    input_dir = sys.argv[1]
    print("Reading from:", input_dir, file=stderr)

    data = []
    for path in listdir(input_dir):
        data.extend(handle_trial(input_dir, path))

    print("spec_name,cores,date,mean_execution_time")
    for d in data:
        print("%s,%d,%d,%f" % d)

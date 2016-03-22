# coding=utf8
from __future__ import print_function
import sys
from sys import stderr
from os import listdir
import json
import re
import os.path
import csv

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

    if unit == "sec":
        return 1e6 * b

    if unit == "ms":
        return 1000 * b

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

    # cpus_line = filter(lambda l: l.startswith('num_cpus'), lines)
    # num_cpus = int(cpus_line[0].split(":")[1])

    date = int(lines[2])

    children_data = []
    for f in listdir(path):
        if f == 'log' or f == 'env':
            continue

        children_data.append(handle_single_spec(path, f))

    my_data = []
    for name, time in children_data:
        my_data.append({
            'spec-name': name,
            'date': date,
            'mean-runtime': time
            })

    return my_data

def handle_host(host_dir):
    print("host: ", host_dir, file=stderr)

    try:

        # get the number of cores this host has
        mejson = None
        with open(os.path.join(host_dir, 'gcloud.json')) as me:
            mejson = json.loads(me.read())

        mename = None
        with open(os.path.join(host_dir, 'me')) as me:
            mename = me.readlines()[0].strip()

        me = None
        for d in mejson:
            if d[u'name'] == mename:
                me = d
                break

        plaform = me['cpuPlatform']
        # might as well get the number of cores from the name, since its string
        # manip to get it from the json anyway
        # of course, find the biggest hack possible and use that
        cores = int(re.sub(r"\D", "", mename.split('n')[0]))

        # for this host, get all the data from the benchmark directory
        bdir = os.path.join(host_dir, 'out')

        trials = []
        for path in listdir(bdir):
            trials.extend(handle_trial(bdir, path))

        for t in trials:
            t['platform'] = plaform
            t['host']     = mename
            t['cores']    = cores

        return trials

    except Exception, e:
        print(str(e), file=stderr)
        return []

if __name__ == "__main__":
    if len(sys.argv) != 2:
        usage();
        sys.exit()

    input_dir = sys.argv[1]
    print("Reading from:", input_dir, file=stderr)

    ds = []
    for path in listdir(input_dir):
        ds.extend(handle_host(os.path.join(input_dir, path)))

    w = csv.DictWriter(sys.stdout, sorted(ds[0].keys()))
    w.writeheader()
    for d in ds:
        w.writerow(d)

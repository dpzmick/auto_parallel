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

    # get the args
    args_line = filter(lambda l: l.startswith("running"), lines)[0]
    args = re.findall('\((.*)\)', args_line)[0]

    variance_lines = filter(lambda l: "variance" in l.lower(), lines)
    if len(variance_lines) != 0:
        vline = variance_lines[0]
        if "severely" in vline.lower():
            print("ignored ", trial_dir, spec_name, file=stderr)
            return None

    return (spec_name, mean_time, args.split(' '))

def java_trial(input_dir, trial_name):
    # trial is in log.bench
    lines = []
    with open(os.path.join(input_dir, trial_name, "log.bench")) as bench:
        lines = bench.readlines()

    unit_line = filter(lambda l: "runtime" in l.lower(), lines)[0]
    unit = unit_line.split()[1]

    # need to get the serial time and the FJ time
    fj_line = filter(lambda l: "FibFJ" in l, lines)[1]
    fj_time = to_micros(float(fj_line.split()[1]), unit)

    serial_line = filter(lambda l: "FibSerial" in l, lines)[1]
    serial_time = to_micros(float(serial_line.split()[1]), unit)

    m1 = {
        'spec-name': 'fib.fj_java',
        'mean-runtime': fj_time,
        'args0': None,
        'date': None
    }

    m2 = {
        'spec-name': 'fib.serial_java',
        'mean-runtime': serial_time,
        'args0': None,
        'date': None
    }

    return [m1, m2]


def handle_trial(input_dir, trial_name):
    print("trial: ", trial_name, file=stderr)

    path = os.path.join(input_dir, trial_name)
    trial_date = trial_name.split('-')[1]

    # open the log, make sure we finished the run
    # if we did, get all the stuff we need from the log
    lines = None
    with open(os.path.join(path, 'log')) as log_f:
        lines = log_f.readlines()

    try:

        if lines == None:
            print("didn't get any lines in the log for", path, file=stderr)
            return []

        lines = map(lambda l: l.strip(), lines)

        if "finished test" not in lines:
            print("didn't finish the test:", path, file=stderr)
            print("skipping", file=stderr)
            return []

        date = int(lines[2])

        children_data = []
        for f in listdir(path):
            if f == 'log' or f == 'env':
                continue

            d = handle_single_spec(path, f)
            if d == None:
                return []

            children_data.append(d)

        my_data = []
        for name, time, args in children_data:
            medat = {
                'spec-name': name,
                'date': date,
                'mean-runtime': time
                }

            for i, arg in enumerate(args):
                medat['args' + str(i)] = arg

            my_data.append(medat)

        return my_data

    except Exception, e:
        print(str(e), file=stderr)
        return {}

def handle_host(host_dir):
    print("host: ", host_dir, file=stderr)

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
    meid    = me['id']

    # might as well get the number of cores from the name, since its string
    # manip to get it from the json anyway
    # of course, find the biggest hack possible and use that
    cores = int(re.sub(r"\D", "", mename.split('n')[0]))

    # for this host, get all the data from the benchmark directory
    bdir = os.path.join(host_dir, 'out')

    trials = []
    for path in listdir(bdir):
        if 'is_java' in listdir(os.path.join(bdir, path)):
            trials.extend(java_trial(bdir, path))
        else:
            trials.extend(handle_trial(bdir, path))

    for t in trials:
        t['platform'] = plaform
        t['host']     = mename
        t['cores']    = cores
        t['host-id']  = meid

    return trials

    # except Exception, e:
    #     print(str(e), file=stderr)
    #     return []

if __name__ == "__main__":
    if len(sys.argv) != 2:
        usage();
        sys.exit()

    input_dir = sys.argv[1]
    print("Reading from:", input_dir, file=stderr)

    ds = []
    for path in listdir(input_dir):
        if path == ".git" or path == "archive":
            continue

        ds.extend(handle_host(os.path.join(input_dir, path)))

    w = csv.DictWriter(sys.stdout, sorted(ds[0].keys()))
    w.writeheader()
    for d in ds:
        w.writerow(d)

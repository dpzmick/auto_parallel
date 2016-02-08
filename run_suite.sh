#!/bin/bash

# this script ignores hyperthreading and NUMA
# if you need to care about those things, you'll have to be more careful

display_usage() {
    echo -e "usage $0 number_cpus output_dir specs_to_run"
    echo -e "where"
    echo -e "\tnumber_cpus - number of cpus to allow each process to use"
    echo -e "\toutput_dir - directory to dump results to"
    echo -e "\tspecs_to_run - the remaining arguments (at least one) are benchmark specs to execute"
}

if [ $# -lt 3 ]
then
    display_usage
    exit 1
fi

# first, load the environment needed for benchmarks
# the set -a thing is to force them to export
set -a
source ./env
set +a

num_cpus=$1
cpus=$(seq -s"," 0 $(expr $num_cpus - 1))

output_dir=$2
output_dir=$output_dir/$num_cpus-$(date +"%m.%d.%y.%H.%M.%S")
log_file=$output_dir/log

tmp=( "$@" )
specs=( "${tmp[@]:2}" )

echo "Running with $num_cpus cpus for each benchmark, and writing into $output_dir"

# make a new directory for this run
mkdir -p $output_dir

echo "run starting" > $log_file
date +"%m-%d-%y %H:%m:%S" >> $log_file
date +"%s" >> $log_file

echo >> $log_file
echo "using cpus: $cpus" >> $log_file

echo >> $log_file
echo "running these specs:" >> $log_file
for spec in "${specs[@]}"; do
    echo "$spec" >> $log_file
done

lein compile | tee -a $log_file

# each spec gets its own output file
for spec in "${specs[@]}"; do
    # setup filenames
    base=${spec//\//.}
    base=$output_dir/$base

    # run the spec and do the output
    taskset -c "$cpus" lein benchmark $spec | tee $base
done

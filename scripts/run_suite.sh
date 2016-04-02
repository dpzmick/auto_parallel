#!/bin/bash

# TODO log cpu usage
# TODO log git commit hash

display_usage() {
    echo -e "usage $0 output_dir specs_to_run"
    echo -e "where"
    echo -e "\toutput_dir - directory to dump results to"
    echo -e "\tspecs_to_run - the remaining arguments (at least one) are benchmark specs to execute"
}

if [ $# -lt 2 ]
then
    display_usage
    exit 1
fi

# first, load the environment needed for benchmarks
# the set -a thing is to force them to export
set -a
source ./env
set +a

output_dir=$1
output_dir=$output_dir/$(date +"%s")-$(hostname)
log_file=$output_dir/log

tmp=( "$@" )
specs=( "${tmp[@]:1}" )

echo "Running -- writing into $output_dir"

# make a new directory for this run
mkdir -p $output_dir
cp env $output_dir/env

echo "run starting" | tee $log_file
date +"%m-%d-%y %H:%m:%S" | tee -a $log_file
date +"%s" | tee -a $log_file

echo | tee -a $log_file
echo "running these specs:" | tee -a $log_file
for spec in "${specs[@]}"; do
    echo "$spec" | tee -a $log_file
done

lein compile | tee -a $log_file

cat /proc/cpuinfo | tee -a $log_file

# each spec gets its own output file
for spec in "${specs[@]}"; do
    # setup filenames
    base=${spec//\//.}
    base=$output_dir/$base

    # run the spec and do the output
    lein benchmark $spec | tee $base
done

echo "finished test" >> $log_file

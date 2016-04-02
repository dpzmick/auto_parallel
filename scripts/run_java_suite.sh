#!/bin/bash

# TODO log cpu usage
# TODO log git commit hash

display_usage() {
    echo -e "usage $0 output_dir"
    echo -e "where"
    echo -e "\toutput_dir - directory to dump results to"
}

if [ $# -lt 1 ]
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

echo "Running -- writing into $output_dir"

# make a new directory for this run
mkdir -p $output_dir
cp env $output_dir/env
touch $output_dir/is_java

echo "run starting" | tee $log_file
date +"%m-%d-%y %H:%m:%S" | tee -a $log_file
date +"%s" | tee -a $log_file

echo | tee -a $log_file

lein javac | tee -a $log_file

cat /proc/cpuinfo | tee -a $log_file

# run the spec and do the output
lein jbenchmark | tee $log_file.bench

echo "finished test" >> $log_file

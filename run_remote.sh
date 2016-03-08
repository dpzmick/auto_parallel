#!/bin/bash

# wrapper around run suite that runs the suite remotely

display_usage() {
    echo -e "usage $0 host auto_parallel_local num_cpus local_out specs"
    echo -e "where"
    echo -e "\thost - remote host"
    echo -e "\tauto_parallel_local - directory with auto parallel code locally"
    echo -e "\tnum_cpus - number of cpus to use remotely"
    echo -e "\tlocal_out - local directory to put output in"
    echo -e "\tspecs - specs to run, relative to the auto_parallel root on the remote"
}

if [ $# -lt 4 ]
then
    display_usage
    exit 1
fi

host=$1
auto_parallel_dir_local=$2
auto_parallel_dir_remote=/root/auto_parallel
num_cpus=$3
local_output_dir=$4

# idk
tmp=( "$@" )
specs=( "${tmp[@]:3}" )
specs=$(echo "${specs[*]}")

echo $specs

echo "pushing directory" $auto_parallel_dir_local

# copy a dir to run something
rsync -a $auto_parallel_dir_local root@$host:$(dirname $auto_parallel_dir_remote)

ssh root@$host auto_parallel_dir_remote=$auto_parallel_dir_remote num_cpus=$num_cpus specs=\"$specs\" 'bash -s' <<'ENDSSH'
    cd $auto_parallel_dir_remote
    ./run_suite.sh $num_cpus out $specs
ENDSSH

echo "pulling directory" $auto_parallel_dir_remote/out

# copy an output dir back
rsync -a root@$host:$auto_parallel_dir_remote/out .

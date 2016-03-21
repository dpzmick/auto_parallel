#!/bin/bash

display_usage() {
    echo -e "usage $0 num_cpus num_boxes local_out specs"
    echo -e "where"
    echo -e "\tnum_cpus  - number of cpus in each vm"
    echo -e "\tnum_boxes - number of vms to create"
    echo -e "\tlocal_out - local directory to put output in"
    echo -e "\tspecs - specs to run, relative to the auto_parallel root on the remote"
}

# configs
ssh_config_file=/tmp/vagrant-ssh-cfg
auto_parallel_dir_local=.
auto_parallel_dir_remote=/home/vagrant/auto_parallel

if [ $# -lt 4 ]
then
    display_usage
    exit 1
fi

num_cpus=$1
num_boxes=$2
local_out=$3

# idk
tmp=( "$@" )
specs=( "${tmp[@]:3}" )
specs=$(echo "${specs[*]}")

# bring up all the vms
# save the hosts to the config file one at a time
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n
    vagrant up $host
    vagrant ssh-config $host >> $ssh_config_file
done

# run the benchmarks,
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n
    ( echo cd $auto_parallel_dir_remote ; echo git pull ; rm -rf out ; \
        echo ./run_suite.sh out $specs) | \
        { ssh -F $ssh_config_file $host 2>&1 | sed "s/^/$host==>/" ; } &
done
wait

# copy the results into the local directory
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n
    mkdir -p $local_out/$host
    scp -F $ssh_config_file -r $host:$auto_parallel_dir_remote/out $local_out/$host
done

# shut down all the vms
# for n in `seq 1 $num_boxes` ; do
#     host="cores"$num_cpus"n"$n
#     vagrant halt $host
# done


# echo "pulling directory" $auto_parallel_dir_remote/out

# # copy an output dir back
# rsync -a root@$host:$auto_parallel_dir_remote/out .

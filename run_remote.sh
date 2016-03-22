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
ssh_config_file=~/.ssh/google_compute_engine
auto_parallel_dir_local=.
auto_parallel_dir_remote=/home/dpzmick/auto_parallel
zone=us-central1-a
project=senior-thesis-1257

if [ $# -lt 4 ]
then
    display_usage
    exit 1
fi

num_cpus=$1
num_boxes=$2
local_out=$3
mem=6

# idk
tmp=( "$@" )
specs=( "${tmp[@]:3}" )
specs=$(echo "${specs[*]}")

# bring up all the vms
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n

    gcloud compute instances create $host --custom-cpu $num_cpus \
        --custom-memory $mem --image ap-base-image &

    # vagrant up $host
    # vagrant ssh-config $host >> $ssh_config_file
done
wait

# get the ssh things configured
gcloud compute config-ssh

# run the benchmarks,
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n
    ( echo cd $auto_parallel_dir_remote ; echo git pull ; echo rm -rf out ; \
        echo ./run_suite.sh out $specs) | \
        { gcloud compute ssh $host 2>&1 | sed "s/^/$host==>/" ; } &
done
wait

# copy the results into the local directory
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n

    my_out=$local_out/$host-$(date +"%s")
    mkdir -p $my_out
    gcloud compute instances list --format=json > $my_out/gcloud.json
    echo $host > $my_out/me

    scp -r $host.$zone.$project:$auto_parallel_dir_remote/out $my_out
done

# shut down all the vms
for n in `seq 1 $num_boxes` ; do
    host="cores"$num_cpus"n"$n
    { yes Y | gcloud compute instances delete $host ; } &
done
wait


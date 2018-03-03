#!/bin/bash

## usage ./benchmark_template.sh <path to compose yaml> <ec2-adress>
## note: 
#    * ensure the cloudproto.pem is in the same folder as this script
#    * this script catches no wrong configurations or monitor whether all commands are executed successfully, you may log into ec2 and fix upcomming errors by yourself. 
#    * topology modifications or configurations at the components (e.g. frequency, metrics, ..) you must change manually in the compose file
#    * give your modified compose file a unique name
#    * if you run multiple benchmarks on the same swarm (we should not do that! ), then change the stackname "benchmark" to a unique name
#    * add your benchmark modifications add the step after the stack was deployed ( commented section below)

 


## topo file (docker compose file that contains the topology)
topofilepath=$1

topofile=$(basename $topofilepath)

## ec2 instance adress (e.g. ec2-18-196-231-143.eu-central-1.compute.amazonaws.com)
ec2=$2

## benchmark runtime
time=60

echo "transfer topology configuration to ec2 instance"
scp -i "cloudproto.pem" $topofilepath docker@$ec2:~


echo "Running the Topology: $topofile"
ssh -i "cloudproto.pem" docker@$ec2 "docker stack deploy -c ~/$topofile benchmark"

echo "run the benchmark $time seconds ..."
sleep $time

## add your benchmark modifications HERE

### examples 

## change the scaling to simulate failures (be aware of the correct service groups 
## u=0
## ssh -i "cloudproto.pem" docker@$ec2 "docker service scale benchmark_sensorGroupA=$u benchmark_sensorGroupB=$u benchmark_sensorGroupC=$u benchmark_sensorGroupD=$u"

## run database queries
#echo 'Exporting database'
# put you cql query in a separate file
#cqlsh $ec2 --cqlversion="3.4.4" -f query.cql > Benchmark.csv

## run the benchmark after modificatons further time?
## sleep $time

echo "Removing the Stack Topology: $topofile"
ssh -i "cloudproto.pem" docker@$ec2 "docker stack rm benchmark"

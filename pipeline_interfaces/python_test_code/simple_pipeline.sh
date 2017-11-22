#!/bin/bash

# kill all subshells and processes on exit
trap "kill 0" SIGINT
# start commands in subshells so all their spawn DIE when we exit
( python pipeline_component.py -r endpoint -p 50053 > endpoint.log ) &
( python pipeline_component.py -r gateway -p 50052 -pn 50053 > gateway.log ) &
sleep 1s
( python pipeline_component.py -r source -pn 50052 > source.log )&


echo "Press CTRL-c to kill the pipeline"
wait

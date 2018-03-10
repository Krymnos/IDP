#!/bin/bash

## topo file (docker compose file that contains the topology)
#topofilepath=$1
#topofile=$(basename $topofilepath)



## provenance configurations
TOPOLOGY_FILES_WITH_PROVENANCE=("topology_0_prov.yml" "topology_1_prov.yml" "topology_2_prov.yml")
TOPOLOGY_FILES_WITHOUT_PROVENANCE=("topology_0_noprov.yml" "topology_1_noprov.yml" "topology_2_noprov.yml")

PROV_BUFFER_SIZES=(1 5 10)
PROV_METRICS=("meterid,metricid,loc,line,class,app,ctime,stime,rtime" "meterid,ctime" "metricid,ctime" "loc,ctime" "class,ctime" "app,ctime" "ctime" "stime,ctime" "rtime,ctime")
BENCHMARK_RUNTIME=120

## this is the delay a sensor waits before sending the first measurement. 
## Set this value depending on you hardware (to low delay can lead on slower hardware that the sensor is sending messages to nodes that are not
## yet running
SENSOR_STARTUP_DELAY=60
SENSOR_DATA_FREQUENCY=1000 #milliseconds

DATE=$(date +"%Y%m%d%H%M")
BENCHMARK_MEASURMENTS_FILE="benchmark_measurements_${DATE}.csv"

#write header
echo "TOPOLOGY;PROV_METRICS;BUFFER_CAPACITY;COMPONENT;NET_IN;NET_IN_TYPE;NET_OUT;NET_OUT_TYPE;MSG_NUMBER;NORMALIZED_MSG_SIZE_IN;NORMALIZED_MSG_SIZE_OUT;NORMALIZED_MSG_SIZE_TYPE;BENCHMARK_RUNTIME" > $BENCHMARK_MEASURMENTS_FILE


### helper
trim() {
    local var="$*"
    # remove leading whitespace characters
    var="${var#"${var%%[![:space:]]*}"}"
    # remove trailing whitespace characters
    var="${var%"${var##*[![:space:]]}"}"   
    echo -n "$var"
}

evaluate() {
    echo "write metrics to $BENCHMARK_MEASURMENTS_FILE"

    echo "Topology: ${TOPONAME}, PROV_METRIC: $METRIC, PROV_BUFFER_SIZE: $BUFFER_SIZE"
    IFS='%' read -a DOCKER_ENTRIES <<< $(docker stats --no-stream --format '{{join (split .NetIO "/") ";"}};{{.ID}};{{ join (split .Name "_") ";" }}%')
    for ENTRY in "${DOCKER_ENTRIES[@]}"  
    do
        IFS=';' read -a CURRENT <<< $ENTRY
        CONTAINER_ID=${CURRENT[2]}
        COMPONENT=${CURRENT[4]}


        ## get the number of processed messages for pipeline components (no sensors, no cassandra, ... )
        MSG_NUMBER=""
        if [[ $COMPONENT =~ .*gateway.* ]] || [[ $COMPONENT =~ .*endpoint.* ]];
        then
            MSG_NUMBER=($(docker exec -it $CONTAINER_ID /bin/bash -c "redis-cli DBSIZE"))
            MSG_NUMBER=$(trim ${MSG_NUMBER[1]})
        fi


        NET_IN_ARR=($(grep -Eo '[0-9.]+|[[:alpha:]]+' <<< $(trim ${CURRENT[0]})))
        NET_OUT_ARR=($(grep -Eo '[0-9.]+|[[:alpha:]]+' <<< $(trim ${CURRENT[1]})))

        NET_IN_VAL=${NET_IN_ARR[0]}
        NET_IN_TYPE=${NET_IN_ARR[1]}

        NET_OUT_VAL=${NET_OUT_ARR[0]}
        NET_OUT_TYPE=${NET_OUT_ARR[1]}

        ## convert mb to kb
        if [[ $NET_IN_TYPE =~ MB ]];
        then
            NET_IN_VAL=$(expr $NET_IN_VAL*1000 | bc)
            NET_IN_TYPE="kB"
        fi
                
        if [[ $NET_OUT_TYPE =~ MB ]];
        then
            NET_OUT_VAL=$(expr $NET_OUT_VAL*1000 | bc)
            NET_OUT_TYPE="kB"
        fi

        NORMALIZED_MSG_SIZE_IN=$(bc <<< "scale=4; $NET_IN_VAL/$MSG_NUMBER*1000")
        NORMALIZED_MSG_SIZE_OUT=$(bc <<< "scale=4; $NET_OUT_VAL/$MSG_NUMBER*1000")
        NORMALIZED_MSG_SIZE_TYPE="B"

        echo "${TOPONAME};${METRIC};${BUFFER_SIZE};$COMPONENT;${NET_IN_VAL};${NET_IN_TYPE};${NET_OUT_VAL};${NET_OUT_TYPE};$MSG_NUMBER;$NORMALIZED_MSG_SIZE_IN;$NORMALIZED_MSG_SIZE_OUT;$NORMALIZED_MSG_SIZE_TYPE;$BENCHMARK_RUNTIME" >> $BENCHMARK_MEASURMENTS_FILE
    done
}

setup() {
    #copy
    TOPONAME=${ORIG_TOPOFILE%.*}
    TOPOFILE="temp.yml"
    cp $ORIG_TOPOFILE $TOPOFILE

    #replace provenancedata in the (temporary) topofile
    sed -i s/METRICS=.*/METRICS=$METRIC/g $TOPOFILE
    sed -i s/BUFFER_CAPACITY=.*/BUFFER_CAPACITY=$BUFFER_SIZE/g $TOPOFILE

    ## NOTE: STARTUP_DELAY=60 is only set for the sensors
    sed -i s/"STARTUP_DELAY=60"/"STARTUP_DELAY=$SENSOR_STARTUP_DELAY"/g $TOPOFILE
    sed -i s/"-frequency 1000"/"-frequency $SENSOR_DATA_FREQUENCY"/g $TOPOFILE
}

run() {
    ## benchmark runtime
    echo "Running the Topology: $TOPONAME"
    (docker-compose -f $TOPOFILE up)& 
    echo "sleep $SENSOR_STARTUP_DELAY seconds. Thats the time a sensor waits for sending (to ensure all nodes are running)."
    sleep $SENSOR_STARTUP_DELAY

    echo "run the benchmark $BENCHMARK_RUNTIME seconds ..."
    sleep $BENCHMARK_RUNTIME

    echo "benchmark finished: scale down the workload generator"
    #docker-compose scale source=0

    if [[ $TOPONAME =~ .*topology_2.* ]];
    then
        docker-compose -f $TOPOFILE scale sourceA=0 sourceB=0
    else
        docker-compose -f $TOPOFILE scale source=0
    fi    

    echo "wait 10 seconds to ensure the sensor is shut down and no further messages are produced"
    sleep 10
}


cleanup() {
    echo "Removing the Stack Topology: $TOPOFILE"
    docker-compose -f $TOPOFILE down

    docker stop $(docker ps -a -q)
    docker rm $(docker ps -a -q)
    sleep 1
}

### provenance runs
for ORIG_TOPOFILE in "${TOPOLOGY_FILES_WITH_PROVENANCE[@]}"  
do
    TOPONAME=${ORIG_TOPOFILE%.*}
    for BUFFER_SIZE in "${PROV_BUFFER_SIZES[@]}"  
    do  
        for METRIC in "${PROV_METRICS[@]}"  
        do
            setup
            run
            evaluate
            cleanup
        done
    done
done

### runs without provenance
for ORIG_TOPOFILE in "${TOPOLOGY_FILES_WITHOUT_PROVENANCE[@]}"  
do
    setup
    run
    evaluate
    cleanup
done

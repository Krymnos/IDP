# Pipeline

## Components
The Pipeline consist of three components that can be initialized by the *pipeline_component.py* script:

###Source:

The source generates (dummy) data and send this to a next component

example call:

        python pipeline_component.py -r source -pn 50052
    
Parameters:
    
        -pn --port_next  # receiver port (required)
        -hn --host_next  # receiver host (default: localhost)
        -i  --interval   # intervall the source is pushing data (default: 1 second)
        -n  --number_msg # if set, the source terminates after $number_msgs (default: -1 = no_termination)
        
Note: start the source component not until the gateway was initialized

###Gateway:
The gateway forwards received data immediately to the next component. 

call:
    
        python pipeline_component.py -r gateway -p 50052 -pn 50053

###Enpoint:
The Endpoint prints received data.

call:

        python pipeline_component.py -r endpoint -p 50053


## Simple Pipeline
Running the script **simple_pipeline.sh** starts a non terminating pipeline on localhost. The output for every component is stored 
in {source,gateway,endpoint}.log

        Source -> Gateway -> Enpoint

## Docker

### build the image
build the image for the pipeline component:

        docker build -t pipeline .

### Pipeline in one container
To start the simple_pipeline in a single container run:

        docker run pipeline:latest
        
To get the intermediate results from each component:

        docker exec <container-id> cat source.log
        docker exec <container-id> cat source.log
        docker exec <container-id> cat source.log

To get the container-id, you might use 

        docker ps


### Use docker compose

To start every component within an own container, run docker-compose.

        docker-compose up

The output here is directly written to the container console.



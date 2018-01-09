# IDP
University project


# Pipeline

## Docker
* run the commands from projects root directory.

### build the image sensor image

    docker build -t sensor ./pipeline_interfaces/sensor

### build the image for pipeline component


    docker build -t pipeline_java ./pipeline_interfaces/java_project/pipeline/


## run the pipeline that is defined in the docker-compose.yml

    docker-compose up



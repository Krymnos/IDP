# Compose Files

Use the compose files to run the interconnected components of the provenance system.
It's possible to start the pipeline part and the ui part separately.
The compose files uses the last pushed images on dockerhub ( https://hub.docker.com/u/cloudproto/ )

To start the *Pipeline* run:
    
 ```docker-compose -f docker-compose-pipeline.yml up```

To start the UI (ui-backend and ui-frontend):

 ```docker-compose -f docker-compose-ui.yml up```

The UI is available at *localhost:8080* ( please make sure that this port is not already in use)

To start pipeline and UI at once just run:

 ```docker-compose up```

## locally build images

### Pipeline Component
If you want to use your locally build images (created by the build_docker_image.sh script in pipeline), use the following command:
    ```docker-compose -f docker-compose-pipeline-local.yml up```

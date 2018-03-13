# IOT Delivery Pipeline

# Build States
* branch master: [![Build Status](https://travis-ci.org/Krymnos/IDP.svg?branch=master)](https://travis-ci.org/Krymnos/IDP) 
* branch deployment: [![Build Status](https://travis-ci.org/Krymnos/IDP.svg?branch=deployment)](https://travis-ci.org/Krymnos/IDP)

In the context of a smart grid, it is common to have a setup in which
sensors are not directly connected to a data center to deliver the generated sensor data but connected to a gateway, that is close to the site of where the sensors are deployed. The gateway can then be connected to another intermediary node, forming a  pipeline to deliver the data to the data center. In such a scenario a sensor will send its measurements to the 1st gateway of the pipeline. The gateway will receive the message, possibly perform some transformations to the data like aggregation and then forward the data to the next intermediary node in the pipeline. The intermediary
node might also transform the data and forward it to the next intermediary node. These intermediary nodes differ in their available resources and this process will continue until the endpoint of the pipeline, the data center, will be reached. Such a pipeline is shown below:

![IOT Pipeline](https://image.ibb.co/df70sx/IDP.png)
# Protocol

## Meeting 30.10.17

### Discussing possible use case:
 - Error Detection
  - Depending on the data we could detect various kinds of errors: sensors, network links, storage, etc.
  - analyze trade-off between accuracy and data storage
 
 ### Requirements:
 - security (problem: private users do not want their power usage to be public)
 - scalability
 - being able to visualize data
 - processing time (latency)
 - minimize storage
 - being able to fine tune parameters (also later on)
 
  ### define data format
  - possibly use opentracing.io
  - IDLs
  
  ### tasks to next meeting
  - everybody should think of a use case (e.g. error detection for a specific part in the pipeline) 
   and write exactly what metadata should be recorded at each node and maybe how it could be aggregated.
   
   
  ## Meeting 06.11.17
  
  ### Tracing (related work)
  - opentracing
  - aws tracing
  - there are many existing tracing solutions that we could use as insiration and related work
  - for first prototype we want to aim for very basic visualization
  
 ### data pipeline
  - define message format using google protocol buffers
  - use gRPC to generate APIs from our message definition
  - run gRPC clients/servers to send/receive messages between nodes (see architecture diagram)
  
 ### impementing tracing with gRPC
  - gRPC offers interfaces for tracing system to connect to it (see https://github.com/grpc-ecosystem/grpc-opentracing/tree/master/java)
  - investigate open tracing
  ..- possibly use kafka to manage all the data gathered from all the nodes
  
  ### Tasks after Initial Presentation 08.11.17
   - Implement first Pipeline with gRPC and limited Metadata (Kevin)
   - Solve Question: How should the Metadata be stored and available? (When, where, how) (Makrram)
   - Dig deeper in to the use case from the customers perspective: What parameters do we need save?
   ..- First use case: error detection (Vinoth)
   ..- second (possible) use case: provide proof of who consumed/produced how much and at what time (Ron)
   - come up with more use cases (Gerrit, Dashan, Talal)

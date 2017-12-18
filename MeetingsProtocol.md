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
      - possibly use kafka to manage all the data gathered from all the nodes
  
  ### Tasks after Initial Presentation 08.11.17
   - Implement first Pipeline with gRPC and limited Metadata (Kevin)
   - Solve Question: How should the Metadata be stored and available? (When, where, how) (Makrram)
   - Dig deeper in to the use case from the customers perspective: What parameters do we need save?
       - First use case: error detection (Vinoth)
       - second (possible) use case: provide proof of who consumed/produced how much and at what time (Ron)
   - come up with more use cases (Gerrit, Dashan, Talal)

## Meeting 13.11.17

### Issue 1: gRPC prototype
 - first API used by the sensor to push data to the first server is basically done
 - processing this data server side is up next.
 
### Use cases
 - base is still error detection
 - future use-case: full provenance in system where everybody can produce and consume electricity

### new tasks
 - @Everybody write notes to github issues!
 - Why do we use gRPC vs. others (e.g. zeroMQ)? (Kevin but everybody can contribute information to github issue)
 - look in to deployment with docker (Gerrit)
 - create requirements as google docs
     - list of requirements with motivations
     - section for each major decision we are facing
     - this document should motivate our approach to developing the prototype until decemeber after that we will evaluate the results
     - due to friday 17.11.

## Meeting 15.11.17

### revised tasks
- Design of the tracing system (Mukrram, Darshan) 
- Storage systems for tracing data (Talal)
- Visualization of latencies and dependcies (Vinoth)
- discuss impelmentation of pipeline grpc vs zero MQ = should we implement both to benchmark ourselves? (Kevin, Gerrit)
- investigate parameters (Ron)
- derive requirements to list

## Meeting 20.11.17

### (tracing) database solution
 - investigated opentsdb
     - optimized for timestamps
     - therefore relevant for our IoT context
 - investigated Neo4j
 
 ### question of 0mq vs. gRPC
 - not clear which is better
     - different papers and forums do not agree which is better
     - for the sake of a first prototype gRPC seems less effort to implement
 
 ### what is the actual tracing system going to look like
  - still not clear what framework/technologies we use to implement
  
 ### Components of our project so far
  - general data model
  - pipeline
  - tracing system
  - tracing db
  - frontend and visualization
 
## Meeting 22.11.

### new task
 - try graph-based database for saving dependencies together with a document based db for saving the larger context information (Vinoth, Talal)
 - try a noSQL DB for saving both dependencies and context together (Darshan)
 - send dominik the .proto file (Kevin)
 - finalize parameters for context (Mukrram, Ron)
 - type of queries we will support from the customer perspective (Mukrram)
 - look on existing tracing systems regarding:
     - interface to pipeline (Kevin, Ron)
     - interface to database (Gerrit)
     - what DBs are used in tracing (Vinoth, darshan, Talal)
    
## Meeting 29.11

### interface of grpc and tracing
 - open tracing library as mentioned before
 - still need to decide on parameters before implementing interface
 - writing our own collector might be feasible
 - scientific comparisons of different implementations (e.g. jaeger vs. zipkin) not found
 
### databases
 - ? results from talal and vinoth


### tasks
 - finalize parameters (Mukrram)
 - Implementing Collector based ZipKin (Kevin, Ron)
 - Implementing Collector based on Jaeger (Gerrit)
 - Create UI (Darshan)
  
  
 ## Meeting 4.12.
 
 ### tasks
 - Benchmark both DBs based on dummy (Vinoth, Darshan) Thursday
 - port pipeline to java (Gerrit, Kevin)
 - implement aggregation of messages on pipeline level for different nodes (Gerrit)
 - implement a provenance process that collects data based on the configuration file and writes it to the db (Mukrram)
 - hardcode template for GUI (Ron)
 
 ## Meeting 11.12. 
 present Members: Mukrram, Gerrit, Ron, Darshan, Talal
 
 ### Agenda
 - compare individual slides
 - discuss who presents what
 
 Result: 
 - individual parts not yet ready
 - Mukrram and Ron present
 
 ### Tasks
 - contribute individual slides
 - meet tuesday evening/wednesday morning for pitch practise
 
 
 ## Meeting 18.12
 present: Kevin, Talal, Darshan, Ron, Gerrit
 
 ### Agenda
 - discuss feedback from presentation
 - should we go for Scrum, i.e. weekly based deadlines of tasks?
 - How to measure individual contributions?
 - future tasks in each part
 
 #### from feedback
 - many questions were about the boundaries of our current work
 
 #### Scrum
 - all agree that weekly deadlines are good
 - issues can be created over the week but all new ones need to be discussed on monday
 
 #### Measuring contributions
 - duplicate issues need to be avoided
 - referencing of issues in commits is important
 - note spent time on closed issues
 
 
 
 
 
 

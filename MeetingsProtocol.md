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
  - 
  
  ### tasks to next meeting
  - everybody should think of a use case (e.g. error detection for a specific part in the pipeline) 
   and write exactly what metadata should be recorded at each node and maybe how it could be aggregated.

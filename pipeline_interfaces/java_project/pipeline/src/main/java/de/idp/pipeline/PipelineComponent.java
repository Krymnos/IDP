package de.idp.pipeline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.idp.pipeline.PipelineInterfaces.Grid_data;
import de.idp.pipeline.PipelineInterfaces.measurement_message;
import de.idp.pipeline.PipelineInterfaces.reply;
import de.idp.pipeline.gatewayGrpc.gatewayImplBase;
import de.idp.pipeline.storage.TimedAggregationStorage;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PipelineComponent {
	
	private static final Logger logger = Logger.getLogger(PipelineComponent.class.getName());
	
	public static Grid_data newGridData(String meterId, String metricId, long timestamp, double value) {
		return Grid_data.newBuilder().setMeasurement(measurement_message.newBuilder().setMeterId(meterId).setMetricId(metricId).setTimestamp(timestamp).setValue(value).build()).build();	
	}
	
	
	public static void main(String[] argv) throws IOException, InterruptedException{
        Args args = new Args();
        JCommander comm = JCommander.newBuilder()
                .addObject(args)
                .build();
        comm.parse(argv);

        if(args.help){
          comm.usage();
          return;
        }
        
        System.out.println(args.toString());
        // Start server with args.port or default port
        if(args.port == null || args.port_next == null || args.host_next == null || args.aggregationTime == null || args.storageTime == null) {
        	// use default values if a parameter is not set
        	gatewayServer server = new gatewayServer(50051, 50052, "localhost", 10, 5);
        	server.start();
        	server.blockUntilShutdown();
        	
        	// Client test
        	//gatewayClient client = new gatewayClient("localhost", 50052);
        	//client.pushData(testMessages);
        	 
        } else {
        	// implement pipeline topology and config parameters
        	gatewayServer server = new gatewayServer(args.port, args.port_next, args.host_next, args.aggregationTime, args.storageTime);
        	server.start();
        	server.blockUntilShutdown();
        	
        	//Client test
        	//gatewayClient client = new gatewayClient("localhost", 50052);
        	//client.pushData(testMessages);
        }
    }

}
// class for server implementation
class gatewayServer {
	private static final Logger logger = Logger.getLogger(gatewayServer.class.getName());

	private final int port;
	private final Server server;
	private final int portNext;
	private final String hostNext;
	static TimedAggregationStorage<measurement_message> aggregationStorage;

	public gatewayServer(int port, int portNext, String hostNext, int aggregationTime_s, int storagetime_m) throws IOException{
	    this.port = port;
	    this.portNext = portNext;
	    this.hostNext = hostNext;

	    server = ServerBuilder.forPort(port).addService(new pushDataService(hostNext, portNext, aggregationTime_s, storagetime_m)).build();
	}

	public void start() throws IOException{
		server.start();
		logger.info("Server started and is listening on " + server.getPort());
		Runtime.getRuntime().addShutdownHook(new Thread() {
		      @Override
		      public void run() {
		        System.err.println("*** shutting down gRPC server since JVM is shutting down");
		        gatewayServer.this.stop();
		        System.err.println("*** server shut down");
		      }
		});
	}
	public void stop() {
	    if (server != null) {
	      server.shutdown();
	    }
	}
	
	public void blockUntilShutdown() throws InterruptedException {
	    if (server != null) {
	      server.awaitTermination();
	    }
	}
	// implement push data service
	private static class pushDataService extends gatewayImplBase{
		private static final Logger logger = Logger.getLogger(PipelineComponent.class.getName());
		private final int portNext;
		private final String hostNext;
		private final int aggregationTime_s;


		public pushDataService(String hostNext, int portNext, int aggregationTime_s, int storagetime_m) {
			this.portNext = portNext;
		    this.hostNext = hostNext;
		    this.aggregationTime_s = aggregationTime_s;

		     if(aggregationTime_s > 0) {
				 aggregationStorage = new TimedAggregationStorage<measurement_message>(aggregationTime_s, TimeUnit.MINUTES.toSeconds(storagetime_m)) {
					 @Override public DoubleSummaryStatistics aggregate(List<measurement_message> items) {
						 return items.stream().mapToDouble(measurement_message::getValue).summaryStatistics();
					 }
				 };
			 }
		}

		@Override
		public StreamObserver<Grid_data> pushData(final StreamObserver<reply> responseObserver){
			return new StreamObserver<Grid_data>() {
				List<Grid_data> gDataList = new ArrayList<Grid_data>();
				
				// add each request message to gDataList
				@Override
				public void onNext(Grid_data request) {
			        // Process the request and send a response or an error.
			        try {
			          // Accept and enqueue the request.
			          String message = request.toString();
			          logger.info("Request: " + message);
			          gDataList.add(request);

			          if(aggregationTime_s > 0){
						aggregationStorage.put(request.getMeasurement());
					  }

		            } catch (Throwable throwable) {
		              throwable.printStackTrace();
		              responseObserver.onError(
		                  Status.UNKNOWN.withDescription("Error handling request").withCause(throwable).asException());
		            }
			      }

				 @Override
		          public void onError(Throwable t) {
		            // End the response stream if the client presents an error.
					 logger.log(Level.WARNING, "pushData cancelled");
		          }

				  // send gDataList to next hop after request stream finished
		          @Override
		          public void onCompleted() {
		            // Signal the end of work when the client ends the request stream.
					  String response_content = "200";
					  logger.info("Response: " + response_content);

                      //iam an endpoint
					  if(hostNext == null || portNext < 0){
		        		reply response = reply.newBuilder().setResponseCode(response_content).build();
						responseObserver.onNext(response);
						logger.info("COMPLETED");

						responseObserver.onCompleted();
		        		return;
					}
		            gatewayClient client = new gatewayClient(hostNext, portNext);
		            try {
						client.pushData(gDataList);	
					} catch (InterruptedException e) {
						 //TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							client.shutdown();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
		            reply response = reply.newBuilder().setResponseCode(response_content).build();
		            responseObserver.onNext(response);
		        	logger.info("COMPLETED");
		           
		            responseObserver.onCompleted();
		            
		          }
			};
		}
		}
		
	}

// class for client implementation
class gatewayClient{
	private ManagedChannel channel;
	private static gatewayGrpc.gatewayStub asyncStub;
	private static final Logger logger = Logger.getLogger(PipelineComponent.class.getName());
	
	public gatewayClient(String host, int port) {
	    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
	}
	
	public gatewayClient (ManagedChannelBuilder<?> channelBuilder) {
	channel = channelBuilder.build();
    asyncStub = gatewayGrpc.newStub(channel);
	}
	
	public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	public void pushData(List<Grid_data> gridDataList) throws InterruptedException{
		logger.info("--------Push Data-------");
		final CountDownLatch finishLatch = new CountDownLatch(1);
		StreamObserver<reply> responseObserver = new StreamObserver<reply>() {
			@Override
			public void onNext(reply response) {
				logger.info("Response code: " + response.getResponseCode());
			}
			
			@Override
			public void onError(Throwable t) {
				logger.warning("Push Data Failed: {0}" + Status.fromThrowable(t));
				finishLatch.countDown();
			}
		      @Override
		      public void onCompleted() {
		        logger.info("------Data Pushed------");
		        finishLatch.countDown();
		}
		};
	StreamObserver<Grid_data> requestObserver = asyncStub.pushData(responseObserver);
	try {
		for (int i = 0; i < gridDataList.size(); i++) {
			Grid_data gData = gridDataList.get(i);
			requestObserver.onNext(gData);
			if (finishLatch.getCount() == 0) {
				return;
			}
		}
	} catch (RuntimeException e) {
	      // Cancel RPC
	      requestObserver.onError(e);
	      throw e;
	  }
	requestObserver.onCompleted();
	// Receiving happens asynchronously
	if (!finishLatch.await(1, TimeUnit.MINUTES)) {
	  logger.warning("push data can not finish within 1 minutes");
	}
	}
}

class Args {
    @Parameter(names = { "-p", "--port" }, description = "port for this component")
    public Integer port;

    @Parameter(names = { "-pn", "--port_next" }, description = "port for the next_hop")
    public Integer port_next;

    @Parameter(names = { "-hn", "--host_next" }, description = "host for the next hop. If this is not set then this"
                                                             + " node is an endpoint.")
    public String host_next;

    @Parameter(names = {"-agg", "--aggregationTime"}, description = "aggregate message values (min, max, avg) of the last n"
                                                            + " seconds. If this is not set then no aggregation takes place.")
    public Integer aggregationTime;

    @Parameter(names = {"-st", "--storagetime"}, description = "specify the storagetime of messages / aggregation (in seconds)")
    public Integer storageTime;

    @Parameter(names = "--help", description = "prints this help", help = true)
    public boolean help;

    @Override public String toString() {
      return "Args{" +
              "port=" + port +
              ", port_next=" + port_next +
              ", host_next='" + host_next + '\'' +
              ", aggregationTime=" + aggregationTime +
              ", storageTime=" + storageTime +
              '}';
    }
}
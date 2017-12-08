package de.idp.pipeline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.idp.pipeline.PipelineInterfaces.Grid_data;
import de.idp.pipeline.PipelineInterfaces.measurement_message;
import de.idp.pipeline.PipelineInterfaces.reply;
import de.idp.pipeline.gatewayGrpc.gatewayImplBase;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class PipelineComponent {
	
	private static final Logger logger = Logger.getLogger(PipelineComponent.class.getName());
	
	private static Grid_data newGridData(String meterId, String metricId, long timestamp, double value) {
		return Grid_data.newBuilder().setMeasurement(measurement_message.newBuilder().setMeterId(meterId).setMetricId(metricId).setTimestamp(timestamp).setValue(value).build()).build();	
	}
	
	// test function to create testmessages to be sent by test client
	private static List<Grid_data>createDummyTestMessages (int numMessages) {
		List<Grid_data> gDataList = new ArrayList<Grid_data>() ;
		
		for (int i = 0; i< numMessages; i++) {
			gDataList.add(newGridData("meter"+i, "metric"+i, i, i+1));
		}
		return gDataList;
	}
	
	
	public static void main(String[] argv) throws IOException, InterruptedException{
        Args args = new Args();
        JCommander comm = JCommander.newBuilder()
                .addObject(args)
                .build();
        List<Grid_data> testMessages = createDummyTestMessages(10);
        logger.info(testMessages.toString());
        comm.parse(argv);

        if(args.help){
          comm.usage();
          return;
        }
        
        System.out.println(args.toString());
        // Start server with args.port or default port
        if(args.port == null) {
        	gatewayServer server = new gatewayServer(50051);
        	server.start();
        	server.blockUntilShutdown();
        	
        	// Client test
        	//gatewayClient client = new gatewayClient("localhost", 50052);
        	//client.pushData(testMessages);
        	 
        } else {
        	gatewayServer server = new gatewayServer(args.port);
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
	
	public gatewayServer(int port) throws IOException{
	    this.port = port;
	    server = ServerBuilder.forPort(port).addService(new pushDataService()).build();
	    
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
		            gatewayClient client = new gatewayClient("localhost", 50052);
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

    @Parameter(names = {"-agg", "--aggregate"}, description = "aggregate message values (min, max, avg) of the last n"
                                                            + " seconds. If this is not set then no aggregation takes place.")
    public Integer aggregationTime;

    @Parameter(names = {"-st", "--storagetime"}, description = "specify the storagetime of messages (in seconds)")
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
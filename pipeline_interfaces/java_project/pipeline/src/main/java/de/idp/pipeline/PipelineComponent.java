package de.idp.pipeline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.idp.pipeline.PipelineInterfaces.Grid_data;
import de.idp.pipeline.PipelineInterfaces.measurement_message;
import de.idp.pipeline.PipelineInterfaces.reply;
import de.idp.pipeline.gatewayGrpc.gatewayImplBase;
import de.idp.pipeline.storage.TimedAggregationStorage;
import de.idp.pipeline.util.SystemHelper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisHashAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.provenance.ProvenanceContext;
import io.provenance.exception.ConfigParseException;
import io.provenance.exception.SetupException;
import io.provenance.types.Context;
import io.provenance.types.ContextBuilder;
import io.provenance.types.Datapoint;
import io.provenance.types.InputDatapoint;
import io.provenance.types.Location;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class PipelineComponent {
	
	private static final Logger logger = Logger.getLogger(PipelineComponent.class.getName());
	public static Grid_data newGridData(String meterId, String metricId, long timestamp, double value) {
		return Grid_data.newBuilder().setMeasurement(measurement_message.newBuilder().setMeterId(meterId).setMetricId(metricId).setTimestamp(timestamp).setValue(value).build()).build();	
	}
	
	
	public static void main(String[] argv) throws Exception {
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

        if(args.propertiesfile != null ){
            SystemHelper.setPropertiesFile(args.propertiesfile);
        } else{
            SystemHelper.setPropertiesFile();
        }

        // Start server with args.port or default port
        if(argv.length == 0) {
        	// use default values if parameters are not set
        	gatewayServer server = new gatewayServer(50051, 50052, "localhost", "default", 5, args.no_prov);
        	server.setVerbose(args.verbose);
        	server.setNoProv(args.no_prov);
        	server.start();
        	server.blockUntilShutdown();

        	
        	// Client test
        	//gatewayClient client = new gatewayClient("localhost", 50052);
        	//client.pushData(testMessages);
        	 
        	//args indicate endpoint
        } else if (argv.length >= 1 && (args.port_next==null || args.host_next==null)) {
        	logger.info("endpoint recognized");
        	String location;
        	int storage_time;
        	int port;
        	if (args.port==null) {
        		port = 50051;
        	} else {
        		port =args.port;
        	}
        	if (args.location == null) {
        		location = "default";
        	} else {
        		location = args.location;
        	}
        	if (args.storageTime == null) {
        		storage_time = 5;
        	} else {
        		storage_time = args.storageTime;
        	}
        	gatewayServer server = new gatewayServer(port, -1, "", location, storage_time, args.no_prov);
        	server.setVerbose(args.verbose);
        	server.setNoProv(args.no_prov);
        	server.start();
        	server.blockUntilShutdown();
        	
        	//args indicate gateway
        } else if (argv.length >= 1 && (args.port == null || args.location == null || args.storageTime == null)) {
        	logger.info("gateway recognized");
        	String location;
        	int storage_time;
        	int port;
        	if (args.port==null) {
        		port = 50051;
        	} else {
        		port =args.port;
        	}
        	if (args.location == null) {
        		location = "default";
        	} else {
        		location = args.location;
        	}
        	if (args.storageTime == null) {
        		storage_time = 5;
        	} else {
        		storage_time = args.storageTime;
        	}
        	gatewayServer server = new gatewayServer(port, args.port_next, args.host_next, location, storage_time, args.no_prov);
        	server.setVerbose(args.verbose);
        	server.setNoProv(args.no_prov);
        	server.start();
        	server.blockUntilShutdown();
        }	
        	// args are set
        else {
        	// implement pipeline topology and config parameters
        	gatewayServer server = new gatewayServer(args.port, args.port_next, args.host_next, args.location, args.storageTime, args.no_prov);
			server.setVerbose(args.verbose);
			server.setNoProv(args.no_prov);
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
	private final String location;
	public static String className;
	public static RedisClient dbClient;
	public static StatefulRedisConnection<String, String> dbConnection;
	public static double sendCount;
	public static double receiveCount;
	public static PrintWriter pw;
	public boolean no_prov2;
	public boolean noProv;
	
	static TimedAggregationStorage<measurement_message> aggregationStorage;
	static gatewayServer.pushDataService pushDataService;
	gatewayServer.localStorageTimer localStorageTimer;
	static gatewayServer.markAliveTimer markAliveTimer1;
	private sendReceiveTimer sendReceiveT;

	public gatewayServer(int port, int portNext, String hostNext, String location, int storagetime_m, boolean no_prov)
			throws IOException, ConfigParseException, SetupException {
	    this.port = port;
	    this.portNext = portNext;
	    this.hostNext = hostNext;
	    this.location = location;
	    this.noProv=no_prov;
	    className = this.getClass().getSimpleName();
	    dbClient = RedisClient.create("redis://localhost:6379");
	    dbConnection = dbClient.connect();
	    
	    
	    if(dbConnection.isOpen()){
	    	logger.info("Redis Connection to localhost:6379 was established");
		}
	    this.pushDataService = new pushDataService(hostNext, portNext, location, storagetime_m,noProv );
	    this.localStorageTimer = new localStorageTimer(storagetime_m);
	    this.markAliveTimer1 = new markAliveTimer();
	    this.sendReceiveT = new sendReceiveTimer();
	    receiveCount=0;
	    sendCount=0;
	    server = ServerBuilder.forPort(port).addService(pushDataService).build();
	}

	public void setVerbose(boolean verbose){
		this.pushDataService.setVerbose(verbose);
	}
	public void setNoProv(boolean no_prov){
		this.pushDataService.setNoProv(no_prov);
		this.no_prov2 = no_prov;
		if(no_prov2) {
		    try {
				pw = new PrintWriter(new File("/mnt/timestamp.csv"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
			    dbConnection.close();
				dbClient.shutdown();
				pw.close();
		      }
		});
	}
	public void stop() {
	    if (server != null) {
	      server.shutdown();
	    }
	    if (dbConnection != null) {
	    	dbConnection.close();
	    	dbClient.shutdown();
	    
	    }
	    if (pw != null) {
	    	pw.close();
	    }
	}
	
	public void blockUntilShutdown() throws InterruptedException {
	    if (server != null) {
	      server.awaitTermination();
	    }
	    if (dbConnection != null) {
	    	dbConnection.close();
	    	dbClient.shutdown();
	    }
	    if (pw != null) {
	    	pw.close();
	    }
	}
    private static class localStorageTimer implements ActionListener {
		RedisClient dbClient;
		StatefulRedisConnection<String, String> dbConnection;
		RedisCommands<String, String> commands;
		static Timer myTimer;
		
		
		public localStorageTimer(int storagetime) {
			storagetime= storagetime*60*1000;
			myTimer = new Timer(storagetime, this);
			myTimer.start();
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("DB gets flushed");
			dbClient = RedisClient.create("redis://localhost:6379");
			dbConnection = dbClient.connect();
			commands = dbConnection.sync();
			commands.flushdb();
		}
	}
    static class markAliveTimer implements ActionListener {
		
		static Timer markTimer;
		public markAliveTimer() {
			int markTime= 2*1000;
			markTimer = new Timer(markTime, this);
			markTimer.start();
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			pushDataService.callPcMarkAlive();
		}
	}
    
	class sendReceiveTimer implements ActionListener {
		
		Timer sendReceiveT;
		public sendReceiveTimer() {
			int time= 1000;
			sendReceiveT = new Timer(time, this);
			sendReceiveT.start();
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if (hostNext==null || portNext<1) {
				logger.info(":::::: Receive count: " + receiveCount);
				pushDataService.receiveSet(receiveCount);
				receiveCount=0;
			}
			else {
			
				pushDataService.sendReceiveSet(sendCount, receiveCount);
				logger.info(":::::: Send count: " + sendCount);
				logger.info(":::::: Receive count: " + receiveCount);
				sendCount=0;
				receiveCount=0;
				
				
				
			}
		}
	}
    
	// implement push data service
	private static class pushDataService extends gatewayImplBase{
		private static final Logger logger = Logger.getLogger(PipelineComponent.class.getName());
		private final int portNext;
		private final String hostNext;
		private final String location;
		private final String appName;
		final ProvenanceContext pc;
		private RedisHashAsyncCommands<String, String> asyncCommands;
		private boolean verbose;
		private boolean no_prov;
		public static double lat;
		public static double lg;
		private final String context;
		private boolean noProv;

	
		
		public pushDataService(String hostNext, int portNext, String location, int storagetime_m, boolean noProv)
				throws ConfigParseException, SetupException {
		
			this.portNext = portNext;
		    this.hostNext = hostNext;
		    this.location = location;
		    this.noProv = noProv;
		    
		    //local storage client init
		    asyncCommands = dbConnection.async();
		    System.out.println(noProv);
		    if (noProv==false) {
		    	 pc = ProvenanceContext.getOrCreate();
		    	 String[] contextParams = pc.getContextParams();
				 context = String.join(",", contextParams);
		    }else {
		    	pc=null;
		    	context=null;
		    }
		   
		    
		    appName = this.getClass().getSimpleName() ;
		    lat=7.772406+(Math.random()*(2.46408));
		    lg=51.657817+(Math.random()*(2.262556));
		}


		public void setVerbose(boolean verbose){
			this.verbose = verbose;
		}
		public boolean getNoProv() {
			return this.no_prov;
		}
		public void setNoProv(boolean no_prov){
			this.no_prov = no_prov;
		}
		public void sendReceiveSet(double sendRate, double receiveRate) {
			try {
				if (getNoProv()==false) {
				this.pc.rate(sendRate, receiveRate);
				markAliveTimer.markTimer.restart();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void receiveSet(double receiveRate) {
			try {
				if (getNoProv()==false) {
				this.pc.receiveRate(receiveRate);
				}
				markAliveTimer.markTimer.restart();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void callPcMarkAlive () {
			//logger.info(".........................................markAlive called");
			if (getNoProv()==false) {
	    	this.pc.markAlive();
			}
	    }

		@Override
		public StreamObserver<Grid_data> pushData(final StreamObserver<reply> responseObserver){
			return new StreamObserver<Grid_data>() {
				private List<Grid_data> gDataList = new ArrayList<Grid_data>();
				private List<Context> provContextList = new ArrayList<Context>();
				// add each request message to gDataList
				
				@Override
				public void onNext(Grid_data request) {
			        // Process the request and send a response or an error.
			        try {
			          logger.info("gDataList length: "+ gDataList.size());
			          // Accept and enqueue the request.
			          String message = request.toString();
			          Grid_data newMessage;
			          newMessage = null;
			          receiveCount++;
			          //logger.info("-----Receive count: "+ receiveCount);
			          // Save every parameter for easy handling and local saving
			          String meterID = request.getMeasurement().getMeterId();
			          String metricID = request.getMeasurement().getMetricId();
			          Date receiveTime = new Date();
			          long receiveTime2=System.currentTimeMillis();
			          logger.info("receive time: " + receiveTime);
			         // logger.info("Request: " + message);
			          if (no_prov==false) {
			          ContextBuilder cBuilder = new ContextBuilder();
			          Context context = cBuilder.build();
			          context.setAppName(appName);
			          context.setClassName(className);
			          context.setReceiveTime(receiveTime);
			          // for now loc is just gateway for every hop lets think of sth later
			          context.setLoc(new Location(location, lat, lg));
			          context.setLineNo((long) 185);
			          context.setTimestamp(System.currentTimeMillis());
			          context.setMeterId(meterID);
			          context.setMetricId(metricID);
			        
			          //uncomment for prov API arguments output
			          /*logger.info("Appname: " + context.getAppName());
			          logger.info("Class name: " + context.getClassName());
			          logger.info("Receive Time: " + context.getReceiveTime());
			          logger.info("location: " + context.getLoc().getLable());
			          logger.info("Line no: " + context.getLineNo());
			          logger.info("timestamp: " + context.getTimestamp());
					*/
			          provContextList.add(context);
			          }else {
			        	  
			        	  newMessage = Grid_data.newBuilder().setMeasurement(request.getMeasurement()).setProvId(request.getProvId()+", " + receiveTime2).build();		        	  
			        	  logger.info("Prov_id: " + newMessage);
			          }
			          if (newMessage == null) {
			          gDataList.add(request);
			          }else {
			          gDataList.add(newMessage);
			          }
			          if(verbose){
						  System.out.println("got message: " + request.toString());
					  }
			          if (gDataList.size()>=10){
			        	  sendInbetween();			        	  
			          }

		            } catch (Throwable throwable) {
		              throwable.printStackTrace();
		              responseObserver.onError(
		                  Status.UNKNOWN.withDescription("Error handling request").withCause(throwable).asException());
		            }
			      }
				  public void sendInbetween() {
					  if(hostNext == null || portNext < 0){
						  String[] provIds;
						  Date sendTime;
						  if (no_prov==false) {
						  
						  sendTime = new Date();
						  logger.info("send time: " + sendTime);
						  
				          Datapoint[] dpList = new Datapoint[provContextList.size()];
				          InputDatapoint[] inputdatapoints= new InputDatapoint[1];
				          for (int i=0; i < gDataList.size(); i++) {
					          provContextList.get(i).setSendTime(sendTime);
					          dpList[i] = new Datapoint(provContextList.get(i));
					          
					          if (gDataList.get(i).getProvId()!= "") {
					            	System.out.println("print");
					            	inputdatapoints[0] = new InputDatapoint((gDataList.get(i).getProvId()), "simple");
					            	//System.out.println(inputdatapoints[0]);
					            	dpList[i].setInputDatapoints(inputdatapoints);
					            	logger.info("input datapoints= " + dpList[i].getInputDatapoints()[0].getId());
					            } 	
					      }
				          // uncomment following lines if DB is ready for provenance API
				          provIds = new String[dpList.length];
						  try {
							  provIds = pc.save(dpList);
							  //logger.info(".............................timer restart gets called");
							  markAliveTimer.markTimer.restart();
							  
						  } catch (InterruptedException e) {
							//TODO: handle error
						  	e.printStackTrace();
						  }
						  }
						  else {
							  
							  provIds = new String[gDataList.size()];
							  for (int i =0; i<gDataList.size();i++) {
								  provIds[i]=String.valueOf(System.nanoTime())+String.valueOf(i) ;
							  for (int j=0; j < provIds.length; j++) {
									  pw.write(gDataList.get(j).getProvId()+'\n');
								  }
							  }
							  
						  }
						  for (int i=0; i < provIds.length; i++) {
                              asyncCommands.hset(provIds[i], "metricID", gDataList.get(i).getMeasurement().getMetricId());
                              asyncCommands.hset(provIds[i], "value", "" + gDataList.get(i).getMeasurement().getValue());
                              asyncCommands.hset(provIds[i], "meterID", "" + gDataList.get(i).getMeasurement().getMeterId());
                              asyncCommands.hset(provIds[i], "timestamp", "" + gDataList.get(i).getMeasurement().getTimestamp());
                              
                              
                              
                              if (gDataList.get(i).getProvId() != "") {
                                asyncCommands.hset(provIds[i], "ProvID", gDataList.get(i).getProvId());
                              }
						  }
						  gDataList.removeAll(gDataList);
						  provContextList.removeAll(provContextList);
						  return;
					}
					  Date sendTime;
					  gatewayClient client = new gatewayClient(hostNext, portNext);
					  try {
						String[] provIds;
						 if (no_prov==false) {
						sendTime = new Date();
						InputDatapoint[] inputdatapoints= new InputDatapoint[1];
			            Datapoint[] dpList = new Datapoint[provContextList.size()];
			            for (int i=0; i < gDataList.size(); i++) {
				            provContextList.get(i).setSendTime(sendTime);
				            dpList[i] = new Datapoint(provContextList.get(i));
				            if (gDataList.get(i).getProvId()!= "") {
				            	inputdatapoints[0] = new InputDatapoint((gDataList.get(i).getProvId()), "simple");
				            	//System.out.println(inputdatapoints[0]);
				            	dpList[i].setInputDatapoints(inputdatapoints);
				            	logger.info("input datapoints= " + dpList[i].getInputDatapoints()[0].getId());
				            }
				            
				            }
			            // uncomment following lines if DB is ready for provenance API
			            
			            provIds = new String[dpList.length];
			            logger.info("pc.save will be executed");
			            provIds = pc.save(dpList);
						//logger.info(".........................timer restart gets called");
						markAliveTimer.markTimer.restart();
			            String pIds = "";
			            for (int i=0; i < provIds.length; i++) {
			            	if (i==0){
			            		pIds = pIds + provIds[i];
			            	}
			            	else {
			            	pIds=pIds +  ", " + provIds[i];
			            	}
			            }
			            logger.info("list of prov_ids: " + pIds);
						 }else {
							 provIds = new String[gDataList.size()];
							 for (int i=0; i<gDataList.size();i++) {
								 provIds[i]=String.valueOf(System.nanoTime())+String.valueOf(i);
							 }
							 long sendTime2= System.currentTimeMillis();
							 for (int i=0; i<gDataList.size();i++) {
								 Grid_data message = gDataList.get(i);
						 Grid_data newMessage = Grid_data.newBuilder().setMeasurement(message.getMeasurement()).setProvId(message.getProvId()+", " + sendTime2).setContext(context).build();

								 gDataList.set(i, newMessage);
							 }
							 
						 }
			            for (int i=0; i < provIds.length; i++) {
                            asyncCommands.hset(provIds[i], "metricID", gDataList.get(i).getMeasurement().getMetricId());
                            asyncCommands.hset(provIds[i], "value", "" + gDataList.get(i).getMeasurement().getValue());
                            asyncCommands.hset(provIds[i], "meterID", "" + gDataList.get(i).getMeasurement().getMeterId());
                            asyncCommands.hset(provIds[i], "timestamp", "" + gDataList.get(i).getMeasurement().getTimestamp());
                            if (gDataList.get(i).getProvId() != "") {
                              asyncCommands.hset(provIds[i], "ProvID", gDataList.get(i).getProvId());
                            }
			            }
			            if (no_prov==false) {
			            for(int i=0; i<provIds.length; i++) {
			            	Grid_data message = gDataList.get(i);
			            	Grid_data newMessage = Grid_data.newBuilder().setMeasurement(message.getMeasurement()).setProvId(provIds[i]).setContext(context).build();
			            	gDataList.set(i, newMessage);
			            	sendCount++;
			            }}
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
					  gDataList.removeAll(gDataList);
					  provContextList.removeAll(provContextList);
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
					  logger.info("gDataList size: "+gDataList.size());
					 
					  // if still messages in the queues process them
					  if (gDataList.size()>0) {

	                      //iam an endpoint
						  if(hostNext == null || portNext < 0){
							  String[] provIds;
								 if (no_prov==false) {
							  Date sendTime;
							  sendTime = new Date();
							  logger.info("send time: " + sendTime);
					          Datapoint[] dpList = new Datapoint[provContextList.size()];
					          InputDatapoint[] inputdatapoints= new InputDatapoint[1];
					          for (int i=0; i < gDataList.size(); i++) {
						          provContextList.get(i).setSendTime(sendTime);
						          dpList[i] = new Datapoint(provContextList.get(i));
						          
						          if (gDataList.get(i).getProvId()!= "") {
						            	System.out.println("print");
						            	inputdatapoints[0] = new InputDatapoint((gDataList.get(i).getProvId()), "simple");
						            	//System.out.println(inputdatapoints[0]);
						            	dpList[i].setInputDatapoints(inputdatapoints);
						            	logger.info("input datapoints= " + dpList[i].getInputDatapoints()[0].getId());
						            } 	
						      }
					          // uncomment following lines if DB is ready for provenance API
					          provIds = new String[dpList.length];
							  try {
								  provIds = pc.save(dpList);
								  logger.info(".................................timer restart gets called");
								  markAliveTimer.markTimer.restart();
							  } catch (InterruptedException e) {
								//TODO: handle error
							  	e.printStackTrace();
							  }
								 }else {
									  provIds = new String[gDataList.size()];
									  for (int i =0; i<gDataList.size();i++) {
										  provIds[i]=String.valueOf(System.nanoTime())+String.valueOf(i) ;
									  }
									  for (int j=0; j < provIds.length; j++) {
										  pw.write(gDataList.get(j).getProvId()+'\n');
									  }
								  }
							  for (int i=0; i < provIds.length; i++) {
	                              asyncCommands.hset(provIds[i], "metricID", gDataList.get(i).getMeasurement().getMetricId());
	                              asyncCommands.hset(provIds[i], "value", "" + gDataList.get(i).getMeasurement().getValue());
	                              asyncCommands.hset(provIds[i], "meterID", "" + gDataList.get(i).getMeasurement().getMeterId());
	                              asyncCommands.hset(provIds[i], "timestamp", "" + gDataList.get(i).getMeasurement().getTimestamp());
	                              if (gDataList.get(i).getProvId() != "") {
	                                asyncCommands.hset(provIds[i], "ProvID", gDataList.get(i).getProvId());
	                              }
							  }
							  reply response = reply.newBuilder().setResponseCode(response_content).build();
							responseObserver.onNext(response);
							logger.info("COMPLETED");
							
							responseObserver.onCompleted();
			        		return;
						}
						  
						  Date sendTime;
						  gatewayClient client = new gatewayClient(hostNext, portNext);
						  try {
							  String[] provIds;
								 if (no_prov==false) {
							sendTime = new Date();
							InputDatapoint[] inputdatapoints= new InputDatapoint[1];
				            Datapoint[] dpList = new Datapoint[provContextList.size()];
				            for (int i=0; i < gDataList.size(); i++) {
					            provContextList.get(i).setSendTime(sendTime);
					            dpList[i] = new Datapoint(provContextList.get(i));
					            if (gDataList.get(i).getProvId()!= "") {
					            	inputdatapoints[0] = new InputDatapoint((gDataList.get(i).getProvId()), "simple");
					            	//System.out.println(inputdatapoints[0]);
					            	dpList[i].setInputDatapoints(inputdatapoints);
					            	logger.info("input datapoints= " + dpList[i].getInputDatapoints()[0].getId());
					            }
					            
					            }
				            // uncomment following lines if DB is ready for provenance API
				            
				            provIds = new String[dpList.length];
				            logger.info("pc.save will be executed");
				            provIds = pc.save(dpList);
							//logger.info("................................................timer restart gets called");
							markAliveTimer.markTimer.restart();
				            String pIds = "";
				            for (int i=0; i < provIds.length; i++) {
				            	if (i==0){
				            		pIds = pIds + provIds[i];
				            	}
				            	else {
				            	pIds=pIds +  ", " + provIds[i];
				            	}
				            }
				            logger.info("list of prov_ids: " + pIds);
							} else {
									  provIds = new String[gDataList.size()];
									  for (int i =0; i<gDataList.size();i++) {
										  provIds[i]=String.valueOf(System.nanoTime())+String.valueOf(i) ;
									  }
									  long sendTime2= System.currentTimeMillis();
									  //sendTime= new Date(sendTime);
									  for (int i=0; i<gDataList.size();i++) {
										  Grid_data message = gDataList.get(i);
										  Grid_data newMessage = Grid_data.newBuilder().setMeasurement(message.getMeasurement()).setProvId(message.getProvId()+", " + sendTime2).setContext(context).build();

										  gDataList.set(i, newMessage);
									  }
								  }
				            for (int i=0; i < provIds.length; i++) {
	                            asyncCommands.hset(provIds[i], "metricID", gDataList.get(i).getMeasurement().getMetricId());
	                            asyncCommands.hset(provIds[i], "value", "" + gDataList.get(i).getMeasurement().getValue());
	                            asyncCommands.hset(provIds[i], "meterID", "" + gDataList.get(i).getMeasurement().getMeterId());
	                            asyncCommands.hset(provIds[i], "timestamp", "" + gDataList.get(i).getMeasurement().getTimestamp());
	                            if (gDataList.get(i).getProvId() != "") {
	                              asyncCommands.hset(provIds[i], "ProvID", gDataList.get(i).getProvId());
	                            }
				            }
				            if (no_prov==false) {
				            for(int i=0; i<provIds.length; i++) {
				            	Grid_data message = gDataList.get(i);
				            	Grid_data newMessage = Grid_data.newBuilder().setMeasurement(message.getMeasurement()).setProvId(provIds[i]).setContext(context).build();
				            	logger.info(""+newMessage);
				            	gDataList.set(i, newMessage);	
				            	sendCount++;
				            }}
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
			            // if no messages left just send response
			          } else {
			        	  	reply response = reply.newBuilder().setResponseCode(response_content).build();
				            responseObserver.onNext(response);
				        	logger.info("COMPLETED");
				           
				            responseObserver.onCompleted();
			          }
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

    @Parameter(names = {"-loc", "--location"}, description = "location of this component")
    public String location;

    @Parameter(names = {"-st", "--storagetime"}, description = "specify the storagetime of messages / aggregation (in seconds)")
    public Integer storageTime;

    @Parameter(names = {"-prop", "--propertiesfile"}, description = "path to propertiesfile")
    public String propertiesfile;

	@Parameter(names = {"-v", "--verbose"}, description = "prints incomming messages to stdout")
	public boolean verbose;

	@Parameter(names = {"-no", "--no_prov"}, description = "if set no provenance data will be generated")
	public boolean no_prov;
	
    @Parameter(names = "--help", description = "prints this help", help = true)
    public boolean help;

    @Override public String toString() {
      return "Args{" +
              "port=" + port +
              ", port_next=" + port_next +
              ", host_next='" + host_next + '\'' +
              ", location=" + location +
              ", storageTime=" + storageTime +
			  ", verbose=" + verbose +
              '}';
    }
}

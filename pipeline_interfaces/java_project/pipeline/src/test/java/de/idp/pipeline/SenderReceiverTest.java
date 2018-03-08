package de.idp.pipeline;

import de.idp.pipeline.util.SystemHelper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.provenance.ProvenanceContext;
import io.provenance.exception.ConfigParseException;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static de.idp.pipeline.PipelineComponent.newGridData;

public class SenderReceiverTest extends TestCase
{
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public SenderReceiverTest( String testName )
  {
    super( testName );
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite()
  {
    return new TestSuite( SenderReceiverTest.class );
  }

  /**
   * Rigourous Test :-)
 * @throws ConfigParseException 
   */
  public void test_1() throws Exception {
	
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    SystemHelper.setPropertiesFile();

    
    //Client test
	//ProvenanceContext pc =ProvenanceContext.getOrCreate();
		
	
	/*
    gatewayServer gateway_1 = new gatewayServer(50051, 50052, "localhost", "TEL1", 1);
    gateway_1.start();
    
    //gatewayServer gateway_2 = new gatewayServer(50052, 50053, "localhost", "TEL2", 1);
    //gateway_2.start();
    
    //gatewayServer gateway_3 = new gatewayServer(50053, 50054, "localhost", "TEL3", 1);
    //gateway_3.start();
    
    gatewayServer endpoint = new gatewayServer(50052, -1, "", "TEL4", 1);
    //endpoint.setVerbose(true);
    endpoint.start();

    
    gatewayClient client = new gatewayClient("localhost", 50051);
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    client.pushData(createDummyTestMessages(5));
   // client.pushData(createDummyTestMessages(1));
    
    
    client.shutdown();


    // TODO: better evaluation. so far we check if there happens an aggregation on last gateway --> data arrived ;)
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    //System.out.println(endpoint.aggregationStorage.getAggregations());
    //Assert.assertTrue(endpoint.aggregationStorage.getAggregations().size() > 0);

    gateway_1.stop();
    //gateway_2.stop();
    //gateway_3.stop();
    endpoint.stop();
    
    RedisClient dbClient;
	StatefulRedisConnection<String, String> dbConnection;
	dbClient = RedisClient.create("redis://localhost:6379");
	dbConnection = dbClient.connect();
	RedisCommands<String, String> commands;
	commands = dbConnection.sync();
    // test if local storage is filled --> if yes everything was ok
	List<String> local_keys = new ArrayList<>();
	local_keys = commands.keys("*");
	System.out.println(local_keys);
	//Assert.assertEquals(12, local_keys.size());*/
  }


  // test function to create testmessages to be sent by test client
  private static List<PipelineInterfaces.Grid_data> createDummyTestMessages (int numMessages) {
    List<PipelineInterfaces.Grid_data> gDataList = new ArrayList<PipelineInterfaces.Grid_data>() ;

    for (int i = 0; i< numMessages; i++) {
      gDataList.add(newGridData("meter"+i, "metric"+i, i, i+1));
    }
    return gDataList;
  }
}

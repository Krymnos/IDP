package de.idp.pipeline;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.provenance.ProvenanceContext;
import io.provenance.exception.ConfigParseException;

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
  public void test_1() throws IOException, InterruptedException, ConfigParseException {
    //Client test
	ProvenanceContext pc =ProvenanceContext.getOrCreate();
    gatewayServer gateway_1 = new gatewayServer(50051, 50052, "localhost", -1, -1);
    gateway_1.start();

    gatewayServer endpoint = new gatewayServer(50052, -1, null, 1, 1);
    endpoint.setVerbose(true);
    endpoint.start();

    gatewayClient client = new gatewayClient("localhost", 50051);
    client.pushData(createDummyTestMessages(1));
    client.shutdown();


    // TODO: better evaluation. so far we check if there happens an aggregation on last gateway --> data arrived ;)
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    System.out.println(endpoint.aggregationStorage.getAggregations());
    Assert.assertTrue(endpoint.aggregationStorage.getAggregations().size() > 0);

    gateway_1.stop();
    endpoint.stop();
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

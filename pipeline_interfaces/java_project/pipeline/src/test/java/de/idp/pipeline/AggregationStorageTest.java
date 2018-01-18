package de.idp.pipeline;

import de.idp.pipeline.storage.TimedAggregationStorage;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static de.idp.pipeline.PipelineComponent.newGridData;

/**
 * Unit test for simple App.
 */
public class AggregationStorageTest
        extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AggregationStorageTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AggregationStorageTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testStorage() throws InterruptedException {
        final long STORAGE_TIME_S = 3;
        final long AGGREGATION_INTERVAL_S = 1;

        final long TOLERANCE_MS = 100;

        final TimedAggregationStorage<PipelineInterfaces.measurement_message> storage =
                new TimedAggregationStorage<PipelineInterfaces.measurement_message>(AGGREGATION_INTERVAL_S,
                        STORAGE_TIME_S) {
                    @Override public DoubleSummaryStatistics aggregate(
                            List<PipelineInterfaces.measurement_message> items) {
                        return items.stream().mapToDouble(PipelineInterfaces.measurement_message::getValue)
                                .summaryStatistics();
                    }
                };

        List<PipelineInterfaces.Grid_data> messages = createDummyTestMessages(3);

        Assert.assertTrue(storage.getAggregations().size() == 0);
        storage.put(messages.get(0).getMeasurement());
        storage.put(messages.get(1).getMeasurement());
        storage.put(messages.get(2).getMeasurement());

        Thread.sleep(TimeUnit.SECONDS.toMillis(AGGREGATION_INTERVAL_S)+ TOLERANCE_MS);
        System.out.println(storage.getAggregations());
        Assert.assertTrue(storage.getAggregations().size() == 1);

        storage.put(messages.get(0).getMeasurement());
        storage.put(messages.get(1).getMeasurement());
        Thread.sleep(TimeUnit.SECONDS.toMillis(AGGREGATION_INTERVAL_S) + TOLERANCE_MS);

        System.out.println(storage.getAggregations());
        Assert.assertTrue(storage.getAggregations().size() == 2);

        // remaining time before the first aggregation entry is removed
        Thread.sleep(TimeUnit.SECONDS.toMillis(1) + TOLERANCE_MS);
        Assert.assertTrue(storage.getAggregations().size() == 1);
        System.out.println(storage.getAggregations());

        Thread.sleep(TimeUnit.SECONDS.toMillis(1) + TOLERANCE_MS);
        Assert.assertTrue(storage.getAggregations().size() == 0);
        System.out.println(storage.getAggregations());
    }


    private static List<PipelineInterfaces.Grid_data> createDummyTestMessages (int numMessages) {
        List<PipelineInterfaces.Grid_data> gDataList = new ArrayList<PipelineInterfaces.Grid_data>() ;

        for (int i = 0; i< numMessages; i++) {
            gDataList.add(newGridData("meter"+i, "metric"+i, i, i+1));
        }
        return gDataList;
    }
}




package de.idp.pipeline;

import de.idp.pipeline.storage.TimedStorage;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple App.
 */
public class TimedStorageTest
        extends TestCase
{
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public TimedStorageTest( String testName )
  {
    super( testName );
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite()
  {
    return new TestSuite( TimedStorageTest.class );
  }

  /**
   * Rigourous Test :-)
   */
  public void testStorage() throws InterruptedException {
    final long STORAGE_TIME_MS = 300;
    final long TOLERANCE_MS = 2;
    final TimedStorage<String> storage = new TimedStorage<>(STORAGE_TIME_MS, TimeUnit.MILLISECONDS);

    final String[] items = new String[]{"item_1", "item_2","item_3"};
    final Long[] insertTime = new Long[3];


    Thread consumerThread = new Thread(new Runnable() {
      @Override public void run() {
        int curr = 0;
        try {
          while(curr < items.length){
            String item = storage.take();
            long time = System.currentTimeMillis();

            assertTrue(items[curr].equals(item));
            assertTrue(String.format("Error, storagetime is too high (inserted: %d, removed: %d, "
                            + "expected: %d",insertTime[curr], time, insertTime[curr]+STORAGE_TIME_MS),
                    insertTime[curr]+STORAGE_TIME_MS+TOLERANCE_MS >= time);

            assertTrue(String.format("Error, storagetime is too low  (inserted: %d, removed: %d, "
                            + "expected: %d",insertTime[curr], time, insertTime[curr]+STORAGE_TIME_MS),
                    insertTime[curr]+STORAGE_TIME_MS-TOLERANCE_MS  <= time);
            System.out.println("OK");
            curr++;
          }

        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    consumerThread.start();

    for(int i = 0 ; i < items.length; i++){
      insertTime[i] = System.currentTimeMillis();
      storage.add(items[i]);
      Thread.sleep(150);
    }

    consumerThread.join();
  }
}
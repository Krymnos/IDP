package de.idp.pipeline.storage;

import java.util.concurrent.DelayQueue;

public class TimedStorage<T> implements Runnable{
  private DelayQueue<TimedStorageItem> storage;
  private long storageTime_ms;

  public TimedStorage(Long storageTime_ms) {
    this.storage = new DelayQueue<>();
    this.storageTime_ms = storageTime_ms;
  }

  public void setStorageTime(long storageTime_ms) {
    this.storageTime_ms = storageTime_ms;
  }

  public void add(T data){
    this.storage.add(new TimedStorageItem<T>(data, storageTime_ms));
  }

  @Override public void run() {
    while(true){
      try {
        TimedStorageItem item = storage.take();

        /**
         * TODO: add grpc call to the next node or call an ActionListener
         */

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}

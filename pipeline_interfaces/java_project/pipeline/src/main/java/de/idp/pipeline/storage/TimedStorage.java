package de.idp.pipeline.storage;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

public class TimedStorage<T>{
  private DelayQueue<TimedStorageItem> storage;
  private long storageTime_ms;

  public TimedStorage(long storageTime, TimeUnit unit) {
    this.storage = new DelayQueue<>();
    setStorageTime(storageTime, unit);
  }

  public void setStorageTime(long storageTime, TimeUnit unit) {
    this.storageTime_ms = unit.toMillis(storageTime);
  }

  public T take() throws InterruptedException {
    return (T) storage.take().getData();
  }

  public void add(T data){
    this.storage.add(new TimedStorageItem<T>(data, storageTime_ms));
  }
}

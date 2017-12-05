package de.idp.pipeline.storage;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

class TimedStorageItem<T> implements Delayed {
  private final T data;
  private final long delay;
  private final long creationTime;

  public TimedStorageItem(T data, long delayInMilliseconds) {
    this.data = data;
    this.creationTime = System.currentTimeMillis();
    this.delay = delayInMilliseconds;
  }

  @Override public long getDelay(TimeUnit unit) {
    return unit.convert(System.currentTimeMillis() - creationTime - delay, TimeUnit.MILLISECONDS);
  }

  @Override public int compareTo(Delayed o) {
    return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
  }

  public T getData() {
    return data;
  }

  public long getDelay() {
    return delay;
  }

  public long getCreationTime() {
    return creationTime;
  }
}

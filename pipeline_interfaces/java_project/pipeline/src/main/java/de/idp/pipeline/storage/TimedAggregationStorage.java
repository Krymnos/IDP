package de.idp.pipeline.storage;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class TimedAggregationStorage<T>{
    private long aggregationInterval;
    List<T> items;
    TimedStorage<DoubleSummaryStatistics> aggregations;

    private Thread aggregationThread;
    private Thread storageCleanerThread;


    public TimedAggregationStorage(long aggregationInterval, long storageTime_s) {
        this.aggregationInterval = aggregationInterval;
        this.aggregations = new TimedStorage<>(storageTime_s, TimeUnit.SECONDS);
        items = new ArrayList<>();
        start();
    }

    private void start(){
        this.aggregationThread = new Thread(() -> {
            while(true){
                synchronized (items) {
                    if(items.size() > 0) {
                        aggregations.add(aggregate(items));
                        items.clear();
                    }
                }

                try {
                    Thread.sleep(aggregationInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        aggregationThread.start();

        this.storageCleanerThread = new Thread(() -> {
           while (true){
               try {
                   //blocking
                   aggregations.take();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
        });

        storageCleanerThread.start();
    }

    public void put(T item){
        items.add(item);
    }

    public abstract DoubleSummaryStatistics aggregate(List<T> items);

    public List<DoubleSummaryStatistics> getAggregations(){
        return aggregations.getStorage().stream().map(timedStorageItem -> (DoubleSummaryStatistics) timedStorageItem.getData()).collect(
                Collectors.toList());
    }
}

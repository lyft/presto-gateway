package com.lyft.data.gateway.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class QueryHistoryManagerImpl implements QueryHistoryManager {

  private int size = 2000; // TODO: take this input from config.
  private QueryDetail[] queryHistory = new QueryDetail[size];
  private AtomicLong queryCounter = new AtomicLong();

  @Override
  public void submitQueryDetail(QueryDetail queryDetail) {
    int pos = (int) queryCounter.incrementAndGet() % size;
    queryHistory[pos] = queryDetail;
  }

  @Override
  public List<QueryDetail> fetchQueryHistory() {
    List<QueryDetail> history = new ArrayList<>();
    for (QueryDetail q : queryHistory) {
      if (q != null) {
        history.add(q);
      }
    }
    Collections.sort(history);
    return history;
  }
}

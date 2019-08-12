package com.lyft.data.gateway.router.impl;

import com.lyft.data.gateway.router.QueryHistoryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class QueryHistoryManagerImpl implements QueryHistoryManager {

  private final int size;
  private final QueryDetail[] queryHistory;
  private final AtomicLong queryCounter = new AtomicLong();

  public QueryHistoryManagerImpl(int size) {
    this.size = size;
    this.queryHistory = new QueryDetail[size];
  }

  @Override
  public void submitQueryDetail(QueryDetail queryDetail) {
    String queryText = queryDetail.getQueryText();
    if (queryText.startsWith("EXPLAIN (TYPE VALIDATE) ")) {
      return;
    }
    int pos = (int) queryCounter.incrementAndGet() % size;
    queryHistory[pos] = queryDetail;
    if (queryCounter.get() >= Long.MAX_VALUE - 1) {
      queryCounter.set(0);
    }
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

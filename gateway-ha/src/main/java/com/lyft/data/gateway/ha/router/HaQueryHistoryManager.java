package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.QueryHistory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaQueryHistoryManager implements QueryHistoryManager {
  private JdbcConnectionManager connectionManager;

  public HaQueryHistoryManager(JdbcConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public void submitQueryDetail(QueryDetail queryDetail) {
    try {
      connectionManager.open();
      QueryHistory dao = new QueryHistory();
      QueryHistory.create(dao, queryDetail);
    } finally {
      connectionManager.close();
    }
  }

  @Override
  public List<QueryDetail> fetchQueryHistory() {
    try {
      connectionManager.open();
      return QueryHistory.upcast(QueryHistory.findAll().limit(2000).orderBy("created desc"));
    } finally {
      connectionManager.close();
    }
  }

  public String getBackendForQueryId(String queryId) {
    String backend = null;
    try {
      connectionManager.open();
      QueryHistory queryHistory = QueryHistory.findById(queryId);
      if (queryHistory != null) {
        backend = queryHistory.get("backend_url").toString();
      }
    } finally {
      connectionManager.close();
    }
    return backend;
  }
}

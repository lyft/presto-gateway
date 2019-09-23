package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.QueryHistoryManager.QueryDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@IdName("query_id")
@Table("query_history")
@Cached
public class QueryHistory extends Model {
  private static final String queryId = "query_id";
  private static final String queryText = "query_text";
  private static final String backendUrl = "backend_url";
  private static final String userName = "user_name";
  private static final String source = "source";
  private static final String created = "created";

  public static List<QueryDetail> upcast(List<QueryHistory> queryHistoryList) {
    List<QueryDetail> queryDetails = new ArrayList<>();
    for (QueryHistory dao : queryHistoryList) {
      QueryDetail queryDetail = new QueryDetail();
      queryDetail.setQueryId(dao.getString(queryId));
      queryDetail.setQueryText(dao.getString(queryText));
      queryDetail.setCaptureTime(dao.getLong(created));
      queryDetail.setBackendUrl(dao.getString(backendUrl));
      queryDetail.setUser(dao.getString(userName));
      queryDetail.setSource(dao.getString(source));
      queryDetails.add(queryDetail);
    }
    return queryDetails;
  }

  public static void create(QueryHistory model, QueryDetail queryDetail) {
    model.set(queryId, queryDetail.getQueryId());
    model.set(queryText, queryDetail.getQueryText());
    model.set(backendUrl, queryDetail.getBackendUrl());
    model.set(userName, queryDetail.getUser());
    model.set(source, queryDetail.getSource());
    model.set(created, queryDetail.getCaptureTime());
    model.insert();
  }
}

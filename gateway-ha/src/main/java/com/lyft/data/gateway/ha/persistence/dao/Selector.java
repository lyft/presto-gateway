package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.PrestoResourceManager.SelectorDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@BelongsTo(parent = ResourceGroup.class, foreignKeyName = "resource_group_id")
@IdName("resource_group_id")
@Table("selectors")
@Cached
public class Selector extends Model {
  private static final String resourceGroupId = "resource_group_id";
  private static final String priority = "priority";

  private static final String userRegex = "user_regex";
  private static final String sourceRegex = "source_regex";

  private static final String queryType = "query_type";
  private static final String clientTags = "client_tags";
  private static final String selectorResourceEstimate = "selector_resource_estimate";

  public static List<SelectorDetail> upcast(List<Selector> selectorList) {
    List<SelectorDetail> selectorDetails = new ArrayList<>();
    for (Selector dao : selectorList) {
      SelectorDetail selectorDetail = new SelectorDetail();
      selectorDetail.setResourceGroupId(dao.getLong(resourceGroupId));
      selectorDetail.setPriority(dao.getLong(priority));
      selectorDetail.setUserRegex(dao.getString(userRegex));
      selectorDetail.setSourceRegex(dao.getString(sourceRegex));
      selectorDetail.setQueryType(dao.getString(queryType));
      selectorDetail.setClientTags(dao.getString(clientTags));
      selectorDetail.setSelectorResourceEstimate(dao.getString(selectorResourceEstimate));
      selectorDetails.add(selectorDetail);
    }
    return selectorDetails;
  }

  public static void create(Selector model, SelectorDetail selectorDetail) {
    model.set(resourceGroupId, selectorDetail.getResourceGroupId());
    model.set(priority, selectorDetail.getPriority());
    model.set(userRegex, selectorDetail.getUserRegex());
    model.set(sourceRegex, selectorDetail.getSourceRegex());
    model.set(queryType, selectorDetail.getQueryType());
    model.set(clientTags, selectorDetail.getClientTags());
    model.set(selectorResourceEstimate, selectorDetail.getSelectorResourceEstimate());

    model.insert();
  }

  public static void update(Selector model, SelectorDetail selectorDetail) {
    model.set(resourceGroupId, selectorDetail.getResourceGroupId());
    model.set(priority, selectorDetail.getPriority());
    model.set(userRegex, selectorDetail.getUserRegex());
    model.set(sourceRegex, selectorDetail.getSourceRegex());
    model.set(queryType, selectorDetail.getQueryType());
    model.set(clientTags, selectorDetail.getClientTags());
    model.set(selectorResourceEstimate, selectorDetail.getSelectorResourceEstimate());

    model.saveIt();
  }
}

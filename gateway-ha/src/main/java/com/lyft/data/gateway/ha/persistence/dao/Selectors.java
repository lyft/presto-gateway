package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.SelectorsDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@BelongsTo(parent = ResourceGroups.class, foreignKeyName = "resource_group_id")
@IdName("resource_group_id")
@Table("selectors") // located in gateway-ha-persistence.sql
@Cached
public class Selectors extends Model {
  private static final String resourceGroupId = "resource_group_id";
  private static final String priority = "priority";

  private static final String userRegex = "user_regex";
  private static final String sourceRegex = "source_regex";

  private static final String queryType = "query_type";
  private static final String clientTags = "client_tags";
  private static final String selectorResourceEstimate = "selector_resource_estimate";

  /**
   * Retrieves all existing selectors and returns them in a List.
   *
   * @param selectorList
   * @return a list of all existing selectors
   */
  public static List<SelectorsDetail> upcast(List<Selectors> selectorList) {
    List<SelectorsDetail> selectorDetails = new ArrayList<>();
    for (Selectors dao : selectorList) {
      SelectorsDetail selectorDetail = new SelectorsDetail();
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

  /**
   * Create a new Selector model with the given selector details.
   *
   * @param model
   * @param selectorDetail
   */
  public static void create(Selectors model, SelectorsDetail selectorDetail) {
    model.set(resourceGroupId, selectorDetail.getResourceGroupId());
    model.set(priority, selectorDetail.getPriority());
    model.set(userRegex, selectorDetail.getUserRegex());
    model.set(sourceRegex, selectorDetail.getSourceRegex());
    model.set(queryType, selectorDetail.getQueryType());
    model.set(clientTags, selectorDetail.getClientTags());
    model.set(selectorResourceEstimate, selectorDetail.getSelectorResourceEstimate());

    model.insert();
  }

  /**
   * Update an existing Selector model with the given selector details.
   *
   * @param model
   * @param selectorDetail
   */
  public static void update(Selectors model, SelectorsDetail selectorDetail) {
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

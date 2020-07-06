package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.ExactSelectorsDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@CompositePK({"environment", "source", "query_type"})
@Table("exact_match_source_selectors") // located in gateway-ha-persistence.sql
@Cached
public class ExactMatchSourceSelectors extends Model {
  private static final String resourceGroupId = "resource_group_id";
  private static final String updateTime = "update_time";

  private static final String source = "source";
  private static final String environment = "environment";
  private static final String queryType = "query_type";

  /**
   * Returns the most specific exact-match selector for a given environment, source and query type.
   * NULL values in the environment and query type fields signify wildcards.
   *
   * @param exactMatchSourceSelectorsList
   * @return List of ExactMatchSourceSelectors
   */
  public static List<ExactSelectorsDetail> upcast(
      List<ExactMatchSourceSelectors> exactMatchSourceSelectorsList) {
    List<ExactSelectorsDetail> exactSelectors = new ArrayList<>();
    for (ExactMatchSourceSelectors dao : exactMatchSourceSelectorsList) {
      ExactSelectorsDetail exactSelectorDetail = new ExactSelectorsDetail();
      exactSelectorDetail.setResourceGroupId(dao.getString(resourceGroupId));
      exactSelectorDetail.setUpdateTime(dao.getString(updateTime));

      exactSelectorDetail.setSource(dao.getString(source));
      exactSelectorDetail.setEnvironment(dao.getString(environment));
      exactSelectorDetail.setQueryType(dao.getString(queryType));

      exactSelectors.add(exactSelectorDetail);
    }
    return exactSelectors;
  }

  /**
   * Create a new exactMatchSourceSelector.
   *
   * @param model
   * @param exactSelectorsDetail
   */
  public static void create(
      ExactMatchSourceSelectors model, ExactSelectorsDetail exactSelectorsDetail) {
    model.set(resourceGroupId, exactSelectorsDetail.getResourceGroupId());
    model.set(updateTime, exactSelectorsDetail.getUpdateTime());

    model.set(source, exactSelectorsDetail.getSource());
    model.set(environment, exactSelectorsDetail.getEnvironment());
    model.set(queryType, exactSelectorsDetail.getQueryType());

    model.insert();
  }

  /**
   * Update existing exactMatchSourceSelector.
   *
   * @param model
   * @param exactSelectorsDetail
   */
  public static void update(
      ExactMatchSourceSelectors model, ExactSelectorsDetail exactSelectorsDetail) {
    model.set(resourceGroupId, exactSelectorsDetail.getResourceGroupId());
    model.set(updateTime, exactSelectorsDetail.getUpdateTime());

    model.set(source, exactSelectorsDetail.getSource());
    model.set(environment, exactSelectorsDetail.getEnvironment());
    model.set(queryType, exactSelectorsDetail.getQueryType());

    model.saveIt();
  }
}

package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.ExactSelectorsDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@CompositePK({"environment", "source", "query_type"})
@Table("exact_match_source_selectors")
@Cached
public class ExactMatchSourceSelectors extends Model {
  private static final String resourceGroupId = "resource_group_id";
  private static final String updateTime = "update_time";

  private static final String source = "source";
  private static final String environment = "environment";
  private static final String queryType = "query_type";

  // PRIMARY KEY (environment, source, query_type),
  // UNIQUE (source, environment, query_type, resource_group_id)

  public static List<ExactSelectorsDetail> upcast(
      List<ExactMatchSourceSelectors> exactMatchSourceSelectorList) {
    List<ExactSelectorsDetail> exactSelectors = new ArrayList<>();
    for (ExactMatchSourceSelectors dao : exactMatchSourceSelectorList) {
      ExactSelectorsDetail exactSelectorDetail = new ExactSelectorsDetail();
      exactSelectorDetail.setResourceGroupId(dao.getString(resourceGroupId));
      exactSelectorDetail.setUpdateTime(dao.getString(updateTime)); // TODO: change to datetime

      exactSelectorDetail.setSource(dao.getString(source));
      exactSelectorDetail.setEnvironment(dao.getString(environment));
      exactSelectorDetail.setQueryType(dao.getString(queryType));

      exactSelectors.add(exactSelectorDetail);
    }
    return exactSelectors;
  }

  public static void create(
          ExactMatchSourceSelectors model, ExactSelectorsDetail exactSelectorDetail) {
    model.set(resourceGroupId, exactSelectorDetail.getResourceGroupId());
    model.set(updateTime, exactSelectorDetail.getUpdateTime());

    model.set(source, exactSelectorDetail.getSource());
    model.set(environment, exactSelectorDetail.getEnvironment());
    model.set(queryType, exactSelectorDetail.getQueryType());

    model.insert();
  }

  public static void update(
          ExactMatchSourceSelectors model, ExactSelectorsDetail exactSelectorDetail) {
    model.set(resourceGroupId, exactSelectorDetail.getResourceGroupId());
    model.set(updateTime, exactSelectorDetail.getUpdateTime());

    model.set(source, exactSelectorDetail.getSource());
    model.set(environment, exactSelectorDetail.getEnvironment());
    model.set(queryType, exactSelectorDetail.getQueryType());

    model.saveIt();
  }
}

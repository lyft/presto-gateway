package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.GlobalPropertiesDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@IdName("name")
@Table("resource_groups_global_properties")
@Cached
public class ResourceGroupsGlobalProperties extends Model {
  private static final String name = "name";
  private static final String value = "value";

  // CHECK (name in ('cpu_quota_period'))

  public static List<GlobalPropertiesDetail> upcast(
      List<ResourceGroupsGlobalProperties> globalPropertyList) {
    List<GlobalPropertiesDetail> globalProperties = new ArrayList<>();
    for (ResourceGroupsGlobalProperties dao : globalPropertyList) {
      GlobalPropertiesDetail globalPropertyDetail = new GlobalPropertiesDetail();
      globalPropertyDetail.setName(dao.getString(name));
      globalPropertyDetail.setValue(dao.getString(value));

      globalProperties.add(globalPropertyDetail);
    }
    return globalProperties;
  }

  public static void create(
          ResourceGroupsGlobalProperties model, GlobalPropertiesDetail globalPropertyDetail) {
    model.set(name, globalPropertyDetail.getName());
    model.set(value, globalPropertyDetail.getValue());

    model.insert();
  }

  public static void update(
          ResourceGroupsGlobalProperties model, GlobalPropertiesDetail globalPropertyDetail) {
    model.set(name, globalPropertyDetail.getName());
    model.set(value, globalPropertyDetail.getValue());

    model.saveIt();
  }
}

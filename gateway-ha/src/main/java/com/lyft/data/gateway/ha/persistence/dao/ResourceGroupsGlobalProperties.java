package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.GlobalPropertiesDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@IdName("name")
@Table("resource_groups_global_properties") // located in gateway-ha-persistence.sql
@Cached
public class ResourceGroupsGlobalProperties extends Model {
  private static final String name = "name";
  private static final String value = "value";

  /**
   * Reads all existing global properties and returns them in a List.
   *
   * @param globalPropertiesList
   * @return List of ResourceGroupGlobalProperties
   */
  public static List<GlobalPropertiesDetail> upcast(
      List<ResourceGroupsGlobalProperties> globalPropertiesList) {
    List<GlobalPropertiesDetail> globalProperties = new ArrayList<>();
    for (ResourceGroupsGlobalProperties dao : globalPropertiesList) {
      GlobalPropertiesDetail globalPropertiesDetail = new GlobalPropertiesDetail();
      globalPropertiesDetail.setName(dao.getString(name));
      globalPropertiesDetail.setValue(dao.getString(value));

      globalProperties.add(globalPropertiesDetail);
    }
    return globalProperties;
  }

  /**
   * Creates a new global property.
   *
   * @param model
   * @param globalPropertiesDetail
   */
  public static void create(
      ResourceGroupsGlobalProperties model, GlobalPropertiesDetail globalPropertiesDetail) {
    model.set(name, globalPropertiesDetail.getName());
    model.set(value, globalPropertiesDetail.getValue());

    model.insert();
  }

  /**
   * Updates existing global property.
   *
   * @param model
   * @param globalPropertiesDetail
   */
  public static void update(
      ResourceGroupsGlobalProperties model, GlobalPropertiesDetail globalPropertiesDetail) {
    model.set(name, globalPropertiesDetail.getName());
    model.set(value, globalPropertiesDetail.getValue());

    model.saveIt();
  }
}

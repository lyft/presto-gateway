package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.ExactMatchSourceSelectors;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroups;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroupsGlobalProperties;
import com.lyft.data.gateway.ha.persistence.dao.Selectors;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaResourceGroupsManager implements ResourceGroupsManager {
  private JdbcConnectionManager connectionManager;

  public HaResourceGroupsManager(JdbcConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  /**
   * Creates and returns a resource group with the given parameters.
   *
   * @param resourceGroup
   * @param routingGroupDatabase
   * @return the created ResourceGroupDetail object
   */
  @Override
  public ResourceGroupsDetail createResourceGroup(ResourceGroupsDetail resourceGroup,
                                                  @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      ResourceGroups.create(new ResourceGroups(), resourceGroup);
    } finally {
      connectionManager.close();
    }
    return resourceGroup;
  }

  /**
   * Retrieves a list of all existing resource groups for a specified database.
   * @param routingGroupDatabase
   * @return all existing resource groups as a list of ResourceGroupDetail objects
   */
  @Override
  public List<ResourceGroupsDetail> readAllResourceGroups(@Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      List<ResourceGroups> resourceGroupList = ResourceGroups.findAll();
      return ResourceGroups.upcast(resourceGroupList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Retrieves a specific resource group based on its resourceGroupId for a specific database.
   * @param resourceGroupId
   * @param routingGroupDatabase
   * @return a specific resource group as a ResourceGroupDetail object
   */
  @Override
  public List<ResourceGroupsDetail> readResourceGroup(long resourceGroupId,
                                                      @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      List<ResourceGroups> resourceGroup =
              ResourceGroups.where("resource_group_id = ?", resourceGroupId);
      return ResourceGroups.upcast(resourceGroup);
    } finally {
      connectionManager.close();
    }
  }


  /**
   * Updates an existing resource group with new values.
   *
   * @param resourceGroup
   * @param routingGroupDatabase
   * @return the updated ResourceGroupDetail object
   */
  @Override
  public ResourceGroupsDetail updateResourceGroup(ResourceGroupsDetail resourceGroup,
                                                  @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      ResourceGroups model =
          ResourceGroups.findFirst("resource_group_id = ?",
                  resourceGroup.getResourceGroupId());

      if (model == null) {
        ResourceGroups.create(new ResourceGroups(), resourceGroup);
      } else {
        ResourceGroups.update(model, resourceGroup);
      }
    } finally {
      connectionManager.close();
    }
    return resourceGroup;
  }

  /**
   * Search for resource group by its resourceGroupId and delete it.
   *
   * @param resourceGroupId
   * @param routingGroupDatabase
   */
  @Override
  public void deleteResourceGroup(long resourceGroupId, @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      ResourceGroups.delete("resource_group_id = ?", resourceGroupId);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Creates and returns a selector with the given parameters.
   *
   * @param selector
   * @param routingGroupDatabase
   * @return selector
   */
  @Override
  public SelectorsDetail createSelector(SelectorsDetail selector,
                                        @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      Selectors.create(new Selectors(), selector);
    } finally {
      connectionManager.close();
    }
    return selector;
  }

  /**
   * Retrieves a list of all existing resource groups.
   * @param routingGroupDatabase
   * @return all existing selectors as a list of SelectorDetail objects
   */
  @Override
  public List<SelectorsDetail> readAllSelectors(@Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      List<Selectors> selectorList = Selectors.findAll();
      return Selectors.upcast(selectorList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Retrieves the selector.
   * @param resourceGroupId
   * @param routingGroupDatabase
   * @return the selectors
   */
  @Override
  public List<SelectorsDetail> readSelector(long resourceGroupId,
                                            @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      List<Selectors> selectorList = Selectors.where("resource_group_id = ?",
              resourceGroupId);
      return Selectors.upcast(selectorList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Updates a selector given the specified selector and its updated version.
   *
   * @param selector
   * @param updatedSelector
   * @param routingGroupDatabase
   * @return updated selector
   */
  @Override
  public SelectorsDetail updateSelector(SelectorsDetail selector, SelectorsDetail updatedSelector,
                                        @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      String query =
          String.format(
              "resource_group_id %s and priority %s "
                  + "and user_regex %s and source_regex %s "
                  + "and query_type %s and client_tags %s "
                  + "and selector_resource_estimate %s",
              getMatchingString(selector.getResourceGroupId()),
              getMatchingString(selector.getPriority()),
              getMatchingString(selector.getUserRegex()),
              getMatchingString(selector.getSourceRegex()),
              getMatchingString(selector.getQueryType()),
              getMatchingString(selector.getClientTags()),
              getMatchingString(selector.getSelectorResourceEstimate()));
      Selectors model = Selectors.findFirst(query);

      if (model == null) {
        Selectors.create(new Selectors(), updatedSelector);
      } else {
        Selectors.update(model, updatedSelector);
      }
    } finally {
      connectionManager.close();
    }
    return updatedSelector;
  }

  /**
   * Search for selector by its exact properties and delete it.
   * @param routingGroupDatabase
   * @param selector
   */
  @Override
  public void deleteSelector(SelectorsDetail selector, @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      String query =
          String.format(
              "resource_group_id %s and priority %s "
                  + "and user_regex %s and source_regex %s "
                  + "and query_type %s and client_tags %s "
                  + "and selector_resource_estimate %s",
              getMatchingString(selector.getResourceGroupId()),
              getMatchingString(selector.getPriority()),
              getMatchingString(selector.getUserRegex()),
              getMatchingString(selector.getSourceRegex()),
              getMatchingString(selector.getQueryType()),
              getMatchingString(selector.getClientTags()),
              getMatchingString(selector.getSelectorResourceEstimate()));
      Selectors.delete(query);

    } finally {
      connectionManager.close();
    }
  }

  /**
   * Create new global property with given parameters.
   *
   * @param globalPropertyDetail
   * @param routingGroupDatabase
   * @return created global property
   */
  @Override
  public GlobalPropertiesDetail createGlobalProperty(GlobalPropertiesDetail globalPropertyDetail,
                                                     @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      ResourceGroupsGlobalProperties.create(
          new ResourceGroupsGlobalProperties(), globalPropertyDetail);
    } finally {
      connectionManager.close();
    }
    return globalPropertyDetail;
  }

  /**
   * Read all existing global properties.
   * param routingGroupDatabase
   * @return a list of global properties
   */
  @Override
  public List<GlobalPropertiesDetail> readAllGlobalProperties(
          @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      List<ResourceGroupsGlobalProperties> globalPropertyList =
          ResourceGroupsGlobalProperties.findAll();
      return ResourceGroupsGlobalProperties.upcast(globalPropertyList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Read specific global property based on the given name.
   *
   * @param name
   * @param routingGroupDatabase
   * @return corresponding global property
   */
  @Override
  public List<GlobalPropertiesDetail> readGlobalProperty(String name,
                                                         @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      List<ResourceGroupsGlobalProperties> globalPropertyList =
          ResourceGroupsGlobalProperties.where("name = ?", name);
      return ResourceGroupsGlobalProperties.upcast(globalPropertyList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Updates a global property based on the given name.
   *
   * @param globalProperty
   * @param routingGroupDatabase
   * @return the updated global property
   */
  @Override
  public GlobalPropertiesDetail updateGlobalProperty(GlobalPropertiesDetail globalProperty,
                                                     @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      ResourceGroupsGlobalProperties model =
          ResourceGroupsGlobalProperties.findFirst("name = ?", globalProperty.getName());

      if (model == null) {
        ResourceGroupsGlobalProperties.create(new ResourceGroupsGlobalProperties(), globalProperty);
      } else {
        ResourceGroupsGlobalProperties.update(model, globalProperty);
      }
    } finally {
      connectionManager.close();
    }
    return globalProperty;
  }

  /**
   * Deletes a global property from the table based on its name.
   * @param routingGroupDatabase
   * @param name
   */
  @Override
  public void deleteGlobalProperty(String name, @Nullable String routingGroupDatabase) {
    try {
      connectionManager.open(routingGroupDatabase);
      ResourceGroupsGlobalProperties.delete("name = ?", name);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Creates exact match source selector for db.
   * @param exactSelectorDetail
   * @return
   */
  @Override
  public ExactSelectorsDetail createExactMatchSourceSelector(
      ExactSelectorsDetail exactSelectorDetail) {
    try {
      connectionManager.open();
      ExactMatchSourceSelectors.create(new ExactMatchSourceSelectors(), exactSelectorDetail);
    } finally {
      connectionManager.close();
    }
    return exactSelectorDetail;
  }

  /**
   * Reads exact match source selector from db.
   * @return
   */
  @Override
  public List<ExactSelectorsDetail> readExactMatchSourceSelector() {
    try {
      connectionManager.open();
      List<ExactMatchSourceSelectors> exactMatchSourceSelectorList =
          ExactMatchSourceSelectors.findAll();
      return ExactMatchSourceSelectors.upcast(exactMatchSourceSelectorList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Gets exact match source selector from db.
   * @param exactSelectorDetail
   * @return
   */
  @Override
  public ExactSelectorsDetail getExactMatchSourceSelector(
      ExactSelectorsDetail exactSelectorDetail) {
    try {
      connectionManager.open();
      ExactMatchSourceSelectors model =
          ExactMatchSourceSelectors.findFirst(
              "resource_group_id = ? and update_time = ? "
                  + "and source = ? and environment = ? and query_type = ?",
              exactSelectorDetail.getResourceGroupId(),
              exactSelectorDetail.getUpdateTime(),
              exactSelectorDetail.getSource(),
              exactSelectorDetail.getEnvironment(),
              exactSelectorDetail.getQueryType());

      List<ExactMatchSourceSelectors> exactMatchSourceSelectorList = new ArrayList();
      exactMatchSourceSelectorList.add(model);

      if (model == null) {
        return null;
      } else {
        ExactMatchSourceSelectors.upcast(exactMatchSourceSelectorList);
      }
    } finally {
      connectionManager.close();
    }
    return exactSelectorDetail;
  }

  public String getMatchingString(Object detail) {
    if (detail == null) {
      return "IS NULL";
    } else if (detail.getClass().equals(String.class)) {
      return "= '" + detail + "'";
    }
    return "= " + detail;
  }
}

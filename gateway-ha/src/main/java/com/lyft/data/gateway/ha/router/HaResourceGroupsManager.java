package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.ExactMatchSourceSelectors;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroups;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroupsGlobalProperties;
import com.lyft.data.gateway.ha.persistence.dao.Selectors;

import java.util.ArrayList;
import java.util.List;

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
   * @return the created ResourceGroupDetail object
   */
  @Override
  public ResourceGroupsDetail createResourceGroup(ResourceGroupsDetail resourceGroup) {
    try {
      connectionManager.open();
      ResourceGroups.create(new ResourceGroups(), resourceGroup);
    } finally {
      connectionManager.close();
    }
    return resourceGroup;
  }

  /**
   * Retrieves a list of all existing resource groups.
   *
   * @return all existing resource groups as a list of ResourceGroupDetail objects
   */
  @Override
  public List<ResourceGroupsDetail> readAllResourceGroups() {
    try {
      connectionManager.open();
      List<ResourceGroups> resourceGroupList = ResourceGroups.findAll();
      return ResourceGroups.upcast(resourceGroupList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Retrieves a specific resource group based on its resourceGroupId.
   *
   * @param resourceGroupId
   * @return a specific resource group as a ResourceGroupDetail object
   */
  @Override
  public List<ResourceGroupsDetail> readResourceGroup(long resourceGroupId) {
    try {
      connectionManager.open();
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
   * @return the updated ResourceGroupDetail object
   */
  @Override
  public ResourceGroupsDetail updateResourceGroup(ResourceGroupsDetail resourceGroup) {
    try {
      connectionManager.open();
      ResourceGroups model =
          ResourceGroups.findFirst("resource_group_id = ?", resourceGroup.getResourceGroupId());

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
   */
  @Override
  public void deleteResourceGroup(long resourceGroupId) {
    try {
      connectionManager.open();
      ResourceGroups.delete("resource_group_id = ?", resourceGroupId);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Creates and returns a selector with the given parameters.
   *
   * @param selector
   * @return
   */
  @Override
  public SelectorsDetail createSelector(SelectorsDetail selector) {
    try {
      connectionManager.open();
      Selectors.create(new Selectors(), selector);
    } finally {
      connectionManager.close();
    }
    return selector;
  }

  /**
   * Retrieves a list of all existing resource groups.
   *
   * @return all existing selectors as a list of SelectorDetail objects
   */
  @Override
  public List<SelectorsDetail> readAllSelectors() {
    try {
      connectionManager.open();
      List<Selectors> selectorList = Selectors.findAll();
      return Selectors.upcast(selectorList);
    } finally {
      connectionManager.close();
    }
  }

  @Override
  public List<SelectorsDetail> readSelector(long resourceGroupId) {
    try {
      connectionManager.open();
      List<Selectors> selectorList = Selectors.where("resource_group_id = ?", resourceGroupId);
      return Selectors.upcast(selectorList);
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Updates an existing resource group with new values.
   *
   * @param selector
   * @return
   */
  @Override
  public SelectorsDetail updateSelector(SelectorsDetail selector, SelectorsDetail updatedSelector) {
    try {
      connectionManager.open();
      Selectors model =
          Selectors.findFirst(
              "resource_group_id = ? and priority = ? "
                  + "and user_regex = ? and source_regex = ? "
                  + "and query_type = ? and client_tags = ? "
                  + "and selector_resource_estimate = ?",
              selector.getResourceGroupId(),
              selector.getPriority(),
              selector.getUserRegex(),
              selector.getSourceRegex(),
              selector.getQueryType(),
              selector.getClientTags(),
              selector.getSelectorResourceEstimate());
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
   * Search for selector by its resourceGroupId and delete it.
   *
   * @param selector
   */
  @Override
  public void deleteSelector(SelectorsDetail selector) {
    try {
      connectionManager.open();
      Selectors.delete(
          "resource_group_id = ? and priority = ? "
              + "and user_regex = ? and source_regex = ? "
              + "and query_type = ? and client_tags = ? "
              + "and selector_resource_estimate = ?",
          selector.getResourceGroupId(),
          selector.getPriority(),
          selector.getUserRegex(),
          selector.getSourceRegex(),
          selector.getQueryType(),
          selector.getClientTags(),
          selector.getSelectorResourceEstimate());
    } finally {
      connectionManager.close();
    }
  }

  /**
   * Create new global property with given parameters.
   *
   * @param globalPropertyDetail
   * @return created global property
   */
  @Override
  public GlobalPropertiesDetail createGlobalProperty(GlobalPropertiesDetail globalPropertyDetail) {
    try {
      connectionManager.open();
      ResourceGroupsGlobalProperties.create(
          new ResourceGroupsGlobalProperties(), globalPropertyDetail);
    } finally {
      connectionManager.close();
    }
    return globalPropertyDetail;
  }

  /**
   * Read all existing global properties.
   *
   * @return a list of global properties
   */
  @Override
  public List<GlobalPropertiesDetail> readAllGlobalProperties() {
    try {
      connectionManager.open();
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
   * @return corresponding global property
   */
  @Override
  public List<GlobalPropertiesDetail> readGlobalProperty(String name) {
    try {
      connectionManager.open();
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
   * @return the updated global property
   */
  @Override
  public GlobalPropertiesDetail updateGlobalProperty(GlobalPropertiesDetail globalProperty) {
    try {
      connectionManager.open();
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
   *
   * @param name
   */
  @Override
  public void deleteGlobalProperty(String name) {
    try {
      connectionManager.open();
      ResourceGroupsGlobalProperties.delete("name = ?", name);
    } finally {
      connectionManager.close();
    }
  }

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
}

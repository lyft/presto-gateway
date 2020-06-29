package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroup;
import com.lyft.data.gateway.ha.persistence.dao.Selector;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceGroupManager implements PrestoResourceManager {
  private JdbcConnectionManager connectionManager;

  public ResourceGroupManager(JdbcConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  /**
   * Creates and returns a resource group with the given parameters.
   *
   * @param resourceGroup
   * @return the created ResourceGroupDetail object
   */
  @Override
  public ResourceGroupDetail createResourceGroup(ResourceGroupDetail resourceGroup) {
    try {
      connectionManager.open();
      ResourceGroup.create(new ResourceGroup(), resourceGroup);
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
  public List<ResourceGroupDetail> readResourceGroup() {
    try {
      connectionManager.open();
      List<ResourceGroup> resourceGroupList = ResourceGroup.findAll();
      return ResourceGroup.upcast(resourceGroupList);
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
  public ResourceGroupDetail updateResourceGroup(ResourceGroupDetail resourceGroup) {
    try {
      connectionManager.open();
      ResourceGroup model =
          ResourceGroup.findFirst("resource_group_id = ?", resourceGroup.getResourceGroupId());

      if (model == null) {
        ResourceGroup.create(new ResourceGroup(), resourceGroup);
      } else {
        ResourceGroup.update(model, resourceGroup);
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
      ResourceGroup.delete("resource_group_id = ?", resourceGroupId);
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
  public SelectorDetail createSelector(SelectorDetail selector) {
    try {
      connectionManager.open();
      Selector.create(new Selector(), selector);
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
  public List<SelectorDetail> readSelector() {
    try {
      connectionManager.open();
      List<Selector> selectorList = Selector.findAll();
      return Selector.upcast(selectorList);
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
  public SelectorDetail updateSelector(SelectorDetail selector) {
    try {
      connectionManager.open();
      Selector model = Selector.findFirst("resource_group_id = ?", selector.getResourceGroupId());

      if (model == null) {
        Selector.create(new Selector(), selector);
      } else {
        Selector.update(model, selector);
      }
    } finally {
      connectionManager.close();
    }
    return selector;
  }

  /**
   * Search for selector by its resourceGroupId and delete it.
   *
   * @param resourceGroupId
   */
  @Override
  public void deleteSelector(long resourceGroupId) {
    try {
      connectionManager.open();
      Selector.delete("resource_group_id = ?", resourceGroupId);
    } finally {
      connectionManager.close();
    }
  }
}

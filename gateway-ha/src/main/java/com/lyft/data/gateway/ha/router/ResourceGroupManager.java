package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroup;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceGroupManager implements PrestoResourceManager {
  private JdbcConnectionManager connectionManager;

  public ResourceGroupManager(JdbcConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

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

  @Override
  public List<ResourceGroupDetail> readResourceGroup() {
    // TODO: reads all resource groups currently..change?
    try {
      connectionManager.open();
      List<ResourceGroup> resourceGroupList = ResourceGroup.findAll();
      return ResourceGroup.upcast(resourceGroupList);
    } finally {
      connectionManager.close();
    }
  }

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

  @Override
  public void deleteResourceGroup(long resourceGroupId) {
    try {
      connectionManager.open();
      ResourceGroup.delete("resource_group_id = ?", resourceGroupId);
    } finally {
      connectionManager.close();
    }
  }
}

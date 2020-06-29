package com.lyft.data.gateway.ha.router;

import java.util.List;
import lombok.Data;
import lombok.ToString;

public interface PrestoResourceManager {
  ResourceGroupDetail createResourceGroup(ResourceGroupDetail resourceGroup);

  List<ResourceGroupDetail> readResourceGroup();

  ResourceGroupDetail updateResourceGroup(ResourceGroupDetail resourceGroup);

  void deleteResourceGroup(long resourceGroupId);

  SelectorDetail createSelector(SelectorDetail selector);

  List<SelectorDetail> readSelector();

  SelectorDetail updateSelector(SelectorDetail selector);

  void deleteSelector(long resourceGroupId);

  //  void readGlobalProperty();
  //  void updateGlobalProperty();

  @Data
  @ToString
  class ResourceGroupDetail implements Comparable<ResourceGroupDetail> {
    private long resourceGroupId;
    private String name;

    /* OPTIONAL POLICY CONTROLS */
    private long parent;
    private boolean jmxExport;
    private String schedulingPolicy;
    private int schedulingWeight;

    /* REQUIRED QUOTAS */
    private String softMemoryLimit;
    private int maxQueued;
    private int hardConcurrencyLimit;

    /* OPTIONAL QUOTAS */
    private int softConcurrencyLimit;
    private String softCpuLimit;
    private String hardCpuLimit;
    private String environment;

    @Override
    public int compareTo(ResourceGroupDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }

  @Data
  @ToString
  class SelectorDetail implements Comparable<SelectorDetail> {
    private long resourceGroupId;
    private long priority;

    private String userRegex;
    private String sourceRegex;

    private String queryType;
    private String clientTags;
    private String selectorResourceEstimate;

    @Override
    public int compareTo(SelectorDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }
}

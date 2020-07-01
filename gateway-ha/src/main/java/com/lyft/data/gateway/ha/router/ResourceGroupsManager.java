package com.lyft.data.gateway.ha.router;

import java.util.List;

import lombok.Data;
import lombok.ToString;

public interface ResourceGroupsManager {
  ResourceGroupsDetail createResourceGroup(ResourceGroupsDetail resourceGroup);

  List<ResourceGroupsDetail> readResourceGroup();

  ResourceGroupsDetail updateResourceGroup(ResourceGroupsDetail resourceGroup);

  void deleteResourceGroup(long resourceGroupId);

  SelectorsDetail createSelector(SelectorsDetail selector);

  List<SelectorsDetail> readSelector();

  SelectorsDetail updateSelector(SelectorsDetail selector);

  void deleteSelector(long resourceGroupId);

  GlobalPropertiesDetail createGlobalProperty(GlobalPropertiesDetail globalPropertyDetail);

  List<GlobalPropertiesDetail> readGlobalProperty();

  GlobalPropertiesDetail updateGlobalProperty(GlobalPropertiesDetail globalProperty);

  void deleteGlobalProperty(String name);

  ExactSelectorsDetail createExactMatchSourceSelector(ExactSelectorsDetail exactSelectorDetail);

  List<ExactSelectorsDetail> readExactMatchSourceSelector();

  ExactSelectorsDetail updateExactMatchSourceSelector(ExactSelectorsDetail exactSelectorDetail);

  void deleteExactMatchSourceSelector(String environment); // TODO: change this to multiple params

  @Data
  @ToString
  class ResourceGroupsDetail implements Comparable<ResourceGroupsDetail> {
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
    public int compareTo(ResourceGroupsDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }

  @Data
  @ToString
  class SelectorsDetail implements Comparable<SelectorsDetail> {
    private long resourceGroupId;
    private long priority;

    private String userRegex;
    private String sourceRegex;

    private String queryType;
    private String clientTags;
    private String selectorResourceEstimate;

    @Override
    public int compareTo(SelectorsDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }

  @Data
  @ToString
  class GlobalPropertiesDetail implements Comparable<GlobalPropertiesDetail> {
    private String name;
    private String value;

    @Override
    public int compareTo(GlobalPropertiesDetail o) {
      return 0;
    }
  }

  @Data
  @ToString
  class ExactSelectorsDetail implements Comparable<ExactSelectorsDetail> {
    private String resourceGroupId;
    private String updateTime;

    private String source;
    private String environment;
    private String queryType;

    @Override
    public int compareTo(ExactSelectorsDetail o) {
      return 0;
    }
  }
}

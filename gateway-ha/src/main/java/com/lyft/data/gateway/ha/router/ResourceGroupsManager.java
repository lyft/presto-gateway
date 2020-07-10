package com.lyft.data.gateway.ha.router;

import java.util.List;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

public interface ResourceGroupsManager {
  ResourceGroupsDetail createResourceGroup(ResourceGroupsDetail resourceGroup);

  List<ResourceGroupsDetail> readAllResourceGroups();

  List<ResourceGroupsDetail> readResourceGroup(long resourceGroupId);

  ResourceGroupsDetail updateResourceGroup(ResourceGroupsDetail resourceGroup);

  void deleteResourceGroup(long resourceGroupId);

  SelectorsDetail createSelector(SelectorsDetail selector);

  List<SelectorsDetail> readAllSelectors();

  List<SelectorsDetail> readSelector(long resourceGroupId);

  SelectorsDetail updateSelector(SelectorsDetail selector);

  void deleteSelector(long resourceGroupId);

  GlobalPropertiesDetail createGlobalProperty(GlobalPropertiesDetail globalPropertyDetail);

  List<GlobalPropertiesDetail> readAllGlobalProperties();

  List<GlobalPropertiesDetail> readGlobalProperty(String name);

  GlobalPropertiesDetail updateGlobalProperty(GlobalPropertiesDetail globalProperty);

  void deleteGlobalProperty(String name);

  ExactSelectorsDetail createExactMatchSourceSelector(ExactSelectorsDetail exactSelectorDetail);

  List<ExactSelectorsDetail> readExactMatchSourceSelector();

  ExactSelectorsDetail getExactMatchSourceSelector(ExactSelectorsDetail exactSelectorDetail);

  @RequiredArgsConstructor
  @Data
  @ToString
  class ResourceGroupsDetail implements Comparable<ResourceGroupsDetail> {
    @NonNull private long resourceGroupId;
    @NonNull private String name;

    /* OPTIONAL POLICY CONTROLS */
    private Long parent;
    private Boolean jmxExport;
    private String schedulingPolicy;
    private Integer schedulingWeight;

    /* REQUIRED QUOTAS */
    @NonNull private String softMemoryLimit;
    @NonNull private int maxQueued;
    @NonNull private int hardConcurrencyLimit;

    /* OPTIONAL QUOTAS */
    private Integer softConcurrencyLimit;
    private String softCpuLimit;
    private String hardCpuLimit;
    private String environment;

    public ResourceGroupsDetail() {}

    @Override
    public int compareTo(ResourceGroupsDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }

  @RequiredArgsConstructor
  @Data
  @ToString
  class SelectorsDetail implements Comparable<SelectorsDetail> {
    @NonNull private long resourceGroupId;
    @NonNull private long priority;

    private String userRegex;
    private String sourceRegex;

    private String queryType;
    private String clientTags;
    private String selectorResourceEstimate;

    public SelectorsDetail() {}

    @Override
    public int compareTo(SelectorsDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }

  @RequiredArgsConstructor
  @Data
  @ToString
  class GlobalPropertiesDetail implements Comparable<GlobalPropertiesDetail> {
    @NonNull private String name;
    private String value;

    public GlobalPropertiesDetail() {}

    @Override
    public int compareTo(GlobalPropertiesDetail o) {
      return 0;
    }
  }

  @RequiredArgsConstructor
  @Data
  @ToString
  class ExactSelectorsDetail implements Comparable<ExactSelectorsDetail> {
    @NonNull private String resourceGroupId;
    @NonNull private String updateTime;

    @NonNull private String source;
    private String environment;
    private String queryType;

    public ExactSelectorsDetail() {}

    @Override
    public int compareTo(ExactSelectorsDetail o) {
      return 0;
    }
  }
}

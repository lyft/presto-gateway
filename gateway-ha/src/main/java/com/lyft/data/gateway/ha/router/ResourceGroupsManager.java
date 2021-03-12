package com.lyft.data.gateway.ha.router;

import com.sun.istack.Nullable;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;



public interface ResourceGroupsManager {
  ResourceGroupsDetail createResourceGroup(ResourceGroupsDetail resourceGroup,
                                           @Nullable String routingGroupDatabase);

  List<ResourceGroupsDetail> readAllResourceGroups(@Nullable String routingGroupDatabase);

  List<ResourceGroupsDetail> readResourceGroup(long resourceGroupId,
                                               @Nullable String routingGroupDatabase);

  ResourceGroupsDetail updateResourceGroup(ResourceGroupsDetail resourceGroup,
                                           @Nullable String routingGroupDatabase);

  void deleteResourceGroup(long resourceGroupId, @Nullable String routingGroupDatabase);

  SelectorsDetail createSelector(SelectorsDetail selector, @Nullable String routingGroupDatabase);

  List<SelectorsDetail> readAllSelectors(@Nullable String routingGroupDatabase);

  List<SelectorsDetail> readSelector(long resourceGroupId, @Nullable String routingGrouoDatabase);

  SelectorsDetail updateSelector(SelectorsDetail selector, SelectorsDetail updatedSelector,
                                 @Nullable String routingGroupDatabase);

  void deleteSelector(SelectorsDetail selector, @Nullable String routingGroupDatabase);

  GlobalPropertiesDetail createGlobalProperty(GlobalPropertiesDetail globalPropertyDetail,
                                              @Nullable String routingGroupDatabase);

  List<GlobalPropertiesDetail> readAllGlobalProperties(@Nullable String routingGroupDatabase);

  List<GlobalPropertiesDetail> readGlobalProperty(String name,
                                                  @Nullable String routingGroupDatabase);

  GlobalPropertiesDetail updateGlobalProperty(GlobalPropertiesDetail globalProperty,
                                              @Nullable String routingGroupDatabase);

  void deleteGlobalProperty(String name, @Nullable String routingGroupDatabase);

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

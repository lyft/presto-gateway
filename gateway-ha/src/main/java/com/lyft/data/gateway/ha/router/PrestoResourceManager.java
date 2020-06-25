package com.lyft.data.gateway.ha.router;

import java.util.List;
import lombok.Data;
import lombok.ToString;

public interface PrestoResourceManager {
  ResourceGroupDetail createResourceGroup(ResourceGroupDetail resourceGroup);

  List<ResourceGroupDetail> readResourceGroup();

  ResourceGroupDetail updateResourceGroup(ResourceGroupDetail resourceGroup);

  void deleteResourceGroup(long resourceGroupId);

  //  void createSelector();
  //  void readSelector();
  //  void updateSelector();
  //  void deleteSelector();
  //
  //  void readGlobalProperty();
  //  void updateGlobalProperty();

  @Data
  @ToString
  class ResourceGroupDetail implements Comparable<ResourceGroupDetail> {
    private long resourceGroupId; // resource_group_id BIGINT NOT NULL AUTO_INCREMENT
    private String name; // name VARCHAR(250) NOT NULL UNIQUE

    /* OPTIONAL POLICY CONTROLS */
    private long parent; // parent BIGINT NULL,
    private boolean jmxExport; // jmx_export BOOLEAN NULL,
    private String schedulingPolicy; // scheduling_policy VARCHAR(128) NULL,
    private int schedulingWeight; // scheduling_weight INT NULL,

    /* REQUIRED QUOTAS */
    private String softMemoryLimit; // soft_memory_limit VARCHAR(128) NOT NULL,
    private int maxQueued; // max_queued INT NOT NULL,
    private int hardConcurrencyLimit; // hard_concurrency_limit INT NOT NULL,

    /* OPTIONAL QUOTAS */
    private int softConcurrencyLimit; // soft_concurrency_limit INT NULL,
    private String softCpuLimit; // soft_cpu_limit VARCHAR(128) NULL,
    private String hardCpuLimit; // hard_cpu_limit VARCHAR(128) NULL,
    private String environment; // environment VARCHAR(128) NULL,

    @Override
    public int compareTo(ResourceGroupDetail o) {
      if (this.resourceGroupId < o.resourceGroupId) {
        return 1;
      } else {
        return this.resourceGroupId == o.resourceGroupId ? 0 : -1;
      }
    }
  }
}

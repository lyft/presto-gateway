package com.lyft.data.gateway.ha.persistence.dao;

import static com.lyft.data.gateway.ha.router.PrestoResourceManager.ResourceGroupDetail;

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@IdName("resource_group_id")
@Table("resource_groups")
@Cached
public class ResourceGroup extends Model {
  private static final String resourceGroupId =
      "resource_group_id"; // resource_group_id BIGINT NOT NULL AUTO_INCREMENT
  private static final String name = "name"; // name VARCHAR(250) NOT NULL UNIQUE

  /* OPTIONAL POLICY CONTROLS */
  private static final String parent = "parent"; // parent BIGINT NULL,
  private static final String jmxExport = "jmx_export"; // jmx_export BOOLEAN NULL,
  private static final String schedulingPolicy =
      "scheduling_policy"; // scheduling_policy VARCHAR(128) NULL,
  private static final String schedulingWeight = "scheduling_weight"; // scheduling_weight INT NULL,

  /* REQUIRED QUOTAS */
  private static final String softMemoryLimit =
      "soft_memory_limit"; // soft_memory_limit VARCHAR(128) NOT NULL,
  private static final String maxQueued = "max_queued"; // max_queued INT NOT NULL,
  private static final String hardConcurrencyLimit =
      "hard_concurrency_limit"; // hard_concurrency_limit INT NOT NULL,

  /* OPTIONAL QUOTAS */
  private static final String softConcurrencyLimit =
      "soft_concurrency_limit"; // soft_concurrency_limit INT NULL,
  private static final String softCpuLimit = "soft_cpu_limit"; // soft_cpu_limit VARCHAR(128) NULL,
  private static final String hardCpuLimit = "hard_cpu_limit"; // hard_cpu_limit VARCHAR(128) NULL,
  private static final String environment = "environment"; // environment VARCHAR(128) NULL,

  // PRIMARY KEY(resource_group_id),
  // KEY(name),
  // FOREIGN KEY (parent) REFERENCES resource_groups (resource_group_id)

  public static List<ResourceGroupDetail> upcast(List<ResourceGroup> resourceGroupList) {
    List<ResourceGroupDetail> resourceGroupDetails = new ArrayList<>();
    for (ResourceGroup dao : resourceGroupList) {
      ResourceGroupDetail resourceGroupDetail = new ResourceGroupDetail();
      resourceGroupDetail.setResourceGroupId(dao.getLong(resourceGroupId));
      resourceGroupDetail.setName(dao.getString(name));

      resourceGroupDetail.setParent(dao.getLong(parent));
      resourceGroupDetail.setJmxExport(dao.getBoolean(jmxExport));
      resourceGroupDetail.setSchedulingPolicy(dao.getString(schedulingPolicy));
      resourceGroupDetail.setSchedulingWeight(dao.getInteger(schedulingWeight));

      resourceGroupDetail.setSoftMemoryLimit(dao.getString(softMemoryLimit));
      resourceGroupDetail.setMaxQueued(dao.getInteger(maxQueued));
      resourceGroupDetail.setHardConcurrencyLimit(dao.getInteger(hardConcurrencyLimit));

      resourceGroupDetail.setSoftConcurrencyLimit(dao.getInteger(softConcurrencyLimit));
      resourceGroupDetail.setSoftCpuLimit(dao.getString(softCpuLimit));
      resourceGroupDetail.setHardCpuLimit(dao.getString(hardCpuLimit));
      resourceGroupDetail.setEnvironment(dao.getString(environment));

      resourceGroupDetails.add(resourceGroupDetail);
    }
    return resourceGroupDetails;
  }

  public static void create(ResourceGroup model, ResourceGroupDetail resourceGroupDetail) {
    model.set(resourceGroupId, resourceGroupDetail.getResourceGroupId());
    model.set(name, resourceGroupDetail.getName());

    model.set(parent, resourceGroupDetail.getParent());
    //        model.set(jmxExport, resourceGroupDetail.getJmxExport()); //TODO: why boolean not get
    model.set(schedulingPolicy, resourceGroupDetail.getSchedulingPolicy());
    model.set(schedulingWeight, resourceGroupDetail.getSchedulingWeight());

    model.set(softMemoryLimit, resourceGroupDetail.getSoftMemoryLimit());
    model.set(maxQueued, resourceGroupDetail.getMaxQueued());
    model.set(hardConcurrencyLimit, resourceGroupDetail.getHardConcurrencyLimit());

    model.set(softConcurrencyLimit, resourceGroupDetail.getSoftConcurrencyLimit());
    model.set(softCpuLimit, resourceGroupDetail.getSoftCpuLimit());
    model.set(hardCpuLimit, resourceGroupDetail.getHardCpuLimit());
    model.set(environment, resourceGroupDetail.getEnvironment());

    model.insert();
  }

  public static void update(ResourceGroup model, ResourceGroupDetail resourceGroupDetail) {
    model
        .set(resourceGroupId, resourceGroupDetail.getResourceGroupId())
        .set(name, resourceGroupDetail.getName())
        .set(parent, resourceGroupDetail.getParent())
        //        .set(jmxExport, resourceGroupDetail.getJmxExport()) //TODO: why boolean no get
        .set(schedulingPolicy, resourceGroupDetail.getSchedulingPolicy())
        .set(schedulingWeight, resourceGroupDetail.getSchedulingWeight())
        .set(softMemoryLimit, resourceGroupDetail.getSoftMemoryLimit())
        .set(maxQueued, resourceGroupDetail.getMaxQueued())
        .set(hardConcurrencyLimit, resourceGroupDetail.getHardConcurrencyLimit())
        .set(softConcurrencyLimit, resourceGroupDetail.getSoftConcurrencyLimit())
        .set(softCpuLimit, resourceGroupDetail.getSoftCpuLimit())
        .set(hardCpuLimit, resourceGroupDetail.getHardCpuLimit())
        .set(environment, resourceGroupDetail.getEnvironment())
        .saveIt();
  }
}

package com.lyft.data.gateway.ha.resource;

import com.google.inject.Inject;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/presto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PrestoResource {
  @Inject private ResourceGroupsManager resourceGroupsManager;

  @POST
  @Path("/resourcegroup/create")
  public Response createResourceGroup(ResourceGroupsDetail resourceGroup) {
    ResourceGroupsDetail updatedResourceGroup =
        this.resourceGroupsManager.createResourceGroup(resourceGroup);
    return Response.ok(updatedResourceGroup).build();
  }

  @GET
  @Path("/resourcegroup/read")
  public Response readResourceGroup() {
    return Response.ok(this.resourceGroupsManager.readResourceGroup()).build();
  }

  @Path("/resourcegroup/update")
  @POST
  public Response updateResourceGroup(ResourceGroupsDetail resourceGroup) {
    ResourceGroupsDetail updatedResourceGroup =
        this.resourceGroupsManager.updateResourceGroup(resourceGroup);
    return Response.ok(updatedResourceGroup).build();
  }

  @Path("/resourcegroup/delete")
  @POST
  public Response deleteResourceGroup(long resourceGroupId) {
    resourceGroupsManager.deleteResourceGroup(resourceGroupId);
    return Response.ok().build();
  }
}

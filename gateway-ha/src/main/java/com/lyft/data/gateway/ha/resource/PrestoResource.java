package com.lyft.data.gateway.ha.resource;

import com.google.inject.Inject;
import com.lyft.data.gateway.ha.router.PrestoResourceManager;
import com.lyft.data.gateway.ha.router.PrestoResourceManager.ResourceGroupDetail;

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
public class PrestoResource {
  @Inject private PrestoResourceManager prestoResourceManager;

  @POST
  @Path("/resourcegroup/create")
  public Response createResourceGroup(ResourceGroupDetail resourceGroup) {
    ResourceGroupDetail updatedResourceGroup =
        this.prestoResourceManager.createResourceGroup(resourceGroup);
    return Response.ok(updatedResourceGroup).build();
  }

  @GET
  @Path("/resourcegroup/read")
  public Response readResourceGroup() {
    return Response.ok(this.prestoResourceManager.readResourceGroup()).build();
  }

  @Path("/resourcegroup/update")
  @POST
  public Response updateResourceGroup(ResourceGroupDetail resourceGroup) {
    ResourceGroupDetail updatedResourceGroup =
        this.prestoResourceManager.updateResourceGroup(resourceGroup);
    return Response.ok(updatedResourceGroup).build();
  }

  @Path("/resourcegroup/delete")
  @POST
  public Response deleteResourceGroup(long resourceGroupId) {
    prestoResourceManager.deleteResourceGroup(resourceGroupId);
    return Response.ok().build();
  }
}

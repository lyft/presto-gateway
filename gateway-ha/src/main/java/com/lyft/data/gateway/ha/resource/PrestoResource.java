package com.lyft.data.gateway.ha.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.lyft.data.gateway.ha.persistence.dao.ResourceGroups;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.SelectorsDetail;

import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/presto")
@Produces(MediaType.APPLICATION_JSON)
// @Consumes(MediaType.APPLICATION_JSON)
public class PrestoResource {
  @Inject private ResourceGroupsManager resourceGroupsManager;
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @POST
  @Path("/resourcegroup/create")
  public Response createResourceGroup(String jsonPayload) {
    try {
      ResourceGroupsDetail resourceGroup =
          OBJECT_MAPPER.readValue(jsonPayload, ResourceGroupsDetail.class);
      ResourceGroupsDetail updatedResourceGroup =
          this.resourceGroupsManager.createResourceGroup(resourceGroup);
      return Response.ok(updatedResourceGroup).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
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
  //
  //  @POST
  //  @Path("/selector/create")
  //  public Response createSelector(String jsonPayload) {
  //    try {
  //      SelectorsDetail selector = OBJECT_MAPPER.readValue(jsonPayload, SelectorsDetail.class);
  //      SelectorsDetail updatedSelector = this.resourceGroupsManager.createSelector(selector);
  //      return Response.ok(updatedSelector).build();
  //    } catch (IOException e) {
  //      log.error(e.getMessage(), e);
  //      throw new WebApplicationException(e);
  //    }
  //  }
  //
  //  @GET
  //  @Path("/selector/read")
  //  public Response readSelector() {
  //    return Response.ok(this.resourceGroupsManager.readSelector()).build();
  //  }

  //  @Path("/selector/update")
  //  @POST
  //  public Response updateSelector(SelectorsDetail selector) {
  //    SelectorsDetail updatedSelector = this.resourceGroupsManager.updateSelector(selector);
  //    return Response.ok(updatedSelector).build();
  //  }
  //
  //  @Path("/selector/delete")
  //  @POST
  //  public Response deleteSelector(long resourceGroupId) {
  //    resourceGroupsManager.deleteSelector(resourceGroupId);
  //    return Response.ok().build();
  //  }
}

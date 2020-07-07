package com.lyft.data.gateway.ha.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import com.lyft.data.gateway.ha.persistence.dao.ResourceGroups;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.GlobalPropertiesDetail;
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
public class PrestoResource {
  @Inject private ResourceGroupsManager resourceGroupsManager;
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @POST
  @Path("/resourcegroup/create")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createResourceGroup(String jsonPayload) {
    //    return Response.ok("hi").build();
    try {
      ResourceGroupsDetail resourceGroup =
          OBJECT_MAPPER.readValue(jsonPayload, ResourceGroupsDetail.class);
      ResourceGroupsDetail newResourceGroup =
          this.resourceGroupsManager.createResourceGroup(resourceGroup);
      return Response.ok(newResourceGroup).build();
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
  public Response updateResourceGroup(String jsonPayload) {
    try {
      ResourceGroupsDetail resourceGroup =
          OBJECT_MAPPER.readValue(jsonPayload, ResourceGroupsDetail.class);
      ResourceGroupsDetail updatedResourceGroup =
          this.resourceGroupsManager.updateResourceGroup(resourceGroup);
      return Response.ok(updatedResourceGroup).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }

  @Path("/resourcegroup/delete")
  @POST
  public Response deleteResourceGroup(long resourceGroupId) {
    resourceGroupsManager.deleteResourceGroup(resourceGroupId);
    return Response.ok().build();
  }

  @POST
  @Path("/selector/create")
  public Response createSelector(String jsonPayload) {
    try {
      SelectorsDetail selector = OBJECT_MAPPER.readValue(jsonPayload, SelectorsDetail.class);
      SelectorsDetail updatedSelector = this.resourceGroupsManager.createSelector(selector);
      return Response.ok(updatedSelector).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/selector/read")
  public Response readSelector() {
    return Response.ok(this.resourceGroupsManager.readSelector()).build();
  }

  @Path("/selector/update")
  @POST
  public Response updateSelector(String jsonPayload) {
    try {
      SelectorsDetail selector = OBJECT_MAPPER.readValue(jsonPayload, SelectorsDetail.class);
      SelectorsDetail updatedSelector = this.resourceGroupsManager.updateSelector(selector);
      return Response.ok(updatedSelector).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }

  @Path("/selector/delete")
  @POST
  public Response deleteSelector(long resourceGroupId) {
    resourceGroupsManager.deleteSelector(resourceGroupId);
    return Response.ok().build();
  }

  @POST
  @Path("/globalproperty/create")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createGlobalProperty(String jsonPayload) {
    try {
      GlobalPropertiesDetail globalProperty =
          OBJECT_MAPPER.readValue(jsonPayload, ResourceGroupsManager.GlobalPropertiesDetail.class);
      GlobalPropertiesDetail newGlobalProperty =
          this.resourceGroupsManager.createGlobalProperty(globalProperty);
      return Response.ok(newGlobalProperty).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/globalproperty/read")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readGlobalProperty() {
    return Response.ok(this.resourceGroupsManager.readGlobalProperty()).build();
  }

  @Path("/globalproperty/update")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateGlobalProperty(String jsonPayload) {
    try {
      GlobalPropertiesDetail globalProperty =
          OBJECT_MAPPER.readValue(jsonPayload, ResourceGroupsManager.GlobalPropertiesDetail.class);
      GlobalPropertiesDetail updatedGlobalProperty =
          this.resourceGroupsManager.updateGlobalProperty(globalProperty);
      return Response.ok(updatedGlobalProperty).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }
}

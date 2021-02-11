package com.lyft.data.gateway.ha.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import com.lyft.data.gateway.ha.router.ResourceGroupsManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.GlobalPropertiesDetail;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.SelectorsDetail;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
  public Response createResourceGroup(String jsonPayload) {
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
  public Response readAllResourceGroups(@QueryParam("routingGroupDatabase")
                                                String routingGroupDatabase) {
    log.info("asdas");
    return Response.ok(this.resourceGroupsManager.readAllResourceGroups(
            routingGroupDatabase)).build();
  }

  @GET
  @Path("/resourcegroup/read/{resourceGroupId}")
  public Response readResourceGroup(@PathParam("resourceGroupId") String resourceGroupIdStr) {
    if (Strings.isNullOrEmpty(resourceGroupIdStr)) { // if query not specified, return all
      return Response.ok(this.resourceGroupsManager.readAllResourceGroups()).build();
    }
    long resourceGroupId = Long.parseLong(resourceGroupIdStr);
    List<ResourceGroupsDetail> resourceGroup =
        this.resourceGroupsManager.readResourceGroup(resourceGroupId);
    return Response.ok(resourceGroup).build();
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

  @Path("/resourcegroup/delete/{resourceGroupId}")
  @POST
  public Response deleteResourceGroup(@PathParam("resourceGroupId") String resourceGroupIdStr) {
    if (Strings.isNullOrEmpty(resourceGroupIdStr)) { // if query not specified, return all
      throw new WebApplicationException("EntryType can not be null");
    }
    long resourceGroupId = Long.parseLong(resourceGroupIdStr);
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
  public Response readAllSelectors() {
    return Response.ok(this.resourceGroupsManager.readAllSelectors()).build();
  }

  @GET
  @Path("/selector/read/{resourceGroupId}")
  public Response readSelector(@QueryParam("resourceGroupId") String resourceGroupIdStr) {
    if (Strings.isNullOrEmpty(resourceGroupIdStr)) { // if query not specified, return all
      return Response.ok(this.resourceGroupsManager.readAllSelectors()).build();
    }
    long resourceGroupId = Long.parseLong(resourceGroupIdStr);
    List<SelectorsDetail> selectors = this.resourceGroupsManager.readSelector(resourceGroupId);
    return Response.ok(selectors).build();
  }

  @Path("/selector/update")
  @POST
  public Response updateSelector(String jsonPayload) {
    try {
      JsonNode selectors = OBJECT_MAPPER.readValue(jsonPayload, JsonNode.class);
      SelectorsDetail selector =
          OBJECT_MAPPER.readValue(selectors.get("current").toString(), SelectorsDetail.class);
      SelectorsDetail newSelector =
          OBJECT_MAPPER.readValue(selectors.get("update").toString(), SelectorsDetail.class);

      SelectorsDetail updatedSelector =
          this.resourceGroupsManager.updateSelector(selector, newSelector);
      return Response.ok(updatedSelector).build();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }

  @Path("/selector/delete/")
  @POST
  public Response deleteSelector(String jsonPayload) {
    if (Strings.isNullOrEmpty(jsonPayload)) {
      throw new WebApplicationException("EntryType can not be null");
    }
    try {
      SelectorsDetail selector = OBJECT_MAPPER.readValue(jsonPayload, SelectorsDetail.class);
      resourceGroupsManager.deleteSelector(selector);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return Response.ok().build();
  }

  @POST
  @Path("/globalproperty/create")
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
  public Response readAllGlobalProperties() {
    return Response.ok(this.resourceGroupsManager.readAllGlobalProperties()).build();
  }

  @GET
  @Path("/globalproperty/read/{name}")
  public Response readGlobalProperty(@PathParam("name") String name) {
    if (Strings.isNullOrEmpty(name)) {
      return Response.ok(this.resourceGroupsManager.readAllGlobalProperties()).build();
    }
    List<GlobalPropertiesDetail> globalProperty =
        this.resourceGroupsManager.readGlobalProperty(name);
    return Response.ok(globalProperty).build();
  }

  @Path("/globalproperty/update")
  @POST
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

  @Path("/globalproperty/delete/{name}")
  @POST
  public Response deleteGlobalProperty(@PathParam("name") String name) {
    resourceGroupsManager.deleteGlobalProperty(name);
    return Response.ok().build();
  }

  /* Unused API for ExactMatchSourceSelectors, as it is currently not used in Lyft
    @POST
    @Path("/exactmatchsourceselector/create")
    public Response createExactMatchSourceSelector(String jsonPayload) {
      try {
        ExactSelectorsDetail exactMatchSourceSelector =
                OBJECT_MAPPER.readValue(jsonPayload, ExactSelectorsDetail.class);
        ExactSelectorsDetail newExactMatchSourceSelector =
                this.resourceGroupsManager.createExactMatchSourceSelector(exactMatchSourceSelector);
        return Response.ok(newExactMatchSourceSelector).build();
      } catch (IOException e) {
        log.error(e.getMessage(), e);
        throw new WebApplicationException(e);
      }
    }

    @POST
    @Path("/exactmatchsourceselector/read")
    public Response readExactMatchSourceSelector() {
      return Response.ok(this.resourceGroupsManager.readExactMatchSourceSelector()).build();
    }
  */

}

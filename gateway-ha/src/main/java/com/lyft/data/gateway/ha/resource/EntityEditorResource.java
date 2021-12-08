package com.lyft.data.gateway.ha.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import com.lyft.data.gateway.ha.router.HaResourceGroupsManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager.SelectorsDetail;
import io.dropwizard.views.View;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
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
@Path("entity")
public class EntityEditorResource {

  @Inject
  private GatewayBackendManager gatewayBackendManager;
  @Inject
  private ResourceGroupsManager resourceGroupsManager;

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @GET
  @Produces(MediaType.TEXT_HTML)
  public EntityView entityUi() {
    return new EntityView("/template/entity-view.ftl");
  }

  public static class EntityView extends View {
    protected EntityView(String templateName) {
      super(templateName, Charset.defaultCharset());
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<EntityType> getAllEntityTypes() {
    return Arrays.asList(EntityType.values());
  }

  @POST
  public Response updateEntity(@QueryParam("entityType") String entityTypeStr, String jsonPayload) {
    if (Strings.isNullOrEmpty(entityTypeStr)) {
      throw new WebApplicationException("EntryType can not be null");
    }
    EntityType entityType = EntityType.valueOf(entityTypeStr);
    try {
      //TODO: allow selector of databases
      switch (entityType) {
        case GATEWAY_BACKEND:
          ProxyBackendConfiguration backend =
              OBJECT_MAPPER.readValue(jsonPayload, ProxyBackendConfiguration.class);
          gatewayBackendManager.updateBackend(backend);
          break;
        case RESOURCE_GROUP:
          ResourceGroupsDetail resourceGroupDetails = OBJECT_MAPPER.readValue(jsonPayload,
              ResourceGroupsDetail.class);
          resourceGroupsManager.updateResourceGroup(resourceGroupDetails, null);
          break;
        case SELECTOR:
          SelectorsDetail selectorDetails = OBJECT_MAPPER.readValue(jsonPayload,
              SelectorsDetail.class);
          List<SelectorsDetail> oldSelectorDetails =
              resourceGroupsManager.readSelector(selectorDetails.getResourceGroupId(), null);
          if (oldSelectorDetails.size() >= 1) {
            resourceGroupsManager.updateSelector(oldSelectorDetails.get(0), selectorDetails, null);
          } else {
            resourceGroupsManager.createSelector(selectorDetails, null);
          }
          break;
        default:
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
    return Response.ok().build();
  }

  @GET
  @Path("/{entityType}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllEntitiesForType(@PathParam("entityType") String entityTypeStr,
      @PathParam("database") String database) {
    EntityType entityType = EntityType.valueOf(entityTypeStr);

    switch (entityType) {
      case GATEWAY_BACKEND:
        return Response.ok(gatewayBackendManager.getAllBackends()).build();
      case RESOURCE_GROUP:
        return Response.ok(resourceGroupsManager.readAllResourceGroups(database)).build();
      case SELECTOR:
        return Response.ok(resourceGroupsManager.readAllSelectors(database)).build();
      default:
    }
    return Response.ok(ImmutableList.of()).build();
  }
}

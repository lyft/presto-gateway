package com.lyft.data.gateway.resource;

import com.google.inject.Inject;
import com.lyft.data.gateway.router.GatewayBackendManager;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/gateway")
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {

  @Inject private GatewayBackendManager gatewayBackendManager;

  @GET
  public Response ok(@Context Request request) {
    return Response.ok("ok").build();
  }

  @Path("/backend/all")
  @GET
  public Response getAllBackends() {
    return Response.ok(this.gatewayBackendManager.getAllBackends()).build();
  }

  @Path("/backend/active")
  @GET
  public Response getActiveBackends() {
    return Response.ok(this.gatewayBackendManager.getActiveBackends()).build();
  }

  @Path("/backend/deactivate/{name}")
  @POST
  public Response deactivateBackend(@PathParam("name") String name) {
    try {
      this.gatewayBackendManager.deactivateBackend(name);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Response.status(Response.Status.NOT_FOUND)
          .entity(e.getMessage())
          .type("text/plain")
          .build();
    }
    return Response.ok().build();
  }

  @Path("/backend/activate/{name}")
  @POST
  public Response activateBackend(@PathParam("name") String name) {
    try {
      this.gatewayBackendManager.activateBackend(name);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Response.status(Response.Status.NOT_FOUND)
          .entity(e.getMessage())
          .type("text/plain")
          .build();
    }
    return Response.ok().build();
  }
}

package com.lyft.data.gateway.ha.resource;

import com.google.inject.Inject;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import com.lyft.data.gateway.ha.router.HaGatewayManager;

import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("gateway/backend/modify")
@Produces(MediaType.APPLICATION_JSON)
public class HaGatewayResource {

  @Inject private GatewayBackendManager haGatewayManager;

  @Path("/add")
  @POST
  public Response addBackend(ProxyBackendConfiguration backend) {
    ProxyBackendConfiguration updatedBackend =
        ((HaGatewayManager) haGatewayManager).addBackend(backend);
    return Response.ok(updatedBackend).build();
  }

  @Path("/update")
  @POST
  public Response updateBackend(ProxyBackendConfiguration backend) {
    ProxyBackendConfiguration updatedBackend =
        ((HaGatewayManager) haGatewayManager).updateBackend(backend);
    return Response.ok(updatedBackend).build();
  }

  @Path("/delete")
  @POST
  public Response removeBackend(String name) {
    ((HaGatewayManager) haGatewayManager).deleteBackend(name);
    return Response.ok().build();
  }
}

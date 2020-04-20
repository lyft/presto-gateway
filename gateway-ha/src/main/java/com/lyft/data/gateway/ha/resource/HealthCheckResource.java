package com.lyft.data.gateway.ha.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/")
public class HealthCheckResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
  @Path("healthcheck")
  public Response check() {
    log.debug("Running healthcheckresource");
    return Response.ok().build();
  }
}

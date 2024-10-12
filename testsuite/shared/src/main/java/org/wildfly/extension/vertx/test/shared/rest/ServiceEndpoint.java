/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.shared.rest;

import org.wildfly.extension.vertx.test.shared.ejb.EchoService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * AsyncServlet which requests a response from `echo` Vert.x EventBus address.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@Path("/echo")
public class ServiceEndpoint {

    @Inject
    private EchoService echoService;

    @GET
    @Path("{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sayHi(@PathParam("name") String name) {
        try {
            String message = echoService.echo(name).get();
            return Response.ok(message).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }
}

/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.vertx.test.mini.injection;

import io.vertx.core.Vertx;
import org.junit.Assert;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/injection")
public class InjectionEndPoint {

    @Resource(name = "java:/vertx/default")
    private Vertx vertxFromJNDI;

    @Inject
    private Vertx vertx;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkInjection(@PathParam("name") String name) {
        Assert.assertNotNull("Vertx from JNDI lookup is null", this.vertxFromJNDI);
        Assert.assertNotNull("Vertx using Injection is null", this.vertx);
        Assert.assertEquals("Vertx using Injection should be equal to the one lookup from JNDI", this.vertx,
                this.vertxFromJNDI);
        return Response.ok("True").build();
    }

}

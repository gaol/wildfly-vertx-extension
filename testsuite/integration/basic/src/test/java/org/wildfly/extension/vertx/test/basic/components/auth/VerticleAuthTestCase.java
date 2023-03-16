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

package org.wildfly.extension.vertx.test.basic.components.auth;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.annotation.WebServlet;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.shared.AbstractEventBusConsumerVerticle;
import org.wildfly.extension.vertx.test.shared.StreamUtils;
import org.wildfly.extension.vertx.test.shared.servlet.AbstractVertxServlet;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.authorization.WildcardPermissionBasedAuthorization;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.auth.properties.PropertyFileAuthorization;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test war deployment with verticle inside which needs authentication.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VerticleAuthTestCase {

    @ArquillianResource
    private URL url;

    @WebServlet(urlPatterns = "/testauth", asyncSupported = true)
    static class VertxAuthServlet extends AbstractVertxServlet<String, JsonObject> {
        @Override
        protected boolean resultRequired() {
            return true;
        }

        @Override
        protected String address() {
            return testAddress();
        }

        @Override
        protected String payload(HttpServletRequest httpRequest) throws IOException {
            return StreamUtils.streamToString(httpRequest.getInputStream());
        }

        @Override
        protected void sendResponse(HttpServletResponse httpResponse, String payload, JsonObject vertxResponse) throws IOException {
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(vertxResponse.toString());
        }
    }

    public static class VertxAuthVerticle extends AbstractEventBusConsumerVerticle<String, JsonObject> {
        private PropertyFileAuthentication authnProvider;
        private PropertyFileAuthorization authzProvider;
        @Override
        protected void doInit() {
            super.doInit();
            authnProvider = PropertyFileAuthentication.create(vertx, "vertx-auth.properties");
            authzProvider = PropertyFileAuthorization.create(vertx, "vertx-auth.properties");
        }

        @Override
        protected Future<JsonObject> responseOf(String body) {
            JsonObject authentication = new JsonObject(body);
            return authnProvider.authenticate(authentication)
              .flatMap(user -> authzProvider.getAuthorizations(user).flatMap(v -> {
                  JsonObject respContent = new JsonObject();
                  respContent.put("user", user.principal());
                  respContent.put("roles", new JsonArray());
                  respContent.put("permissions", new JsonArray());
                  Set<Authorization> authorities = user.authorizations().get("vertx-auth.properties");
                  for (Authorization auth: authorities) {
                      if (auth instanceof RoleBasedAuthorization) {
                          RoleBasedAuthorization roleAuth = (RoleBasedAuthorization)auth;
                          respContent.getJsonArray("roles").add(roleAuth.getRole());
                      } else if (auth instanceof WildcardPermissionBasedAuthorization) {
                          WildcardPermissionBasedAuthorization wpa = (WildcardPermissionBasedAuthorization)auth;
                          respContent.getJsonArray("permissions").add(wpa.getPermission());
                      }
                  }
                  return Future.succeededFuture(respContent);
              }));
        }

        @Override
        protected String address() {
            return testAddress();
        }

    }

    private static String testAddress() {
        return "vertx-auth";
    }

    @Deployment
    public static Archive<?> deployment() {
        WebArchive web = ShrinkWrap.create(WebArchive.class, "test-verticle.war")
                .addAsWebInfResource(new StringAsset("{\n" +
                    "  \"deployments\": [\n" +
                    "    {\n" +
                    "      \"verticle-class\": \"org.wildfly.extension.vertx.test.basic.components.auth.VerticleAuthTestCase$VertxAuthVerticle\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}"), "vertx-deployment.json")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("vertx-auth.properties")
                .addClasses(VertxAuthServlet.class,
                  AbstractVertxServlet.class,
                  VertxAuthVerticle.class,
                  AbstractEventBusConsumerVerticle.class,
                  StreamUtils.class);
        return web;
    }

    @Test
    public void testVertxAuthInServlet() throws Exception {
        String authentication = new JsonObject().put("username", "lgao").put("password", "secret").toString();
        String res = HttpRequest.post( url.toExternalForm() + "testauth", authentication, 10, TimeUnit.SECONDS);
        Assert.assertNotNull(res);
        JsonObject resp = new JsonObject(res);
        Assert.assertEquals("lgao", resp.getJsonObject("user").getString("username"));
        Assert.assertTrue(resp.getJsonArray("roles").contains("author"));
        Assert.assertTrue(resp.getJsonArray("roles").contains("developer"));
        Assert.assertTrue(resp.getJsonArray("permissions").contains("writing"));
        Assert.assertTrue(resp.getJsonArray("permissions").contains("push"));
        Assert.assertNull(resp.getValue("error"));

    }

}

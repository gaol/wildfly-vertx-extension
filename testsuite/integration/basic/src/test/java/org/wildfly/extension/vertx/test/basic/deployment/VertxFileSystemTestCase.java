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

package org.wildfly.extension.vertx.test.basic.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

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
import io.vertx.core.json.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test accessing resources inside of the deployment using vertx filesystem API.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VertxFileSystemTestCase {

    @ArquillianResource
    private URL url;

    public static class FileAccessVerticle extends AbstractEventBusConsumerVerticle<String, JsonObject> {

        @Override
        protected Future<JsonObject> responseOf(String body) {
            final JsonObject json = new JsonObject();
            return vertx.fileSystem().readFile("geo.json").flatMap(buffer -> {
                try {
                    json.put("geo.json", new JsonObject(buffer));
                    return vertx.fileSystem().readFile("assets/config.properties");
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            }).flatMap(config -> {
                try {
                    json.put("config", StreamUtils.stringToProperties(config.toString()));
                    return Future.succeededFuture(json);
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            });
        }

        @Override
        protected String address() {
            return testAddress();
        }

    }

    @WebServlet(urlPatterns = "/file-access", asyncSupported = true)
    static class FileAccessServlet extends AbstractVertxServlet<String, JsonObject> {

        @Override
        protected String payload(HttpServletRequest httpRequest) throws IOException {
            return "";
        }

        @Override
        protected void sendResponse(HttpServletResponse httpResponse, String payload, JsonObject vertxResponse) throws IOException {
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(vertxResponse.toString());
        }

        @Override
        protected String address() {
            return testAddress();
        }

        @Override
        protected boolean resultRequired() {
            return true;
        }
    }

    private static String testAddress() {
        return "file-access";
    }

    @Deployment
    public static Archive<?> deployment() {
        WebArchive web = ShrinkWrap.create(WebArchive.class, "test-vertx-filesystem.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset("{\n" +
                    "  \"deployments\": [\n" +
                    "    {\n" +
                    "      \"verticle-class\": \"org.wildfly.extension.vertx.test.basic.deployment.VertxFileSystemTestCase$FileAccessVerticle\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}"), "vertx.json")
                .addAsResource("assets")
                .addAsResource("geo.json")
                .addClasses(FileAccessServlet.class,
                  FileAccessVerticle.class,
                  AbstractEventBusConsumerVerticle.class,
                  AbstractVertxServlet.class,
                  StreamUtils.class);
        return web;
    }

    @Test
    public void testFileAccess() throws Exception {
        String res = HttpRequest.get( url.toExternalForm() + "file-access", 10, TimeUnit.SECONDS);
        JsonObject json = new JsonObject(res);
        Assert.assertEquals("Beijing", json.getJsonObject("geo.json").getString("location"));
        Assert.assertEquals("2022", json.getJsonObject("config").getString("covid.year.vanish"));
        Assert.assertNull(json.getValue("error"));
    }

}

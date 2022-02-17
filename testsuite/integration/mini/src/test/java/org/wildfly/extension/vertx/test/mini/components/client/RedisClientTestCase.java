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
package org.wildfly.extension.vertx.test.mini.components.client;

import io.vertx.core.json.JsonObject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Test Redis client to interact with redis server.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(RedisClientTestCase.RedisContainerSetup.class)
@RunAsClient
public class RedisClientTestCase extends ContainerBasedTestCase {

    static GenericContainer<?> redis;

    public static class RedisContainerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            redis = new GenericContainer<>("docker.io/library/redis:6.0.6")
                    .withExposedPorts(6379);
            redis.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            redis.stop();
        }
    }

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive web = ShrinkWrap.create(WebArchive.class, "test-redis.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "vertx-deployment.json")
                .addPackage(RedisClientTestCase.class.getPackage())
                .addClasses(RedisMessageServlet.class);
        return web;
    }

    @Test
    public void testRedisMessage() throws Exception {
        String message = "hello";
        String res = HttpRequest.get( url.toExternalForm() + "test-redis?message=" + message +
                "&host=" + redis.getContainerIpAddress() +
                "&port=" + redis.getFirstMappedPort(), 10, TimeUnit.SECONDS);
        Assert.assertNotNull(res);
        JsonObject resp = new JsonObject(res);
        Assert.assertNull(resp.getValue("error"));
        Assert.assertEquals(message, resp.getString("response"));
    }

}

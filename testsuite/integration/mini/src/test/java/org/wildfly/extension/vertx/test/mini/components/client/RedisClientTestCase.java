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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.JsonObject;

/**
 * Test Redis client to interact with redis server.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RedisClientTestCase extends ContainerBasedTestCase {

    public final static int REDIS_PORT = 6379;

    @ClassRule
    public static ContainerBaseRule rule = new ContainerBaseRule("docker.io/library/redis:6.0.6", REDIS_PORT);

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, "test-redis.war")
          .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(RedisClientTestCase.class.getPackage())
                .addClasses(RedisMessageServlet.class);
    }

    protected String getContainerConnStr() {
        return "redis://" + rule.getContainerIPAddress() + ":" + rule.getMappedPort();
    }

    @Test
    public void testRedisMessage() throws Exception {
        String message = "hello";
        String res = HttpRequest.get( url.toExternalForm() + "test-redis?message=" + message
          + "&connStr=" + getServiceConnStrEncoded(), 10, TimeUnit.SECONDS);
        Assert.assertNotNull(res);
        JsonObject resp = new JsonObject(res);
        Assert.assertNull(resp.getValue("error"));
        Assert.assertEquals(message, resp.getString("response"));
    }

}

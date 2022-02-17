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

package org.wildfly.extension.vertx.test.mini.deployment.multiple;

import io.vertx.core.json.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 * When same Vertx instance is used for multiple deployments
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleFileAccessTestCase {

    @ArquillianResource
    @OperateOnDeployment("test-res1")
    private URL res1URL;

    @ArquillianResource
    @OperateOnDeployment("test-res2")
    private URL res2URL;

    private static URL packageResURL(String res) {
        String path = MultipleFileAccessTestCase.class.getPackage().getName().replace(".", "/") + "/" + res;
        return MultipleFileAccessTestCase.class.getClassLoader().getResource(path);
    }

    @Deployment(name = "test-res1")
    public static Archive<?> deployment1() {
        WebArchive web = ShrinkWrap.create(WebArchive.class, "test-res1.war")
                .addAsWebInfResource(packageResURL("vertx-deployment.json"), "vertx-deployment.json")
                .addAsResource(new StringAsset("{\"name\": \"test-res1\"}"), "config.json")
                .addClasses(ResourceAccessServlet.class, ResourceAccessVerticle.class);
        return web;
    }

    @Deployment(name = "test-res2")
    public static Archive<?> deployment2() {
        WebArchive web = ShrinkWrap.create(WebArchive.class, "test-res2.war")
                .addAsWebInfResource(packageResURL("vertx-deployment.json"), "vertx-deployment.json")
                .addAsResource(new StringAsset("{\"name\": \"test-res2\"}"), "config.json")
                .addClasses(ResourceAccessServlet.class, ResourceAccessVerticle.class);
        return web;
    }

    @Test
    @OperateOnDeployment("test-res2")
    public void testFileAccessInMultipleDeployments() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse res1Resp = client.execute(new HttpGet(res1URL.toString() + "resource-access?res=config.json"));
            Assert.assertEquals(200, res1Resp.getStatusLine().getStatusCode());
            JsonObject configRes1 = new JsonObject(EntityUtils.toString(res1Resp.getEntity()));
            Assert.assertEquals("test-res1", configRes1.getString("name"));

            HttpResponse res2Resp = client.execute(new HttpGet(res2URL.toString() + "resource-access?res=config.json"));
            Assert.assertEquals(200, res2Resp.getStatusLine().getStatusCode());
            JsonObject configRes2 = new JsonObject(EntityUtils.toString(res2Resp.getEntity()));
            Assert.assertEquals("test-res2", configRes2.getString("name"));

        }
    }

}

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

package org.wildfly.extension.vertx.test.mini.deployment;

import io.vertx.core.json.JsonObject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.shared.StreamUtils;
import org.wildfly.extension.vertx.test.shared.VertxSetupTaskBase;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Test accessing resources inside of the deployment using vertx filesystem API.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(VertxSetupTaskBase.class)
@RunAsClient
public class VertxFileSystemTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive web = ShrinkWrap.create(WebArchive.class, "test-vertx-filesystem.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "vertx-deployment.json")
                .addAsResource("assets")
                .addAsResource("geo.json")
                .addClasses(FileAccessServelt.class, StreamUtils.class);
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

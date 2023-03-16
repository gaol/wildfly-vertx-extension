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
package org.wildfly.extension.vertx.test.basic.components.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VertxConfigTestCase {

  @Deployment
  public static Archive<?> deployment() throws Exception {
    WebArchive web = ShrinkWrap.create(WebArchive.class, "test-vertx-config.war")
      .addAsWebInfResource(new StringAsset("{\n" +
        "  \"deployments\": [\n" +
        "    {\n" +
        "      \"verticle-class\": \"org.wildfly.extension.vertx.test.basic.components.config.VertxConfigTestCase$BasicVertxConfigFromFileVerticle\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"), "vertx-deployment.json")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addAsResource(new StringAsset("{\n" +
        "  \"name\": \"lgao\",\n" +
        "  \"year\": 2022\n" +
        "}"), "file-config.json")
      .addClasses(VertxConfigVerticle.class, VertxConfigTestCase.class, BasicVertxConfigFromFileVerticle.class);
    return web;
  }

  public static class BasicVertxConfigFromFileVerticle extends VertxConfigVerticle {

    @Override
    protected List<ConfigStoreOptions> getConfigStores() {
      List<ConfigStoreOptions> options = new ArrayList<>();
      options.add(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "file-config.json")));
      return options;
    }
  }

  @Test
  public void testGetConfig() throws Exception {
    JsonObject json = new JsonObject(HttpRequest.get("http://" + getHost() + ":" + getHttpPort(), 4, TimeUnit.SECONDS));
    Assert.assertEquals("lgao", json.getString("name"));
    Assert.assertEquals(2022, json.getInteger("year").intValue());
  }

  static String getHost() {
    return System.getProperty("vertx.http.host", "localhost");
  }

  static int getHttpPort() {
    return Integer.getInteger("vertx.http.port", 8880);
  }

}

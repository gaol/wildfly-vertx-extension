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
package org.wildfly.extension.vertx.test.basic.components.client.mqtt;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.basic.components.client.ContainerBaseRule;
import org.wildfly.extension.vertx.test.basic.components.client.ContainerBasedTestCase;

import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MQTTClientTestCase extends ContainerBasedTestCase {

  @ArquillianResource
  private URL url;

  @ClassRule
  public static ContainerBaseRule rule = new ContainerBaseRule("ansi/mosquitto", 1883);

  @Deployment
  public static Archive<?> deployment() {
    return ShrinkWrap.create(WebArchive.class, "test-mqtt.war")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addAsWebInfResource(new StringAsset("{\n" +
        "  \"deployments\": [\n" +
        "    {\n" +
        "      \"verticle-class\": \"org.wildfly.extension.vertx.test.basic.components.client.mqtt.MQTTClientVerticle\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"), "vertx-deployment.json")
      .addClasses(ContainerBasedTestCase.class, MQTTClientTestCase.class, MQTTClientServlet.class, MQTTClientVerticle.class);
  }

  @Override
  protected String getContainerConnStr() {
    throw new IllegalStateException("MQTT does not have a connection string, use host and port");
  }

  @Test
  public void testMQTTPublishAndSubscribe() throws Exception {
    final String message = "Hello Vertx MQTT !!";
    String res = HttpRequest.get( url.toExternalForm() + "test-mqtt?host="
      + rule.getContainerIPAddress() + "&port=" + rule.getMappedPort()
      + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8), 10, TimeUnit.SECONDS);
    Assert.assertNotNull(res);
    JsonObject resp = new JsonObject(res);
    Assert.assertNull(resp.getValue("error"));
    Assert.assertEquals(message, resp.getString("response"));
  }

}

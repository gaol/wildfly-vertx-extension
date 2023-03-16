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
package org.wildfly.extension.vertx.test.basic.components.client.mongo;

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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;
import org.wildfly.extension.vertx.test.basic.components.client.AbstractAsyncServlet;
import org.wildfly.extension.vertx.test.basic.components.client.ContainerBaseRule;
import org.wildfly.extension.vertx.test.basic.components.client.ContainerBasedTestCase;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MongoClientTestCase extends ContainerBasedTestCase {

  @ArquillianResource
  private URL url;

  @ClassRule
  public static ContainerBaseRule rule = new ContainerBaseRule("mongo:4.2.1", 27017) {
    @Override
    protected void updateContainerBeforeStart(GenericContainer<?> container) {
      final MountableFile mountableFile = MountableFile.forClasspathResource("init-mongo.js");
      container
        .withEnv("MONGO_INITDB_ROOT_USERNAME", "mongo")
        .withEnv("MONGO_INITDB_ROOT_PASSWORD", "mongo")
        .withEnv("MONGO_INITDB_DATABASE", "exam")
        .withFileSystemBind(mountableFile.getFilesystemPath(), "/docker-entrypoint-initdb.d/init-mongo.js", BindMode.READ_ONLY)
      ;
    }
  };

  @Deployment
  public static Archive<?> deployment() {
    return ShrinkWrap.create(WebArchive.class, "test-mongo.war")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addClasses(AbstractAsyncServlet.class, ContainerBasedTestCase.class, MongoClientTestCase.class, MongoClientServlet.class);
  }

  @Override
  protected String getContainerConnStr() {
    return "mongodb://mongo:mongo@" + rule.getContainerIPAddress() + ":" + rule.getMappedPort();
  }

  @Test
  public void testMongoQuery() throws Exception {
    String res = HttpRequest.get( url.toExternalForm() + "test-mongo?connStr=" + getServiceConnStrEncoded() + "&message=exam", 10, TimeUnit.SECONDS);
    Assert.assertNotNull(res);
    JsonObject resp = new JsonObject(res);
    Assert.assertNull(resp.getValue("error"));
    JsonArray jsonArray = resp.getJsonArray("response");
    Assert.assertEquals(3, jsonArray.size());
    JsonObject score = jsonArray.getJsonObject(0);
    Assert.assertEquals("Math", score.getString("name"));
    Assert.assertEquals(98, score.getInteger("score").intValue());
    score = jsonArray.getJsonObject(1);
    Assert.assertEquals("Chinese Language", score.getString("name"));
    Assert.assertEquals(99, score.getInteger("score").intValue());
    score = jsonArray.getJsonObject(2);
    Assert.assertEquals("English Language", score.getString("name"));
    Assert.assertEquals(100, score.getInteger("score").intValue());
  }

}

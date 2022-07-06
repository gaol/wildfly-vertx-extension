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
package org.wildfly.extension.vertx.test.mini.components.client.sql;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.vertx.core.json.JsonArray;
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
import org.wildfly.extension.vertx.test.mini.components.client.AbstractAsyncServlet;
import org.wildfly.extension.vertx.test.mini.components.client.ContainerBaseRule;
import org.wildfly.extension.vertx.test.mini.components.client.ContainerBasedTestCase;

import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MySQLClientTestCase extends ContainerBasedTestCase {

  @ArquillianResource
  private URL url;

  @ClassRule
  public static ContainerBaseRule rule = new ContainerBaseRule("docker.io/library/mysql:5.7", 3306) {
    @Override
    protected void updateContainerBeforeStart(final GenericContainer<?> container) {
      final MountableFile mountableFile = MountableFile.forClasspathResource("init-mysql.sql");
      container
        .withEnv("MYSQL_USER", "mysql")
        .withEnv("MYSQL_PASSWORD", "password")
        .withEnv("MYSQL_ROOT_PASSWORD", "password")
        .withEnv("MYSQL_DATABASE", "geo")
        .withFileSystemBind(mountableFile.getFilesystemPath(), "/docker-entrypoint-initdb.d/init-mysql.sql", BindMode.READ_ONLY)
      ;
    }
  };

  @Deployment
  public static Archive<?> deployment() {
    return ShrinkWrap.create(WebArchive.class, "test-mysql.war")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addClasses(AbstractAsyncServlet.class, ContainerBasedTestCase.class, MySQLClientTestCase.class, MySQLClientServlet.class);
  }

  @Override
  protected String getContainerConnStr() {
    return "mysql://mysql:password@" + rule.getContainerIPAddress() + ":" + rule.getMappedPort() + "/geo";
  }

  @Test
  public void testMySQLQuery() throws Exception {
    String res = HttpRequest.get( url.toExternalForm() + "test-mysql?connStr=" + getServiceConnStrEncoded(), 10, TimeUnit.SECONDS);
    Assert.assertNotNull(res);
    JsonObject resp = new JsonObject(res);
    Assert.assertNull(resp.getValue("error"));
    JsonArray jsonArray = resp.getJsonArray("response");
    Assert.assertEquals(3, jsonArray.size());
    JsonObject city = jsonArray.getJsonObject(0);
    Assert.assertEquals("Beijing", city.getString("name"));
    Assert.assertEquals("China", city.getString("country"));
    city = jsonArray.getJsonObject(1);
    Assert.assertEquals("London", city.getString("name"));
    Assert.assertEquals("United Kingdom", city.getString("country"));
    city = jsonArray.getJsonObject(2);
    Assert.assertEquals("Brno", city.getString("name"));
    Assert.assertEquals("Czechia", city.getString("country"));
  }

}

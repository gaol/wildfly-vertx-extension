/*
 *  Copyright (c) 2023 The original author or authors
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
package org.wildfly.extension.vertx.test.basic.multiresolving;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.basic.deployment.multiple.MultipleFileAccessTestCase;
import org.wildfly.extension.vertx.test.basic.deployment.multiple.ResourceAccessServlet;
import org.wildfly.extension.vertx.test.shared.AbstractEventBusConsumerVerticle;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultiResolvingTestCase {

  @ArquillianResource
  @OperateOnDeployment("test-war-a")
  private URL res1URL;

  @ArquillianResource
  @OperateOnDeployment("test-war-b")
  private URL res2URL;

  // war a does not expose
  @Deployment(name = "test-war-a")
  public static Archive<?> deploymentA() {
    return ShrinkWrap.create(WebArchive.class, "test-war-a.war")
      .addAsWebInfResource(new StringAsset("{\n" +
        "  \"deployments\": [\n" +
        "    {\n" +
        "      \"verticle-class\": \"org.wildfly.extension.vertx.test.basic.deployment.multiple.MultipleFileAccessTestCase$ResourceAccessVerticle1\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"), "vertx.json")
      .addAsResource(new StringAsset("This can only be accessed in war a"), "private.txt")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addClasses(ResourceAccessServlet.class,
        MultipleFileAccessTestCase.ResourceAccessVerticle1.class,
        AbstractEventBusConsumerVerticle.class,
        ReadResourceServlet.class);
  }

  // war-b has exposed some resources
  @Deployment(name = "test-war-b")
  public static Archive<?> deploymentB() {
    return ShrinkWrap.create(WebArchive.class, "test-war-b.war")
      .addAsWebInfResource(new StringAsset("{\n" +
        "  \"deployments\": [\n" +
        "    {\n" +
        "      \"verticle-class\": \"org.wildfly.extension.vertx.test.basic.deployment.multiple.MultipleFileAccessTestCase$ResourceAccessVerticle2\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"expose\": true,\n" +
        "  \"exposed-resources\": [\"/public/*\", \"/protected-b/anybody.txt\"]\n" +
        "}"), "vertx.json")
      .addAsResource(new StringAsset("<html><body>Public Page</body></html>"), "public/index.html")
      .addAsResource(new StringAsset("anybody can access"), "protected-b/anybody.txt")
      .addAsResource(new StringAsset("only me can access"), "protected-b/secret.txt")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addClasses(ResourceAccessServlet.class,
        MultipleFileAccessTestCase.ResourceAccessVerticle1.class,
        MultipleFileAccessTestCase.ResourceAccessVerticle2.class,
        AbstractEventBusConsumerVerticle.class,
        ReadResourceServlet.class);
  }

  @WebServlet(urlPatterns = "/read-file", asyncSupported = true)
  static class ReadResourceServlet extends HttpServlet {
    @Inject
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String path = req.getParameter("path");
      AsyncContext asyncContext = req.startAsync();
      vertx.fileSystem().readFile(path).map(Buffer::toString).onComplete(r -> {
        try {
          if (r.succeeded()) {
            resp.getWriter().write(r.result());
          } else {
            r.cause().printStackTrace();
            resp.sendError(500, r.cause().getMessage());
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          asyncContext.complete();
        }
      });
    }
  }

  @Test
  @OperateOnDeployment("test-war-b")
  public void testFileAccessInMultipleDeployments() throws Exception {
    // tests read files from war-a's verticle
    testReadFromWarAVerticle();
    // tests read files from war-a's servlet
    testReadFromWarAServlet();

    // tests read files from war-b's verticle
      testReadFromWarBVerticle();
    // tests read files from war-b's servlet
      testReadFromWarBServlet();
  }

  private void testReadFromWarAVerticle() throws IOException {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      String path;
      HttpResponse resp;
      // requests to war-a which reads file from verticle:
      //   - private.txt inside war-a
      path = urlEncodedPath("private.txt");
      resp = client.execute(new HttpGet(res1URL.toString() + "resource-access?addr=res-access-1&res=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("This can only be accessed in war a", EntityUtils.toString(resp.getEntity()));

      //   - public/index.html in war-b
      path = urlEncodedPath("public/index.html");
      resp = client.execute(new HttpGet(res1URL.toString() + "resource-access?addr=res-access-1&res=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("<html><body>Public Page</body></html>", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/anybody.txt in war-b
      path = urlEncodedPath("protected-b/anybody.txt");
      resp = client.execute(new HttpGet(res1URL.toString() + "resource-access?addr=res-access-1&res=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("anybody can access", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/secret.txt in war-b, no, it cannot
      path = urlEncodedPath("protected-b/secret.txt");
      resp = client.execute(new HttpGet(res1URL.toString() + "resource-access?addr=res-access-1&res=" + path));
      Assert.assertEquals(500, resp.getStatusLine().getStatusCode());
    }
  }

  private void testReadFromWarAServlet() throws IOException {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      HttpResponse resp;
      String path;
      // requests to war-a which reads file from servlet:
      //   - private.txt in war-a, which is not exposed, there is no guarantee that you can read it.
      path = urlEncodedPath("private.txt");
      resp = client.execute(new HttpGet(res1URL.toString() + "read-file?path=" + path));
      // it maybe or maybe not read the file in the same archive.
      Assert.assertTrue(resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 500);

      //   - public/index.html in war-b
      path = urlEncodedPath("public/index.html");
      resp = client.execute(new HttpGet(res1URL.toString() + "read-file?path=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("<html><body>Public Page</body></html>", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/anybody.txt in war-b
      path = urlEncodedPath("protected-b/anybody.txt");
      resp = client.execute(new HttpGet(res1URL.toString() + "read-file?path=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("anybody can access", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/secret.txt in war-b, no, it cannot
      path = urlEncodedPath("protected-b/secret.txt");
      resp = client.execute(new HttpGet(res1URL.toString() + "read-file?path=" + path));
      Assert.assertEquals(500, resp.getStatusLine().getStatusCode());
    }
  }

  private void testReadFromWarBVerticle() throws IOException {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      String path;
      HttpResponse resp;
      // requests to war-b which reads file from verticle:
      //   - private.txt inside war-a, no, it should not be able to read.
      path = urlEncodedPath("private.txt");
      resp = client.execute(new HttpGet(res2URL.toString() + "resource-access?addr=res-access-2&res=" + path));
      Assert.assertEquals(500, resp.getStatusLine().getStatusCode());

      //   - public/index.html in war-b
      path = urlEncodedPath("public/index.html");
      resp = client.execute(new HttpGet(res2URL.toString() + "resource-access?addr=res-access-2&res=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("<html><body>Public Page</body></html>", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/anybody.txt in war-b
      path = urlEncodedPath("protected-b/anybody.txt");
      resp = client.execute(new HttpGet(res2URL.toString() + "resource-access?addr=res-access-2&res=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("anybody can access", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/secret.txt in war-b, yes, it works
      path = urlEncodedPath("protected-b/secret.txt");
      resp = client.execute(new HttpGet(res2URL.toString() + "resource-access?addr=res-access-2&res=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("only me can access", EntityUtils.toString(resp.getEntity()));
    }
  }

  private void testReadFromWarBServlet() throws IOException {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      HttpResponse resp;
      String path;
      // requests to war-b which reads file from servlet:
      //   - private.txt in war-a, no, it should not
      path = urlEncodedPath("private.txt");
      resp = client.execute(new HttpGet(res2URL.toString() + "read-file?path=" + path));
      Assert.assertEquals(500, resp.getStatusLine().getStatusCode());

      //   - public/index.html in war-b
      path = urlEncodedPath("public/index.html");
      resp = client.execute(new HttpGet(res2URL.toString() + "read-file?path=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("<html><body>Public Page</body></html>", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/anybody.txt in war-b
      path = urlEncodedPath("protected-b/anybody.txt");
      resp = client.execute(new HttpGet(res2URL.toString() + "read-file?path=" + path));
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      Assert.assertEquals("anybody can access", EntityUtils.toString(resp.getEntity()));

      //   - protected-b/secret.txt in war-b, there is no guarantee.
      path = urlEncodedPath("protected-b/secret.txt");
      resp = client.execute(new HttpGet(res2URL.toString() + "read-file?path=" + path));
      // it maybe or maybe not read the file in the same archive.
      Assert.assertTrue(resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 500);
    }
  }

  private String urlEncodedPath(String path) {
    return URLEncoder.encode(path, StandardCharsets.UTF_8);
  }
}

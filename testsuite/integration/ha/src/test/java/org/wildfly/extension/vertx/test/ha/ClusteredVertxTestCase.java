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
package org.wildfly.extension.vertx.test.ha;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.ext.cluster.infinispan.InfinispanClusterManager;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This is a test covering the case that messages flowing in the event bus between 2 Vertx instances.
 * <p>
 * It starts with a http request to a servlet which will send a string message to address 'inbox', the
 * consumer in client side will be triggered which replies the message so that the server side gets the reply message
 * and responds to the http request with the status check.
 * </p>
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ClusteredVertxTestCase.ClusteredTestSetupTask.class)
@RunAsClient
public class ClusteredVertxTestCase {

  private static Vertx clusteredVertx;

  public static class ClusteredTestSetupTask implements ServerSetupTask {
//    private Vertx clusteredVertx;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
      ClusteredVertxTestCase.setUp();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
      ClusteredVertxTestCase.tearDown();
    }
  }

//  @Before
  public static void setUp() throws Exception {
    Promise<Vertx> promise = Promise.promise();
    VertxOptions vertxOptions = new VertxOptions();
    InfinispanClusterManager clusterManager = new InfinispanClusterManager();
    vertxOptions.setClusterManager(clusterManager);
    System.out.println("\n\nCurrent Thread: " + Thread.currentThread() +
      ", class loader: " + Thread.currentThread().getContextClassLoader() +
      ", class define loader: " + ClusteredVertxTestCase.class.getClassLoader() + "\n\n");
    new VertxBuilder(vertxOptions)
      // default InfinispanClusterManager
      .clusterManager(clusterManager)
      .init()
      .clusteredVertx(promise);
    CompletableFuture<Vertx> vertxFuture = promise.future().toCompletionStage().toCompletableFuture();
    clusteredVertx = vertxFuture.get();
    clusteredVertx.eventBus().<String>consumer("inbox")
      .handler(msg -> msg.reply("Got your message: " + msg.body()));
  }

//  @After
  public static void tearDown() throws Exception {
    // close the clustered Vertx instance
    clusteredVertx.close().toCompletionStage().toCompletableFuture().get();
  }

  @ArquillianResource
  private URL url;

  @Deployment
  public static Archive<?> deployment() {
    return ShrinkWrap.create(WebArchive.class, "test-send-and-check.war")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addPackage(ClusteredVertxTestCase.class.getPackage())
//      .addPackage(HttpRequest.class.getPackage())
      .addClasses(SendMessageAndCheckServlet.class);
  }

  @WebServlet(value = "/sendAndCheck", asyncSupported = true)
  private static class SendMessageAndCheckServlet extends HttpServlet {

    // in ha configuration, it is a clustered vertx instance
    @Inject
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String message = req.getParameter("message") == null || req.getParameter("message").length() == 0 ? "Ni Hao" : req.getParameter("message");
      if (vertx == null) {
        resp.sendError(500, "Vertx instance is null");
        return;
      }
      final AsyncContext asyncContext = req.startAsync();
      vertx.eventBus()
        .request("inbox", message)
        .toCompletionStage()
        .whenComplete((m, e) -> {
          try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
            if (e != null) {
              writer.print(e.getMessage());
            } else {
              if (m.body().equals("Got your message: " + message)) {
                writer.print("OK");
              } else {
                writer.print("Not the one I expected: " + m.body());
              }
            }
          } catch (IOException ioException) {
            ioException.printStackTrace();
          } finally {
            asyncContext.complete();
          }
        });
    }
  }

  @Test
  public void testEchoAsyncServlet() throws Exception {
    String message = "Please check and respond";
    String res = HttpRequest.get( url.toExternalForm() + "sendAndCheck?message=" + URLEncoder.encode(message, "UTF-8"), 4, TimeUnit.SECONDS);
    Assert.assertEquals("OK", res);
  }

}

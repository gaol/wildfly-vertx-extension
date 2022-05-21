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

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
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
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.removeVertxOperation;

/**
 * This is a test covering the case that messages flowing in the event bus between 2 Vertx instances.
 * <p>
 * It is similar with {@link ClusteredVertxTestCase}, but this test case tries to set up a new clustered Vertx instance
 * using a customized <i>test-jgroups-stack.xml</i> file.
 * </p>
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ClusteredVertxJGroupsFileTestCase.ClusteredTestSetupTask.class)
public class ClusteredVertxJGroupsFileTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String CLUSTERED_VERTX_NAME = "clusterVertx";

  public static class ClusteredTestSetupTask extends SnapshotRestoreSetupTask {
    private Vertx clusteredVertx;
    private String tmpFileLoc;

    private static final String JGROUPS_FILE_PING_LOCATION = "jgroups.file.location";
    private static final String JGROUPS_FILE_LOCATION = "test-jgroups-stack.xml";

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
      tmpFileLoc = temporaryFolder.newFolder().getAbsolutePath();
      // remove the default vertx instance
      executeOperation(managementClient, removeVertxOperation("default"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

      System.setProperty("vertx.jgroups.config", JGROUPS_FILE_LOCATION);
      System.setProperty(JGROUPS_FILE_PING_LOCATION, tmpFileLoc);

      // set system property of jgroups.file.location to a temp dir for FILE_PING protocol
      ModelNode operation = new ModelNode();
      operation.get(OP_ADDR).set(new ModelNode().add("system-property", JGROUPS_FILE_PING_LOCATION));
      operation.get(OP).set(ADD);
      operation.get(VALUE).set(tmpFileLoc);
      executeOperation(managementClient, operation);

      // setup vertx with jgroups-stack-file=test-jgroups-stack.xml
      ModelNode addVertxOperation = addVertxOperation(CLUSTERED_VERTX_NAME);
      addVertxOperation.get("clustered").set(true);
      addVertxOperation.get("jgroups-stack-file").set(Paths.get(getClass().getClassLoader().getResource(JGROUPS_FILE_LOCATION).toURI()).toString());
      executeOperation(managementClient, addVertxOperation);

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

      InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
      Promise<Vertx> promise = Promise.promise();
      VertxOptions vertxOptions = new VertxOptions();
      InfinispanClusterManager clusterManager = new InfinispanClusterManager();
      vertxOptions.setClusterManager(clusterManager);
      new VertxBuilder(vertxOptions)
        // default InfinispanClusterManager
        .clusterManager(clusterManager)
        .init()
        .clusteredVertx(promise);
      promise.future().flatMap(v -> {
        clusteredVertx = v;
        Promise<Void> p = Promise.promise();
        clusteredVertx.eventBus().<String>consumer("inbox")
          .handler(msg -> msg.reply("Got your message: " + msg.body())).completionHandler(p);
        return p.future();
      }).toCompletionStage().toCompletableFuture().get();
    }

    @Override
    public void nonManagementCleanUp() throws Exception {
      System.clearProperty("vertx.jgroups.config");
      System.clearProperty(JGROUPS_FILE_PING_LOCATION);
      clusteredVertx.close().toCompletionStage().toCompletableFuture().get();
    }
  }

  @ArquillianResource
  private URL url;

  @Deployment
  public static Archive<?> deployment() throws Exception {
    return ShrinkWrap.create(WebArchive.class, "test-send-and-check-jgroups-stack-file.war")
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
      .addClasses(ClusteredVertxJGroupsFileTestCase.class, SendMessageAndCheckServlet.class);
  }

  @WebServlet(value = "/sendAndCheck", asyncSupported = true)
  private static class SendMessageAndCheckServlet extends HttpServlet {

    // in ha configuration, it is a clustered vertx instance
    @Inject
    private Vertx clusterVertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String message = req.getParameter("message") == null || req.getParameter("message").length() == 0 ? "Ni Hao" : req.getParameter("message");
      final AsyncContext asyncContext = req.startAsync();
      clusterVertx.eventBus()
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

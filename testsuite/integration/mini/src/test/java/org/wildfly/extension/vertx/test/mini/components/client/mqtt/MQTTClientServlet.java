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
package org.wildfly.extension.vertx.test.mini.components.client.mqtt;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@WebServlet(value = "/test-mqtt", asyncSupported = true)
public class MQTTClientServlet extends HttpServlet {

  final static String EVENTBUS_ADDR = "mqtt-test";

    @Inject
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      final AsyncContext asyncContext = req.startAsync();
      String host = req.getParameter("host");
      String message = req.getParameter("message");
      int port = Integer.parseInt(req.getParameter("port"));
      vertx.eventBus().<Buffer>request(EVENTBUS_ADDR, new JsonObject()
        .put("host", host)
        .put("port", port)
        .put("message", message))
        .onComplete(result -> {
          JsonObject respContent = new JsonObject();
          if (result.succeeded()) {
            respContent.put("response", result.result().body().toString());
          } else {
            respContent.put("error", result.cause().getMessage());
          }
          resp.setContentType("application/json");
          try (PrintWriter writer = resp.getWriter()) {
            writer.print(respContent);
          } catch (IOException ioException) {
            ioException.printStackTrace();
          } finally {
            asyncContext.complete();
          }
        });
    }
}

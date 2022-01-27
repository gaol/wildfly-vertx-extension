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
package org.wildfly.extension.vertx.test.mini.components.client;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(value = "/test-redis", asyncSupported = true)
public class RedisMessageServlet extends HttpServlet {

    @Resource(name = "java:/vertx/default")
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final AsyncContext asyncContext = req.startAsync();
        resp.setContentType("application/json");
        String host = req.getParameter("host");
        int port = Integer.parseInt(req.getParameter("port"));
        String connStr = "redis://" + host + ":" + port;
        String message = req.getParameter("message");
        final Redis redis = Redis.createClient(vertx, connStr);
        redis.send(Request.cmd(Command.ECHO).arg(message)).onComplete(result -> {
            JsonObject respContent = new JsonObject();
            if (result.succeeded()) {
                respContent.put("response", result.result().toString());
            } else {
                respContent.put("error", result.cause().getMessage());
            }
            try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
                writer.print(respContent);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                redis.close();
                asyncContext.complete();
            }
        });
    }

}

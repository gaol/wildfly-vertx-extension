/*
 *  Copyright (c) 2020 - 2021 The original author or authors
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
package org.wildfly.extension.vertx.test.shared.servlet;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

/**
 * AsyncServlet which requests a response from `echo` Vert.x EventBus address.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@WebServlet(value = "/async", asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    @Resource(name = "java:/vertx/default")
    private Vertx vertx;

    private MessageConsumer<String> consumer;

    @Override
    public void init() throws ServletException {
        consumer = vertx.eventBus()
                .<String>localConsumer("echo")
                .handler(msg -> msg.reply(msg.body()));
    }

    @Override
    public void destroy() {
        if (consumer != null) {
            try {
                consumer.unregister().toCompletionStage().toCompletableFuture().get();
                consumer = null;
            } catch (Exception e) {
                ; //ignore
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String message = req.getParameter("message") == null || req.getParameter("message").length() == 0 ? "Ni Hao" : req.getParameter("message");
        final AsyncContext asyncContext = req.startAsync();
        vertx.eventBus()
                .request("echo", message)
                .toCompletionStage()
                .whenComplete((m, e) -> {
                    try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
                        if (e != null) {
                            writer.print(e.getMessage());
                        } else {
                            writer.print("" + m.body());
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    } finally {
                        asyncContext.complete();
                    }
                });
    }
}

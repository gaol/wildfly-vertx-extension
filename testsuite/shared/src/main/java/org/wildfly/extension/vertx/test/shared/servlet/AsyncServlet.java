/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.shared.servlet;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * AsyncServlet which requests a response from `echo` Vert.x EventBus address.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@WebServlet(value = "/async", asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    @Inject
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
                //ignore
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

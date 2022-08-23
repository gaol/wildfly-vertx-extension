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
package org.wildfly.extension.vertx.examples.ejb;

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
@WebServlet(value = "/echo", asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    @Inject
    private Vertx vertx;

    private MessageConsumer<String> consumer;

    @Override
    public void init() throws ServletException {
        consumer = vertx.eventBus()
                .<String>consumer("echo")
                .handler(msg -> msg.reply(msg.body()));
    }

    @Override
    public void destroy() {
        if (consumer != null) {
            consumer.unregister();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String message = req.getParameter("message") == null ? "Hello" : req.getParameter("message");
        final AsyncContext asyncContext = req.startAsync();
        PrintWriter writer = resp.getWriter();
          vertx.eventBus()
                .request("echo", message)
                .onComplete(r -> {
                    if (r.succeeded()) {
                        writer.print("" + r.result().body());
                    } else {
                        writer.print(r.cause().getMessage());
                    }
                    asyncContext.complete();
                });
    }
}

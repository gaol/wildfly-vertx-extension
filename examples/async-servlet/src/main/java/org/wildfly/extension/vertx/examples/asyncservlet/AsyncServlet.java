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
package org.wildfly.extension.vertx.examples.asyncservlet;

import io.vertx.core.Vertx;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * AsyncServlet which requests a response from `echo` Vert.x EventBus address.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@WebServlet(value = "/async", asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    @Resource(name = "default-vertx")
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String message = req.getParameter("message") == null || req.getParameter("message") == "" ? "Ni Hao" : req.getParameter("message");
        final AsyncContext asyncContext = req.startAsync();
        vertx.eventBus()
                .request("echo", "Hello from Servlet: " + message)
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

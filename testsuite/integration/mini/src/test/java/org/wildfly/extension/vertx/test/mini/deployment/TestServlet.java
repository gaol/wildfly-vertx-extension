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
package org.wildfly.extension.vertx.test.mini.deployment;

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

@WebServlet(value = "/test", asyncSupported = true)
public class TestServlet extends HttpServlet {

    @Resource(name = "java:/vertx/default")
    private Vertx vertx;

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

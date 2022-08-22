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
package org.wildfly.extension.vertx.test.mini.deployment.multiple;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.annotation.Resource;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.vertx.core.Vertx;

/**
 * A servlet that uses Vert.x filesystem API to read resources from bundle.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@WebServlet(value = "/resource-access", asyncSupported = true)
public class ResourceAccessServlet extends HttpServlet {

    @Resource(name = "java:/vertx/default")
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String res = req.getParameter("res");
        if (res == null || res.length() == 0) {
            res = "config.json";
        }
        String addr = req.getParameter("addr");
        final AsyncContext asyncContext = req.startAsync();
        vertx.eventBus().<String>request(addr, res)
                .onComplete(result -> {
                    if (result.succeeded()) {
                        try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
                            writer.print(result.result().body());
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } finally {
                            asyncContext.complete();
                        }
                    } else {
                        try {
                            resp.sendError(500, result.cause().getMessage());
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } finally {
                            asyncContext.complete();
                        }
                    }
                });
    }

}

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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.wildfly.extension.vertx.test.shared.StreamUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A servlet that uses Vert.x filesystem API to read resources from bundle.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@WebServlet(value = "/file-access", asyncSupported = true)
public class FileAccessServelt extends HttpServlet {

    @Resource(name = "java:/vertx/vertx-test")
    private Vertx vertx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        final JsonObject json = new JsonObject();
        final AsyncContext asyncContext = req.startAsync();
        vertx.fileSystem().readFile("geo.json").flatMap(buffer -> {
            json.put("geo.json", new JsonObject(buffer));
            return vertx.fileSystem().readFile("assets/config.properties");
        }).flatMap(config -> {
            json.put("config", StreamUtils.stringToProperties(config.toString()));
            return Future.succeededFuture(json);
        }).onComplete(result -> {
            if (result.failed()) {
                json.put("error", result.cause().getMessage());
            }
            try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
                writer.print(json);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                asyncContext.complete();
            }
        });
    }

}

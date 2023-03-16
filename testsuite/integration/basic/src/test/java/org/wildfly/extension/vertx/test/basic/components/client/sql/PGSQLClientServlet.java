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
package org.wildfly.extension.vertx.test.basic.components.client.sql;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.annotation.WebServlet;

import io.vertx.pgclient.PgConnection;
import org.wildfly.extension.vertx.test.basic.components.client.AbstractAsyncServlet;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@WebServlet(value = "/test-pgsql", asyncSupported = true)
public class PGSQLClientServlet extends AbstractAsyncServlet {

    @Inject
    private Vertx vertx;

    @Override
    protected void doExecute(AsyncContext asyncContext, String connStr, String message) {
        PgConnection.connect(vertx, connStr)
          .flatMap(conn -> conn.preparedQuery("SELECT * FROM cities ORDER BY id").execute())
          .onComplete(result -> {
              JsonObject respContent = new JsonObject();
              if (result.succeeded()) {
                  JsonArray jsonArray = new JsonArray();
                  result.result().forEach(row -> jsonArray.add(row.toJson()));
                  respContent.put("response", jsonArray);
              } else {
                  result.cause().printStackTrace();
                  respContent.put("error", result.cause().getMessage());
              }
              asyncContext.getResponse().setContentType("application/json");
              try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
                  writer.print(respContent);
              } catch (IOException ioException) {
                  ioException.printStackTrace();
              } finally {
                  asyncContext.complete();
              }
          })
        ;
    }
}

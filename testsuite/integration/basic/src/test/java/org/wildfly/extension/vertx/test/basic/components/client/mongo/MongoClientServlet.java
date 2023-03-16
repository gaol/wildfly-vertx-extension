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
package org.wildfly.extension.vertx.test.basic.components.client.mongo;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.annotation.WebServlet;

import org.wildfly.extension.vertx.test.basic.components.client.AbstractAsyncServlet;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

@WebServlet(value = "/test-mongo", asyncSupported = true)
public class MongoClientServlet extends AbstractAsyncServlet {

    @Inject
    private Vertx vertx;

    @Override
    protected void doExecute(AsyncContext asyncContext, String connStr, String message) {
      // message in this case is used as the db_name
      JsonObject config = new JsonObject().put("connection_string", connStr).put("db_name", message);
      MongoClient mongoClient = MongoClient.create(vertx, config);
      final String collection = "exam";
      mongoClient.findWithOptions(collection, new JsonObject(), new FindOptions()
        .setSort(new JsonObject().put("score", 1))
        .setFields(new JsonObject().put("_id", false).put("name", true).put("score", true))).onComplete(result -> {
        JsonObject respContent = new JsonObject();
        if (result.succeeded()) {
          JsonArray jsonArray = new JsonArray();
          result.result().forEach(jsonArray::add);
          respContent.put("response", jsonArray);
        } else {
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
      }).flatMap(v -> mongoClient.close());
    }
}

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

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;

@WebServlet(value = "/test-redis", asyncSupported = true)
public class RedisMessageServlet extends AbstractAsyncServlet {

    @Resource(name = "java:/vertx/default")
    private Vertx vertx;

    @Override
    protected void doExecute(AsyncContext asyncContext, String connStr, String message) {
        final Redis redis = Redis.createClient(vertx, connStr);
        redis.send(Request.cmd(Command.ECHO).arg(message)).onComplete(result -> {
            JsonObject respContent = new JsonObject();
            if (result.succeeded()) {
                respContent.put("response", result.result().toString());
            } else {
                respContent.put("error", result.cause().getMessage());
            }
            asyncContext.getResponse().setContentType("application/json");
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

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
package org.wildfly.extension.vertx.test.mini.components.config;

import java.util.Collections;
import java.util.List;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;

import static org.wildfly.extension.vertx.test.mini.components.config.VertxConfigTestCase.getHost;
import static org.wildfly.extension.vertx.test.mini.components.config.VertxConfigTestCase.getHttpPort;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxConfigVerticle extends AbstractVerticle {


  private ConfigRetriever retriever;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions()
      .setIncludeDefaultStores(false);
    List<ConfigStoreOptions> stores = getConfigStores();
    if (stores != null) {
      stores.forEach(retrieverOptions::addStore);
    }
    retriever = ConfigRetriever.create(vertx, retrieverOptions);
    vertx.createHttpServer().requestHandler(req -> retriever.getConfig().onComplete(config -> {
      HttpServerResponse response = req.response();
      if (config.succeeded()) {
        response
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json")
          .end(config.result().toBuffer());
      } else {
        config.cause().printStackTrace();
        response.setStatusCode(500).setStatusMessage(config.cause().getMessage()).end("failed");
      }
    }))
      .listen(getHttpPort(), getHost())
      .flatMap(server -> {
        System.out.println(getClass() + ":  Http Server is listening on: " + server.actualPort());
        return Future.succeededFuture((Void)null);
      })
      .onComplete(startPromise);

  }

  @Override
  public void stop() throws Exception {
    // the created HttpServer will be closed automatically when this verticle gets un-deployed.
    if (retriever != null) {
      retriever.close();
    }
  }

  protected List<ConfigStoreOptions> getConfigStores() {
    return Collections.emptyList();
  }
}

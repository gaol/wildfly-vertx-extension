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
package org.wildfly.extension.vertx.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class ExampleVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.route().handler(rc -> rc.end("Hello World\n"));
    vertx.createHttpServer()
      .exceptionHandler(Throwable::printStackTrace)
      .requestHandler(router)
      .listen(Integer.getInteger("vertx.http.port", 8880))
      .flatMap(server -> {
        System.out.println(getClass() + ":  Http Server is listening on: " + server.actualPort());
        return Future.succeededFuture((Void)null);
      })
      .onComplete(startPromise);

  }

}

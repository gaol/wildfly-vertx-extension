/*
 *  Copyright (c) 2023 The original author or authors
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
package org.wildfly.extension.vertx.test.shared;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class AbstractEventBusConsumerVerticle<S, R> extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.eventBus().<S>consumer(address())
      .handler(this::handleMessage)
      .completionHandler(startPromise);
    doInit();
  }

  protected void doInit() {
  }

  protected void handleMessage(Message<S> message) {
    if (needsReply()) {
      responseOf(message.body()).onComplete(r -> {
        if (r.succeeded()) {
          message.reply(r.result());
        } else {
          message.fail(500, r.cause().getMessage());
        }
      });
    } else {
      System.out.println("Got message: " + message.body());
    }
  }

  protected abstract Future<R> responseOf(S body);

  protected abstract String address();

  protected boolean needsReply() {
    return true;
  }

}

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
package org.wildfly.extension.vertx.test.shared.ejb;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Stateless
public class EchoService {
    @Resource(name = "java:/vertx/default")
    private Vertx vertx;

    private MessageConsumer<String> consumer;

    @PostConstruct
    void setUpConsumer() {
        consumer = vertx.eventBus()
                .<String>localConsumer("echo")
                .handler(msg -> msg.reply(msg.body()));
    }

    @PreDestroy
    void tearDown() {
        if (consumer != null) {
            consumer.unregister();
        }
    }

    @Asynchronous
    public Future<String> echo(String message) {
        return (CompletableFuture<String>)vertx.eventBus().request("echo", message).map(msg -> msg.body().toString()).toCompletionStage();
    }

}

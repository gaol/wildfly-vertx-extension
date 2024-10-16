/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.shared.ejb;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import static org.wildfly.extension.vertx.VertxConstants.CDI_QUALIFIER;

@Stateless
public class EchoService {

    @Any
    @Identifier(CDI_QUALIFIER)
    @Inject
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

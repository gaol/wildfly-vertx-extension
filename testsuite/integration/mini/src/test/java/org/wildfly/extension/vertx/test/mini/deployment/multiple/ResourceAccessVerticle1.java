package org.wildfly.extension.vertx.test.mini.deployment.multiple;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;

public class ResourceAccessVerticle1 extends AbstractVerticle {

    private MessageConsumer<String> consumer;

    @Override
    public void start() throws Exception {
        consumer = vertx.eventBus()
                .<String>localConsumer("res-access-1")
                .handler(msg -> {
                    vertx.fileSystem().readFile(msg.body()).onComplete(result -> {
                        if (result.succeeded()) {
                            msg.reply(result.result().toString());
                        } else {
                            msg.fail(500, result.cause().getMessage());
                        }
                    });
                });
    }

    @Override
    public void stop() throws Exception {
        if (consumer != null) {
            consumer.unregister();
        }
    }

}

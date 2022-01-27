package org.wildfly.extension.vertx.test.mini.components.client;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.redis.client.Redis;

public class RedisVerticle extends AbstractVerticle {

    private Redis redis;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
    }

    @Override
    public void stop() throws Exception {
        if (redis != null) {
            redis.close();
        }
    }
}

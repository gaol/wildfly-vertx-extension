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

package org.wildfly.extension.vertx.test.basic.deployment;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestVerticle extends AbstractVerticle {

    private MessageConsumer<String> consumer;

    private static final AtomicBoolean deployedOnce = new AtomicBoolean(false);

    @Override
    public void start() throws Exception {
        if (deployedOnce.compareAndSet(false, true)) {
            consumer = vertx.eventBus()
                    .<String>localConsumer("echo")
                    .handler(msg -> msg.reply(msg.body()));
        } else {
            throw new IllegalStateException("This verticle can only be deployed once.");
        }
    }

    @Override
    public void stop() throws Exception {
        if (deployedOnce.compareAndSet(true, false)) {
            if (consumer != null) {
                consumer.unregister();
            }
        } else {
            throw new IllegalStateException("This verticle should be deployed once.");
        }
    }
}

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

package org.wildfly.extension.vertx.test.mini.components.client.mqtt;

import static org.wildfly.extension.vertx.test.mini.components.client.mqtt.MQTTClientServlet.EVENTBUS_ADDR;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MQTTClientVerticle extends AbstractVerticle {

    private final static String TOPIC = "/hello";

    private MessageConsumer<JsonObject> consumer;
    private MqttClient client;

    @Override
    public void start() throws Exception {
        client = MqttClient.create(vertx);
        consumer = vertx.eventBus().<JsonObject> localConsumer(EVENTBUS_ADDR).handler(msg -> {
            JsonObject body = msg.body();
            String host = body.getString("host");
            int port = body.getInteger("port");
            String message = body.getString("message");
            System.out.println("Get message::: host= " + host + ", port: " + port + ", message: " + message);
//            client.publishHandler(m -> {
//                System.out.println("\n =========  \n, Got payload:: " + m.payload());
//                msg.reply(m.payload());
//                client.disconnect();
//            });
            client.pingResponseHandler(m -> {
                System.out.println("\n =========  \n, Got payload:: ");
                msg.reply(Buffer.buffer("got it!!"));
                  client.disconnect().onComplete(r -> {
                    if (r.succeeded()) {
                        System.out.println("good");
                    } else {
                        r.cause().printStackTrace();
                    }
                });
            }).connect(port, host)
//              .flatMap(i -> client.subscribe(TOPIC, 1))
//              .flatMap(i -> client.publish(TOPIC,
//                    Buffer.buffer(message), MqttQoS.AT_LEAST_ONCE, false, false));
            ;
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        consumer.unregister().onComplete(stopPromise);
    }
}

/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

/**
 * Each VertxDefinition represents a Vert.x instance.
 */
public class VertxDefinition extends PersistentResourceDefinition {

    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    static final RuntimeCapability<Void> VERTX_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME, true, VertxProxy.class)
                    .setDynamicNameMapper(DynamicNameMappers.PARENT)
                    .build();

    static VertxDefinition INSTANCE = new VertxDefinition();

    VertxDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement("vertx"),
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(new VertxResourceAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(VERTX_RUNTIME_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return VertxAttributes.getSimpleAttributes();
    }

    static class VertxResourceAdd extends AbstractAddStepHandler {
        VertxResourceAdd() {
            super(new Parameters()
                    .addAttribute(VertxAttributes.getSimpleAttributes())
            );
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            final String name = context.getCurrentAddressValue();
            final String jndiName = VertxAttributes.JNDI_NAME.resolveModelAttribute(context, operation).asString();
            final String vertxOptionsFile = VertxAttributes.VERTX_OPTIONS_FILE.resolveModelAttribute(context, operation).asString();
            try {
                final VertxProxy vertxProxy = new VertxProxy();
                vertxProxy.setJndiName(jndiName);
                vertxProxy.setName(name);
                final VertxOptions vertxOptions;
                if (vertxOptionsFile == null) {
                    vertxOptions = new VertxOptions();
                } else {
                    JsonObject json = readJsonFromFile(vertxOptionsFile);
                    vertxProxy.setVertxOptionsFile(vertxOptionsFile);
                    vertxOptions = new VertxOptions(json);
                }
                vertxProxy.setOptions(vertxOptions);
                Vertx coreVertx = Vertx.vertx(vertxOptions);
                VertxDelegate vertxDelegate = new VertxDelegate(coreVertx);
                vertxProxy.setVertx(vertxDelegate);
                VertxProxyService.installService(context, vertxProxy);
            } catch (Exception e) {
                throw VERTX_LOGGER.failedToStartVertx(name, e);
            }
        }

        private JsonObject readJsonFromFile(String vertxOptionsFile) throws IOException {
            URL optionURL = null;
            String opProp = SecurityActions.getSystemProperty(VertxConstants.VERTX_OPTIONS_URL);
            if (opProp != null) {
                // load from system property
                try {
                    optionURL = new URL(opProp);
                } catch (MalformedURLException e) {
                    VERTX_LOGGER.warn("Wrong VertxOptions URL specified.", e);
                }
            } else {
                if (vertxOptionsFile == null || vertxOptionsFile.trim().equals("")) {
                    optionURL = getClass().getClassLoader().getResource("default-vertx-options.json");
                } else {
                    try {
                        optionURL = new URL(vertxOptionsFile);
                    } catch (MalformedURLException e) {
                        VERTX_LOGGER.debug("not a valid URL, try to lookup class path");
                        optionURL = getClass().getClassLoader().getResource(vertxOptionsFile);
                    }
                }
            }
            if (optionURL == null) {
                return new JsonObject();
            }
            String jsonContent;
            try (InputStream inputStream = optionURL.openStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                StreamUtils.copyStream(inputStream, outputStream);
                jsonContent = outputStream.toString();
            }
            if (jsonContent != null) {
                return new JsonObject(jsonContent);
            }
            return new JsonObject();
        }
    }

}

/*
 *  Copyright (c) 2021 The original author or authors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.vertx.VertxConstants;

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

/**
 * Utilities for ManagementClient.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public final class ManagementClientUtils {
    private ManagementClientUtils() {
        // ignore
    }

    /**
     * Returns the vertx address, like: /subsystem=vertx/vertx=xxx
     *
     * @param name the vertx name
     * @return the management address of the specified Vertx
     */
    public static ModelNode vertxPath(String name) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX).add(VertxConstants.ELEMENT_VERTX, name);
        address.protect();
        return address;
    }

    /**
     * Returns the vertx-option address, like: /subsystem=vertx/vertx-option=vo
     *
     * @param optionName the option name
     * @return the management address of the specified vertx-option
     */
    public static ModelNode vertxOptionPath(String optionName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX).add(VertxConstants.ELEMENT_VERTX_OPTION, optionName);
        address.protect();
        return address;
    }

    public static ModelNode addressResolverOptionBase(String addressOptionName, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER, addressOptionName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode eventBusOptionBase(String eventBusName, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_VERTX_EVENTBUS, eventBusName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode keyStoreOptionBase(String keyStoreOptionName, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_KEY_STORE, keyStoreOptionName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode vertxOptionOperationBase(String optionName, String operationName) {
        final ModelNode address = vertxOptionPath(optionName);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode pemKeyCertOptionBase(String pemKeyCertOptionName, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_PEM_KEY_CERT, pemKeyCertOptionName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode pemTrustOptionBase(String pemTrustOptionName, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_PEM_TRUST, pemTrustOptionName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode clusterNodeMetaOptionBase(String clusterNodeMeta, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_CLUSTER_NODE_METADATA, clusterNodeMeta);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static VertxOptions readVertxOptions(final ManagementClient managementClient, String optionName) throws IOException {
        ModelNode result = executeOperation(managementClient, vertxOptionOperationBase(optionName, "show-info")).get(RESULT);
        JsonObject json = new JsonObject(result.toJSONString(true));
        if (json.containsKey("eventBusOptions")) {
            JsonObject eventBusOption = json.getJsonObject("eventBusOptions");
            if (eventBusOption.containsKey("clusterPublicPort") && eventBusOption.getInteger("clusterPublicPort") == -1) {
                eventBusOption.put("clusterPublicPort", 0);
            }
        }
        return new VertxOptions(json);
    }

    public static ModelNode vertxOperationBase(String name, String operationName) {
        final ModelNode address = vertxPath(name);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode readVertxOptionOperation(String optionName) {
        final ModelNode operation = vertxOptionOperationBase(optionName, READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        return operation;
    }

    public static ModelNode readVertxOperation(String name) {
        final ModelNode operation = vertxOperationBase(name, READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        return operation;
    }

    public static ModelNode addVertxOperation(String name) {
        return vertxOperationBase(name, ADD);
    }

    public static ModelNode removeVertxOperation(String name) {
        return vertxOperationBase(name, REMOVE);
    }

    public static ModelNode listVertxOperation() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "vertx");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("list-vertx");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static String serverConfigDir(ManagementClient managementClient) throws IOException {
        ModelNode address = new ModelNode();
        address.add(CORE_SERVICE, "server-environment");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = executeOperation(managementClient, operation).get(RESULT);
        return result.get("config-dir").asString();
    }

    public static boolean isReloadRequired(final ManagementClient managementClient) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(NAME).set("server-state");
        ModelNode result = executeOperation(managementClient, operation).get(RESULT);
        return result.asString().equals(RELOAD_REQUIRED);
    }

    public static ModelNode executeOperation(final ManagementClient managementClient, final ModelNode operation)
            throws IOException {
        return executeOperation(managementClient, operation, true);
    }

    public static ModelNode executeOperation(final ManagementClient managementClient, final ModelNode operation, boolean exceptionOnFailure)
      throws IOException {
        final ModelNode result = managementClient.getControllerClient().execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(
          result.get(ClientConstants.OUTCOME).asString())) {
            return result;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            if (exceptionOnFailure) {
                throw new RuntimeException(failureDesc);
            } else {
                return result.get(ClientConstants.FAILURE_DESCRIPTION);
            }
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }
    }

}

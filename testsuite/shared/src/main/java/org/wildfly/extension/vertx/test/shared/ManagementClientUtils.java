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

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.vertx.VertxConstants;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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
     * Returns the vertx address, /subsystem=vertx/service=vertx
     *
     * @return the management address of the specified Vertx
     */
    public static ModelNode vertxAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX).add(VertxConstants.VERTX_SERVICE, VertxConstants.ELEMENT_VERTX);
        address.protect();
        return address;
    }

    /**
     * Returns the vertx-option address, like: /subsystem=vertx/vertx-option=vo
     *
     * @param optionName the option name
     * @return the management address of the specified vertx-option
     */
    public static ModelNode vertxOptionAddress(String optionName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX).add(VertxConstants.ELEMENT_VERTX_OPTION, optionName);
        address.protect();
        return address;
    }

    public static ModelNode addressResolverOperation(String addressOptionName, String operationName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, VertxConstants.ELEMENT_VERTX)
          .add(VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER, addressOptionName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode vertxOptionOperation(String optionName, String operationName) {
        final ModelNode address = vertxOptionAddress(optionName);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static VertxOptions readVertxOptions(final ManagementClient managementClient, String optionName) throws IOException {
        ModelNode result = executeOperation(managementClient, vertxOptionOperation(optionName, "show-info")).get(RESULT);
        JsonObject json = new JsonObject(result.toJSONString(true));
        return new VertxOptions(json);
    }

    public static ModelNode vertxOperation(String operationName) {
        final ModelNode address = vertxAddress();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode readVertxOptionOperation(String optionName) {
        final ModelNode operation = vertxOptionOperation(optionName, READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        return operation;
    }

    public static ModelNode readVertxOperation() {
        final ModelNode operation = vertxOperation(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
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

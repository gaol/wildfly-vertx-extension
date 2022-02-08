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

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
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

    public static ModelNode vertxPath(String name) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "vertx").add("vertx", name);
        address.protect();
        return address;
    }

    public static ModelNode vertxOperationBase(String name, String operationName) {
        final ModelNode address = vertxPath(name);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(address);
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

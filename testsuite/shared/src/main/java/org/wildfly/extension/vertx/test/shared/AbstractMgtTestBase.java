/*
 *  Copyright (c) 2023 The original author or authors
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

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_CLUSTERED;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_JGROUPS_STACK_FILE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_OPTION_NAME;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class AbstractMgtTestBase {

  @ContainerResource
  protected ManagementClient managementClient;

  protected void setVertxOption(String optionName) throws Exception {
    ModelNode op = ManagementClientUtils.vertxOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
    op.get(ModelDescriptionConstants.NAME).set(ATTR_OPTION_NAME);
    op.get(ModelDescriptionConstants.VALUE).set(optionName);
    executeOperation(managementClient, op);
    reload();
  }

  protected void unSetVertxOption() throws Exception {
    ModelNode op = ManagementClientUtils.vertxOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
    op.get(ModelDescriptionConstants.NAME).set(ATTR_OPTION_NAME);
    executeOperation(managementClient, op);
    reload();
  }

  protected static void updateVertx(ManagementClient managementClient, boolean clustered, String jgroupsStackFile) throws Exception {
    ModelNode address = ManagementClientUtils.vertxAddress();
    ModelNode batch = new ModelNode();
    batch.get(OP).set(COMPOSITE);
    batch.get(OP_ADDR).setEmptyList();

    ModelNode writeAttributeOp = ManagementClientUtils.vertxOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
    writeAttributeOp.get(ModelDescriptionConstants.NAME).set(ATTR_CLUSTERED);
    writeAttributeOp.get(ModelDescriptionConstants.VALUE).set(clustered);
    batch.get(STEPS).add(writeAttributeOp);
    if (jgroupsStackFile != null) {
      writeAttributeOp = ManagementClientUtils.vertxOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
      writeAttributeOp.get(ModelDescriptionConstants.NAME).set(ATTR_JGROUPS_STACK_FILE);
      writeAttributeOp.get(ModelDescriptionConstants.VALUE).set(jgroupsStackFile);
      batch.get(STEPS).add(writeAttributeOp);
    }
    executeOperation(managementClient, batch);
    ServerReload.executeReloadAndWaitForCompletion(managementClient);
  }

  protected void reload() throws Exception {
    ServerReload.executeReloadAndWaitForCompletion(managementClient);
  }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.shared;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

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

  protected void reload() throws Exception {
    ServerReload.executeReloadAndWaitForCompletion(managementClient);
  }
}

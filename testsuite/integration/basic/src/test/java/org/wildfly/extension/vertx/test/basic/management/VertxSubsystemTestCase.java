/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.basic.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOperation;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class VertxSubsystemTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testReadDefault() throws IOException {
        ModelNode response = executeOperation(managementClient, readVertxOperation());
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
    }

}

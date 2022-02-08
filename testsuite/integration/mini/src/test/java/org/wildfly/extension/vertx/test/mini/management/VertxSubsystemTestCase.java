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
package org.wildfly.extension.vertx.test.mini.management;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.vertx.VertxConstants.DEFAULT_JNDI_PREFIX;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.listVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.removeVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.vertxOperationBase;

@RunWith(Arquillian.class)
@RunAsClient
public class VertxSubsystemTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testReadDefault() throws IOException {
        final String vertxName = "default";
        ModelNode response = executeOperation(managementClient, readVertxOperation(vertxName));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("clustered").asBoolean());
        Assert.assertFalse(result.get("forked-channel").asBoolean());
        Assert.assertFalse(result.get("jgroups-channel").isDefined());
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + vertxName, result.get("jndi-name").asString());
    }

    @Test
    public void testAddVertx() throws IOException {
        final String vertxName = "vertx2";
        ModelNode response = executeOperation(managementClient, addVertxOperation(vertxName));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);

        response = executeOperation(managementClient, readVertxOperation(vertxName));
        result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("clustered").asBoolean());
        Assert.assertFalse(result.get("forked-channel").asBoolean());
        Assert.assertFalse(result.get("jgroups-channel").isDefined());
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + vertxName, result.get("jndi-name").asString());

        executeOperation(managementClient, removeVertxOperation(vertxName));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Test
    public void testAddVertxWithJNDINameSpecified() throws IOException {
        final String vertxName = "vertx-jndi";
        final String jndibBindingName = "vertx-jndi-binding-name";
        ModelNode operation = addVertxOperation(vertxName);
        operation.get("jndi-name").set(DEFAULT_JNDI_PREFIX + jndibBindingName);
        ModelNode response = executeOperation(managementClient, operation);
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);

        response = executeOperation(managementClient, readVertxOperation(vertxName));
        result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("clustered").asBoolean());
        Assert.assertFalse(result.get("forked-channel").asBoolean());
        Assert.assertFalse(result.get("jgroups-channel").isDefined());
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + jndibBindingName, result.get("jndi-name").asString());

        // update the jndi-name
        operation = vertxOperationBase(vertxName, "write-attribute");
        operation.get("name").set("jndi-name");
        operation.get("value").set(DEFAULT_JNDI_PREFIX + "different-jndi-name");
        response = executeOperation(managementClient, operation);
        result = response.get(RESULT);
        Assert.assertNotNull(result);

        // reload
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        response = executeOperation(managementClient, readVertxOperation(vertxName));
        result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("clustered").asBoolean());
        Assert.assertFalse(result.get("forked-channel").asBoolean());
        Assert.assertFalse(result.get("jgroups-channel").isDefined());
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + "different-jndi-name", result.get("jndi-name").asString());

        executeOperation(managementClient, removeVertxOperation(vertxName));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Test
    public void testAListVertx() throws IOException {
        final String vertxName = "vertx3";
        ModelNode response = executeOperation(managementClient, addVertxOperation(vertxName));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);

        response = executeOperation(managementClient, readVertxOperation(vertxName));
        result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("clustered").asBoolean());
        Assert.assertFalse(result.get("forked-channel").asBoolean());
        Assert.assertFalse(result.get("jgroups-channel").isDefined());
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + vertxName, result.get("jndi-name").asString());

        response = executeOperation(managementClient, listVertxOperation());
        result = response.get(RESULT);
        List<ModelNode> list = result.asList();
        Assert.assertEquals(2, list.size());
        ModelNode v1 = list.get(0);
        Assert.assertEquals("default", v1.get("name").asString());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + "default", v1.get("jndi-name").asString());
        ModelNode v2 = list.get(1);
        Assert.assertEquals("vertx3", v2.get("name").asString());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + vertxName, v2.get("jndi-name").asString());

        executeOperation(managementClient, removeVertxOperation(vertxName));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

        response = executeOperation(managementClient, listVertxOperation());
        result = response.get(RESULT);
        list = result.asList();
        Assert.assertEquals(1, list.size());
        v1 = list.get(0);
        Assert.assertEquals("default", v1.get("name").asString());
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + "default", v1.get("jndi-name").asString());
    }

    @Test
    public void testAddVertxWithAlias() throws IOException {
        final String vertxName = "vertx-with-alias";
        ModelNode operation = addVertxOperation(vertxName);
        operation.get("alias").add("vertx");// alias with 'vertx' is the default alias defined already
        ModelNode response = executeOperation(managementClient, operation, false);
        System.out.println("========================\n");
        System.out.println(response.asString());
        System.out.println("\n=====================\n");
        Assert.assertTrue(response.asString().contains("Alias vertx has been used already in Vertx: default"));
    }

}

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
package org.wildfly.extension.vertx.test.mini.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addressResolverOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.clusterNodeMetaOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.eventBusOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.isReloadRequired;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.keyStoreOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.pemKeyCertOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.pemTrustOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOptionOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.removeVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.vertxOptionOperationBase;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.VertxConstants;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VertxOptionNoReloadRequiredTestCase implements VertxConstants {

  @ContainerResource
  private ManagementClient managementClient;

  @Test
  public void testVertxOption() throws IOException {
    // adds a vertx option,
    // update to it or delete it won't lead to reload required status
    // refer the vertx option in a vertx instance, update it again, it should lead to reload required status
    final String vertxOptionName = "vo";
    ModelNode operation = vertxOptionOperationBase(vertxOptionName, "add");
    executeOperation(managementClient, operation);
    ModelNode response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it won't lead to reload required
    executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // now add it back
    operation = vertxOptionOperationBase(vertxOptionName, "add");
    operation.get(ATTR_EVENTLOOP_POOL_SIZE).set(10);
    executeOperation(managementClient, operation);
    response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
    result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals(10, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());

    operation = vertxOptionOperationBase(vertxOptionName, "write-attribute");
    operation.get(NAME).set(ATTR_EVENTLOOP_POOL_SIZE);
    operation.get(VALUE).set(20);
    executeOperation(managementClient, operation);

    // it does not need to reload
    Assert.assertFalse(isReloadRequired(managementClient));
    response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
    result = response.get(RESULT);
    Assert.assertEquals(20, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());

    // refer it to the default vertx
    final String vertxName = "vertx-name-test";
    try {
      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));

      // now update the option again
      operation = vertxOptionOperationBase(vertxOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_EVENTLOOP_POOL_SIZE);
      operation.get(VALUE).set(30);
      executeOperation(managementClient, operation);

      // now the update needs reload
      Assert.assertTrue(isReloadRequired(managementClient));

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
      result = response.get(RESULT);
      Assert.assertEquals(30, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }

  @Test
  public void testAddressResolverOption() throws IOException {
    // adds address-resolver-option
    final String addressResolverName = "aro";
    ModelNode operation = addressResolverOptionBase(addressResolverName, "add");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, addressResolverOptionBase(addressResolverName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = addressResolverOptionBase(addressResolverName, "add");
    operation.get(ATTR_MAX_QUERIES).set(10);
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, addressResolverOptionBase(addressResolverName, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals(10, result.get(ATTR_MAX_QUERIES).asInt());

    operation = addressResolverOptionBase(addressResolverName, "write-attribute");
    operation.get(NAME).set(ATTR_MAX_QUERIES);
    operation.get(VALUE).set(15);
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // create a vertx option with this address-resolver-option
    final String vertxName = "vertx-name-test";
    final String vertxOptionName = "vo";
    try {
      operation = vertxOptionOperationBase(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER).set(addressResolverName);
      executeOperation(managementClient, operation);
      operation = addressResolverOptionBase(addressResolverName, "write-attribute");
      operation.get(NAME).set(ATTR_MAX_QUERIES);
      operation.get(VALUE).set(20);
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addressResolverOptionBase(addressResolverName, "write-attribute");
      operation.get(NAME).set(ATTR_MAX_QUERIES);
      operation.get(VALUE).set(25);
      executeOperation(managementClient, operation);
      // now, it is reload required
      Assert.assertTrue(isReloadRequired(managementClient));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, addressResolverOptionBase(addressResolverName, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals(25, result.get(ATTR_MAX_QUERIES).asInt());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      executeOperation(managementClient, addressResolverOptionBase(addressResolverName, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }

  @Test
  public void testEventBusOption() throws IOException {
    // adds eventbus-option
    final String eventbusOptionName = "ebo";
    ModelNode operation = eventBusOptionBase(eventbusOptionName, "add");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, eventBusOptionBase(eventbusOptionName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = eventBusOptionBase(eventbusOptionName, "add");
    operation.get(ATTR_EVENTBUS_PORT).set(8888);
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, eventBusOptionBase(eventbusOptionName, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals(8888, result.get(ATTR_EVENTBUS_PORT).asInt());

    operation = eventBusOptionBase(eventbusOptionName, "write-attribute");
    operation.get(NAME).set(ATTR_EVENTBUS_PORT);
    operation.get(VALUE).set(9999);
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // create a vertx option with this eventbus-option
    final String vertxName = "vertx-name-test";
    final String vertxOptionName = "vo";
    try {
      operation = vertxOptionOperationBase(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_EVENTBUS).set(eventbusOptionName);
      executeOperation(managementClient, operation);
      operation = eventBusOptionBase(eventbusOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_EVENTBUS_PORT);
      operation.get(VALUE).set(6666);
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = eventBusOptionBase(eventbusOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_EVENTBUS_PORT);
      operation.get(VALUE).set(8080);
      executeOperation(managementClient, operation);
      // now, it is needs reload
      Assert.assertTrue(isReloadRequired(managementClient));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, eventBusOptionBase(eventbusOptionName, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals(8080, result.get(ATTR_EVENTBUS_PORT).asInt());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      executeOperation(managementClient, eventBusOptionBase(eventbusOptionName, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }

  @Test
  public void testKeyStoreOption() throws IOException {
    // adds key-store-option
    final String keyStoreOptionName = "kso";
    ModelNode operation = keyStoreOptionBase(keyStoreOptionName, "add");
    operation.get(ATTR_KEYSTORE_TYPE).set("jks");
    operation.get(ATTR_KEYSTORE_VALUE).set("dummy-key-store-value");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, keyStoreOptionBase(keyStoreOptionName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = keyStoreOptionBase(keyStoreOptionName, "add");
    operation.get(ATTR_KEYSTORE_TYPE).set("jks");
    operation.get(ATTR_KEYSTORE_VALUE).set("dummy-key-store-value");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, keyStoreOptionBase(keyStoreOptionName, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals("dummy-key-store-value", result.get(ATTR_KEYSTORE_VALUE).asString());

    operation = keyStoreOptionBase(keyStoreOptionName, "write-attribute");
    operation.get(NAME).set(ATTR_KEYSTORE_VALUE);
    operation.get(VALUE).set("dummy-key-store-value-2");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // create a vertx option with this eventbus-option
    final String vertxName = "vertx-name-test";
    final String vertxOptionName = "vo";
    final String eventBusName = "ebo";
    try {
      // add an eventbus-option with this option
      operation = eventBusOptionBase(eventBusName, "add");
      operation.get(ATTR_EVENTBUS_KEY_CERT_OPTION).set(keyStoreOptionName);
      executeOperation(managementClient, operation);
      // add a vertx-option with the eventbus-option
      operation = vertxOptionOperationBase(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_EVENTBUS).set(eventBusName);
      executeOperation(managementClient, operation);
      // now try to update the attribute
      operation = keyStoreOptionBase(keyStoreOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_KEYSTORE_VALUE);
      operation.get(VALUE).set("dummy-key-store-value-3");
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = keyStoreOptionBase(keyStoreOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_KEYSTORE_VALUE);
      operation.get(VALUE).set("dummy-key-store-value-4");
      executeOperation(managementClient, operation);
      // now, it is needs reload
      Assert.assertTrue(isReloadRequired(managementClient));

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, keyStoreOptionBase(keyStoreOptionName, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals("dummy-key-store-value-4", result.get(ATTR_KEYSTORE_VALUE).asString());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      executeOperation(managementClient, eventBusOptionBase(eventBusName, "remove"));
      executeOperation(managementClient, keyStoreOptionBase(keyStoreOptionName, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }

  @Test
    public void testPemKeyCertOption() throws IOException {
    // adds pem-key-cert-option
    final String pemKeyCertOptionName = "pkcon";
    ModelNode operation = pemKeyCertOptionBase(pemKeyCertOptionName, "add");
    operation.get(ATTR_PEM_KEY_CERT_CERT_VALUE).add("dummy-cert-value");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, pemKeyCertOptionBase(pemKeyCertOptionName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = pemKeyCertOptionBase(pemKeyCertOptionName, "add");
    operation.get(ATTR_PEM_KEY_CERT_CERT_VALUE).add("dummy-cert-value");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, pemKeyCertOptionBase(pemKeyCertOptionName, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals("dummy-cert-value", result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).get(0).asString());

    operation = pemKeyCertOptionBase(pemKeyCertOptionName, "write-attribute");
    operation.get(NAME).set(ATTR_PEM_KEY_CERT_CERT_VALUE + "[0]");
    operation.get(VALUE).set("dummy-cert-value-1");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));
    response = executeOperation(managementClient, pemKeyCertOptionBase(pemKeyCertOptionName, "read-resource"));
    result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals("dummy-cert-value-1", result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).get(0).asString());

    // create a vertx option with this eventbus-option
    final String vertxName = "vertx-name-test";
    final String vertxOptionName = "vo";
    final String eventBusName = "ebo";
    try {
      // add an eventbus-option with this option
      operation = eventBusOptionBase(eventBusName, "add");
      operation.get(ATTR_EVENTBUS_KEY_CERT_OPTION).set(pemKeyCertOptionName);
      executeOperation(managementClient, operation);
      // add a vertx-option with the eventbus-option
      operation = vertxOptionOperationBase(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_EVENTBUS).set(eventBusName);
      executeOperation(managementClient, operation);
      // now try to update the attribute
      operation = pemKeyCertOptionBase(pemKeyCertOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_PEM_KEY_CERT_CERT_VALUE + "[0]");
      operation.get(VALUE).set("dummy-cert-value-2");
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));
      response = executeOperation(managementClient, readVertxOperation(vertxName));

      operation = pemKeyCertOptionBase(pemKeyCertOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_PEM_KEY_CERT_CERT_VALUE + "[0]");
      operation.get(VALUE).set("dummy-cert-value-3");
      executeOperation(managementClient, operation);
      // now, it is needs reload
      Assert.assertTrue(isReloadRequired(managementClient));

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, pemKeyCertOptionBase(pemKeyCertOptionName, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals("dummy-cert-value-3", result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).get(0).asString());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      executeOperation(managementClient, eventBusOptionBase(eventBusName, "remove"));
      executeOperation(managementClient, pemKeyCertOptionBase(pemKeyCertOptionName, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }


  @Test
  public void testPemTrustOption() throws IOException {
    // adds pem-trust-option
    final String pemTrustOptionName = "pkcon";
    ModelNode operation = pemTrustOptionBase(pemTrustOptionName, "add");
    operation.get(ATTR_PEM_KEY_CERT_CERT_VALUE).add("dummy-cert-value");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = pemTrustOptionBase(pemTrustOptionName, "add");
    operation.get(ATTR_PEM_KEY_CERT_CERT_VALUE).add("dummy-cert-value");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals("dummy-cert-value", result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).get(0).asString());

    operation = pemTrustOptionBase(pemTrustOptionName, "write-attribute");
    operation.get(NAME).set(ATTR_PEM_KEY_CERT_CERT_VALUE + "[0]");
    operation.get(VALUE).set("dummy-cert-value-1");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));
    response = executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "read-resource"));
    result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals("dummy-cert-value-1", result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).get(0).asString());

    // create a vertx option with this eventbus-option
    final String vertxName = "vertx-name-test";
    final String vertxOptionName = "vo";
    final String eventBusName = "ebo";
    try {
      // add an eventbus-option with this option
      operation = eventBusOptionBase(eventBusName, "add");
      operation.get(ATTR_EVENTBUS_TRUST_OPTION).set(pemTrustOptionName);
      executeOperation(managementClient, operation);
      // add a vertx-option with the eventbus-option
      operation = vertxOptionOperationBase(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_EVENTBUS).set(eventBusName);
      executeOperation(managementClient, operation);
      // now try to update the attribute
      operation = pemTrustOptionBase(pemTrustOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_PEM_KEY_CERT_CERT_VALUE + "[0]");
      operation.get(VALUE).set("dummy-cert-value-2");
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));
      response = executeOperation(managementClient, readVertxOperation(vertxName));

      operation = pemTrustOptionBase(pemTrustOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_PEM_KEY_CERT_CERT_VALUE + "[0]");
      operation.get(VALUE).set("dummy-cert-value-3");
      executeOperation(managementClient, operation);
      // now, it is needs reload
      Assert.assertTrue(isReloadRequired(managementClient));

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals("dummy-cert-value-3", result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).get(0).asString());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      executeOperation(managementClient, eventBusOptionBase(eventBusName, "remove"));
      executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }

  @Test
  public void testClusterNodeMeta() throws IOException {
    // adds cluster-node-meta
    final String clusterNodeMeta = "cnm";
    ModelNode operation = clusterNodeMetaOptionBase(clusterNodeMeta, "add");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, clusterNodeMetaOptionBase(clusterNodeMeta, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = clusterNodeMetaOptionBase(clusterNodeMeta, "add");
    operation.get(ATTR_PROPERTIES).add("node-name", "vertx-cluster-1-node-1");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, clusterNodeMetaOptionBase(clusterNodeMeta, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals("vertx-cluster-1-node-1", result.get(ATTR_PROPERTIES).get("node-name").asString());

    operation = clusterNodeMetaOptionBase(clusterNodeMeta, "write-attribute");
    operation.get(NAME).set(ATTR_PROPERTIES + ".node-name");
    operation.get(VALUE).set("vertx-cluster-1-node-2");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // create a vertx option with this eventbus-option
    final String vertxName = "vertx-name-test";
    final String vertxOptionName = "vo";
    final String eventBusName = "ebo";
    try {
      // add an eventbus-option with this option
      operation = eventBusOptionBase(eventBusName, "add");
      operation.get(ATTR_EVENTBUS_CLUSTER_NODE_METADATA).set(clusterNodeMeta);
      executeOperation(managementClient, operation);
      // add a vertx-option with the eventbus-option
      operation = vertxOptionOperationBase(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_EVENTBUS).set(eventBusName);
      executeOperation(managementClient, operation);

      operation = clusterNodeMetaOptionBase(clusterNodeMeta, "write-attribute");
      operation.get(NAME).set(ATTR_PROPERTIES + ".node-name");
      operation.get(VALUE).set("vertx-cluster-1-node-3");
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = addVertxOperation(vertxName);
      operation.get(ATTR_OPTION_NAME).set(vertxOptionName);
      executeOperation(managementClient, operation);
      Assert.assertFalse(isReloadRequired(managementClient));

      operation = clusterNodeMetaOptionBase(clusterNodeMeta, "write-attribute");
      operation.get(NAME).set(ATTR_PROPERTIES + ".node-name");
      operation.get(VALUE).set("vertx-cluster-1-node-4");
      executeOperation(managementClient, operation);
      // now, it is needs reload
      Assert.assertTrue(isReloadRequired(managementClient));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, clusterNodeMetaOptionBase(clusterNodeMeta, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals("vertx-cluster-1-node-4", result.get(ATTR_PROPERTIES).get("node-name").asString());
    } finally {
      executeOperation(managementClient, removeVertxOperation(vertxName));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      executeOperation(managementClient, vertxOptionOperationBase(vertxOptionName, "remove"));
      executeOperation(managementClient, eventBusOptionBase(eventBusName, "remove"));
      executeOperation(managementClient, clusterNodeMetaOptionBase(clusterNodeMeta, "remove"));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
  }

}

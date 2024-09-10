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
package org.wildfly.extension.vertx.test.basic.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_EVENTLOOP_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_QUERIES;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addressResolverOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.isReloadRequired;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOptionOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.vertxOptionOperation;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.shared.AbstractMgtTestBase;

/**
 * You can define many vertx options without having it referenced by the vertx instance, at that time, any changes to
 * the option should not lead to server restart state, once the changes belong to an option that is used by the vertx
 * instance, the change will lead to server restart state.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VertxOptionNoReloadRequiredTestCase extends AbstractMgtTestBase {

  @Test
  public void testVertxOption() throws Exception {
    // adds a vertx option,
    // update to it or delete it won't lead to reload required status
    // refer the vertx option in a vertx instance, update it again, it should lead to reload required status
    final String vertxOptionName = "vo";
    ModelNode operation = vertxOptionOperation(vertxOptionName, "add");
    executeOperation(managementClient, operation);
    ModelNode response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it won't lead to reload required
    executeOperation(managementClient, vertxOptionOperation(vertxOptionName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // now add it back
    operation = vertxOptionOperation(vertxOptionName, "add");
    operation.get(ATTR_EVENTLOOP_POOL_SIZE).set(10);
    executeOperation(managementClient, operation);
    response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
    result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals(10, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());

    operation = vertxOptionOperation(vertxOptionName, "write-attribute");
    operation.get(NAME).set(ATTR_EVENTLOOP_POOL_SIZE);
    operation.get(VALUE).set(20);
    executeOperation(managementClient, operation);

    // it does not need to reload
    Assert.assertFalse(isReloadRequired(managementClient));
    response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
    result = response.get(RESULT);
    Assert.assertEquals(20, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());

    // refer it to vertx instance
    try {
      setVertxOption(vertxOptionName);

      // now update the option again
      operation = vertxOptionOperation(vertxOptionName, "write-attribute");
      operation.get(NAME).set(ATTR_EVENTLOOP_POOL_SIZE);
      operation.get(VALUE).set(30);
      executeOperation(managementClient, operation);

      // now the update needs reload
      Assert.assertTrue(isReloadRequired(managementClient));

      reload();
      response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
      result = response.get(RESULT);
      Assert.assertEquals(30, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());
    } finally {
      unSetVertxOption();
      executeOperation(managementClient, vertxOptionOperation(vertxOptionName, "remove"));
      reload();
    }
  }

  @Test
  public void testAddressResolverOption() throws Exception {
    // adds address-resolver-option
    final String addressResolverName = "aro";
    ModelNode operation = addressResolverOperation(addressResolverName, "add");
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // remove it, status is still the same
    executeOperation(managementClient, addressResolverOperation(addressResolverName, "remove"));
    Assert.assertFalse(isReloadRequired(managementClient));

    // add it back
    operation = addressResolverOperation(addressResolverName, "add");
    operation.get(ATTR_MAX_QUERIES).set(10);
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    ModelNode response = executeOperation(managementClient, addressResolverOperation(addressResolverName, "read-resource"));
    ModelNode result = response.get(RESULT);
    Assert.assertNotNull(result);
    Assert.assertEquals(10, result.get(ATTR_MAX_QUERIES).asInt());

    operation = addressResolverOperation(addressResolverName, "write-attribute");
    operation.get(NAME).set(ATTR_MAX_QUERIES);
    operation.get(VALUE).set(15);
    executeOperation(managementClient, operation);
    Assert.assertFalse(isReloadRequired(managementClient));

    // create a vertx option with this address-resolver-option
    final String vertxOptionName = "vo";
    try {
      operation = vertxOptionOperation(vertxOptionName, "add");
      operation.get(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER).set(addressResolverName);
      executeOperation(managementClient, operation);
      operation = addressResolverOperation(addressResolverName, "write-attribute");
      operation.get(NAME).set(ATTR_MAX_QUERIES);
      operation.get(VALUE).set(20);
      executeOperation(managementClient, operation);
      // still no reload required
      Assert.assertFalse(isReloadRequired(managementClient));

      setVertxOption(vertxOptionName);

      operation = addressResolverOperation(addressResolverName, "write-attribute");
      operation.get(NAME).set(ATTR_MAX_QUERIES);
      operation.get(VALUE).set(25);
      executeOperation(managementClient, operation);
      // now, it is reload required
      Assert.assertTrue(isReloadRequired(managementClient));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
      response = executeOperation(managementClient, addressResolverOperation(addressResolverName, "read-resource"));
      result = response.get(RESULT);
      Assert.assertNotNull(result);
      Assert.assertEquals(25, result.get(ATTR_MAX_QUERIES).asInt());
    } finally {
      unSetVertxOption();
      executeOperation(managementClient, vertxOptionOperation(vertxOptionName, "remove"));
      executeOperation(managementClient, addressResolverOperation(addressResolverName, "remove"));
      reload();
    }
  }

}

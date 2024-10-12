/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.basic.management;

import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addressResolverOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOptionOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOptions;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.vertxOptionOperation;

/**
 * Test vertx eventbus message in async ejb on basic set up.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VertxOptionsManagementTestCase implements VertxConstants {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testAddVertxOption() throws IOException {
        final String vertxOptionName = "vo";
        ModelNode operation = vertxOptionOperation(vertxOptionName, "add");
        operation.get(ATTR_EVENTLOOP_POOL_SIZE).set(10);
        operation.get(ATTR_WORKER_POOL_SIZE).set(20);
        operation.get(ATTR_INTERNAL_BLOCKING_POOL_SIZE).set(50);
        operation.get(ATTR_PREFER_NATIVE_TRANSPORT).set(true);
        operation.get(ATTR_BLOCKED_THREAD_CHECK_INTERVAL).set(50);
        operation.get(ATTR_MAX_EVENTLOOP_EXECUTE_TIME).set(60);
        operation.get(ATTR_MAX_WORKER_EXECUTE_TIME).set(70);
        operation.get(ATTR_WARNING_EXECUTION_TIME).set(80);
        executeOperation(managementClient, operation);

        ModelNode response = executeOperation(managementClient, readVertxOptionOperation(vertxOptionName));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.get(ATTR_EVENTLOOP_POOL_SIZE).asInt());
        Assert.assertEquals(20, result.get(ATTR_WORKER_POOL_SIZE).asInt());
        Assert.assertEquals(50, result.get(ATTR_INTERNAL_BLOCKING_POOL_SIZE).asInt());
        Assert.assertTrue(result.get(ATTR_PREFER_NATIVE_TRANSPORT).asBoolean());
        Assert.assertEquals(50L, result.get(ATTR_BLOCKED_THREAD_CHECK_INTERVAL).asLong());
        Assert.assertEquals(60L, result.get(ATTR_MAX_EVENTLOOP_EXECUTE_TIME).asLong());
        Assert.assertEquals(70L, result.get(ATTR_MAX_WORKER_EXECUTE_TIME).asLong());
        Assert.assertEquals(80L, result.get(ATTR_WARNING_EXECUTION_TIME).asLong());

        VertxOptions vertxOptions = readVertxOptions(managementClient, vertxOptionName);
        Assert.assertEquals(10, vertxOptions.getEventLoopPoolSize());
        Assert.assertEquals(20, vertxOptions.getWorkerPoolSize());
        Assert.assertEquals(50, vertxOptions.getInternalBlockingPoolSize());
        Assert.assertFalse(vertxOptions.isHAEnabled());
        Assert.assertTrue(vertxOptions.getPreferNativeTransport());
        Assert.assertEquals(50L, vertxOptions.getBlockedThreadCheckInterval());
        Assert.assertEquals(60L, vertxOptions.getMaxEventLoopExecuteTime());
        Assert.assertEquals(70L, vertxOptions.getMaxWorkerExecuteTime());
        Assert.assertEquals(80L, vertxOptions.getWarningExceptionTime());

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(vertxOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Test
    public void testAddressResolverOption() throws IOException {
        final String addressResolverName = "aro";
        ModelNode operation = addressResolverOperation(addressResolverName, "add");
        operation.get(ATTR_HOSTS_PATH).set("local-path");
        operation.get(ATTR_SERVERS).add("localhost").add("127.0.0.1");
        operation.get(ATTR_OPT_RES_ENABLED).set(true);
        operation.get(ATTR_CACHE_MIN_TTL).set(1024);
        operation.get(ATTR_MAX_TTL).set(4096);
        operation.get(ATTR_NEGATIVE_TTL).set(2468);
        operation.get(ATTR_QUERY_TIMEOUT).set(5000);
        operation.get(ATTR_MAX_QUERIES).set(10);
        operation.get(ATTR_RD_FLAG).set(true);
        operation.get(ATTR_SEARCH_DOMAIN).add("local").add("remote");
        operation.get(ATTR_N_DOTS).set(8);
        operation.get(ATTR_ROTATE_SERVERS).set(true);
        operation.get(ATTR_ROUND_ROBIN_INET_ADDRESS).set(true);
        executeOperation(managementClient, operation);

        ModelNode response = executeOperation(managementClient, addressResolverOperation(addressResolverName, "read-resource"));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertEquals("local-path", result.get(ATTR_HOSTS_PATH).asString());

        List<String> serverList = result.get(ATTR_SERVERS).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(serverList.contains("localhost"));
        Assert.assertTrue(serverList.contains("127.0.0.1"));

        Assert.assertTrue(result.get(ATTR_OPT_RES_ENABLED).asBoolean());
        Assert.assertEquals(1024, result.get(ATTR_CACHE_MIN_TTL).asInt());
        Assert.assertEquals(4096, result.get(ATTR_MAX_TTL).asInt());
        Assert.assertEquals(2468, result.get(ATTR_NEGATIVE_TTL).asInt());
        Assert.assertEquals(5000, result.get(ATTR_QUERY_TIMEOUT).asInt());
        Assert.assertEquals(10, result.get(ATTR_MAX_QUERIES).asInt());
        Assert.assertTrue(result.get(ATTR_RD_FLAG).asBoolean());
        List<String> searchDomains = result.get(ATTR_SEARCH_DOMAIN).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(searchDomains.contains("local"));
        Assert.assertTrue(searchDomains.contains("remote"));
        Assert.assertEquals(8, result.get(ATTR_N_DOTS).asInt());
        Assert.assertTrue(result.get(ATTR_ROTATE_SERVERS).asBoolean());
        Assert.assertTrue(result.get(ATTR_ROUND_ROBIN_INET_ADDRESS).asBoolean());

        final String optionName = "vo";
        ModelNode addVertxOption = vertxOptionOperation(optionName, "add");
        addVertxOption.get(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER).set(addressResolverName);
        executeOperation(managementClient, addVertxOption);
        VertxOptions vertxOptions = readVertxOptions(managementClient, optionName);
        AddressResolverOptions addressResolverOptions = vertxOptions.getAddressResolverOptions();
        Assert.assertNotNull(addressResolverOptions);
        Assert.assertEquals("local-path", addressResolverOptions.getHostsPath());
        Assert.assertTrue(addressResolverOptions.getServers().contains("localhost"));
        Assert.assertTrue(addressResolverOptions.getServers().contains("127.0.0.1"));
        Assert.assertTrue(addressResolverOptions.isOptResourceEnabled());
        Assert.assertEquals(1024, addressResolverOptions.getCacheMinTimeToLive());
        Assert.assertEquals(4096, addressResolverOptions.getCacheMaxTimeToLive());
        Assert.assertEquals(2468, addressResolverOptions.getCacheNegativeTimeToLive());
        Assert.assertEquals(5000, addressResolverOptions.getQueryTimeout());
        Assert.assertEquals(10, addressResolverOptions.getMaxQueries());
        Assert.assertTrue(addressResolverOptions.getRdFlag());
        Assert.assertTrue(addressResolverOptions.getSearchDomains().contains("local"));
        Assert.assertTrue(addressResolverOptions.getSearchDomains().contains("remote"));
        Assert.assertEquals(8, addressResolverOptions.getNdots());
        Assert.assertTrue(addressResolverOptions.isRotateServers());
        Assert.assertTrue(addressResolverOptions.isRoundRobinInetAddress());

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(optionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, addressResolverOperation(addressResolverName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addressResolverOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.clusterNodeMetaOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.eventBusOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.keyStoreOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.pemKeyCertOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.pemTrustOptionBase;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOptionOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOptions;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.vertxOptionOperation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.VertxConstants;
import org.wildfly.extension.vertx.test.shared.ManagementClientUtils;

import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;

/**
 * Test vertx eventbus message in async ejb on mini set up.
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
        operation.get(ATTR_HA_ENABLED).set(false);
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
        Assert.assertFalse(result.get(ATTR_HA_ENABLED).asBoolean());
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

    @Test
    public void testEventBusOption() throws IOException {
        final String eventBusOptionName = "eo";
        ModelNode operation = eventBusOperation(eventBusOptionName, "add");
        operation.get(ATTR_EVENTBUS_SEND_BUFFER_SIZE).set(1024);
        operation.get(ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE).set(1024);
        operation.get(ATTR_EVENTBUS_TRAFFIC_CLASS).set(1);
        operation.get(ATTR_EVENTBUS_REUSE_ADDRESS).set(true);
        operation.get(ATTR_EVENTBUS_LOG_ACTIVITY).set(false);
        operation.get(ATTR_EVENTBUS_REUSE_PORT).set(true);
        operation.get(ATTR_EVENTBUS_TCP_NO_DELAY).set(true);
        operation.get(ATTR_EVENTBUS_TCP_KEEP_ALIVE).set(true);
        operation.get(ATTR_EVENTBUS_SO_LINGER).set(2);
        operation.get(ATTR_EVENTBUS_IDLE_TIMEOUT).set(2048);
        operation.get(ATTR_EVENTBUS_READ_IDLE_TIMEOUT).set(2048);
        operation.get(ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT).set(2048);
        operation.get(ATTR_EVENTBUS_SSL).set(false);
        operation.get(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT).set(5000);
        operation.get(ATTR_EVENTBUS_CRL_PATHS).add("path1").add("path2");
        operation.get(ATTR_EVENTBUS_CRL_VALUES).add("aaa").add("bbb");
        operation.get(ATTR_EVENTBUS_USE_ALPN).set(false);
        operation.get(ATTR_EVENTBUS_TCP_FAST_OPEN).set(true);
        operation.get(ATTR_EVENTBUS_TCP_CORK).set(true);
        operation.get(ATTR_EVENTBUS_TCP_QUICK_ACK).set(true);
        operation.get(ATTR_EVENTBUS_SSL_ENGINE_TYPE).set("OPENSSL");
        operation.get(ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST).set("hostname-A");
        operation.get(ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT).set(3366);
        operation.get(ATTR_EVENTBUS_CLUSTER_PING_INTERVAL).set(2000);
        operation.get(ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL).set(5000);
        operation.get(ATTR_EVENTBUS_HOST).set("my-host");
        operation.get(ATTR_EVENTBUS_PORT).set(8899);
        operation.get(ATTR_EVENTBUS_ACCEPT_BACKLOG).set(2000);
        operation.get(ATTR_EVENTBUS_CLIENT_AUTH).set("REQUEST");
        operation.get(ATTR_EVENTBUS_RECONNECT_ATTEMPTS).set(20);
        operation.get(ATTR_EVENTBUS_RECONNECT_INTERVAL).set(5000);
        operation.get(ATTR_EVENTBUS_CONNECT_TIMEOUT).set(6000);
        operation.get(ATTR_EVENTBUS_TRUST_ALL).set(true);

        executeOperation(managementClient, operation);

        ModelNode response = executeOperation(managementClient, eventBusOperation(eventBusOptionName, "read-resource"));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertEquals(1024, result.get(ATTR_EVENTBUS_SEND_BUFFER_SIZE).asInt());
        Assert.assertEquals(1024, result.get(ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE).asInt());
        Assert.assertEquals(1, result.get(ATTR_EVENTBUS_TRAFFIC_CLASS).asInt());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_REUSE_ADDRESS).asBoolean());
        Assert.assertFalse(result.get(ATTR_EVENTBUS_LOG_ACTIVITY).asBoolean());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_REUSE_PORT).asBoolean());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_TCP_NO_DELAY).asBoolean());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_TCP_KEEP_ALIVE).asBoolean());
        Assert.assertEquals(2, result.get(ATTR_EVENTBUS_SO_LINGER).asInt());
        Assert.assertEquals(2048, result.get(ATTR_EVENTBUS_IDLE_TIMEOUT).asInt());
        Assert.assertEquals(2048, result.get(ATTR_EVENTBUS_READ_IDLE_TIMEOUT).asInt());
        Assert.assertEquals(2048, result.get(ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT).asInt());
        Assert.assertFalse(result.get(ATTR_EVENTBUS_SSL).asBoolean());
        Assert.assertEquals(5000, result.get(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT).asInt());
        List<String> list = result.get(ATTR_EVENTBUS_CRL_PATHS).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(list.contains("path1"));
        Assert.assertTrue(list.contains("path2"));
        list = result.get(ATTR_EVENTBUS_CRL_VALUES).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(list.contains("aaa"));
        Assert.assertTrue(list.contains("bbb"));
        Assert.assertFalse(result.get(ATTR_EVENTBUS_USE_ALPN).asBoolean());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_TCP_FAST_OPEN).asBoolean());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_TCP_CORK).asBoolean());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_TCP_QUICK_ACK).asBoolean());
        Assert.assertEquals("OPENSSL", result.get(ATTR_EVENTBUS_SSL_ENGINE_TYPE).asString());
        Assert.assertEquals("hostname-A", result.get(ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST).asString());
        Assert.assertEquals(3366, result.get(ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT).asInt());
        Assert.assertEquals(2000, result.get(ATTR_EVENTBUS_CLUSTER_PING_INTERVAL).asInt());
        Assert.assertEquals(5000, result.get(ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL).asInt());
        Assert.assertEquals("my-host", result.get(ATTR_EVENTBUS_HOST).asString());
        Assert.assertEquals(8899, result.get(ATTR_EVENTBUS_PORT).asInt());
        Assert.assertEquals(2000, result.get(ATTR_EVENTBUS_ACCEPT_BACKLOG).asInt());
        Assert.assertEquals("REQUEST", result.get(ATTR_EVENTBUS_CLIENT_AUTH).asString());
        Assert.assertEquals(20, result.get(ATTR_EVENTBUS_RECONNECT_ATTEMPTS).asInt());
        Assert.assertEquals(5000, result.get(ATTR_EVENTBUS_RECONNECT_INTERVAL).asInt());
        Assert.assertEquals(6000, result.get(ATTR_EVENTBUS_CONNECT_TIMEOUT).asInt());
        Assert.assertTrue(result.get(ATTR_EVENTBUS_TRUST_ALL).asBoolean());

        final String optionName = "vo";
        ModelNode addVertxOption = vertxOptionOperation(optionName, "add");
        addVertxOption.get(ELEMENT_VERTX_EVENTBUS).set(eventBusOptionName);
        executeOperation(managementClient, addVertxOption);
        VertxOptions vertxOptions = readVertxOptions(managementClient, optionName);
        EventBusOptions eventBusOptions = vertxOptions.getEventBusOptions();

        Assert.assertEquals(1024, eventBusOptions.getSendBufferSize());
        Assert.assertEquals(1024, eventBusOptions.getReceiveBufferSize());
        Assert.assertEquals(1, eventBusOptions.getTrafficClass());
        Assert.assertTrue(eventBusOptions.isReuseAddress());
        Assert.assertFalse(eventBusOptions.getLogActivity());
        Assert.assertTrue(eventBusOptions.isReusePort());
        Assert.assertTrue(eventBusOptions.isTcpNoDelay());
        Assert.assertTrue(eventBusOptions.isTcpKeepAlive());
        Assert.assertEquals(2, eventBusOptions.getSoLinger());
        Assert.assertEquals(2048, eventBusOptions.getIdleTimeout());
        Assert.assertEquals(2048, eventBusOptions.getReadIdleTimeout());
        Assert.assertEquals(2048, eventBusOptions.getWriteIdleTimeout());
        Assert.assertFalse(eventBusOptions.isSsl());
        Assert.assertEquals(5000L, eventBusOptions.getSslHandshakeTimeout());
        Assert.assertTrue(eventBusOptions.getCrlPaths().contains("path1"));
        Assert.assertTrue(eventBusOptions.getCrlPaths().contains("path2"));
        Assert.assertTrue(eventBusOptions.getCrlValues().contains(Buffer.buffer("aaa")));
        Assert.assertTrue(eventBusOptions.getCrlValues().contains(Buffer.buffer("bbb")));
        Assert.assertFalse(eventBusOptions.isUseAlpn());
        Assert.assertTrue(eventBusOptions.isTcpFastOpen());
        Assert.assertTrue(eventBusOptions.isTcpCork());
        Assert.assertTrue(eventBusOptions.isTcpQuickAck());
        Assert.assertNotNull(eventBusOptions.getOpenSslEngineOptions());
        Assert.assertEquals("hostname-A", eventBusOptions.getClusterPublicHost());
        Assert.assertEquals(3366, eventBusOptions.getClusterPublicPort());
        Assert.assertEquals(2000, eventBusOptions.getClusterPingInterval());
        Assert.assertEquals(5000, eventBusOptions.getClusterPingReplyInterval());
        Assert.assertEquals("my-host", eventBusOptions.getHost());
        Assert.assertEquals(8899, eventBusOptions.getPort());
        Assert.assertEquals(2000, eventBusOptions.getAcceptBacklog());
        Assert.assertEquals(ClientAuth.REQUEST, eventBusOptions.getClientAuth());
        Assert.assertEquals(20, eventBusOptions.getReconnectAttempts());
        Assert.assertEquals(5000, eventBusOptions.getReconnectInterval());
        Assert.assertEquals(6000, eventBusOptions.getConnectTimeout());
        Assert.assertTrue(eventBusOptions.isTrustAll());

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(optionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, eventBusOperation(eventBusOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Test
    public void testKeyStoreOptions() throws IOException {
        final String keyStoreOptionName = "keystore";
        ModelNode operation = keyStoreOptionBase(keyStoreOptionName, "add");
        operation.get(ATTR_KEYSTORE_TYPE).set("JKS");
        operation.get(ATTR_KEYSTORE_PASSWORD).set("secret");
        operation.get(ATTR_KEYSTORE_PATH).set("k1.keystore");
        operation.get(ATTR_KEYSTORE_ALIAS).set("alias");
        operation.get(ATTR_KEYSTORE_ALIAS_PASSWORD).set("alias-secret");
        executeOperation(managementClient, operation);

        ModelNode response = executeOperation(managementClient, keyStoreOptionBase(keyStoreOptionName, "read-resource"));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertEquals("JKS", result.get(ATTR_KEYSTORE_TYPE).asString());
        Assert.assertEquals("secret", result.get(ATTR_KEYSTORE_PASSWORD).asString());
        Assert.assertEquals("k1.keystore", result.get(ATTR_KEYSTORE_PATH).asString());
        Assert.assertEquals("alias", result.get(ATTR_KEYSTORE_ALIAS).asString());
        Assert.assertEquals("alias-secret", result.get(ATTR_KEYSTORE_ALIAS_PASSWORD).asString());

        final String eventBusOptionName = "eo";
        operation = eventBusOperation(eventBusOptionName, "add");
        operation.get(ATTR_EVENTBUS_KEY_CERT_OPTION).set(keyStoreOptionName);
        operation.get(ATTR_EVENTBUS_TRUST_OPTION).set(keyStoreOptionName);
        executeOperation(managementClient, operation);

        final String optionName = "vo";
        ModelNode addVertxOption = vertxOptionOperation(optionName, "add");
        addVertxOption.get(ELEMENT_VERTX_EVENTBUS).set(eventBusOptionName);
        executeOperation(managementClient, addVertxOption);
        VertxOptions vertxOptions = readVertxOptions(managementClient, optionName);
        EventBusOptions eventBusOptions = vertxOptions.getEventBusOptions();
        final String serverConfigDir = ManagementClientUtils.serverConfigDir(managementClient);
        JksOptions keyCertOptions = eventBusOptions.getKeyStoreOptions();
        Assert.assertNotNull(keyCertOptions);
        Assert.assertEquals("alias", keyCertOptions.getAlias());
        Assert.assertEquals("alias-secret", keyCertOptions.getAliasPassword());
        Assert.assertEquals("secret", keyCertOptions.getPassword());
        Assert.assertEquals(serverConfigDir + File.separator + "k1.keystore", keyCertOptions.getPath());
        JksOptions trustOptions = eventBusOptions.getTrustStoreOptions();
        Assert.assertNotNull(trustOptions);
        Assert.assertEquals("alias", trustOptions.getAlias());
        Assert.assertEquals("alias-secret", trustOptions.getAliasPassword());
        Assert.assertEquals("secret", trustOptions.getPassword());
        Assert.assertEquals(serverConfigDir + File.separator + "k1.keystore", trustOptions.getPath());

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(optionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, eventBusOperation(eventBusOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, keyStoreOptionBase(keyStoreOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    // using a platform dependent directory to cover Windows path
    private String absolutePathInTempDir(String path) {
        return System.getProperty("java.io.tmpdir") + File.separator + path;
    }

    @Test
    public void testPemKeyCertOptions() throws IOException {
        final String pekKeyCertOptionName = "pemKeyCert";
        ModelNode operation = pemKeyCertOptionBase(pekKeyCertOptionName, "add");
        operation.get(ATTR_PEM_KEY_CERT_KEY_PATH).add("a.key").add(absolutePathInTempDir("b.key"));
        operation.get(ATTR_PEM_KEY_CERT_KEY_VALUE).add("aaa").add("bbb");
        operation.get(ATTR_PEM_KEY_CERT_CERT_PATH).add(absolutePathInTempDir("c.cert")).add("d.cert");
        operation.get(ATTR_PEM_KEY_CERT_CERT_VALUE).add("ccc").add("ddd");
        executeOperation(managementClient, operation);
        ModelNode response = executeOperation(managementClient, pemKeyCertOptionBase(pekKeyCertOptionName, "read-resource"));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        List<String> keyPaths = result.get(ATTR_PEM_KEY_CERT_KEY_PATH).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(keyPaths.contains("a.key"));
        Assert.assertTrue(keyPaths.contains(absolutePathInTempDir("b.key")));
        List<String> keyValues = result.get(ATTR_PEM_KEY_CERT_KEY_VALUE).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(keyValues.contains("aaa"));
        Assert.assertTrue(keyValues.contains("bbb"));
        List<String> certPaths = result.get(ATTR_PEM_KEY_CERT_CERT_PATH).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(certPaths.contains("d.cert"));
        Assert.assertTrue(certPaths.contains(absolutePathInTempDir("c.cert")));
        List<String> certValues = result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(certValues.contains("ccc"));
        Assert.assertTrue(certValues.contains("ddd"));

        final String eventBusOptionName = "eo";
        operation = eventBusOperation(eventBusOptionName, "add");
        operation.get(ATTR_EVENTBUS_KEY_CERT_OPTION).set(pekKeyCertOptionName);
        executeOperation(managementClient, operation);
        final String optionName = "vo";
        ModelNode addVertxOption = vertxOptionOperation(optionName, "add");
        addVertxOption.get(ELEMENT_VERTX_EVENTBUS).set(eventBusOptionName);
        executeOperation(managementClient, addVertxOption);
        VertxOptions vertxOptions = readVertxOptions(managementClient, optionName);
        EventBusOptions eventBusOptions = vertxOptions.getEventBusOptions();
        PemKeyCertOptions keyCertOptions = eventBusOptions.getPemKeyCertOptions();
        Assert.assertNotNull(keyCertOptions);
        final String serverConfigDir = ManagementClientUtils.serverConfigDir(managementClient);
        keyPaths = keyCertOptions.getKeyPaths();
        Assert.assertTrue(keyPaths.contains(serverConfigDir + File.separator + "a.key"));
        Assert.assertTrue(keyPaths.contains(absolutePathInTempDir("b.key")));
        keyValues = keyCertOptions.getKeyValues().stream().map(Buffer::toString).collect(Collectors.toList());
        Assert.assertTrue(keyValues.contains("aaa"));
        Assert.assertTrue(keyValues.contains("bbb"));
        certPaths = keyCertOptions.getCertPaths();
        Assert.assertTrue(certPaths.contains(serverConfigDir + File.separator + "d.cert"));
        Assert.assertTrue(certPaths.contains(absolutePathInTempDir("c.cert")));
        certValues = keyCertOptions.getCertValues().stream().map(Buffer::toString).collect(Collectors.toList());
        Assert.assertTrue(certValues.contains("ccc"));
        Assert.assertTrue(certValues.contains("ddd"));

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(optionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, eventBusOperation(eventBusOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, pemKeyCertOptionBase(pekKeyCertOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Test
    public void testPemTrustOptions() throws IOException {
        final String pemTrustOptionName = "pemTrust";
        ModelNode operation = pemTrustOptionBase(pemTrustOptionName, "add");
        operation.get(ATTR_PEM_KEY_CERT_CERT_PATH).add(absolutePathInTempDir("c.cert")).add("d.cert");
        operation.get(ATTR_PEM_KEY_CERT_CERT_VALUE).add("aaa").add("bbb");
        executeOperation(managementClient, operation);
        ModelNode response = executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "read-resource"));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        List<String> certPaths = result.get(ATTR_PEM_KEY_CERT_CERT_PATH).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(certPaths.contains("d.cert"));
        Assert.assertTrue(certPaths.contains(absolutePathInTempDir("c.cert")));
        List<String> certValues = result.get(ATTR_PEM_KEY_CERT_CERT_VALUE).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        Assert.assertTrue(certValues.contains("aaa"));
        Assert.assertTrue(certValues.contains("bbb"));

        final String eventBusOptionName = "eo";
        operation = eventBusOperation(eventBusOptionName, "add");
        operation.get(ATTR_EVENTBUS_TRUST_OPTION).set(pemTrustOptionName);
        executeOperation(managementClient, operation);
        final String optionName = "vo";
        ModelNode addVertxOption = vertxOptionOperation(optionName, "add");
        addVertxOption.get(ELEMENT_VERTX_EVENTBUS).set(eventBusOptionName);
        executeOperation(managementClient, addVertxOption);
        VertxOptions vertxOptions = readVertxOptions(managementClient, optionName);
        EventBusOptions eventBusOptions = vertxOptions.getEventBusOptions();
        PemTrustOptions pemTrustOptions = eventBusOptions.getPemTrustOptions();
        Assert.assertNotNull(pemTrustOptions);
        final String serverConfigDir = ManagementClientUtils.serverConfigDir(managementClient);
        certPaths = pemTrustOptions.getCertPaths();
        Assert.assertTrue(certPaths.contains(serverConfigDir + File.separator + "d.cert"));
        Assert.assertTrue(certPaths.contains(absolutePathInTempDir("c.cert")));
        certValues = pemTrustOptions.getCertValues().stream().map(Buffer::toString).collect(Collectors.toList());
        Assert.assertTrue(certValues.contains("aaa"));
        Assert.assertTrue(certValues.contains("bbb"));

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(optionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, eventBusOperation(eventBusOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, pemTrustOptionBase(pemTrustOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Test
    public void testClusterNodeMetaOptions() throws IOException {
        final String clusterNodeMeta = "clusterNodeMeta";
        ModelNode operation = clusterNodeMetaOptionBase(clusterNodeMeta, "add");
        operation.get(PROPERTIES).add("keyA", "valueA").add("keyB", "valueB");
        executeOperation(managementClient, operation);
        ModelNode response = executeOperation(managementClient, clusterNodeMetaOptionBase(clusterNodeMeta, "read-resource"));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        List<Property> properties =  result.get(PROPERTIES).asPropertyList();
        Assert.assertTrue(properties.stream().anyMatch(p -> p.getName().equals("keyA") && p.getValue().asString().equals("valueA")));
        Assert.assertTrue(properties.stream().anyMatch(p -> p.getName().equals("keyB") && p.getValue().asString().equals("valueB")));

        final String eventBusOptionName = "eo";
        operation = eventBusOperation(eventBusOptionName, "add");
        operation.get(ATTR_EVENTBUS_CLUSTER_NODE_METADATA).set(clusterNodeMeta);
        executeOperation(managementClient, operation);
        final String optionName = "vo";
        ModelNode addVertxOption = vertxOptionOperation(optionName, "add");
        addVertxOption.get(ELEMENT_VERTX_EVENTBUS).set(eventBusOptionName);
        executeOperation(managementClient, addVertxOption);
        VertxOptions vertxOptions = readVertxOptions(managementClient, optionName);
        EventBusOptions eventBusOptions = vertxOptions.getEventBusOptions();
        JsonObject jsonObject = eventBusOptions.getClusterNodeMetadata();
        Assert.assertNotNull(jsonObject);
        Assert.assertEquals("valueA", jsonObject.getString("keyA"));
        Assert.assertEquals("valueB", jsonObject.getString("keyB"));

        // clear resources
        executeOperation(managementClient, vertxOptionOperation(optionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, eventBusOperation(eventBusOptionName, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        executeOperation(managementClient, clusterNodeMetaOptionBase(clusterNodeMeta, "remove"));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
}

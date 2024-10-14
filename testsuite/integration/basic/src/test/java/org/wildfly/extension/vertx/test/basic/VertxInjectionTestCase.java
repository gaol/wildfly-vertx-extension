/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.shared.ejb.EchoService;
import org.wildfly.extension.vertx.test.shared.rest.RestApp;
import org.wildfly.extension.vertx.test.shared.rest.ServiceEndpoint;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Test injection in servlet, ejb, REST resource endpoints.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("io.netty.netty-transport needs to be added as dependency to module: io.smallrye.reactive.mutiny.vertx-core")
public class VertxInjectionTestCase {
    @ArquillianResource
    private URL url;

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test-injection.war");
        war.addClasses(EchoService.class, RestApp.class, ServiceEndpoint.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testInjection() throws Exception {
        String res = HttpRequest.get(url.toExternalForm() + "rest/echo/Hello", 4, TimeUnit.SECONDS);
        Assert.assertEquals("Hello", res);
    }
}

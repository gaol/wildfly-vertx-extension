/*
 *  Copyright (c) 2020 - 2021 The original author or authors
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
package org.wildfly.extension.vertx.test.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.shared.ejb.EchoService;
import org.wildfly.extension.vertx.test.shared.rest.RestApp;
import org.wildfly.extension.vertx.test.shared.rest.ServiceEndpoint;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Test vertx eventbus message in async ejb on basic set up.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AsyncEjbTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "async-ejb-basic.war");
        war.addClasses(EchoService.class, RestApp.class, ServiceEndpoint.class)
          .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testEchoAsyncEJB() throws Exception {
        String message = "Hello";
        String res = HttpRequest.get( url.toExternalForm() + "rest/echo/" + message, 4, TimeUnit.SECONDS);
        Assert.assertEquals(message, res);
    }

}

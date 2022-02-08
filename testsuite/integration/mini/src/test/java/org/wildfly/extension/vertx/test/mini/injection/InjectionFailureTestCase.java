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
package org.wildfly.extension.vertx.test.mini.injection;

import java.net.URL;
import java.util.concurrent.TimeUnit;

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

/**
 * Test injection failure.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InjectionFailureTestCase {

    @ArquillianResource
    private URL url;

    @Deployment()
    public static WebArchive failDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test-injection-failure.war");
        war.addClasses(InjectionFailureServlet.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testDeployInjectionFailureWar() {
        try {
            HttpRequest.get(url.toExternalForm() + "failure", 4, TimeUnit.SECONDS);
            Assert.fail("Should Fail");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Cannot inject Vertx not_A_Alias"));
        }
    }

}

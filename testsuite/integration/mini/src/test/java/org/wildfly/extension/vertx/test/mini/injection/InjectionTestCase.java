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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.vertx.test.shared.rest.RestApp;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Test injection in servlet, ejb, REST resource endpoints.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InjectionTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test-injection.war");
        war.addClasses(InjectionTestServlet.class, RestApp.class, InjectionEndPoint.class, InjectionEJB.class);
        return war;
    }

    @Test
    public void testInjectionInServlet() throws Exception {
        String res = HttpRequest.get(url.toExternalForm() + "injection?check=servlet", 4, TimeUnit.SECONDS);
        Assert.assertEquals("True", res);
    }

    @Test
    public void testInjectionInEJB() throws Exception {
        String res = HttpRequest.get(url.toExternalForm() + "injection?check=ejb", 4, TimeUnit.SECONDS);
        Assert.assertEquals("True", res);
    }

    @Test
    public void testInjectionInRestEndpoint() throws Exception {
        String res = HttpRequest.get(url.toExternalForm() + "rest/injection", 4, TimeUnit.SECONDS);
        Assert.assertEquals("True", res);
    }

}

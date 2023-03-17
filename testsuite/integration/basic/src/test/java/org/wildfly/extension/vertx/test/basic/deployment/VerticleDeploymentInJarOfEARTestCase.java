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

package org.wildfly.extension.vertx.test.basic.deployment;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

/**
 * Test ear deployment with verticle jar inside the ear, and META-INF/vertx.json in the jar within the ear.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VerticleDeploymentInJarOfEARTestCase {

    @ArquillianResource
    private URL url;

    /**
     * The EAR deployment, which has the structure:
     *     - test-servlet.war
     *        - TestServlet.class
     *     - test-verticle.jar
     *        - TestVerticle.class
     *        - META-INF/vertx.json
     */
    @Deployment()
    public static Archive<?> deployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "in-jar-of-ear.ear")
                .addAsModule(ShrinkWrap.create(WebArchive.class, "test-servlet.war")
                        .addClasses(TestServlet.class))
                .addAsModule(ShrinkWrap.create(JavaArchive.class, "test-verticle.jar")
                        .addClasses(TestVerticle.class)
                        .addAsManifestResource("vertx.json"))
                .addAsManifestResource(VerticleDeploymentInJarOfEARTestCase.class.getPackage(), "application.xml", "application.xml");
    }

    @Test
    public void testEchoAsyncServlet() throws Exception {
        String message = "Hi from Vertx Extension from basic setup";
        String res = HttpRequest.get( url.toExternalForm() + "test?message=" + URLEncoder.encode(message, "UTF-8"), 4, TimeUnit.SECONDS);
        Assert.assertEquals(message, res);
    }

}

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOperation;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class VertxSubsystemTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testReadDefault() throws IOException {
        ModelNode response = executeOperation(managementClient, readVertxOperation());
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get("clustered").asBoolean());
        Assert.assertFalse(result.get("forked-channel").asBoolean());
        Assert.assertFalse(result.get("jgroups-channel").isDefined());
        Assert.assertFalse(result.get("vertx-options-file").isDefined());
    }

}

package org.wildfly.extension.vertx.test.shared;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.vertx.VertxConstants.DEFAULT_JNDI_PREFIX;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.addVertxOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.executeOperation;
import static org.wildfly.extension.vertx.test.shared.ManagementClientUtils.readVertxOperation;

public class VertxSetupTaskBase extends SnapshotRestoreSetupTask {

    protected  String vertxName() {
        return "vertx-test";
    }

    @Override
    public void doSetup(ManagementClient managementClient, String s) throws Exception {
        ModelNode response = executeOperation(managementClient, addVertxOperation(vertxName()));
        ModelNode result = response.get(RESULT);
        Assert.assertNotNull(result);

        response = executeOperation(managementClient, readVertxOperation(vertxName()));
        result = response.get(RESULT);
        Assert.assertNotNull(result);
        Assert.assertEquals(DEFAULT_JNDI_PREFIX + vertxName(), result.get("jndi-name").asString());
    }

}

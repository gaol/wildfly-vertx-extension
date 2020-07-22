/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.vertx;

import org.wildfly.extension.vertx.deployment.VertxSubsystemDeploymentProcessor;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
class VertxSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final VertxSubsystemAdd INSTANCE = new VertxSubsystemAdd();

    private VertxSubsystemAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        //Add deployment processors here
        //Remove this if you don't need to hook into the deployers, or you can add as many as you like
        //see SubDeploymentProcessor for explanation of the phases
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(VertxSubsystemExtension.SUBSYSTEM_NAME, VertxSubsystemDeploymentProcessor.PHASE, VertxSubsystemDeploymentProcessor.PRIORITY, new VertxSubsystemDeploymentProcessor());

            }
        }, OperationContext.Stage.RUNTIME);

        //TODO install VertxProxy Service with configuration as VertxOptions

    }
}

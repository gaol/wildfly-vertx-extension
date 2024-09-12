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
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import java.util.Optional;

import static org.wildfly.extension.vertx.VertxConstants.ATTR_OPTION_NAME;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER;
import static org.wildfly.extension.vertx.VertxConstants.VERTX_SERVICE;
import static org.wildfly.extension.vertx.VertxResourceDefinition.VERTX_CAPABILITY_NAME;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class AbstractVertxOptionsResourceDefinition extends SimpleResourceDefinition {

  static final RuntimeCapability<Void> VERTX_OPTIONS_CAPABILITY =
    RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME + ".options", true, NamedVertxOptions.class)
      .build();

  protected AbstractVertxOptionsResourceDefinition(SimpleResourceDefinition.Parameters parameters) {
    super(parameters);
  }


  protected static class VertxOptionRemoveHandler extends AbstractVertxOptionRemoveHandler {
    @Override
    protected void doPerform(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      ServiceName vertxServiceName = VertxOptionFileResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(context.getCurrentAddressValue());
      context.removeService(vertxServiceName);
    }

    @Override
    protected boolean isOptionUsedInRuntime(OperationContext context) {
      return isVertxOptionUsed(context, context.getCurrentAddressValue());
    }
  }

  protected static class AttrWriteHandler extends AbstractWriteAttributeHandler<Void> {
    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
      return isVertxOptionUsed(context, context.getCurrentAddressValue());
    }
    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
      // no-op
    }

  }

  private static Resource readVertxRootResource(OperationContext context) {
    return context.readResourceFromRoot(PathAddress.pathAddress(VertxSubsystemExtension.SUBSYSTEM_PATH));
  }

  private static boolean isVertxOptionUsedInternal(Resource vertxResource, String vertxOptionName) {
    return vertxResource.getChildren(VERTX_SERVICE).stream().anyMatch(re -> re.getModel().get(ATTR_OPTION_NAME).asString().equals(vertxOptionName));
  }

  static boolean isVertxOptionUsed(OperationContext context, String vertxOptionName) {
    Resource vertxResource = readVertxRootResource(context);
    return isVertxOptionUsedInternal(vertxResource, vertxOptionName);
  }

  static boolean isAddressResolverUsed(OperationContext context, String addressResolverName) {
    Resource vertxResource = readVertxRootResource(context);
    Optional<String> vertxOptionName = vertxResource.getChildren(ELEMENT_VERTX_OPTION).stream()
      .filter(re -> re.getModel().get(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER).asString().equals(addressResolverName))
      .map(Resource.ResourceEntry::getName)
      .findAny();
    return vertxOptionName.isPresent() && isVertxOptionUsedInternal(vertxResource, vertxOptionName.get());
  }

}

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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class ClusterNodeMetadataResourceDefinition extends SimpleResourceDefinition implements VertxConstants {

  static final RuntimeCapability<Void> VERTX_CLUSTER_NODE_METADATA_CAPABILITY =
    RuntimeCapability.Builder.of(VertxResourceDefinition.VERTX_CAPABILITY_NAME + ".options.cluster-node-meta", true, JsonObject.class)
      .build();

  // AddressResolverOptions
  public static final PropertiesAttributeDefinition ATTR_CLUSTER_NODE_METADATA = new PropertiesAttributeDefinition.Builder(ATTR_PROPERTIES, false)
    .setAttributeParser(AttributeParser.PROPERTIES_PARSER)
    .setRequired(false)
    .setAllowExpression(false)
    .build();

  private static final List<AttributeDefinition> CLUSTER_NODE_META_ATTRS = new ArrayList<>();
  static {
    CLUSTER_NODE_META_ATTRS.add(ATTR_CLUSTER_NODE_METADATA);
  }

  static List<AttributeDefinition> getClusterNodeMetaAttrs() {
    return CLUSTER_NODE_META_ATTRS;
  }
  static ClusterNodeMetadataResourceDefinition INSTANCE = new ClusterNodeMetadataResourceDefinition();

  ClusterNodeMetadataResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_CLUSTER_NODE_METADATA),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_CLUSTER_NODE_METADATA))
      .setAddHandler(new AddClusterNodeMetaHandler())
      .setRemoveHandler(new RemoveHandler())
      .setCapabilities(VERTX_CLUSTER_NODE_METADATA_CAPABILITY)
    );
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    super.registerAttributes(resourceRegistration);
    AbstractVertxOptionsResourceDefinition.AttrWriteHandler handler = new AbstractVertxOptionsResourceDefinition.AttrWriteHandler(getClusterNodeMetaAttrs()) {
      @Override
      protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        return AbstractVertxOptionsResourceDefinition.isCluseterNodeMetaUsed(context, context.getCurrentAddressValue());
      }
    };
    for (AttributeDefinition attr : getClusterNodeMetaAttrs()) {
      resourceRegistration.registerReadWriteAttribute(attr, null, handler);
    }
  }

  private static class RemoveHandler extends AbstractRemoveStepHandler {
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      boolean needsReload = AbstractVertxOptionsResourceDefinition.isCluseterNodeMetaUsed(context, name);
      ServiceName serviceName = VERTX_CLUSTER_NODE_METADATA_CAPABILITY.getCapabilityServiceName(name);
      context.removeService(serviceName);
      if (needsReload) {
        context.reloadRequired();
      }
    }
  }

  private static class AddClusterNodeMetaHandler extends AbstractAddStepHandler {
    AddClusterNodeMetaHandler() {
      super(new Parameters().addAttribute(CLUSTER_NODE_META_ATTRS));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      ServiceName serviceName = VERTX_CLUSTER_NODE_METADATA_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(serviceName);
      final Consumer<JsonObject> consumer = serviceBuilder.provides(serviceName);
      final JsonObject clusterNodeMeta = parseNode(operation);
      Service service = new Service() {

        @Override
        public void start(StartContext startContext) throws StartException {
          consumer.accept(clusterNodeMeta);
        }

        @Override
        public void stop(StopContext stopContext) {
        }
      };
      serviceBuilder
        .setInstance(service)
        .setInitialMode(ServiceController.Mode.ON_DEMAND)
        .install();
    }

    private JsonObject parseNode(ModelNode operation) throws OperationFailedException {
      JsonObject json = new JsonObject();
      if (operation.hasDefined(ATTR_CLUSTER_NODE_METADATA.getName())) {
        List<Property> properties = ATTR_CLUSTER_NODE_METADATA.validateOperation(operation).asPropertyList();
        for (Property prop: properties) {
          json.put(prop.getName(), prop.getValue().asString());
        }
      }
      return json;
    }
  }
}

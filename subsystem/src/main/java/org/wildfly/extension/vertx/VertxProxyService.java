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

import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.vertx.core.Future;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxBuilder;

/**
 * The msc service to initialize a Vertx instance.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxyService implements Service, VertxConstants {
    private final VertxProxy vertxProxy;
    private final Supplier<ChannelFactory> channelFactorySupplier;
    private final Supplier<String> clusterSupplier;
    private final Supplier<NamedVertxOptions> optionsSupplier;
    private final Supplier<ServerEnvironment> serverEnvSupplier;
    private final String jgroupsStackFile;
    final Consumer<VertxProxy> vertxProxytConsumer;

    static void installService(OperationContext context, VertxProxy vertxProxy) {
        final String optionName = vertxProxy.getOptionName();
        VertxProxyService vertxProxyService;
        ServiceName vertxServiceName = VertxResourceDefinition.VERTX_RUNTIME_CAPABILITY.getCapabilityServiceName();
        ServiceBuilder<?> vertxServiceBuilder = context.getServiceTarget().addService(vertxServiceName);
        Supplier<ServerEnvironment> serverEnvSupplier = vertxServiceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
        final Consumer<VertxProxy> vertxProxytConsumer = vertxServiceBuilder.provides(vertxServiceName);
        Supplier<NamedVertxOptions> optionsSupplier;
        if (optionName == null) {
            optionsSupplier = () -> NamedVertxOptions.DEFAULT;
        } else {
            ServiceName optionServiceName = AbstractVertxOptionsResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(optionName);
            optionsSupplier = vertxServiceBuilder.requires(optionServiceName);
        }
        if (vertxProxy.isClustered()) {
            final String jgroupChannel = vertxProxy.getJgroupChannelName();
            final String jgroupsStackFile = vertxProxy.getJgroupsStackFile();
            // channel factory can be either from source or forked.
            // new channel and transport ports need to be specified if multiple Vertx instances are to be created.
            // cluster name cannot be overridden.
            Supplier<ChannelFactory> cacheManagerSupplier = null;
            if (jgroupChannel != null) {
                final ServiceName channelFactoryServiceName;
                if (vertxProxy.isForkedChannel()) {
                    channelFactoryServiceName = JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, jgroupChannel);
                } else {
                    channelFactoryServiceName = JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, jgroupChannel);
                }
                cacheManagerSupplier = vertxServiceBuilder.requires(channelFactoryServiceName);
            }
            ServiceName clusterServiceName = JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, jgroupChannel);
            Supplier<String> clusterSupplier = vertxServiceBuilder.requires(clusterServiceName);
            vertxProxyService = new VertxProxyService(cacheManagerSupplier, clusterSupplier, vertxProxy, optionsSupplier, vertxProxytConsumer, serverEnvSupplier, jgroupsStackFile);
        } else  {
            vertxProxyService = new VertxProxyService(null, null, vertxProxy, optionsSupplier, vertxProxytConsumer, serverEnvSupplier, null);
        }
        vertxServiceBuilder.setInstance(vertxProxyService);
        vertxServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        vertxServiceBuilder.install();

        final String jndiName = VertxConstants.VERTX_JNDI_NAME;
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        VertxManagementReferenceFactory valueManagedReferenceFactory = new VertxManagementReferenceFactory(vertxProxyService);
        binderService.getManagedObjectInjector().inject(valueManagedReferenceFactory);
        final ServiceBuilder<?> builder = context.getServiceTarget().addService(bindInfo.getBinderServiceName());
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(vertxServiceName);
        builder.setInstance(binderService)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new LifecycleListener() {
                    private volatile boolean bound;
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                VERTX_LOGGER.vertxStarted(jndiName);
                                bound = true;
                                break;
                            }
                            case DOWN: {
                                if (bound) {
                                    VERTX_LOGGER.vertxStopped(jndiName);
                                    bound = false;
                                }
                                break;
                            }
                        }
                    }
                })
                .install();
    }

    Vertx getValue() {
        return this.vertxProxy.getVertx();
    }

    private VertxProxyService(Supplier<ChannelFactory> channelFactorySupplier, Supplier<String> clusterSupplier,
                              VertxProxy vertxProxy, Supplier<NamedVertxOptions> optionsSupplier,
                              Consumer<VertxProxy> vertxProxytConsumer,
                              Supplier<ServerEnvironment> serverEnvSupplier,
                              String jgroupsStackFile) {
        this.vertxProxy = vertxProxy;
        this.clusterSupplier = clusterSupplier;
        this.channelFactorySupplier = channelFactorySupplier;
        this.optionsSupplier = optionsSupplier;
        this.vertxProxytConsumer = vertxProxytConsumer;
        this.serverEnvSupplier = serverEnvSupplier;
        this.jgroupsStackFile = jgroupsStackFile;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            vertxProxy.instrument(createVertx());
        } catch (Exception e) {
            throw VERTX_LOGGER.failedToStartVertxService(e);
        }
        vertxProxytConsumer.accept(vertxProxy);
    }

    private Vertx createVertx() throws Exception {
        VertxOptions vertxOptions = optionsSupplier.get().getVertxOptions();
        if (vertxOptions == null) {
            vertxOptions = new VertxOptions();
        }
        // keep TCCL so that verticle has correct classloader
        vertxOptions.setDisableTCCL(false);
        vertxOptions.getFileSystemOptions()
          .setFileCacheDir(serverEnvSupplier.get().getServerTempDir() + File.separator + "vertx-cache-default");
        VertxFileResolver fileResolver = new VertxFileResolver(vertxOptions.getFileSystemOptions(), serverEnvSupplier.get());
        VertxBuilder vb = new VertxBuilder(vertxOptions).fileResolver(fileResolver);
        if (vertxProxy.isClustered()) {
            return ClusterVertxHolder.getInstance()
              .clusterVertx(vb, channelFactorySupplier, clusterSupplier, jgroupsStackFile, vertxOptions, serverEnvSupplier, vertxProxy);
        } else {
            return vb.init().vertx();
        }
    }

    @Override
    public void stop(StopContext context) {
        CompletableFuture<Void> closeFuture = (CompletableFuture<Void>)this.vertxProxy.getVertx().close()
                .flatMap(v -> {
                    if (vertxProxy.isClustered()) {
                        return ClusterVertxHolder.getInstance().stopClusterVertx();
                    }
                    return Future.succeededFuture();
                })
                .toCompletionStage();
        try {
            closeFuture.join();
        } catch (Exception e) {
            VERTX_LOGGER.errorWhenClosingVertx(e);
        } finally {
            vertxProxy.release();
        }
    }

}

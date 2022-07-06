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
package org.wildfly.extension.vertx.test.mini.components.client;

import com.github.dockerjava.api.model.Bind;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class ContainerBaseRule extends ExternalResource {

  private final GenericContainer<?> container;
  private final String image;
  private final Integer servicePort;

  public ContainerBaseRule(String image, Integer servicePort) {
    this.image = image;
    this.servicePort = servicePort;
    this.container = new GenericContainer<>(image)
      .withExposedPorts(servicePort);
  }

  protected void updateContainerBeforeStart(GenericContainer<?> container) {

  }

  @Override
  protected void before() throws Throwable {
    updateContainerBeforeStart(this.container);
    for (Bind b : container.getBinds()) {
      System.out.println("BBB:" + b.toString());
    }
    container.start();
  }

  @Override
  protected void after() {
    container.stop();
  }

  public String getContainerIPAddress() {
    return container.getContainerIpAddress();
  }

  public Integer getMappedPort() {
    return container.getMappedPort(this.servicePort);
  }

}

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

import io.vertx.core.Vertx;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class InjectionEJB {

    @Resource(name = "java:/vertx/default")
    private Vertx vertxFromJNDI;

    @Inject
    private Vertx vertx;

    public Vertx getVertx() {
        return vertx;
    }

    public Vertx getVertxFromJNDI() {
        return vertxFromJNDI;
    }

}

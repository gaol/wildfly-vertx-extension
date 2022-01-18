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
package org.wildfly.extension.vertx.test.mini.components.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.authorization.WildcardPermissionBasedAuthorization;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.auth.properties.PropertyFileAuthorization;
import org.wildfly.extension.vertx.test.shared.StreamUtils;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

@WebServlet(value = "/testauth", asyncSupported = true)
public class VertxAuthServlet extends HttpServlet {

    @Resource(name = "java:/vertx/vertx-test")
    private Vertx vertx;

    private PropertyFileAuthentication authnProvider;
    private PropertyFileAuthorization authzProvider;

    @Override
    public void init() throws ServletException {
        authnProvider = PropertyFileAuthentication.create(vertx, "vertx-auth.properties");
        authzProvider = PropertyFileAuthorization.create(vertx, "vertx-auth.properties");
    }

    @Override
    public void destroy() {
        authnProvider = null;
        authzProvider = null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final AsyncContext asyncContext = req.startAsync();
        String body = StreamUtils.streamToString(req.getInputStream());
        JsonObject authentication = new JsonObject(body);
        authnProvider.authenticate(authentication)
                    .flatMap(user -> {
                        Vertx.currentContext().putLocal("auth.user", user);
                        return authzProvider.getAuthorizations(user);
                    })
                    .onComplete(authz -> {
                        JsonObject respContent = new JsonObject();
                        if (authz.failed()) {
                            respContent.put("error", authz.cause().getMessage());
                        } else {
                            User user = Vertx.currentContext().getLocal("auth.user");
                            respContent.put("user", user.principal());
                            respContent.put("roles", new JsonArray());
                            respContent.put("permissions", new JsonArray());
                            Set<Authorization> authorities = user.authorizations().get("vertx-auth.properties");
                            for (Authorization auth: authorities) {
                                if (auth instanceof RoleBasedAuthorization) {
                                    RoleBasedAuthorization roleAuth = (RoleBasedAuthorization)auth;
                                    respContent.getJsonArray("roles").add(roleAuth.getRole());
                                } else if (auth instanceof WildcardPermissionBasedAuthorization) {
                                    WildcardPermissionBasedAuthorization wpa = (WildcardPermissionBasedAuthorization)auth;
                                    respContent.getJsonArray("permissions").add(wpa.getPermission());
                                }
                            }
                        }
                        resp.setContentType("application/json");
                        try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
                            writer.print(respContent);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } finally {
                            asyncContext.complete();
                        }
                    });

    }
}

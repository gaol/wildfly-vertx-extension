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
package org.wildfly.extension.vertx.test.basic.injection;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Assert;
import org.wildfly.extension.vertx.VertxConstants;

import io.vertx.core.Vertx;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(value = "/injection", asyncSupported = true)
public class InjectionTestServlet extends HttpServlet {

    @Resource(lookup = VertxConstants.VERTX_JNDI_NAME)
    private Vertx vertxFromJNDI;

    @Inject
    private Vertx vertx;

    @EJB
    private InjectionEJB ejb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String check = req.getParameter("check");
        if (check == null) {
            check = "servlet";
        }
        if (check.equals("servlet")) {
            checkServlet(req, resp);
        } else if (check.equals("ejb")) {
            checkEJB(req, resp);
        } else {
            resp.sendError(400, "Unknown check: " + check);
        }
    }

    private void checkEJB(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Assert.assertNotNull("EJB in the servlet is null", this.ejb);
        Assert.assertNotNull("Vertx from JNDI lookup in EJB is null", ejb.getVertxFromJNDI());
        Assert.assertNotNull("Vertx using Injection in EJB is null", ejb.getVertx());
        Assert.assertEquals("Vertx using Injection should be equal to the one lookup from JNDI in EJB", ejb.getVertx(),
                ejb.getVertxFromJNDI());
        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();
        writer.print("True");
        writer.flush();
    }

    private void checkServlet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Assert.assertNotNull("Vertx from JNDI lookup is null", this.vertxFromJNDI);
        Assert.assertNotNull("Vertx using Injection is null", this.vertx);
        Assert.assertEquals("Vertx using Injection should be equal to the one lookup from JNDI", this.vertx,
                this.vertxFromJNDI);
        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();
        writer.print("True");
        writer.flush();
    }
}

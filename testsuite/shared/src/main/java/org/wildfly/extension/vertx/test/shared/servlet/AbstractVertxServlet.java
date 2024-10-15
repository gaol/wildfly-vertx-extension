/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.test.shared.servlet;

import java.io.IOException;

import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.wildfly.extension.vertx.VertxConstants.CDI_QUALIFIER;

/**
 * An abstract servlet class which communicates with a Vert.x eventbus address.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class AbstractVertxServlet<S, R> extends HttpServlet {

  @Any
  @Identifier(CDI_QUALIFIER)
  @Inject
  private Vertx vertx;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    execute(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    execute(req, resp);
  }

  private void execute(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!req.isAsyncSupported()) {
      throw new ServletException("Please add 'asyncSupported = true' to the @WebServlet annotation.");
    }
    String address = address();
    S payload = payload(req);
    final AsyncContext asyncContext = req.startAsync();
    if (resultRequired()) {
      vertx.eventBus().<R>request(address, payload).onComplete(r -> {
        try {
          if (r.failed()) {
            r.cause().printStackTrace();
            resp.sendError(500, r.cause().getMessage());
          } else {
            sendResponse(resp, payload, r.result().body());
          }
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        } finally {
          asyncContext.complete();
        }
      });
    } else {
      vertx.eventBus().send(address, payload);
      sendResponse(resp, payload, null);
      asyncContext.complete();
    }
  }

  /**
   * @return true if you need the response from eventbus handler
   */
  protected boolean resultRequired() {
    return true;
  }

  /**
   * The target eventbus address to send payload to
   */
  protected abstract String address();

  /**
   * @return the payload from the http request
   */
  protected abstract S payload(HttpServletRequest httpRequest) throws IOException;

  /**
   * send the response according to the response from eventbus address handler.
   */
  protected abstract void sendResponse(HttpServletResponse httpResponse, S payload, R vertxResponse) throws IOException;
}

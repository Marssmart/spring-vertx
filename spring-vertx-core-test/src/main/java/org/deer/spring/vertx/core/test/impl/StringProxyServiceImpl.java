package org.deer.spring.vertx.core.test.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.deer.spring.vertx.core.test.StringProxyServiceVertxEBProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringProxyServiceImpl extends StringProxyServiceVertxEBProxy {

  private static final Logger LOG = LoggerFactory.getLogger(StringProxyServiceImpl.class);

  public StringProxyServiceImpl(Vertx vertx, String address) {
    super(vertx, address);
  }

  @Override
  public void doSomething(String input, Handler<AsyncResult<String>> resultHandler) {
    LOG.info("Request came[input={}]", input);
    resultHandler.handle(Future.succeededFuture(input + "_processed"));
  }
}

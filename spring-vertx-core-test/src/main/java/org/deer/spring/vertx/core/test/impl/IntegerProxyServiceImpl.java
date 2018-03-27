package org.deer.spring.vertx.core.test.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.deer.spring.vertx.core.test.IntegerProxyServiceVertxEBProxy;

public class IntegerProxyServiceImpl extends IntegerProxyServiceVertxEBProxy {

  private static final Logger LOG = LoggerFactory.getLogger(IntegerProxyServiceImpl.class);

  public IntegerProxyServiceImpl(Vertx vertx, String address) {
    super(vertx, address);
  }

  @Override
  public void doSomething(int input, Handler<AsyncResult<Integer>> resultHandler) {
    LOG.info("Request came[input={}]", input);
    resultHandler.handle(Future.succeededFuture(input + 4));
  }
}

package org.deer.spring.vertx.core.test;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen
public interface IntegerProxyService {

  void doSomething(final int input, final Handler<AsyncResult<Integer>> resultHandler);
}

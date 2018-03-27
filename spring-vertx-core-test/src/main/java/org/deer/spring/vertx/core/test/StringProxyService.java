package org.deer.spring.vertx.core.test;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen
public interface StringProxyService {

  void doSomething(final String input, final Handler<AsyncResult<String>> resultHandler);
}

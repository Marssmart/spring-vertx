package org.deer.spring.vertx.core.test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.TestCase.fail;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import io.vertx.core.Vertx;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.deer.spring.vertx.core.test.TestConfig.VertxHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class ClusterSingletonTest {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterSingletonTest.class);

  @Autowired
  private VertxHolder vertxHolder;

  @Autowired
  private IntegerProxyService integerService;

  @Autowired
  private StringProxyService stringService;

  @Test
  public void testClusterSingletonIntegerService() throws ExecutionException, InterruptedException {
    LOG.info("Invoking integerService.doSomething()");

    AtomicBoolean received = new AtomicBoolean(false);
    integerService.doSomething(3, integerAsyncResult -> {
      if (integerAsyncResult.succeeded()) {
        LOG.info("integerService.doSomething() finished successfully");
        assertEquals(7, integerAsyncResult.result().intValue());
        received.set(true);
      } else {
        fail("Error while invoking integer service " + integerAsyncResult.cause());
      }
    });

    await().atMost(5, SECONDS).until(received::get);
  }

  @Test
  public void testClusterSingletonStringService() throws ExecutionException, InterruptedException {
    LOG.info("Invoking stringService.doSomething()");

    AtomicBoolean received = new AtomicBoolean(false);
    stringService.doSomething("input", stringAsyncResult -> {
      if (stringAsyncResult.succeeded()) {
        LOG.info("stringService.doSomething() finished successfully");
        assertEquals("input_processed", stringAsyncResult.result());
        received.set(true);
      } else {
        fail("Error while invoking string service " + stringAsyncResult.cause());
      }
    });

    await().atMost(5, SECONDS).until(received::get);
  }

  @Test
  public void testEventBusComunication() {
    final Vertx vertx = vertxHolder.getVertx();

    LOG.info("Registering test event consumer");
    AtomicBoolean received = new AtomicBoolean(false);
    vertx.eventBus()
        .consumer("test-consumer")
        .handler(event -> {
          received.set(true);
          LOG.info("Test event recieved");
        });

    LOG.info("Sending test event");
    vertx.eventBus()
        .send("test-consumer", null);

    await().atMost(5, SECONDS).until(received::get);
  }
}

package org.deer.spring.vertx.core.test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import org.deer.spring.vertx.core.ClusterSingletonProxyFactory;
import org.deer.spring.vertx.core.ClusterSingletonRegistratorBean;
import org.deer.spring.vertx.core.test.impl.IntegerProxyServiceImpl;
import org.deer.spring.vertx.core.test.impl.StringProxyServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class TestConfig {

  private static final Logger LOG = LoggerFactory.getLogger(TestConfig.class);

  private static Vertx createClusteredNode()
      throws InterruptedException, ExecutionException {

    CompletableFuture<Vertx> vertxResult = new CompletableFuture<>();
    Vertx.clusteredVertx(new VertxOptions()
            .setMaxEventLoopExecuteTime(Long.MAX_VALUE)
            .setEventLoopPoolSize(10)
            .setClusterManager(new HazelcastClusterManager()),
        vertxAsyncResult -> {
          if (vertxAsyncResult.succeeded()) {
            vertxResult.complete(vertxAsyncResult.result());
          } else {
            vertxResult.completeExceptionally(vertxAsyncResult.cause());
          }
        });
    return vertxResult.get();
  }

  @Bean
  @Scope("singleton")
  public VertxHolder vertxHolder() {
    LOG.info("Creating instance of Vertx");
    final Vertx clusteredNode;
    try {
      clusteredNode = createClusteredNode();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }

    return () -> clusteredNode;
  }

  @Bean
  public FactoryBean<IntegerProxyService> testIntegerProxyService(
      @Autowired final VertxHolder vertxHolder) {
    return new ClusterSingletonProxyFactory<>("integer-proxy-service",
        vertxHolder.getVertx(),
        IntegerProxyService.class);
  }

  @Bean
  public FactoryBean<StringProxyService> testStringProxyService(
      @Autowired final VertxHolder vertxHolder) {
    return new ClusterSingletonProxyFactory<>("string-proxy-service",
        vertxHolder.getVertx(),
        StringProxyService.class);
  }

  @Bean
  @Scope("singleton")
  @Named("node-singleton-registrator")
  public ClusterSingletonRegistratorBean registrations(@Autowired final VertxHolder vertxHolder)
      throws ExecutionException, InterruptedException {
    return createAndBindSingletonRegistrator(vertxHolder.getVertx());
  }

  private ClusterSingletonRegistratorBean createAndBindSingletonRegistrator(Vertx vertx) {
    final ClusterSingletonRegistratorBean registratorBean =
        new ClusterSingletonRegistratorBean(vertx);

    registratorBean.registerClusterWideSingleton("string-proxy-service",
        StringProxyService.class,
        () -> new StringProxyServiceImpl(vertx, "string-proxy-service"));

    registratorBean.registerClusterWideSingleton("integer-proxy-service",
        IntegerProxyService.class,
        () -> new IntegerProxyServiceImpl(vertx, "integer-proxy-service"));

    return registratorBean;
  }

  public interface VertxHolder {

    Vertx getVertx();

    @PreDestroy
    default void preDestroy() {
      getVertx().close();
    }
  }
}

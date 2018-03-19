package org.deer.spring.vertx.node.cfg;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource(value = {"classpath:application.yml"})
public class NodeConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(NodeConfiguration.class);

  @Bean
  public static PropertySourcesPlaceholderConfigurer configurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  @Scope("singleton")
  public Vertx createVertx(@Value("${application.cluster.enabled}") boolean clusterEnabled)
      throws ExecutionException, InterruptedException {

    // to enable log delegation from vertx
    System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory.class.getName());

    if (clusterEnabled) {
      LOG.info("Starting clustered Vertx instance");
      CompletableFuture<Vertx> vertx = new CompletableFuture<>();

      final VertxOptions options = new VertxOptions()
          .setClusterManager(new HazelcastClusterManager());

      Vertx.clusteredVertx(options, vertxAsyncResult -> {
        if (vertxAsyncResult.succeeded()) {
          LOG.info("Clustered Vertx successfully started");
          vertx.complete(vertxAsyncResult.result());
        } else {
          LOG.error("Error while starting clustered Vertx", vertxAsyncResult.cause());
          vertx.completeExceptionally(vertxAsyncResult.cause());
        }
      });

      return vertx.get();
    } else {
      LOG.info("Starting single-node Vertx instance");
      return Vertx.vertx();
    }
  }
}

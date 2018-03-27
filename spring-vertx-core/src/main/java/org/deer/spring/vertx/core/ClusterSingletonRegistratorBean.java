package org.deer.spring.vertx.core;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Lock;
import io.vertx.serviceproxy.ServiceBinder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterSingletonRegistratorBean {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterSingletonRegistratorBean.class);
  private static final String SINGLETON_MAP = "cluster-wide-singleton-map";

  private final Vertx vertx;

  public ClusterSingletonRegistratorBean(Vertx vertx) {
    this.vertx = vertx;
  }

  private static String descriptor(final String serviceAddress, final Class<?> serviceInterface) {
    return serviceAddress + "[" + serviceInterface.getName() + "]";
  }

  /**
   * Registers cluster wide implementation of service. Instance is obtained by lambda, so the
   * instance won't be created unless the specific node that got permission to deploy this will call
   * it. This prevents instance creation without actually deploying it. <br><br> Situation can be
   * described as:
   *
   * Node 2 retrieves the deployment lock, and finds out that this service has not yet been
   * deployed
   *
   * <br><br> Node 1: Will block until deployment runs on Node 2, then it will find out that service
   * is already deployed, so it will skip. <br><br> Node 2: Will retrieved deployment lock, and
   * deploy service.<b>This will be the only node that called instanceProvider</b> <br><br> Node 3:
   * Same as Node 2.
   *
   * @param serviceAddress event bus address of the service
   * @param iface interface to bound
   * @param instanceProvider provider that will be called to create instance
   */
  public <T> void registerClusterWideSingleton(final String serviceAddress,
      final Class<T> iface,
      final Provider<? extends T> instanceProvider) {

    final String descriptor = descriptor(serviceAddress, iface);

    final CompletableFuture<Void> deployFuture = new CompletableFuture<>();

    try (CloseableLock lock = lockDeploy(descriptor)) {
      final AsyncMap<String, Boolean> deploymentRegistry = getSingletonMarkerMap();

      retrieveDeploymentMarker(descriptor, deploymentRegistry)
          .compose(marker -> {
            if (marker == null) {
              LOG.info("Deployment marker for {} not present, deploying on this node", descriptor);

              return deployService(serviceAddress, iface, instanceProvider)
                  .compose(event -> markServiceDeployed(descriptor, deploymentRegistry))
                  .map(aVoid -> deployFuture.complete(null))
                  .otherwise(deployFuture::completeExceptionally);
            } else {
              LOG.info("Deployment marker found for {}, skipping deployment on this node",
                  descriptor);
              deployFuture.complete(null);
              return Future.succeededFuture();
            }
          }).otherwise(deployFuture::completeExceptionally);
      deployFuture.get();
    } catch (Exception e) {
      throw new IllegalStateException("Error while deploying cluster singleton", e);
    }
  }

  private Future<Void> markServiceDeployed(String descriptor,
      AsyncMap<String, Boolean> deploymentRegistry) {
    Future<Void> deployMarkingResult = Future.future();
    deploymentRegistry.put(descriptor, true, voidAsyncResult -> {
      if (voidAsyncResult.succeeded()) {
        LOG.info("Service {} marked as deployed", descriptor);
        deployMarkingResult.complete(voidAsyncResult.result());
      } else {
        deployMarkingResult.fail(voidAsyncResult.cause());
      }
    });
    return deployMarkingResult;
  }

  private <T> Future<Void> deployService(String serviceAddress, Class<T> iface,
      Provider<? extends T> instanceProvider) {
    Future<Void> deployFuture = Future.future();
    final String descriptor = descriptor(serviceAddress, iface);
    LOG.info("Deploying service {}", descriptor);
    new ServiceBinder(vertx)
        .setAddress(serviceAddress)
        .register(iface, instanceProvider.get())
        .completionHandler(voidAsyncResult -> {
          if (voidAsyncResult.succeeded()) {
            LOG.info("Service {} deployed", descriptor);
            deployFuture.complete(voidAsyncResult.result());
          } else {
            LOG.info("Service {} deploy failed", descriptor, voidAsyncResult.cause());
            deployFuture.fail(voidAsyncResult.cause());
          }
        });
    return deployFuture;
  }

  private Future<Boolean> retrieveDeploymentMarker(String descriptor,
      AsyncMap<String, Boolean> map) {
    Future<Boolean> markerResult = Future.future();
    map.get(descriptor, booleanAsyncResult -> {
      if (booleanAsyncResult.succeeded()) {
        markerResult.complete(booleanAsyncResult.result());
      } else {
        markerResult.fail(booleanAsyncResult.cause());
      }
    });
    return markerResult;
  }

  private CloseableLock lockDeploy(String descriptor) {
    CompletableFuture<Lock> lockFutureResult = new CompletableFuture<>();
    vertx.sharedData().getLock(descriptor, lockAsyncResult -> {
      if (lockAsyncResult.succeeded()) {
        LOG.info("Deployment lock {} retrieved", descriptor);
        lockFutureResult.complete(lockAsyncResult.result());
      } else {
        lockFutureResult.completeExceptionally(lockAsyncResult.cause());
      }
    });
    try {
      return new CloseableLock(lockFutureResult.get());
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Error while locking deploy of cluster singleton", e);
    }
  }

  private AsyncMap<String, Boolean> getSingletonMarkerMap() {
    CompletableFuture<AsyncMap<String, Boolean>> mapResultFuture = new CompletableFuture<>();
    vertx.sharedData().<String, Boolean>getClusterWideMap(SINGLETON_MAP, mapResult -> {
      if (mapResult.succeeded()) {
        LOG.info("Singleton marker map retrieved");
        mapResultFuture.complete(mapResult.result());
      } else {
        mapResultFuture.completeExceptionally(mapResult.cause());
      }
    });
    try {
      return mapResultFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Error while retrieving deployment registry", e);
    }
  }

  private static final class CloseableLock implements AutoCloseable {

    private final Lock lock;

    private CloseableLock(Lock lock) {
      this.lock = lock;
    }

    @Override
    public void close() throws Exception {
      lock.release();
    }
  }
}

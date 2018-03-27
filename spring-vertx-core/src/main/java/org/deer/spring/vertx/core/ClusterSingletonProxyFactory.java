package org.deer.spring.vertx.core;

import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class ClusterSingletonProxyFactory<TYPE> implements FactoryBean<TYPE> {

  private final String serviceAddress;
  private final Vertx vertx;
  private final Class<TYPE> providedBeanType;

  /**
   * @param serviceAddress address for service discovery
   * @param vertx Vertx instance
   * @param providedBeanType type of provided bean
   */
  public ClusterSingletonProxyFactory(
      @NonNull final String serviceAddress,
      @NonNull final Vertx vertx,
      @NonNull final Class<TYPE> providedBeanType) {
    this.serviceAddress = serviceAddress;
    this.vertx = vertx;
    this.providedBeanType = providedBeanType;
  }

  @Nullable
  @Override
  public TYPE getObject() throws Exception {
    return new ServiceProxyBuilder(vertx).setAddress(serviceAddress).build(providedBeanType);
  }

  @Override
  public Class<?> getObjectType() {
    return providedBeanType;
  }

  /**
   * This implementation returns just proxies, the service itself will be deployed as single
   * instance within the cluster. So for performance reasons, letting it create proxy prototypes
   */
  @Override
  public boolean isSingleton() {
    return false;
  }
}

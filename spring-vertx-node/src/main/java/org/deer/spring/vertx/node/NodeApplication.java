package org.deer.spring.vertx.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NodeApplication {

  private static final Logger LOG = LoggerFactory.getLogger(NodeApplication.class);

  public static void main(String[] args) throws InterruptedException {
    LOG.info("Starting node");
    SpringApplication bootApp = new SpringApplication(NodeApplication.class);
    bootApp.run(args);
    LOG.info("Node started");
    Thread.currentThread().join();
  }
}

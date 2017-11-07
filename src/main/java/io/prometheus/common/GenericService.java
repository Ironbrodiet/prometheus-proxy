/*
 *  Copyright 2017, Paul Ambrose All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.prometheus.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jmx.JmxReporter;
import com.github.kristofa.brave.Brave;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class GenericService
    extends AbstractExecutionThreadService
    implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(GenericService.class);

  private final MetricRegistry      metricRegistry      = new MetricRegistry();
  private final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
  private final List<Service>       services            = Lists.newArrayList(this);

  private final ConfigVals            configVals;
  private final boolean               testMode;
  private final JmxReporter           jmxReporter;
  private final MetricsService        metricsService;
  private final ZipkinReporterService zipkinReporterService;
  private final AdminService          adminService;
  private ServiceManager serviceManager = null;

  protected GenericService(final ConfigVals configVals,
                           final AdminConfig adminConfig,
                           final MetricsConfig metricsConfig,
                           final ZipkinConfig zipkinConfig,
                           final boolean testMode) {
    this.configVals = configVals;
    this.testMode = testMode;

    this.jmxReporter = JmxReporter.forRegistry(this.metricRegistry).build();

    if (adminConfig.enabled()) {
      this.adminService = new AdminService(this,
                                           adminConfig.port(),
                                           adminConfig.pingPath(),
                                           adminConfig.versionPath(),
                                           adminConfig.healthCheckPath(),
                                           adminConfig.threadDumpPath());
      this.addService(this.adminService);
    }
    else {
      logger.info("Admin service disabled");
      this.adminService = null;
    }

    if (metricsConfig.enabled()) {
      final int port = metricsConfig.port();
      final String path = metricsConfig.path();
      this.metricsService = new MetricsService(port, path);
      this.addService(this.metricsService);
      SystemMetrics.initialize(metricsConfig.standardExportsEnabled(),
                               metricsConfig.memoryPoolsExportsEnabled(),
                               metricsConfig.garbageCollectorExportsEnabled(),
                               metricsConfig.threadExportsEnabled(),
                               metricsConfig.classLoadingExportsEnabled(),
                               metricsConfig.versionInfoExportsEnabled());
    }
    else {
      logger.info("Metrics service disabled");
      this.metricsService = null;
    }

    if (zipkinConfig.enabled()) {
      final String zipkinUrl = format("http://%s:%d/%s",
                                      zipkinConfig.hostname(), zipkinConfig.port(), zipkinConfig.path());
      this.zipkinReporterService = new ZipkinReporterService(zipkinUrl, zipkinConfig.serviceName());
      this.addService(this.zipkinReporterService);
    }
    else {
      logger.info("Zipkin reporter service disabled");
      this.zipkinReporterService = null;
    }

    this.addListener(new GenericServiceListener(this), MoreExecutors.directExecutor());
  }

  public void init() {
    this.serviceManager = new ServiceManager(this.services);
    this.serviceManager.addListener(this.newListener());
    this.registerHealthChecks();
  }

  @Override
  protected void startUp()
      throws Exception {
    super.startUp();
    if (this.jmxReporter != null)
      this.jmxReporter.start();
    if (this.isMetricsEnabled())
      this.metricsService.startAsync();
    if (this.isAdminEnabled())
      this.adminService.startAsync();
    Runtime.getRuntime().addShutdownHook(Utils.shutDownHookAction(this));
  }

  @Override
  protected void shutDown()
      throws Exception {
    if (this.isAdminEnabled())
      this.adminService.shutDown();
    if (this.isMetricsEnabled())
      this.metricsService.stopAsync();
    if (this.isZipkinEnabled())
      this.zipkinReporterService.shutDown();
    if (this.jmxReporter != null)
      this.jmxReporter.stop();
    super.shutDown();
  }

  @Override
  public void close()
      throws IOException {
    this.stopAsync();
  }

  protected void addService(final Service service) { this.services.add(service); }

  protected void addServices(final Service service, final Service... services) {
    this.services.addAll(Lists.asList(service, services));
  }

  protected void registerHealthChecks() {
    this.getHealthCheckRegistry().register("thread_deadlock", new ThreadDeadlockHealthCheck());
    if (this.isMetricsEnabled())
      this.getHealthCheckRegistry().register("metrics_service", this.metricsService.getHealthCheck());
    this.getHealthCheckRegistry()
        .register(
            "all_services_healthy",
            new HealthCheck() {
              @Override
              protected Result check()
                  throws Exception {
                return serviceManager.isHealthy()
                       ? Result.healthy()
                       : Result.unhealthy(format("Incorrect state: %s",
                                                 Joiner.on(", ")
                                                       .join(serviceManager.servicesByState()
                                                                           .entries()
                                                                           .stream()
                                                                           .filter(kv -> kv.getKey() != State.RUNNING)
                                                                           .peek(kv -> logger.warn("Incorrect state - {}: {}",
                                                                                                   kv.getKey(), kv.getValue()))
                                                                           .map(kv -> format("%s: %s", kv.getKey(), kv.getValue()))
                                                                           .collect(Collectors.toList()))));
              }
            });
  }

  protected ServiceManager.Listener newListener() {
    final String serviceName = this.getClass().getSimpleName();
    return new ServiceManager.Listener() {
      @Override
      public void healthy() { logger.info("All {} services healthy", serviceName); }

      @Override
      public void stopped() { logger.info("All {} services stopped", serviceName); }

      @Override
      public void failure(final Service service) { logger.info("{} service failed: {}", serviceName, service); }
    };
  }

  protected ConfigVals getGenericConfigVals() { return this.configVals; }

  public MetricRegistry getMetricRegistry() { return this.metricRegistry; }

  public HealthCheckRegistry getHealthCheckRegistry() { return this.healthCheckRegistry; }

  public boolean isTestMode() { return this.testMode; }

  public boolean isZipkinEnabled() { return this.zipkinReporterService != null; }

  public boolean isAdminEnabled() { return this.adminService != null; }

  public boolean isMetricsEnabled() { return this.metricsService != null; }

  protected AdminService getAdminService() { return this.adminService; }

  protected MetricsService getMetricsService() { return this.metricsService; }

  public ZipkinReporterService getZipkinReporterService() { return this.zipkinReporterService; }

  public Brave getBrave() { return this.getZipkinReporterService().getBrave(); }
}
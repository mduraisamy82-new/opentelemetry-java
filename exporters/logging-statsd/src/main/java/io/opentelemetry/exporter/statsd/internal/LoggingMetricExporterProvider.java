package io.opentelemetry.exporter.statsd.internal;

import io.opentelemetry.exporter.statsd.StatsDMetricLogExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class LoggingMetricExporterProvider implements ConfigurableMetricExporterProvider {

	@Override
	public MetricExporter createExporter(ConfigProperties configProperties) {
		return StatsDMetricLogExporter.create();
	}

	@Override
	public String getName() {
		return "logging-statsd";
	}

}

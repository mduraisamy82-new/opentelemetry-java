package io.opentelemetry.exporter.statsd;

import io.opentelemetry.exporter.statsd.internal.StatsDBuilder;
import io.opentelemetry.exporter.statsd.internal.StatsDLogBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StatsDMetricLogExporter implements MetricExporter {

	private static final Logger logger = Logger.getLogger(StatsDMetricLogExporter.class.getName());

	private final AtomicBoolean isShutdown = new AtomicBoolean();

	private final AggregationTemporality aggregationTemporality;

	private StatsDMetricLogExporter(AggregationTemporality aggregationTemporality) {
		this.aggregationTemporality = aggregationTemporality;
	}

	public static StatsDMetricLogExporter create() {
		return new StatsDMetricLogExporter(AggregationTemporality.CUMULATIVE);
	}

	public static StatsDMetricLogExporter create(AggregationTemporality aggregationTemporality) {
		return new StatsDMetricLogExporter(aggregationTemporality);
	}

	@Override
	public CompletableResultCode export(Collection<MetricData> metrics) {
		if (isShutdown.get()) {
			return CompletableResultCode.ofFailure();
		}
		for (MetricData metricData : metrics) {
			StatsDBuilder<String> builder = StatsDLogBuilder.statsDBuilderFactory(metricData.getType());
			if (builder != null) {
				builder.setMetricName(metricData.getName())
					.setValue(metricData.getData().getPoints().stream().findFirst().get());
				logger.log(Level.INFO, "{0}", builder.build());
			}
		}
		return CompletableResultCode.ofSuccess();
	}

	@Override
	public CompletableResultCode flush() {
		CompletableResultCode resultCode = new CompletableResultCode();
		for (Handler handler : logger.getHandlers()) {
			try {
				handler.flush();
			}
			catch (Throwable t) {
				return resultCode.fail();
			}
		}
		return resultCode.succeed();
	}

	@Override
	public CompletableResultCode shutdown() {
		if (!isShutdown.compareAndSet(false, true)) {
			logger.log(Level.INFO, "Calling shutdown() multiple times.");
			return CompletableResultCode.ofSuccess();
		}
		return flush();
	}

	@Override
	public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
		return aggregationTemporality;
	}

	@Override
	public String toString() {
		return "StatsDMetricLogExporter{}";
	}

}

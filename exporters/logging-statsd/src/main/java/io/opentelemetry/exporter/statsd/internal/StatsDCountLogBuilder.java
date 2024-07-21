package io.opentelemetry.exporter.statsd.internal;

public abstract class StatsDCountLogBuilder extends StatsDLogBuilder {

	@Override
	protected void appendMetricType() {
		getBuilder().append('c');
	}

}

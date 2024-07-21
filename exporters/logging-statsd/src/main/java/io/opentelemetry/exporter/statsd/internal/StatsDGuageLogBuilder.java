package io.opentelemetry.exporter.statsd.internal;


public abstract class StatsDGuageLogBuilder extends StatsDLogBuilder {

	@Override
	protected void appendMetricType() {
		getBuilder().append('g');
	}

}

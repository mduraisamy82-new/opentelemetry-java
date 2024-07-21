package io.opentelemetry.exporter.statsd.internal;

import io.opentelemetry.sdk.metrics.data.LongPointData;

public class StatsDGuageLongLogBuilder extends StatsDGuageLogBuilder {

	@Override
	protected void appendMetricValue() {
		LongPointData pointData = (LongPointData) getValue();
    if(pointData !=null) {
      getBuilder().append(pointData.getValue());
    }
	}
}

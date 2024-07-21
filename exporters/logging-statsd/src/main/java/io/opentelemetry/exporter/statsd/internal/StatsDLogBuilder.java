package io.opentelemetry.exporter.statsd.internal;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public abstract class StatsDLogBuilder implements StatsDBuilder<String> {

  @Nullable
	private String metricName;

  @Nullable
	private PointData value;

	private final StringBuilder builder = new StringBuilder();

  @Override
	public StatsDBuilder<String> setMetricName(@Nonnull String metricName) {
    this.metricName = metricName;
		return this;

	}

	@Override
	public StatsDBuilder<String> setValue(@Nonnull PointData value) {
		this.value = value;
		return this;
	}

	@Override
	public String build() {
    builder.append(metricName);
		builder.append(":");
    if(value !=null) {
      appendMetricValue();
    }
		builder.append("|");
		appendMetricType();
		appendSampling();
		appendDimensions();
		return builder.toString();
	}

	protected void appendSampling() {
		// later
	}

	protected void appendDimensions() {
    if(value !=null) {
      Attributes attributes = value.getAttributes();
      if (attributes !=null && !attributes.isEmpty()) {
        builder.append("|#");
        Map<AttributeKey<?>, Object> mapValue = attributes.asMap();
        mapValue.forEach((k, v) -> builder.append(k).append(":").append(v.toString()).append(","));
        builder.deleteCharAt(builder.length() - 1);
      }
    }

	}

	protected StringBuilder getBuilder() {
		return builder;
	}

  @Nullable
	protected PointData getValue() {
		return value;
	}


	protected abstract void appendMetricType();

	protected abstract void appendMetricValue();

  @Nullable
	public static StatsDBuilder<String> statsDBuilderFactory(MetricDataType metricDataType) {
    StatsDBuilder<String> statsDBuilder = null;

		switch (metricDataType) {
      case DOUBLE_GAUGE : {
				statsDBuilder = new StatsDGuageDoubleLogBuilder();
				break;
			}
      case LONG_GAUGE : {
				statsDBuilder = new StatsDGuageLongLogBuilder();
				break;
			}
      case DOUBLE_SUM : {
				statsDBuilder = new StatsDCountDoubleLogBuilder();
				break;
			}
      case LONG_SUM : {
				statsDBuilder = new StatsDCountLongLogBuilder();
				break;
			}
      default :{
        statsDBuilder = null;
      }
		}

		return statsDBuilder;
	}

}

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.view;

import static io.opentelemetry.sdk.metrics.internal.view.ViewRegistry.DEFAULT_REGISTERED_VIEW;
import static io.opentelemetry.sdk.metrics.internal.view.ViewRegistry.toGlobPatternPredicate;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.internal.testing.slf4j.SuppressLogger;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.InstrumentValueType;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.internal.debug.SourceInfo;
import io.opentelemetry.sdk.metrics.internal.descriptor.InstrumentDescriptor;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ViewRegistryTest {

  @RegisterExtension LogCapturer logs = LogCapturer.create().captureForType(ViewRegistry.class);

  private static final InstrumentationScopeInfo INSTRUMENTATION_SCOPE_INFO =
      InstrumentationScopeInfo.builder("name")
          .setVersion("version")
          .setSchemaUrl("schema_url")
          .build();

  private static RegisteredView registeredView(InstrumentSelector instrumentSelector, View view) {
    return RegisteredView.create(
        instrumentSelector, view, AttributesProcessor.noop(), SourceInfo.fromCurrentStack());
  }

  @Test
  void findViews_SelectionOnType() {
    RegisteredView registeredView =
        registeredView(
            InstrumentSelector.builder().setType(InstrumentType.COUNTER).build(),
            View.builder().setDescription("description").build());
    ViewRegistry viewRegistry =
        ViewRegistry.create(
            DefaultAggregationSelector.getDefault(), Collections.singletonList(registeredView));

    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(registeredView));
    // this one hasn't been configured, so it gets the default still.
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.UP_DOWN_COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));

    assertThat(logs.getEvents()).hasSize(0);
  }

  @Test
  void findViews_SelectionOnName() {
    RegisteredView registeredView =
        registeredView(
            InstrumentSelector.builder().setName("overridden").build(),
            View.builder().setDescription("description").build());
    ViewRegistry viewRegistry =
        ViewRegistry.create(
            DefaultAggregationSelector.getDefault(), Collections.singletonList(registeredView));

    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "overridden", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(registeredView));
    // this one hasn't been configured, so it gets the default still.
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "default", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));

    assertThat(logs.getEvents()).hasSize(0);
  }

  @Test
  void findViews_MultipleMatchingViews() {
    RegisteredView registeredView1 =
        registeredView(
            InstrumentSelector.builder().setName("overridden").build(),
            View.builder().setAggregation(Aggregation.explicitBucketHistogram()).build());
    RegisteredView registeredView2 =
        registeredView(
            InstrumentSelector.builder().setName("*").build(),
            View.builder().setAggregation(Aggregation.sum()).build());

    ViewRegistry viewRegistry =
        ViewRegistry.create(
            DefaultAggregationSelector.getDefault(),
            Arrays.asList(registeredView1, registeredView2));

    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "overridden", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Arrays.asList(registeredView1, registeredView2));
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "default", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(registeredView2));

    assertThat(logs.getEvents()).hasSize(0);
  }

  @Test
  void findViews_SelectionTypeAndName() {
    RegisteredView registeredView =
        registeredView(
            InstrumentSelector.builder()
                .setType(InstrumentType.COUNTER)
                .setName("overrides")
                .build(),
            View.builder().setAggregation(Aggregation.explicitBucketHistogram()).build());
    ViewRegistry viewRegistry =
        ViewRegistry.create(
            DefaultAggregationSelector.getDefault(), Collections.singletonList(registeredView));

    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "overrides", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(registeredView));
    // this one hasn't been configured, so it gets the default still..
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "overrides", "", "", InstrumentType.UP_DOWN_COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    // this one hasn't been configured, so it gets the default still..
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "default", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));

    assertThat(logs.getEvents()).hasSize(0);
  }

  @Test
  @SuppressLogger(ViewRegistry.class)
  void findViews_DefaultAggregationSelector() {
    RegisteredView registeredView =
        registeredView(
            InstrumentSelector.builder().setName("overridden").build(),
            View.builder().setDescription("description").build());
    // Configure a default aggregation selector that defaults to exponential histogram aggregation
    // for histogram and gauge instruments. Note, gauges are incompatible with exponential
    // histograms.
    DefaultAggregationSelector defaultAggregationSelector =
        instrumentType ->
            instrumentType == InstrumentType.HISTOGRAM
                    || instrumentType == InstrumentType.OBSERVABLE_GAUGE
                ? Aggregation.base2ExponentialBucketHistogram()
                : Aggregation.defaultAggregation();

    ViewRegistry viewRegistry =
        ViewRegistry.create(defaultAggregationSelector, Collections.singletonList(registeredView));

    // Counter instrument should result in default view
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "test", "", "", InstrumentType.COUNTER, InstrumentValueType.DOUBLE),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    // Histogram instrument named overridden should match the registered view, for which the
    // aggregation is explicit bucket histogram
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "overridden", "", "", InstrumentType.HISTOGRAM, InstrumentValueType.DOUBLE),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(registeredView));
    // Histogram instrument named default should match no views, and should receive a default view
    // with the exponential histogram aggregation as dictated by the default aggregation
    // selector
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "default", "", "", InstrumentType.HISTOGRAM, InstrumentValueType.DOUBLE),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(
            Collections.singletonList(
                registeredView(
                    InstrumentSelector.builder().setName("*").build(),
                    View.builder()
                        .setAggregation(Aggregation.base2ExponentialBucketHistogram())
                        .build())));
    // At this point, no warning logs should have been produced.
    assertThat(logs.getEvents()).hasSize(0);
    // Gauge instrument matches no views, and should receive a default view with the exponential
    // histogram aggregation as dictated by the default aggregation selector. However, gauge is
    // incompatible with exponential histogram aggregation and the default view should be returned.
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "default", "", "", InstrumentType.OBSERVABLE_GAUGE, InstrumentValueType.DOUBLE),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    logs.assertContains(
        "Instrument default aggregation base2_exponential_bucket_histogram is incompatible with instrument default of type OBSERVABLE_GAUGE");
  }

  @Test
  @SuppressLogger(ViewRegistry.class)
  void findViews_IncompatibleViewIgnored() {
    RegisteredView registeredView =
        registeredView(
            InstrumentSelector.builder().setType(InstrumentType.OBSERVABLE_COUNTER).build(),
            View.builder().setAggregation(Aggregation.explicitBucketHistogram()).build());
    ViewRegistry viewRegistry =
        ViewRegistry.create(
            DefaultAggregationSelector.getDefault(), Collections.singletonList(registeredView));

    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "test", "", "", InstrumentType.OBSERVABLE_COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));

    logs.assertContains(
        "View aggregation explicit_bucket_histogram is incompatible with instrument test of type OBSERVABLE_COUNTER");
  }

  @Test
  void defaults() {
    ViewRegistry viewRegistry = ViewRegistry.create();
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.UP_DOWN_COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.HISTOGRAM, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.OBSERVABLE_COUNTER, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "", "", "", InstrumentType.OBSERVABLE_GAUGE, InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));
    assertThat(
            viewRegistry.findViews(
                InstrumentDescriptor.create(
                    "",
                    "",
                    "",
                    InstrumentType.OBSERVABLE_UP_DOWN_COUNTER,
                    InstrumentValueType.LONG),
                INSTRUMENTATION_SCOPE_INFO))
        .isEqualTo(Collections.singletonList(DEFAULT_REGISTERED_VIEW));

    assertThat(logs.getEvents()).hasSize(0);
  }

  @Test
  void matchesName() {
    assertThat(toGlobPatternPredicate("foo").test("foo")).isTrue();
    assertThat(toGlobPatternPredicate("foo").test("Foo")).isTrue();
    assertThat(toGlobPatternPredicate("foo").test("bar")).isFalse();
    assertThat(toGlobPatternPredicate("fo?").test("foo")).isTrue();
    assertThat(toGlobPatternPredicate("fo??").test("fooo")).isTrue();
    assertThat(toGlobPatternPredicate("fo?").test("fob")).isTrue();
    assertThat(toGlobPatternPredicate("fo?").test("fooo")).isFalse();
    assertThat(toGlobPatternPredicate("*").test("foo")).isTrue();
    assertThat(toGlobPatternPredicate("*").test("bar")).isTrue();
    assertThat(toGlobPatternPredicate("*").test("baz")).isTrue();
    assertThat(toGlobPatternPredicate("*").test("foo.bar.baz")).isTrue();
    assertThat(toGlobPatternPredicate("fo*").test("fo")).isTrue();
    assertThat(toGlobPatternPredicate("fo*").test("foo")).isTrue();
    assertThat(toGlobPatternPredicate("fo*").test("fooo")).isTrue();
    assertThat(toGlobPatternPredicate("fo*").test("foo.bar.baz")).isTrue();
    assertThat(toGlobPatternPredicate("f()[]$^.{}|").test("f()[]$^.{}|")).isTrue();
    assertThat(toGlobPatternPredicate("f()[]$^.{}|?").test("f()[]$^.{}|o")).isTrue();
    assertThat(toGlobPatternPredicate("f()[]$^.{}|*").test("f()[]$^.{}|ooo")).isTrue();
  }
}

package datadog.trace.instrumentation.kafka_clients;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateNext;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.closePrevious;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.propagate;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.core.datastreams.TagsProcessor.GROUP_TAG;
import static datadog.trace.core.datastreams.TagsProcessor.PARTITION_TAG;
import static datadog.trace.core.datastreams.TagsProcessor.TOPIC_TAG;
import static datadog.trace.core.datastreams.TagsProcessor.TYPE_TAG;
import static datadog.trace.instrumentation.kafka_clients.KafkaDecorator.BROKER_DECORATE;
import static datadog.trace.instrumentation.kafka_clients.KafkaDecorator.KAFKA_DELIVER;
import static datadog.trace.instrumentation.kafka_clients.KafkaDecorator.KAFKA_LEGACY_TRACING;
import static datadog.trace.instrumentation.kafka_clients.TextMapExtractAdapter.GETTER;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import datadog.trace.api.Config;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import datadog.trace.bootstrap.instrumentation.api.InstrumentationTags;
import datadog.trace.bootstrap.instrumentation.api.PathwayContext;
import java.util.LinkedHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IteratorUtils {
  private static final Logger log = LoggerFactory.getLogger(IteratorUtils.class);

  public static void startNewRecordSpan(ConsumerRecord<?, ?> val, CharSequence operationName, String group, KafkaDecorator decorator) {
    try {
      closePrevious(true);
      AgentSpan span, queueSpan = null;
      if (val != null) {
        if (!Config.get().isKafkaClientPropagationDisabledForTopic(val.topic())) {
          final Context spanContext = propagate().extract(val.headers(), TextMapExtractAdapter.GETTER);
          long timeInQueueStart = TextMapExtractAdapter.GETTER.extractTimeInQueueStart(val.headers());
          if (timeInQueueStart == 0 || KAFKA_LEGACY_TRACING) {
            span = startSpan(operationName, spanContext);
          } else {
            queueSpan =
                startSpan(
                    KAFKA_DELIVER, spanContext, MILLISECONDS.toMicros(timeInQueueStart), false);
            BROKER_DECORATE.afterStart(queueSpan);
            BROKER_DECORATE.onTimeInQueue(queueSpan, val);
            span = startSpan(operationName, queueSpan.context());
            BROKER_DECORATE.beforeFinish(queueSpan);
            // The queueSpan will be finished after inner span has been activated to ensure that
            // spans are written out together by TraceStructureWriter when running in strict mode
          }
          PathwayContext pathwayContext =
              propagate().extractBinaryPathwayContext(val.headers(), TextMapExtractAdapter.GETTER);
          span.mergePathwayContext(pathwayContext);

          LinkedHashMap<String, String> sortedTags = new LinkedHashMap<>();
          sortedTags.put(GROUP_TAG, group);
          sortedTags.put(PARTITION_TAG, String.valueOf(val.partition()));
          sortedTags.put(TOPIC_TAG, val.topic());
          sortedTags.put(TYPE_TAG, "kafka");
          AgentTracer.get().setDataStreamCheckpoint(span, sortedTags);
        } else {
          span = startSpan(operationName, null);
        }
        if (val.value() == null) {
          span.setTag(InstrumentationTags.TOMBSTONE, true);
        }
        decorator.afterStart(span);
        decorator.onConsume(span, val, group);
        activateNext(span);
        if (null != queueSpan) {
          queueSpan.finish();
        }
      }
    } catch (final Exception e) {
      log.debug("Error starting new record span", e);
    }
  }
}
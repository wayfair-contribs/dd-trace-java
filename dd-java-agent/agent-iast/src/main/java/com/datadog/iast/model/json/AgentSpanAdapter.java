package com.datadog.iast.model.json;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AgentSpanAdapter extends JsonAdapter<AgentSpan> {

  @Override
  public void toJson(@Nonnull final JsonWriter writer, final @Nullable AgentSpan value)
      throws IOException {
    if (value == null || value.getSpanId() == null) {
      writer.nullValue();
      return;
    }
    writer.value(value.getSpanId().toLong());
  }

  @Nullable
  @Override
  public AgentSpan fromJson(@Nonnull final JsonReader reader) throws IOException {
    throw new UnsupportedOperationException("AgentSpan deserialization is not supported");
  }
}

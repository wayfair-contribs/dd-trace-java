package com.datadog.appsec;

import com.datadog.appsec.event.ChangeableFlow;
import com.datadog.appsec.event.EventType;
import com.datadog.appsec.gateway.AppSecRequestContext;
import com.google.auto.service.AutoService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@AutoService(AppSecModule.class)
public class RequestContextModule implements AppSecModule {
  @Override
  public String getName() {
    return "request_context";
  }

  @Override
  public Collection<EventSubscription> getEventSubscriptions() {
    return Arrays.asList(
      new EventSubscription(EventType.REQUEST_START, 0) {
        @Override
        public void onEvent(ChangeableFlow flow, AppSecRequestContext ctx, EventType eventType) {
          flow.setResult(ctx);
        }
      },
      new EventSubscription(EventType.REQUEST_END, 0) {
        @Override
        public void onEvent(ChangeableFlow flow, AppSecRequestContext ctx, EventType eventType) {

        }
      }
    );
  }

  @Override
  public Collection<DataSubscription> getDataSubscriptions() {
    return Collections.emptyList();
  }
}

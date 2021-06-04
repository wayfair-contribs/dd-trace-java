package com.datadog.appsec.event;

import datadog.trace.api.gateway.Flow;

public class ChangeableFlow<T> implements Flow<T> {
  Action action = Action.Noop.INSTANCE;

  private T result;

  public boolean isBlocking() {
    return action.isBlocking();
  }

  public void setAction(Action blockingAction) {
    this.action = blockingAction;
  }

  @Override
  public Action getAction() {
    return action;
  }

  @Override
  public T getResult() {
    return result;
  }

  public void setResult(T result) {
    this.result = result;
  }
}

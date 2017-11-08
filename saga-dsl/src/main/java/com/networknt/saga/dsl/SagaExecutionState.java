package com.networknt.saga.dsl;

import org.apache.commons.lang.builder.ToStringBuilder;

public class SagaExecutionState {

  private int currentlyExecuting;
  private boolean compensating;
  private boolean endState;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public SagaExecutionState() {
  }

  public SagaExecutionState(int currentlyExecuting, boolean compensating) {
    this.currentlyExecuting = currentlyExecuting;
    this.compensating = compensating;
  }

  public int getCurrentlyExecuting() {
    return currentlyExecuting;
  }

  public void setCurrentlyExecuting(int currentlyExecuting) {
    this.currentlyExecuting = currentlyExecuting;
  }

  public boolean isCompensating() {
    return compensating;
  }

  public void setCompensating(boolean compensating) {
    this.compensating = compensating;
  }

  public SagaExecutionState startCompensating() {
    return new SagaExecutionState(currentlyExecuting, true);
  }

  public SagaExecutionState nextState(int size) {
    return new SagaExecutionState(compensating ? currentlyExecuting - size : currentlyExecuting + size, compensating);
  }

  public boolean isEndState() {
    return endState;
  }

  public void setEndState(boolean endState) {
    this.endState = endState;
  }

  public static SagaExecutionState makeEndState() {
    SagaExecutionState x = new SagaExecutionState();
    x.setEndState(true);
    return x;
  }
}

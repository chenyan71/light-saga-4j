package com.networknt.saga.cdc.mysql;


import com.networknt.eventuate.cdc.common.BinLogEvent;
import com.networknt.eventuate.cdc.common.BinlogFileOffset;
import com.networknt.saga.core.message.common.MessageImpl;


public class MessageWithDestination implements BinLogEvent {
  private final String destination;
  private final MessageImpl message;
  private BinlogFileOffset binlogFileOffset;

  public MessageWithDestination(String destination, MessageImpl message, BinlogFileOffset binlogFileOffset) {
    this.destination = destination;
    this.message = message;
    this.binlogFileOffset = binlogFileOffset;
  }

  public String getDestination() {
    return destination;
  }

  public MessageImpl getMessage() {
    return message;
  }

  public BinlogFileOffset getBinlogFileOffset() {
    return binlogFileOffset;
  }
}

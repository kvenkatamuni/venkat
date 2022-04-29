package com.paanini.jiffy.models;

import java.util.UUID;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ImportData {
  private UUID transactionId;

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
  }
}

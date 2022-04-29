package com.paanini.jiffy.proc.api;

import com.paanini.jiffy.exception.DataProcessingException;

public interface Ingestible {
  void ingest(IngestContext ctx) throws DataProcessingException;
}

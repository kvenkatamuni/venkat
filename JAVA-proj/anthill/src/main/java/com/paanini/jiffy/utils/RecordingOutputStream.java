package com.paanini.jiffy.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RecordingOutputStream extends BufferedOutputStream {

  private boolean record;
  private ByteArrayOutputStream bos;

  public RecordingOutputStream(ByteArrayOutputStream out, boolean record) {
    super(out);
    bos = out;
    this.record = record;
  }

  public void record(boolean record) {
    this.record = record;
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    if(this.record) {
      super.write(b, off, len);
    }
  }
  @Override
  public synchronized void write(int b) throws IOException {
    if(this.record) {
      super.write(b);
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    if(this.record) {
      super.write(b);
    }
  }

  @Override
  public String toString() {
    return new String(bos.toByteArray());
  }
}

package com.paanini.jiffy.utils;

import com.paanini.jiffy.exception.InvalidHeaderException;
import com.paanini.jiffy.exception.ProcessingException;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Created by rahul on 5/9/15.
 */
public class StreamProcessor {

  InputStreamReader ir;

  private boolean skipLineFeed;

  public StreamProcessor(InputStream is){
    this(new BOMInputStream(is,false,
                    ByteOrderMark.UTF_8 /*, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE*/),
            Charset.defaultCharset());
  }

  public StreamProcessor(InputStream is, Charset charset){
    ir = new InputStreamReader(is ,charset);
  }

  /**
   A Logical limit to search for a header line. Beyond this limit the header
   might not be found and we might end up in reading the whole stuff into
   memory.
   */
  private static final long CSV_HEADER_LENGTH = 1024L * 512L;

  /**
   * @return true if the header line ends with a CODE_PT_CR. The subsequent readers
   * will have to check if the next character is a CODE_PT_LF and skip when this
   * flag is set.
   */
  public boolean isSkipLineFeed() {
    return skipLineFeed;
  }

  static int CODE_PT_LF = 10;
  static int CODE_PT_CR = 13;

  public String readHeader() throws InvalidHeaderException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < CSV_HEADER_LENGTH; i++) {
      try {
        int cp = ir.read();
        if(cp == -1){
          throw new InvalidHeaderException("No Header Found");
        }

        if ((cp == CODE_PT_LF) || (cp == CODE_PT_CR)) {
          if(cp == CODE_PT_CR){
            this.skipLineFeed = true;
          }
          return sb.toString();
        }
        sb.appendCodePoint(cp);

      } catch (IOException e) {
        throw new InvalidHeaderException(e);
      }
    }
    throw new InvalidHeaderException("Could not detect the header within the limits");
  }


  public long copyRest(OutputStream sink) throws IOException, ProcessingException {

    try(OutputStreamWriter osw = new OutputStreamWriter(sink,ir.getEncoding())) {

      if(this.isSkipLineFeed()){
        int cp = ir.read();
        if(cp == -1){
          return -1; // Nothing to read just return
        }

        if(cp != CODE_PT_LF){
          throw new ProcessingException("Inappropriate line ending \\r, must be \\n or \\r\\n");
          //osw.write(cp);
        }
      }

      char[] buf = new char[1024*8];
      long nread = 0L;
      int n;
      while ((n = ir.read(buf)) > 0) {
        osw.write(buf, 0, n);
        nread += n;
      }
      osw.flush();
      return nread;
    }
  }
}
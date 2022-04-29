package com.paanini.jiffy.proc.impl;

import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.InvalidHeaderException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.proc.api.IngestContext;
import com.paanini.jiffy.proc.api.Ingestible;
import com.paanini.jiffy.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by appmgr on 27/4/16.
 */
public class AnyFile implements Ingestible {

  private final boolean text;

  public AnyFile(){
    this(true);

  }

  public AnyFile(boolean text){
    this.text = text;
  }

  @Override
  public void ingest(IngestContext ctx) throws DataProcessingException {
    try {
      Path temp = Paths.get(FileUtils.getTempFileName());
      Files.deleteIfExists(temp);
      if(this.text) {
        FileUtils.copyTextStream(ctx.getInputStream(), temp);
      }else{
        FileUtils.copyBinaryStream(ctx.getInputStream(), temp);
      }
      ctx.setStagedFileLocation(temp);

    } catch (IOException |
            InvalidHeaderException |
            ProcessingException e) {
      throw new DataProcessingException(e.getMessage(), e);
    }
  }
}
package com.paanini.jiffy.proc.impl;

import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.proc.api.IngestContext;
import com.paanini.jiffy.proc.api.Ingestible;

public class CsvFile  implements Ingestible {
  public static final String CRLF = "crlf";

  @Override
  public void ingest(IngestContext ctx) throws DataProcessingException {
    throw new ProcessingException("not yet supported");
    /*try {

      Path temp = Paths.get(FileUtils.getTempFileName());
      Files.deleteIfExists(temp);
      Pair<DataSheetSchema, String[]> result = FileUtils.processStream(ctx.getInputStream(), temp);
      ctx.setIngestedSchema(result.getF());

      String[] warnings = result.getS();
      if (warnings != null) {
        if(Stream.of(warnings).anyMatch(CRLF::equals)){
          ctx.setCRLFEnding();
        }
      }

      ctx.setStagedFileLocation(temp);

    } catch (IOException |
            InvalidHeaderException |
            ProcessingException e) {
      throw new DataProcessingException(e.getMessage(), e);
    }*/
  }
}

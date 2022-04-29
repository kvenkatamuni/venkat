package com.paanini.jiffy.utils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StreamingDocument implements StreamingOutput {

  static Logger logger = LoggerFactory.getLogger(StreamingDocument.class);
  private final JsonConsumer consumer;
  private final BasicFileProps file;

  public StreamingDocument(BasicFileProps file , JsonConsumer consumer) {
    this.file = file;
    this.consumer = consumer;
  }

  @Override
  public void write(OutputStream os) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    RecordingOutputStream ros = new RecordingOutputStream(bos, false);

    TeeOutputStream tos = new TeeOutputStream(os, ros);

    JsonFactory jf = new JsonFactory();
    JsonGenerator g = jf.createGenerator(tos, JsonEncoding.UTF8);
    g.writeStartObject(); // Outer Object

    g.writeStringField("id", file.getId());
    g.writeStringField("name", file.getName());
    g.writeStringField("type", file.getType().toString());
    g.writeNumberField("createAt", file.getCreateAt());
    g.writeNumberField("lastModified", file.getLastModified());
    g.writeStringField("createdBy", file.getCreatedBy());
    //        g.writeStringField("linkId", file.getLinkId());
    if(file.getLastError() != null) {
      g.writeStringField("lastError", file.getLastError());
    } else {
      g.writeStringField("lastError", null);
    }
    if(file.getSubType() != null) {
      g.writeStringField("subType", file.getSubType().name());
    }
    if(file.getStatus() != null) {
      g.writeStringField("status", file.getStatus().toString());
    }

    if(file instanceof ExtraFileProps) {
      ExtraFileProps ft = (ExtraFileProps) file;
      g.writeStringField("parentId", ft.getParentId());
      g.writeFieldName("privileges");
      g.writeStartArray();
      for (AccessEntry entry : ft.getPrivileges()) {

        g.writeStartObject();
        g.writeStringField("email", entry.getEmail());
        g.writeFieldName("permissions");
        g.writeStartArray();
        for (Permission permission : entry.getPermissions()) {
          g.writeString(permission.toString());
        }
        g.writeEndArray();
        g.writeEndObject();
      }
      g.writeEndArray();
    }
    consumer.consume(g, ros);
    g.writeEndObject(); // Outer object
    g.close();

  }

  @FunctionalInterface
  public interface JsonConsumer{
    void consume(JsonGenerator g, RecordingOutputStream ros) throws IOException;
  }
/*
    public void queryAndLoadData(DataSheet ds, JsonGenerator g)  {
        QueryContext ctx = new QueryContext(null,this.drill,this.documentStore);
        ctx.setDataSheet(ds);
        ctx.setId(ds.getId());
        ctx.setJsonGenerator(g);
        try(Queryable queryable = getQueryObject(ds)) {
            queryable.query(ctx);
                try {
                    queryable.stream(ctx);
                } catch (DataProcessingException|IOException e) {
                    //Nothing much can be done here at this stage
                    logger.error(e.getMessage(), e);
                }
        }catch(DataProcessingException e){
            logger.error(e.getMessage(), e);
            //TODO : Write Error here
        }

    }

    Queryable getQueryObject(DataSheet ds){
        if (ds.getStatus().equals(SimpleFile.Status.UNPUBLISHED)) {
            if (ds.getSourceType().equals(DataSheet.SRC_TYPE_JSON)){
                return new SourceJsonQuery();
            } else if (ds.getSourceType().equals(DataSheet.SRC_TYPE_SQL)) {
                return new RawSqlQuery();
            } else {
                return new SourceCsvQuery();
            }
        } else {
            return new SqlQuery();
        }
    }
    */
}

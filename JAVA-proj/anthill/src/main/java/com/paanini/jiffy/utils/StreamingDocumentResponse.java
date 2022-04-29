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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StreamingDocumentResponse {


  static Logger logger = LoggerFactory.getLogger(StreamingDocument.class);
  private final StreamingDocumentResponse.JsonConsumer consumer;
  private final BasicFileProps file;

  public StreamingDocumentResponse(BasicFileProps file , StreamingDocumentResponse.JsonConsumer consumer) {
    this.file = file;
    this.consumer = consumer;
  }


  public StreamingResponseBody getResponse(){
    return outputStream -> {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      RecordingOutputStream ros = new RecordingOutputStream(bos, false);

      TeeOutputStream tos = new TeeOutputStream(outputStream, ros);

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
        processExtraFileProps(g);
      }
      consumer.consume(g, ros);
      g.writeEndObject(); // Outer object
      g.close();
    };
  }

  private void processExtraFileProps(JsonGenerator g) throws IOException {
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

  @FunctionalInterface
  public interface JsonConsumer{
    void consume(JsonGenerator g, RecordingOutputStream ros) throws IOException;
  }
}

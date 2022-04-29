package com.paanini.jiffy.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.nodes.Status;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface WebResponseHandler {
  default Response okResponse(ResultMap r) {
    return Response.ok().entity(r.build()).build();
  }

  default ResponseEntity okResponseEntity(ResultMap r) {
    return ResponseEntity.ok(r.build());
  }



  default Response okResponse(String key, String value) {
    return Response.ok().entity(
            new ResultMap()
                    .add(key, value)
                    .build()).build();
  }

  default ResponseEntity okResponseEntity(String key, String value){
    return ResponseEntity.ok(new ResultMap()
            .add(key, value)
            .build());
  }

  default Response status(Response.Status status, String message) {
    return Response.status(status).entity(
            new ResultMap()
                    .add("status", "error")
                    .add("message",message)
                    .build()).build();
  }



  default ResponseEntity errorResponseEntity(String message) {
    return ResponseEntity.badRequest().body(new ResultMap()
            .add("status","error")
            .add("message", message)
            .build());
  }

  default Response errorResponse(String message) {
    return Response
            .serverError()
            .entity( new ResultMap()
                    .add("status","error")
                    .add("message", message)
                    .build())
            .build();
  }


  default Response errorResponse(String message, String errCode) {
    return Response
            .serverError()
            .entity( new ResultMap()
                    .add("status","error")
                    .add("errorCode", errCode)
                    .add("message", message)
                    .build())
            .build();
  }

  default Response errorResponseSql(String message) {
    return Response
            .serverError()
            .entity(message)
            .build();
  }



  default Response newStreamingDocument(BasicFileProps file,
                                        StreamingDocument.JsonConsumer c) {
    try {
      StreamingOutput so = new StreamingDocument(file, c);
      return Response.ok(so).build();
    }catch (Exception e){
      return errorResponse(e.getMessage());
    }
  }

  default StreamingResponseBody newStreamingResponseDocument(BasicFileProps file,
                                                             StreamingDocumentResponse.JsonConsumer c) {
    StreamingDocumentResponse documentResponse = new StreamingDocumentResponse(file,c);
    return documentResponse.getResponse();
  }



  default Optional<String> getReferrer(HttpHeaders headers) {
    if (headers == null) {
      return Optional.empty();
    }
    List<String> referrer = headers.getRequestHeader("referrer");
    return referrer == null ? Optional.empty() : Optional.of(referrer.get(0));
  }
}

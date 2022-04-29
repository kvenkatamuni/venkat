package com.paanini.jiffy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paanini.jiffy.models.ApiResponse;
import com.paanini.jiffy.services.PresentationService;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.files.Presentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.ws.rs.Consumes;
@RestController
@RequestMapping("/documents/v2")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class DocumentsWSV2 implements WebResponseHandler {

    @Autowired
    private VfsManager vfsManager;

    @Autowired
    PresentationService presentationService;

    static Logger logger = LoggerFactory.getLogger(DocumentsWSV2.class);

    @GetMapping("/presentation/{id}")
    public ResponseEntity<StreamingResponseBody> getDocument(@PathVariable("id") String id) {

       return getPresentation(id);
    }

    @PostMapping("/presentation")
    public ResponseEntity<StreamingResponseBody> saveDashboard(@RequestBody Presentation presentation,
                                             @RequestParam(name ="parentId") String parentId,
                                             @RequestParam(name ="path") String parentPath) {
        String id = presentationService.saveDashboardService(presentation, parentId);
        return getPresentation(id);
    }

    @PutMapping("/presentation/{id}")
    @Consumes(MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody>  updateDashboard(@RequestBody Presentation presentation,
                                @PathVariable("id") String id) {
        presentationService.updateDashboardService(presentation);
        return getPresentation(id);
    }

    private ResponseEntity<StreamingResponseBody> getPresentation(String id) {
        Presentation ppt = vfsManager.getFile(id);
        Presentation updatedPpt = vfsManager.updateCardUUID(ppt);
        StreamingDocumentResponse.JsonConsumer c = (g, os) -> {
            ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
            g.writeFieldName("content");
            try {
                objectMapper.writeValue(g, updatedPpt.getContent());
            } catch (Exception e) {
                throw e;
            }
        };
        StreamingResponseBody responseBody = newStreamingResponseDocument(updatedPpt,c);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(responseBody);
    }

    @GetMapping("/presentation/{id}/cardId")
    @ResponseBody
    public ResponseEntity getPresentationCardId(@PathVariable("id") String id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(presentationService.getCardIds(id));
    }

}

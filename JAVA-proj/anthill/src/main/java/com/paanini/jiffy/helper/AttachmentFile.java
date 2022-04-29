package com.paanini.jiffy.helper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.dto.JfsFile;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.models.JiffyTableAttachment;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class AttachmentFile extends JiffyTableAttachment {

  Path tempLoc;
  static Logger logger = LoggerFactory.getLogger(AttachmentFile.class);

  private AttachmentFile(String attachment) {
    super();

    this.setName(attachment);
    this.setTempRef(UUID.randomUUID().toString());
  }

  public static AttachmentFile from(MultipartFile attachment, String tempLocation) throws IOException {
    AttachmentFile attachmentFile = new AttachmentFile(AttachmentFile.getFileName(attachment) );
    InputStream inStream = attachment.getInputStream();
    attachmentFile.moveToTemp(tempLocation, inStream);
    attachmentFile.setTempLoc(Paths.get(tempLocation, attachmentFile.getTempRef()));
    return attachmentFile;
  }

  public static AttachmentFile from(JfsFile jfsFile, String tempLocation, String jfsBaseUrl,
                                    CipherService cipherService) throws IOException {
    String jfsUrl = "%s/handle/download/%s/content";

    try {
      String jfsFileName = getJFSFileName(jfsFile, jfsBaseUrl, cipherService);
      DocubeHTTPRequest request = HttpRequestBuilder
              .get(String.format(jfsUrl, jfsBaseUrl, jfsFile.getfId()))
              .useJWT(cipherService)
              .bypassSsl()
              .build();
      File file = request.download();
      AttachmentFile attachmentFile = new AttachmentFile(jfsFileName);
      InputStream inStream = new FileInputStream(file);
      attachmentFile.moveToTemp(tempLocation, inStream);
      attachmentFile.setTempLoc(Paths.get(tempLocation, attachmentFile.getTempRef()));
      return attachmentFile;
    } catch (DocubeHTTPException e) {
      logger.error("[Attachment file] : {}", e.getMessage());
      throw new IOException(e.getMessage());
    }
  }

  public static AttachmentFile from(Path attachmentFolder,String tempLocation,
                                    String name) throws IOException {
    AttachmentFile attachmentFile = new AttachmentFile(name);
    Path toPath = Paths.get(tempLocation, attachmentFile.getTempRef());

    logger.debug("[AF] moving {} to {}", attachmentFolder, toPath);
    FileUtils.moveDirectory(attachmentFolder, toPath);
    attachmentFile.setTempLoc(toPath);
    return attachmentFile;
  }

  private static String getJFSFileName(JfsFile jfsFile, String jfsBase, CipherService cipherService) throws IOException {
    String jfsUrl = "%s/handle/download/%s/meta";
    try {
      DocubeHTTPRequest request = HttpRequestBuilder
              .get(String.format(jfsUrl, jfsBase, jfsFile.getfId()))
              .useJWT(cipherService)
              .bypassSsl()
              .build();
      String result = request.execute();
      JfsFile meta = ObjectMapperFactory.createObjectMapper()
              .readValue(result, JfsFile.class);
      return meta.getName();
    } catch (DocubeHTTPException e) {
      logger.error("[Attachment file] : JFS file : {}", e.getMessage());
      throw new IOException(e.getMessage());
    } catch (JsonParseException | JsonMappingException e) {
      logger.error("[Attachment file] : JFS file : {}", e.getMessage());
      throw new IOException("Error While parsing output from service :" + e.getMessage());
    }
  }

  private void moveToTemp(String tempLocation, InputStream inStream) throws IOException {
    // make sure attachment is proper file and moved it to system temp directory
    Path tempfilePath = Paths.get(FileUtils.getTempFileName());
    Files.copy(inStream, tempfilePath, StandardCopyOption.REPLACE_EXISTING);
    Files.move(tempfilePath, Paths.get(tempLocation, this.getTempRef()));
  }

  private static String getFileName(final MultipartFile attachment) {
    return attachment.getOriginalFilename();
  }

  public Path getTempLoc() {
    return tempLoc;
  }

  private void setTempLoc(Path loc) {
    tempLoc = loc;
  }
}

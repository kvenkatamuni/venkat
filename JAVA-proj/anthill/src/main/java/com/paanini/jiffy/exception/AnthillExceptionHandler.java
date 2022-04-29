package com.paanini.jiffy.exception;

import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.utils.ResultMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Aswath Murugan
 * @created 22/10/20
 */
@ControllerAdvice
public class AnthillExceptionHandler {

  static final Logger log = LoggerFactory.getLogger(AnthillExceptionHandler.class);

  @ExceptionHandler(ContentRepositoryException.class)
  public final ResponseEntity<Object> handleContentRepositoryException(ContentRepositoryException ex) {
    log.error("ContentRepositoryException error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ProcessingException.class)
  public final ResponseEntity<Object> handleAnthillProcessingException(
      ProcessingException ex) {
    log.error("Anthill Processing Exception rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(DataProcessingException.class)
  public final ResponseEntity<Object> handleDataProcessingException(
      DataProcessingException ex) {
    log.error("Anthill DataProcessingException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(DocubeException.class)
  public final ResponseEntity<Object> handleDocubeException(
      DocubeException ex) {
    log.error("Anthill DocubeException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(DocubeHTTPException.class)
  public final ResponseEntity<Object> handleDocubeHTTPException(
      DocubeHTTPException ex) {
    log.error("Anthill DocubeHTTPException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ExternalServiceException.class)
  public final ResponseEntity<Object> handleExternalServiceException(
      ExternalServiceException ex) {
    log.error("Anthill ExternalServiceException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(IdentityException.class)
  public final ResponseEntity<Object> handleIdentityException(
      IdentityException ex) {
    log.error("Anthill IdentityException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(InvalidHeaderException.class)
  public final ResponseEntity<Object> handleInvalidHeaderException(
      InvalidHeaderException ex) {
    log.error("Anthill InvalidHeaderException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ValidationException.class)
  public final ResponseEntity<Object> handleValidationException(
      ValidationException ex) {
    log.error("Anthill ValidationException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(VaultException.class)
  public final ResponseEntity<Object> handleValidationException(
      VaultException ex) {
    log.error("Anthill VaultException rest call error {}", ex.getMessage());
    Map<String, Object> response = new ResultMap().add(Common.MESSAGE, ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }


}

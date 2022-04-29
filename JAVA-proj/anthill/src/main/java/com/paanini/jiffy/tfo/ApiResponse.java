package com.paanini.jiffy.tfo;


import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.utils.MessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Aswath Murugan
 * @created 08/09/20
 */
public class ApiResponse {

  private final Boolean status;
  private final Object data;
  private final String token;
  private final List<ErrorSubResponse> errors;
  static Logger LOGGER = LoggerFactory.getLogger(ApiResponse.class);

  private ApiResponse(Builder builder) {
    this.status = builder.status;
    this.token = builder.token;
    this.data = builder.data;
    this.errors = builder.errors;
  }

  public Boolean getStatus() {
    return status;
  }

  public Object getData() {
    return data;
  }

  public String getToken() {
    return token;
  }

  public List<ErrorSubResponse> getErrors() {
    return errors;
  }

  public static class Builder {

    private final List<ErrorSubResponse> errors = new ArrayList<ErrorSubResponse>();
    private Boolean status = true;
    private Object data;
    private String token;

    /**
     * Marks the response as failed and add the error
     * @param code
     * @param code
     * @return
     */
    public Builder addError(MessageCode code) {
      return addError(code, Optional.empty());
    }

    public Builder addError(MessageCode code, Optional<String> argument) {
      try {
        markFailed();
        ErrorSubResponse err =
                new ErrorSubResponse.Builder()
                        .setCode(code.toString())
                        .setMessage(code.getError())
                        .setArguments(argument)
                        .build();
        this.errors.add(err);
      } catch (Exception ex) {
        LOGGER.error("[API] failed while creating error response ", ex);
        throw new DocubeException(MessageCode.DCBE_ERR_RESP);
      }
      return this;
    }

    public Builder addError(String code, String errorMessage) {
      try {
        markFailed();
        ErrorSubResponse err =
                new ErrorSubResponse.Builder()
                        .setCode(code)
                        .setMessage(errorMessage).build();
        this.errors.add(err);
      } catch (Exception ex) {
        LOGGER.error("[API] failed while creating error response ", ex);
        throw new DocubeException(MessageCode.DCBE_ERR_RESP);
      }
      return this;
    }

    public Builder markFailed(){
      this.status = false;
      return this;
    }

    public Builder setData(Object data) {
      this.data = data;
      return this;
    }

    public Builder setToken(String token) {
      this.token = token;
      return this;
    }

    public ApiResponse build() {
      if (this.status == null) {
        throw new DocubeException(MessageCode.DCBE_API_RESPONSE_BUILDER);
      }
      if (!this.status) {
        if (this.errors.isEmpty()) {
          throw new DocubeException(MessageCode.DCBE_API_RESPONSE_BUILDER);
        }
      }
      return new ApiResponse(this);
    }
  }
}
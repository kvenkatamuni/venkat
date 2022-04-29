package com.paanini.jiffy.utils;

import com.paanini.jiffy.constants.ImageFileExtensions;
import com.paanini.jiffy.dto.Configurations;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileValidationUtils {

    static Logger logger = LoggerFactory.getLogger(FileValidationUtils.class);

    @Autowired
    Configurations configurations;


    public boolean isValidFileExtension(String originalFilename) {
        if (!configurations.isFileExtensionValidationEnabled()) {
            return true;
        }
        List<String> validFileExtension = Arrays.stream(configurations.getAllowedFileExtensions().split(","))
                .map(String::trim).map(String::toUpperCase).collect(Collectors.toList());
        return validFileExtension.contains(FilenameUtils.getExtension(originalFilename).toUpperCase());
    }

    public String getFileMediaType(String fileName) {
        String contentType = "";
        String extension = FilenameUtils.getExtension(fileName);
        switch (extension.toUpperCase()) {
            case "PNG":
                contentType = ImageFileExtensions.CONTENT_TYPE_PREFIX.concat
                        (ImageFileExtensions.PNG_EXTENSION.toLowerCase());
                break;

            case "JPEG":
            case "JPG":
                contentType = ImageFileExtensions.CONTENT_TYPE_PREFIX.concat
                        (ImageFileExtensions.JPEG_EXTENSION.toLowerCase());
                break;

            case "TIFF":
                contentType = ImageFileExtensions.CONTENT_TYPE_PREFIX.concat
                        (ImageFileExtensions.TIFF_EXTENSION.toLowerCase());
                break;

            default:
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        }
        logger.debug("[FVU] setting content type as {} image for file {}", contentType, fileName);
        return contentType;
    }

    public boolean isValidFileName(String originalFilename) {
        return StringUtils.containsNone(originalFilename, new char[]{'/', '\\', '%', '\'', '"'});
    }

}
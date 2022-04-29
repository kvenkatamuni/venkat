package com.paanini.jiffy.trader;


import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by Priyanka Bhoir on 8/7/19
 */
public class Compressor {
  private static final String UPLOADED_ZIP_FILE_IS_NOT_SAFE = "Uploaded Zip file is not safe";
  private static Logger logger = LoggerFactory.getLogger(Compressor.class);
  private static final int FILE_ENTRIES_THRESHOLD = 100000;
  private static final int FILE_SIZE_THRESHOLD = 1000000000;

  public List<String> unzip(InputStream inputFiles, String destDir) {
    List<String> docubeFilePaths = new ArrayList<>();
    Path zipLocation = null;
    try {
      zipLocation = saveTemp(inputFiles);
      File dir = new File(destDir);
      if (!dir.exists()) dir.mkdirs();  // create output directory if it doesn't exist
      docubeFilePaths.addAll(readZip(zipLocation, destDir));
    }  catch (IOException e) {
      throw new ProcessingException("Error While unzipping file", e);
    } finally {
      deleteTempLocations(zipLocation);
    }

    return docubeFilePaths;
  }

  public String zip(String exportableEle, String folder, List<String> files) throws IOException {
    String fileLocation = folder + File.separator + exportableEle + ".zip";
    logger.debug("Serving file from {}", fileLocation);
    FileOutputStream fos = new FileOutputStream(fileLocation);
    ZipOutputStream zipOS = new ZipOutputStream(fos);
    for(int i = 0; i < files.size(); i++) {
      writeToZipFile(files.get(i), zipOS, folder);
    }
    zipOS.close();
    fos.close();

    return fileLocation;
  }


  private void writeToZipFile(String path, ZipOutputStream zipStream, String parentPath) throws IOException {
    File aFile = new File(path);
    try(FileInputStream fis = new FileInputStream(aFile)){
      String fileEntry = path.substring(parentPath.length() + 1, path.length());
      ZipEntry zipEntry = new ZipEntry(fileEntry);
      zipStream.putNextEntry(zipEntry);
      byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
        zipStream.write(bytes, 0, length);
      }
      zipStream.closeEntry();
    }
  }

  public void deleteExtractedFiles(List<String> locations) {
    if (locations != null && !locations.isEmpty()){
      for (String location : locations) {
        Path temp = Paths.get(location);
        deleteTempLocations(temp);
      }
    }
  }
  public void deleteTempLocations(Path location) {
    if (location != null) try {
      FileUtils.deleteDirectory(location);
    } catch (Exception e) {
      logger.error("Error While deleting temp zip file");
    }
  }

  private Path saveTemp(InputStream in) throws IOException {
    Path temp = Paths.get(FileUtils.getTempFileName());
    FileUtils.copyBinaryStream(in, temp);
    return temp;
  }

  private List<String> readZip(Path zipFilePath, String canonicalDestPath) throws IOException {
    List<String> docubeFilePaths= new ArrayList<>();
    try(ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      int totalEntryArchive = 0;
      int totalSizeArchive = 0;
      while (entries.hasMoreElements()) {
        ZipEntry ze = entries.nextElement();
        String fileName = ze.getName();
        InputStream in = new BufferedInputStream(zipFile.getInputStream(ze));
        String relativePath = getRelativePath(canonicalDestPath, fileName);
        File newFile = new File(relativePath);

        String canonicalFilePath = newFile.getCanonicalPath();
        if(!canonicalFilePath.startsWith(canonicalDestPath + File.separator)){
          logger.error("The extracted file is not proper, destinationPath is {}, " +
                  "file path is {} ", canonicalDestPath, canonicalFilePath);
          throw new ProcessingException("The extracted file is not proper");
        }
        Optional<String> parent =
                createParentIfRequired(newFile, canonicalDestPath);
        if((logger.isDebugEnabled()) && (parent.isPresent())){
          logger.debug("Parent created during unzip of file {} ", parent.get());
        }

        try (FileOutputStream out = (new FileOutputStream(newFile))){
          totalEntryArchive++;
          int nBytes;
          byte[] buffer = new byte[2048];
          docubeFilePaths.add(relativePath);
          while ((nBytes = in.read(buffer)) > 0) { // Compliant
            out.write(buffer, 0, nBytes);
            totalSizeArchive += nBytes;
          }
        }
        logger.info("totalSizeArchive {}", totalSizeArchive);
        if (totalSizeArchive > FILE_SIZE_THRESHOLD) {
          logger.error("Zip Bomb detected..! Total archive size is greater than limit");
          cleanupExtractedFiles(docubeFilePaths);
          throw new ProcessingException(UPLOADED_ZIP_FILE_IS_NOT_SAFE);
        }

        logger.info("totalEntryArchive {}", totalEntryArchive);
        if (totalEntryArchive > FILE_ENTRIES_THRESHOLD) {
          logger.error("Zip Bomb detected..! Total number no of files, is greater than limit");
          cleanupExtractedFiles(docubeFilePaths);
          throw new ProcessingException(UPLOADED_ZIP_FILE_IS_NOT_SAFE);
        }
      }
    }
    return docubeFilePaths;
  }

  private void cleanupExtractedFiles
          (List<String> docubeFilePaths) {
    deleteExtractedFiles(docubeFilePaths);
  }

  private Optional<String> createParentIfRequired(File newFile, String dest) {
    //create directories for sub directories in zip
    File parent = new File(newFile.getParent());
    if(parent.exists()) {
      return Optional.empty();
    }
    parent.mkdirs();
    return Optional.of(getRelativePath(dest, parent.getName()));

  }

  private String getRelativePath(String dest, String filename) {
    return dest + File.separator + filename;
  }
}

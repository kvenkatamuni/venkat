package com.paanini.jiffy.utils;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.InvalidHeaderException;
import com.paanini.jiffy.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Created by rahul on 19/10/15.
 */
public class FileUtils {

  static Logger l = LoggerFactory.getLogger(FileUtils.class);

  /*public static Pair<DataSheetSchema, String[]> processStream(InputStream in, Path target)
          throws IOException, InvalidHeaderException, ProcessingException {
    return processStream(in,target,true);
  }

  private static Pair<DataSheetSchema, String[]> processStream(InputStream in,
                                                               Path target, boolean readHedear)
          throws IOException, InvalidHeaderException, ProcessingException {

    l.debug("Detecting csv header");
    StreamProcessor processor = new StreamProcessor(in);

    String header = (readHedear) ? processor.readHeader() : "";

    l.debug("Copying rest of csv file");
    try (OutputStream out = newStream(target)) {
      long copied = processor.copyRest(out);
    }

    List<String> warnings = new ArrayList<>();
    TypeScanner ts = new TypeScanner(target);
    ValueType[] dataTypes = ts.detectTypes();
    if(processor.isSkipLineFeed()){
      if(dataTypes.length>0 ){
        warnings.add(CsvFile.CRLF);
      }
    }
    DataSheetSchema dataSheetSchema = (readHedear) ?
            DBHelper.createDataSheetSchema(dataTypes, TypeScanner.parseHeader(header)) : null;
    Pair<DataSheetSchema,String[]> ret = new Pair<>(dataSheetSchema,warnings.toArray(new String[0]));
    return ret;
  }

  public static Pair<DataSheetSchema, String[]> processExistingStream(Path file, DataSheetSchema existingSchema)
          throws IOException {
    List<String> warnings = new ArrayList<>();
    TypeScanner ts = new TypeScanner(file);
    ValueType[] dataTypes = ts.detectTypes();
    String header = existingSchema.getColumns()
            .stream()
            .map(column -> ((ColumnName) column.getSource()).getName())
            .collect(Collectors.joining(","));
    DataSheetSchema dataSheetSchema = DBHelper.createDataSheetSchema(dataTypes, TypeScanner.parseHeader(header));
    return new Pair<>(dataSheetSchema,warnings.toArray(new String[0]));
  }*/

  public static void copyTextStream(InputStream in, Path target)
          throws IOException, InvalidHeaderException, ProcessingException {

    l.debug("Copying file to {}",target);
    StreamProcessor processor = new StreamProcessor(in);

    try (OutputStream out = newStream(target)) {
      long copied = processor.copyRest(out);
    }

  }

  public static void copyBinaryStream(InputStream in, Path target)
          throws IOException{
    l.debug("Copying file to {}",target);
    Files.copy(in,target);
  }

  private static OutputStream newStream(Path target) throws IOException {
    return Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
  }

  static SecureRandom random = new SecureRandom();

  public static String getTempFileName() {
    return System.getProperty("java.io.tmpdir") + "/" + random.nextLong();
  }

  public static void deleteDirectory(Path path) throws IOException {
    deleteDirectory(path,false);
  }

  public static void clearDirectoryContent(Path path) throws IOException {
    deleteDirectory(path, true);
  }
  public static void deleteDirectory(Path path,boolean clearContentsOnly) throws IOException {
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file,
                                       BasicFileAttributes attrs) throws IOException {
        l.warn("Deleting " + file.getFileName());
        Files.delete(file);
        l.warn("DELETED " + file.getFileName());
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        // try to delete the file anyway, even if its attributes could
        // not be read, since delete-only access is theoretically possible
        // I NEVER SEE THIS
        l.warn("Delete file " + file + " failed", exc);
        try {
          Files.delete(file);
        } catch (IOException e) {
          l.warn("Delete file " + file + " failed again", exc);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc)
              throws IOException {
        if (exc != null) {
          throw exc;
        }
        if(!clearContentsOnly) {
          Files.delete(dir);
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void moveDirectory(Path fromPath, Path toPath) throws IOException {
    Files.walkFileTree(fromPath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

        Path targetPath = toPath.resolve(fromPath.relativize(dir));
        if(!Files.exists(targetPath)){
          Files.createDirectory(targetPath);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.move(file, toPath.resolve(fromPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc)
              throws IOException {
        if (exc != null) {
          throw exc;
        }

        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void copyDirectory(Path fromPath, Path toPath) throws IOException {
    Files.walkFileTree(fromPath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

        Path targetPath = toPath.resolve(fromPath.relativize(dir));
        if(!Files.exists(targetPath)){
          Files.createDirectory(targetPath);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.copy(file, toPath.resolve(fromPath.relativize(file)), StandardCopyOption
                .REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc)
              throws IOException {
        if (exc != null) {
          throw exc;
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }


  public static void LinkDirectory(Path fromPath, Path toPath) throws IOException {
    Files.walkFileTree(fromPath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

        Path targetPath = toPath.resolve(fromPath.relativize(dir));
        if(!Files.exists(targetPath)){
          Files.createDirectory(targetPath);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.createLink(file, toPath.resolve(fromPath.relativize(file)));
        return FileVisitResult.CONTINUE;
      }


    });
  }

  public static String getAbsolutePath(List<String> subFolders){
    return new StringBuilder()
            .append("/")
            .append(String.join("/",subFolders))
            .append("/")
            .toString();
  }

  public static String createFolder(String folder) {
    File dir = new File(folder);
    if(!dir.exists()) {
      dir.mkdir();
    }
    return folder;
  }

  public static List<Type> getFileTypes() {
    return Arrays.asList(Type.PRESENTATION, Type.FILESET, Type.DATASHEET,
            Type.SQL_DATASHEET, Type.SQL_APPENDABLE_DATASHEET,
            Type.JIFFY_TABLE, Type.KUDU_DATASHEET, Type.CONFIGURATION,
            Type.DATASHEET_RESTRICTION, Type.SPARK_MODEL_FILE,
            Type.CUSTOM_FILE, Type.NOTEBOOK);
  }

  public static String getPath(String... paths){
    StringBuilder stringBuilder = new StringBuilder("");
    for (int i = 0 ;i < paths.length; i++) {
      if(i!= paths.length-1)
        stringBuilder.append(paths[i])
                .append("/");
      else
        stringBuilder.append(paths[i]);

    }
    return stringBuilder.toString();
  }

  public static String getFileForBackup(String root) throws IOException {
    String fileName = root+"/JiffyTablebackup/"+TenantHelper.getTenantId();
    Path path = Paths.get(fileName);
    if (!Files.exists(path)) {
      Path directory = Files.createDirectories(path);
    }
    return fileName+"/jiffytablebackup";
  }

  public static String checkAndAddFileSeperator(String path){
    String c = path.substring(path.length() - 1, path.length());
    if(File.separator.equals(c)){
      return path;
    }else{
      return path.concat(File.separator);
    }
  }

}
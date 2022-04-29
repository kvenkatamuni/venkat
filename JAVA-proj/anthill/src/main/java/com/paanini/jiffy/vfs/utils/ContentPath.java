package com.paanini.jiffy.vfs.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ContentPath {

  public static final String SHARE = "shared";
  public static final String SHARE1 = "Shared";
  private static final String HOME = "Home";
  final boolean shared;
  final Path path;
  final boolean absolute;

  public ContentPath(String file){

    Path filePath = Paths.get(file);

    this.absolute = filePath.isAbsolute();

    if(filePath.getNameCount() == 0){
      // Handle the root '/'
      path = filePath;
      shared = false;
    } else {

      this.shared = absolute && (
              SHARE.equals(filePath.getName(0).toString()) ||
                      SHARE1.equals(filePath.getName(0).toString())
      );

      //If '/shared/p1/p2', convert to 'p1/p2'
      if ((absolute && (SHARE.equals(getName(filePath, 0))) || SHARE1.equals(getName(filePath, 0)))) {
        filePath = strip(filePath, 1);
      }

      //If '/Home/p1/p2', convert to 'p1/p2'
      if (absolute && HOME.equals(getName(filePath, 0))) {
        filePath = strip(filePath, 1);
      }

      path = stripLeadingFS(filePath);
    }
  }

  private String getName(Path p,int index){
    return p.getName(index).toString();
  }

  //Convert /p1/p2 to p1/p2
  private Path stripLeadingFS(Path basePath) {
    return strip(basePath,0);
  }

  private Path strip(Path basePath, int index) {
    return basePath.subpath(index, basePath.getNameCount());
  }

  public boolean isShared(){
    return shared;
  }

  public boolean isAbsolute() {
    return absolute;
  }

  /**
   * The link of a shared folder. Use only if a shared folder
   * @return
   */
  public String getLink(){
    return path.getName(0).toString();
  }

  public String getPath(){
    if(isShared()){
      //It has a link, the rest is the path
      return (path.getNameCount()==1) ? "" :strip(path,1).toString();
    }
    return path.toString();
  }

}

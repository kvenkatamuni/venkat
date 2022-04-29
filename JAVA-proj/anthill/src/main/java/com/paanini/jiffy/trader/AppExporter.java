package com.paanini.jiffy.trader;

import java.util.concurrent.ExecutionException;

/**
 * @author Athul Krishna N S
 * @since 09/11/20
 */
public interface AppExporter {


  /**
   * Exports app - docube files and workflow files and returns fileserver url from where it can
   * be downloaded
   * @param appPath
   * @return
   */
  String exportApp(String appPath) throws InterruptedException;

}

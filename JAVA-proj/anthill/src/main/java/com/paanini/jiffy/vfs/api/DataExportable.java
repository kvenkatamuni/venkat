package com.paanini.jiffy.vfs.api;

import com.paanini.jiffy.models.ImpexContent;
import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public interface DataExportable {
  List<ImpexContent> retrieveExportables();
  List<ImpexContent> retrieveImportables(String path, String jfsFolderId);
}

package com.paanini.jiffy.trader;

import com.paanini.jiffy.models.DataSetPollingData;
import com.paanini.jiffy.models.DatasetImportOptions;
import com.paanini.jiffy.models.ImportAppOptions;
import com.paanini.jiffy.models.Summary;
import com.paanini.jiffy.models.TraderResult;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public interface AsyncAppImporter {
  UUID importApp(String appgroupName, ImportAppOptions importAppOptions,String transactionId);
  Summary checkStatus(UUID transactionId);
  TraderResult uploadImportZip(MultipartFile attachment);
  Summary getSummary(UUID transactionId);
  UUID importDataset(DatasetImportOptions options, String appGroupName);
  DataSetPollingData getDataSetStatus(UUID transactionId);
}

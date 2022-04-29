package com.paanini.jiffy.proc.api;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.models.JobDetails;

import java.util.Date;
import java.util.List;

public interface JobMonitorManager {
  String createUserJobDetails(String userId, String docId, String fileName, Type type);
  String createUserJobDetails(String userId, String docId, String fileName, Type type, Date nextTriggerTime);
  List<JobDetails> getUserJobs(String userId);
  List<JobDetails> getJobRunDetails(String userId, String docId);
  void updateUserJobDetails(String user, String docId, String jobInstanceId, String status);
  void updateJobErrorDetails(String userId, String docId, String jobInstanceId, String message);
}
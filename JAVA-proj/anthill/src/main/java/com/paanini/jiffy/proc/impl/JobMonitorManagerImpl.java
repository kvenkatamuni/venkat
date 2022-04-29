package com.paanini.jiffy.proc.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.cacheManager.RedisManager;
import com.paanini.jiffy.models.JobDetails;
import com.paanini.jiffy.proc.api.JobMonitorManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
/**
        * JobMonitorManagerImpl manages to create, read, update, delete jobDetails
        */

public class JobMonitorManagerImpl implements JobMonitorManager {

  /**
   *  Maintains a capped list for each document in order to maintain each job run details of that document
   * {"key":"JOB_MON:USER_JOB:{userId}:{docId}"}:["jobDetails"]
   *
   *  Maintains a sorted set to store documentId in sorting order by last execution time
   *  in order to show the last execution of the document on the top of Jobstatus screen
   *  A sorted does not support key-value pair, to store job details for the corresponding docId
   *  it requires a hashmap which holds docId with its job details
   *  {"key":"JOB_MON:USER_DOC:{userId}"}:[{"score":"time","value":"docId"}]
   *
   *  Maintains a hmset to find the value of each document id present in sorted set
   *   {"key":"JOB_MON:RECENT_JOB:{userId}"}:[{"key":"docId","value":"jobDetails"}]
   */


  public static final String PREFIX = "JOB_MON:";
  public static final int MAX_ENTRIES = 50;
  public static final int START_OF_LIST = 0;

  @Autowired
  private RedisManager redisCacheManager;

  public String getUserDocListKey(String userId) {
    return PREFIX +"USER_DOC:"+ userId;
  }

  public String getRecentJobHashmapKey(String userId) {
    return PREFIX + "RECENT_JOB:" + userId;

  }

  public String getUserJobListKey(String userId, String docId) {
    return PREFIX + "USER_JOB:"+ userId + ":" + docId;
  }

  public void setRedisCacheManager(RedisManager redisCacheManager) {
    this.redisCacheManager = redisCacheManager;
  }


  /**
   * Creates jobDetails for the manually triggered jobs.
   * @param userId
   * @param docId
   * @param fileName
   * @param type
   * @return the newly created job instance id
   * @throws RuntimeException
   */
  @Override
  public String createUserJobDetails(String userId, String docId, String fileName, Type type) throws RuntimeException {
    JobDetails jobDetails = new JobDetails();
    jobDetails.setDocId(docId);
    jobDetails.setJobInstanceId(UUID.randomUUID().toString());
    jobDetails.setTriggerTime(new Date(System.currentTimeMillis()));
    jobDetails.setStatus("TRIGGERED");
    jobDetails.setFileName(fileName);
    jobDetails.setType(String.valueOf(type));
    saveJobDetails(userId, jobDetails);
    return jobDetails.getJobInstanceId();
  }

  /**
   * Creates jobDetails for the scheduler triggered jobs.
   * @param userId
   * @param docId
   * @param fileName
   * @param type
   * @param nextTriggerTime
   * @return the newly created job instance id
   */

  @Override
  public String createUserJobDetails(String userId, String docId, String fileName, Type type, Date nextTriggerTime) {
    JobDetails jobDetails = new JobDetails();
    jobDetails.setDocId(docId);
    jobDetails.setJobInstanceId(UUID.randomUUID().toString());
    jobDetails.setTriggerTime(new Date(System.currentTimeMillis()));
    jobDetails.setStatus("TRIGGERED");
    jobDetails.setFileName(fileName);
    jobDetails.setType(String.valueOf(type));
    jobDetails.setNextTriggerTime(nextTriggerTime);
    saveJobDetails(userId, jobDetails);
    return jobDetails.getJobInstanceId();
  }

  /**
   * Saves jobDetails in redis-list in order to maintain each job run details for that document
   * and stores docId in sorted set to maintain last execution order for all document as well as jobDetails in
   * hmset to maintain last execution details
   * @param userId
   * @param jobDetails
   */
  private void saveJobDetails(String userId, JobDetails jobDetails) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      String valueAsString = objectMapper.writeValueAsString(jobDetails);
      Map<String, String> stringJsonObjectMap = new HashMap<>();
      stringJsonObjectMap.put(jobDetails.getDocId(), valueAsString);
      //stored docId as value and trigger time as score in sorted set to maintain last execution order
      redisCacheManager.zadd(getUserDocListKey(userId), jobDetails.getTriggerTime().getTime(), jobDetails.getDocId());
      redisCacheManager.hmset(getRecentJobHashmapKey(userId), stringJsonObjectMap);
      redisCacheManager.lpush(getUserJobListKey(userId, jobDetails.getDocId()), valueAsString);
      redisCacheManager.ltrim(getUserJobListKey(userId, jobDetails.getDocId()), START_OF_LIST, MAX_ENTRIES);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * Updates jobDetails for the document by using docId and jobInstanceId in redis list and deletes existing
   * data and push as the first element in redis list
   * @param userId
   * @param docId
   * @param jobInstanceId
   * @param status
   */

  @Override
  public void updateUserJobDetails(String userId, String docId, String jobInstanceId, String status) {
    if (!status.toLowerCase().equals("available")) {
      String key = getUserJobListKey(userId, docId);
      //return all the jobDetails for the document by docId
      List<String> jobIdList = redisCacheManager.lrange(key);
      //return all the last execution jobDetails for all the document for the user
      Map<String, String> jobRunDetails = redisCacheManager.hmgetAll(getRecentJobHashmapKey(userId));
      ObjectMapper objectMapper = new ObjectMapper();
      String firstElement = jobIdList.get(0);
      try {
        JobDetails existingJobDetails = objectMapper.readValue(firstElement, JobDetails.class);
        if (existingJobDetails.getJobInstanceId().equals(jobInstanceId)) {
          updateUserJobState(userId, docId, status, jobRunDetails, objectMapper, existingJobDetails);
          redisCacheManager.lpop(key);
          redisCacheManager.lpush(key, objectMapper.writeValueAsString(existingJobDetails));
          redisCacheManager.ltrim(key, 0, MAX_ENTRIES);
        } else {
          for (int i = 1; i < jobIdList.size(); i++) {
            JobDetails job = objectMapper.readValue(jobIdList.get(i), JobDetails.class);
            if (job.getJobInstanceId().equals(jobInstanceId)) {
              updateUserJobState(userId,docId,status,jobRunDetails,objectMapper,job);
              redisCacheManager.ldel(key, redisCacheManager.lindex(key, i));
              redisCacheManager.lpush(key, objectMapper.writeValueAsString(job));
              redisCacheManager.ltrim(key, 0, MAX_ENTRIES);
              break;
            }

          }

        }

      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }


  }

  /**
   * Updates last document execution order ,execution time and status in sorted set and hmset in redis
   */
  private void updateUserJobState(String userId, String docId, String status, Map<String, String> jobRunDetails, ObjectMapper objectMapper, JobDetails existingJobDetails) throws JsonProcessingException {
    Date currentDate = new Date(System.currentTimeMillis());
    if (status.toLowerCase().equals("initialized")) {
      existingJobDetails.setStartTime(currentDate);
    } else {
      existingJobDetails.setEndTime(currentDate);
    }
    existingJobDetails.setStatus(status);
    jobRunDetails.put(docId, objectMapper.writeValueAsString(existingJobDetails));
    redisCacheManager.hmset(getRecentJobHashmapKey(userId), jobRunDetails);
    //updating score of the document based on the time to maintain last execution order
    redisCacheManager.zadd(getUserDocListKey(userId), currentDate.getTime(), docId);
  }

  /**
   * Updates error message for jobDetails
   * @param userId
   * @param docId
   * @param jobInstanceId
   * @param message
   */

  @Override
  public void updateJobErrorDetails(String userId, String docId, String jobInstanceId, String message) {
    String key = getUserJobListKey(userId, docId);
    //return all the jobDetails for the document by docId
    List<String> jobIdList = redisCacheManager.lrange(key);
    Map<String, String> jobRunDetails = redisCacheManager.hmgetAll(getRecentJobHashmapKey(userId));
    ObjectMapper objectMapper = new ObjectMapper();
    String firstElement = jobIdList.get(0);
    try {
      JobDetails existingJobDetails = objectMapper.readValue(firstElement, JobDetails.class);
      if (existingJobDetails.getJobInstanceId().equals(jobInstanceId)) {
        existingJobDetails.setErrorMessage(message);
        if(!Objects.isNull(message)) {
          existingJobDetails.setStatus("ERROR");
        }
        updateUserJobErrorState(userId,docId,jobRunDetails,objectMapper,existingJobDetails);
        redisCacheManager.lpop(key);
        redisCacheManager.lpush(key, objectMapper.writeValueAsString(existingJobDetails));
        redisCacheManager.ltrim(key, 0, MAX_ENTRIES);
      } else {
        for (int i = 1; i < jobIdList.size(); i++) {
          JobDetails job = objectMapper.readValue(jobIdList.get(i), JobDetails.class);
          if (job.getJobInstanceId().equals(jobInstanceId)) {
            job.setErrorMessage(message);
            if(!Objects.isNull(message)) {
              job.setStatus("ERROR");
            }
            updateUserJobErrorState(userId,docId,jobRunDetails,objectMapper,existingJobDetails);
            redisCacheManager.ldel(key, redisCacheManager.lindex(key, i));
            redisCacheManager.lpush(key, objectMapper.writeValueAsString(job));
            redisCacheManager.ltrim(key, 0, MAX_ENTRIES);
            break;
          }

        }

      }

    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * Updates last document execution order ,error message in sorted set and hmset in redis
   * @param userId
   * @param docId
   * @param jobRunDetails
   * @param objectMapper
   * @param existingJobDetails
   * @throws JsonProcessingException
   */
  private void updateUserJobErrorState(String userId, String docId, Map<String, String> jobRunDetails, ObjectMapper objectMapper, JobDetails existingJobDetails) throws JsonProcessingException {
    Date currentDate = new Date(System.currentTimeMillis());
    jobRunDetails.put(docId, objectMapper.writeValueAsString(existingJobDetails));
    redisCacheManager.hmset(getRecentJobHashmapKey(userId), jobRunDetails);
    //updating score of the document based on the time to maintain last execution order
    redisCacheManager.zadd(getUserDocListKey(userId), currentDate.getTime(), docId);
  }


  /**
   * @param userId
   * @return list of last job details for the all the documents by userId
   * @throws RuntimeException
   */
  @Override
  public List<JobDetails> getUserJobs(String userId) throws RuntimeException {
    Set<String> zrange = redisCacheManager.zrange(getUserDocListKey(userId));
    List<String> list = new ArrayList<>(zrange);
    Collections.reverse(list);
    Map<String, String> runJobDetails = redisCacheManager.hmgetAll(getRecentJobHashmapKey(userId));
    List<JobDetails> jobDetailsList = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();

    for (String entry : list) {
      try {
        if (runJobDetails.containsKey(entry)) {
          JobDetails jobDetail = objectMapper.readValue(runJobDetails.get(entry), JobDetails.class);
          jobDetailsList.add(jobDetail);
        }

      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    return jobDetailsList;
  }

  /**
   * @param userId
   * @param docId
   * @return list of job run details for the document by documentId
   */

  @Override
  public List<JobDetails> getJobRunDetails(String userId, String docId) {
    List<String> jobRunDetails = redisCacheManager.lrange(getUserJobListKey(userId, docId));
    List<JobDetails> jobDetailsList = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();

    for (String entry : jobRunDetails) {
      try {
        JobDetails jobDetail = objectMapper.readValue(entry ,JobDetails.class);
        jobDetailsList.add(jobDetail);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    return jobDetailsList;
  }
}
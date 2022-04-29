package com.paanini.jiffy.db;

import com.mongodb.DBCursor;
import org.bson.Document;

import java.util.List;
import java.util.Map;


public interface DatabaseOperations {
    public void createTable();
    public void dropCollection(String dbName,String collection);
    public void insertOne(String dbName, String collection, Map<String ,Object> document);
    public void insertMany(String dbName, String collection,List<Document> documents);
    public Document getRow(String key, String value,String collectionName,String dbName);
    public List<Document> getRows(String key, String value, String collectionName, String dbName);
    public void deleteRow(String key, String value,String collectionName,String dbName);
    public void deleteRows(String key, String value,String collectionName,String dbName);
    public void deleteMultipleRow(String key, List<String> values, String collectionName, String dbName);
    public void updateRow(String key,String value,String collectionName,
                          String dbName,Document document);
}

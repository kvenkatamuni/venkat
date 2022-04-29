package com.paanini.jiffy.db;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.paanini.jiffy.models.FilterObject;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataBaseMongoOperations implements DatabaseOperations{
    private MongoClient client;

    public DataBaseMongoOperations(MongoClient mongoClient) {
        this.client = mongoClient;
    }

    @Override
    public void createTable() {
        // Nothing to do
    }

    @Override
    public void dropCollection(String dbName, String collection) {
        MongoDatabase database = client.getDatabase(dbName);
        database.getCollection(collection).drop();
    }

    @Override
    public void insertOne(String dbName, String collection, Map document) {
        MongoDatabase database = client.getDatabase(dbName);
        database.getCollection(collection).insertOne((Document) document);
    }

    @Override
    public void insertMany(String dbName, String collection,List<Document> documents) {
        if(documents.isEmpty()){
            return;
        }
        MongoDatabase database = client.getDatabase(dbName);
        database.getCollection(collection).insertMany(documents);
    }

    @Override
    public Document getRow(String key, String value,String collectionName,String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        BasicDBObject whereQuery = new BasicDBObject(key,value);
        FindIterable<Document> documents = collection.find(whereQuery);
        return documents.first();
    }

    @Override
    public List<Document> getRows(String key, String value, String collectionName, String dbName) {
        List<Document> result = new ArrayList<>();
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        BasicDBObject whereQuery = new BasicDBObject(key,value);
        return collection.find(whereQuery).into(result);
    }

    public List<Document> getRows(List<FilterObject> filterObjects, String collectionName, String dbName) {
        List<Document> result = new ArrayList<>();
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        BasicDBObject whereQuery = new BasicDBObject();
        for (FilterObject f : filterObjects){
            whereQuery.append(f.getKey(),f.getValue());
        }
        return collection.find(whereQuery).into(result);
    }

    public List<Document> getRows(String collectionName,String dbName) {
        List<Document> result = new ArrayList<>();
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find().into(result);
    }

    @Override
    public void deleteRow(String key, String value, String collectionName, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(Filters.eq(key, value));
    }

    @Override
    public void deleteRows(String key, String value, String collectionName, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteMany(Filters.eq(key, value));
    }

    @Override
    public void deleteMultipleRow(String key, List<String> values, String collectionName, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteMany(Filters.in(key,values));
    }

    public void updateRow(String key,String value,String collectionName,
                          String dbName,Document document){
        MongoDatabase database = client.getDatabase(dbName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        BasicDBObject whereQuery = new BasicDBObject(key,value);
        BasicDBObject updateObject = new BasicDBObject();
        updateObject.put("$set", document);
        collection.updateOne(whereQuery,updateObject);
    }

    public void bulkUpset(String dbName, String collection,List<Document> documents){
        MongoDatabase database = client.getDatabase(dbName);
        List<WriteModel<Document>> bulkWriteOperation = new ArrayList<>();
        UpdateOptions options = new UpdateOptions().upsert(true);
        for(Document doc : documents){
            Object value = doc.get("_id");
            UpdateOneModel<Document> model = new UpdateOneModel<>(Filters.eq("_id", value),
                    new Document("$set", new Document(doc)), options);
            bulkWriteOperation.add(model);
        }
        database.getCollection(collection).bulkWrite(bulkWriteOperation);
    }



}

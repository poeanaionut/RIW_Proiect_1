package app;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import org.bson.Document;

public class MongoConnection {
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoConnection(String serverAddress, int port) {
        mongoClient = new MongoClient(new ServerAddress(serverAddress, port));
        database = mongoClient.getDatabase("riwmongo");
    }

    public MongoClient getClient() {
        return mongoClient;
    }

    public boolean existsCollection(String collectionName) {
        MongoIterable<String> collections = database.listCollectionNames();
        for (String colection : collections) {
            if (colection.equals(collectionName)) {
                return true;
            }
        }
        return false;
    }

    // to write the direct index to the database
    public void writeDirectIndexToDatabase(String collectionName,
            HashMap<String, HashMap<String, Double>> directIndex) {

        if (existsCollection(collectionName)) {
            System.out.println(collectionName + "este deja memorat in mongo");
            return;
        }
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<Document> files = new LinkedList<Document>();

        Iterator<String> documentEntry = directIndex.keySet().iterator();
        List<BasicDBObject> elems;
        HashMap<String, Double> wordCollection;

        while (documentEntry.hasNext()) {

            // get the word collection of each key(word)
            String docName = documentEntry.next().toString();
            wordCollection = directIndex.get(docName);

            Document document = new Document("fileName", docName);

            Iterator<String> entry = wordCollection.keySet().iterator();
            elems = new LinkedList<BasicDBObject>();

            while (entry.hasNext()) {

                String word = entry.next().toString();
                String count = Double.toString(wordCollection.get(word));
                BasicDBObject basicDBObject = new BasicDBObject();
                basicDBObject.put("word", word);
                basicDBObject.put("tf", count);

                elems.add(basicDBObject);
            }

            document.append("words", elems);
            files.add(document);
        }
        collection.insertMany(files);
    }

    public void writeIndirectIndexToDatabase(String collectionName, HashMap<String, HashSet<String>> indirectIndex) {

        if (existsCollection(collectionName)) {
            System.out.println(collectionName + "este deja memorat in mongo");
            return;
        }

        Iterator<String> iterator = indirectIndex.keySet().iterator();
        LinkedList<Document> documents = new LinkedList<>();

        while (iterator.hasNext()) {
            String word = iterator.next().toString();
            Document document = new Document("word", word);
            document.append("files", indirectIndex.get(word));
            documents.add(document);
        }

        MongoCollection<Document> mongoCollection = database.getCollection(collectionName);
        mongoCollection.insertMany(documents);

    }

    public void writeAssociatedVectorsToDatabase(String collectionName,
            HashMap<String, HashMap<String, Double>> associatedVectors) {
        if (existsCollection(collectionName)) {
            System.out.println(collectionName + "este deja memorat in mongo");
            return;
        }
        Iterator<String> iterator = associatedVectors.keySet().iterator();
        LinkedList<Document> documents = new LinkedList<>();

        while (iterator.hasNext()) {

            String file = iterator.next().toString();
            Document document = new Document("file", file);
            HashMap<String, Double> words = associatedVectors.get(file);
            LinkedList<BasicDBObject> currentVector = new LinkedList<>();
            for (String word : words.keySet()) {
                BasicDBObject doc = new BasicDBObject("word", word);
                doc.append("tfidfValue", words.get(word));
                currentVector.add(doc);

            }
            document.append("vector", currentVector);
            documents.add(document);
        }

        database.getCollection(collectionName).insertMany(documents);
    }

    public void writeIdfToDatabase(String collectionName, HashMap<String, Double> idf) {
        Iterator<String> iterator = idf.keySet().iterator();

        LinkedList<Document> documents = new LinkedList<>();
        while (iterator.hasNext()) {
            String word = iterator.next().toString();
            Double idfValue = idf.get(word);

            Document document = new Document("word", word);
            document.append("idf", idfValue);
            documents.add(document);
        }

        MongoCollection<Document> mongoCollection = database.getCollection(collectionName);
        mongoCollection.insertMany(documents);
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
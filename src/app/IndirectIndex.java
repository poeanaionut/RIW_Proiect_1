package app;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class IndirectIndex {
    private HashMap<String, HashSet<String>> indirectIndex;
    private HashMap<String, Double> idf;

    public IndirectIndex() {
        super();
        indirectIndex = new HashMap<>();
        idf = new HashMap<>();
    }

    public HashMap<String, HashSet<String>> getIndirectIndex() {
        if (indirectIndex.isEmpty())
            return null;
        return indirectIndex;
    }

    public HashMap<String, Double> getIdf() {
        return idf;
    }

    public void indirectIndex(String collectionName, MongoDatabase database) throws IOException {

        int numberOfDocuments = 0;

        MongoCollection<Document> mongoCollection = database.getCollection(collectionName);
        MongoCursor<Document> directIndexDocuments = mongoCollection.find().iterator();

        while (directIndexDocuments.hasNext()) {
            Document document = directIndexDocuments.next();
            String fileName = document.get("fileName").toString();
            ArrayList<Document> words = (ArrayList<Document>) document.get("words");

            for (Document wordDocument : words) {

                String word = wordDocument.get("word").toString();
                if (indirectIndex.get(word) != null) {
                    indirectIndex.get(word).add(fileName);
                } else {
                    HashSet<String> fileNameSet = new HashSet<>();
                    fileNameSet.add(fileName);
                    indirectIndex.put(word, fileNameSet);
                }
            }
            ++numberOfDocuments;
        }

        for (String word : indirectIndex.keySet()) {
            int numberOfDocumentsMatched;
            numberOfDocumentsMatched = indirectIndex.get(word).size();
            idf.put(word, Math.log((double) numberOfDocuments / (numberOfDocumentsMatched + 1)));
        }
    }
}

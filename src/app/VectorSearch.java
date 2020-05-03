package app;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class VectorSearch {
    private volatile HashMap<String, HashMap<String, Double>> associatedVectorsFile;
    private HashMap<String, Double> idfFile;
    private HashMap<String, HashMap<String, Double>> directIndexFile;

    public VectorSearch() {

        associatedVectorsFile = new HashMap<>();
        idfFile = new HashMap<>();
        directIndexFile = new HashMap<>();

    }

    public void loadIdf(MongoDatabase database, String idfCollectionName) {

        MongoCollection<Document> collection = database.getCollection(idfCollectionName);
        MongoCursor<Document> cursor = collection.find().iterator();
        while (cursor.hasNext()) {
            Document idfDocument = cursor.next();
            idfFile.put(idfDocument.get("word").toString(), Double.parseDouble(idfDocument.get("idf").toString()));
        }
    }

    private void loadDirectIndex(MongoDatabase database, String directIndexCollectionName) {

        MongoCollection<Document> collection = database.getCollection(directIndexCollectionName);
        MongoCursor<Document> cursor = collection.find().iterator();

        while (cursor.hasNext()) {
            Document diDocument = cursor.next();
            String fileName = diDocument.get("fileName").toString();
            ArrayList<Document> words = (ArrayList<Document>) diDocument.get("words");
            HashMap<String, Double> wordMap = new HashMap<>();
            for (Document wordDocument : words) {

                String word = wordDocument.get("word").toString();
                Double tf = Double.parseDouble(wordDocument.get("tf").toString());
                wordMap.put(word, tf);
            }
            directIndexFile.put(fileName, wordMap);
        }
    }

    public void loadAssociatedVectors(MongoDatabase database, String associatedVectorsCollectionName) {

        MongoCollection<Document> collection = database.getCollection(associatedVectorsCollectionName);
        MongoCursor<Document> cursor = collection.find().iterator();
        while (cursor.hasNext()) {
            Document associatedVectorDocument = cursor.next();
            String fileName = associatedVectorDocument.get("file").toString();
            ArrayList<Document> words = (ArrayList<Document>) associatedVectorDocument.get("vector");
            HashMap<String, Double> wordMap = new HashMap<>();
            for (Document wordDocument : words) {
                String word = wordDocument.get("word").toString();
                double tfidf = Double.parseDouble(wordDocument.get("tfidfValue").toString());
                wordMap.put(word, tfidf);
            }
            associatedVectorsFile.put(fileName, wordMap);
        }
    }

    private class CreateAssociatedDocumentTask implements Runnable {

        private HashMap<String, Double> currentDocumentVector;
        private String fileName;
        private HashMap<String, Double> idfFile;
        private HashMap<String, HashMap<String, Double>> directIndexFile;

        public CreateAssociatedDocumentTask(String fileName, HashMap<String, Double> idfFile,
                HashMap<String, HashMap<String, Double>> directIndexFile) {
            this.idfFile = idfFile;
            this.directIndexFile = directIndexFile;
            this.fileName = fileName;
            currentDocumentVector = new HashMap<>();
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            HashMap<String, Double> curentDocument = directIndexFile.get(fileName);
            for (String word : curentDocument.keySet()) {
                Double tf = curentDocument.get(word);
                Double idf = idfFile.get(word);
                currentDocumentVector.put(word, tf * idf);
            }
            associatedVectorsFile.put(fileName, curentDocument);
        }

    }

    public void createAssociatedDocumentVectors(MongoDatabase database, String directIndexCollectionName,
            String indirectIndexCollectionName, String idfCollectionName) {

        loadDirectIndex(database, directIndexCollectionName);
        loadIdf(database, idfCollectionName);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        for (String file : directIndexFile.keySet()) {

            CreateAssociatedDocumentTask task = new CreateAssociatedDocumentTask(file, idfFile, directIndexFile);
            threadPoolExecutor.execute(task);
        }

    }

    private double getTfQuery(String word, ArrayList<String> query) {
        int numberOfApparitions = 0;
        for (String w : query) {
            if (w.equals(word)) {
                ++numberOfApparitions;
            }
        }
        return (double) numberOfApparitions / query.size();
    }

    private double cosineSimilarity(HashMap<String, Double> file, HashMap<String, Double> queryFile) {

        double tfIdf1;
        double tfIdf2;
        double sumSquareLeft = 0;
        double sumSquareRight = 0;
        double dotProd = 0;
        int numberOfCommonWords = 0;

        for (String word : queryFile.keySet()) {

            tfIdf1 = queryFile.get(word);
        
            if (file.containsKey(word)) {
                
                numberOfCommonWords++;
                tfIdf2 = file.get(word);
                dotProd += Math.abs(tfIdf1 * tfIdf2);
            }

            sumSquareLeft += tfIdf1 * tfIdf1;
        }


        if (dotProd == 0 || numberOfCommonWords == 0) {
            return 0;
        }

        for(String word :file.keySet())
        {
            sumSquareRight += file.get(word)* file.get(word);
        }

        Double similarity = dotProd / Math.sqrt(sumSquareLeft) / Math.sqrt(sumSquareRight);

        return similarity;
    }

    static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                int res = e2.getValue().compareTo(e1.getValue());
                return res != 0 ? res : 1;
            }
        });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public HashMap<String, HashMap<String, Double>> getAssociatedVectors() {
        if (associatedVectorsFile.isEmpty())
            return null;
        return associatedVectorsFile;
    }

    public SortedSet<HashMap.Entry<String, Double>> search(String query) {
        ArrayList<String> queryWords = QueryString.parseQueryString(query);

        HashMap<String, Double> queryAssociateVector = new HashMap<>();
        for (String word : queryWords) {

            Double idf = idfFile.get(word) == null ? 0 : idfFile.get(word);
            queryAssociateVector.put(word, getTfQuery(word, queryWords) * idf);
        }

        HashMap<String, Double> similarDocuments = new HashMap<>();
        for (String file : associatedVectorsFile.keySet()) {
            double similarity = cosineSimilarity(associatedVectorsFile.get(file), queryAssociateVector);
            if (similarity != 0) {
                similarDocuments.put(file, similarity);
            }
        }
        return entriesSortedByValues(similarDocuments);
    }
}

package app;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DirectIndex {

    private volatile HashMap<String, HashMap<String, Double>> directIndex;

    private class DirectIndexTask implements Runnable {
        private String fileName;

        public DirectIndexTask(String fileName) {
            super();
            this.fileName = fileName;
            directIndex = new HashMap<String, HashMap<String, Double>>();
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            HashMap<String, Double> wordList = new HashMap<String, Double>();

            int numberOfWordsInDocument = 0;

            FileReader inputStream = null;
            try {
                inputStream = new FileReader(fileName);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            StringBuilder sb = new StringBuilder();

            Stemmer stemmer = new Stemmer();

            int c;
            try {
                while ((c = inputStream.read()) != -1) {
                    if (Character.isLetterOrDigit((char) c)) {
                        sb.append((char) c);
                    } else {
                        String newWord = sb.toString();
                        if (newWord.equals("")) // ignore ""
                        {
                            continue;
                        }

                        if (ExceptionSet.exceptions.contains(newWord)) {
                            if (wordList.containsKey(newWord)) {
                                wordList.put(newWord, wordList.get(newWord) + 1);
                            } else {
                                wordList.put(newWord, 1.0);
                            }
                            ++numberOfWordsInDocument;
                        } else if (StopWordSet.stopwords.contains(newWord)) {

                            sb.setLength(0);
                            continue;
                        } else // it is dictionary word, we use porter stemmer algorithm
                        {

                            stemmer.add(newWord.toLowerCase().toCharArray(), newWord.length());
                            stemmer.stem();
                            newWord = stemmer.toString();

                            if (wordList.containsKey(newWord)) {
                                wordList.put(newWord, wordList.get(newWord) + 1);
                            } else {
                                wordList.put(newWord, 1.0);
                            }
                            ++numberOfWordsInDocument;
                        }

                        sb.setLength(0); // empty the string builder

                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            for (String word : wordList.keySet()) {
                wordList.put(word, (double) wordList.get(word) / numberOfWordsInDocument);
            }
            directIndex.put(fileName, wordList);
        }

    }

    public DirectIndex() {
        directIndex = new HashMap<>();
    }

    public HashMap<String, HashMap<String, Double>> getDirectIndex() {
        if (directIndex.isEmpty())
            return null;
        return directIndex;
    }

    public void directIndex(String websiteFolder) throws IOException {

        LinkedList<String> folderQueue = new LinkedList<>();

        folderQueue.add(websiteFolder);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        while (!folderQueue.isEmpty()) {
            String currentFolder = folderQueue.pop();
            File folder = new File(currentFolder);
            File[] listOfFiles = folder.listFiles();

            for (File file : listOfFiles) {
                if (file.isDirectory()) {

                    folderQueue.add(file.getAbsolutePath());

                } else if (file.isFile() && Files.probeContentType(file.toPath()).equals("text/plain")) {

                    String fileName = file.getAbsolutePath();
                    DirectIndexTask dIndexTask = new DirectIndexTask(fileName);
                    threadPoolExecutor.execute(dIndexTask);
                }
            }
        }
    }
}

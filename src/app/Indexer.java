package app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;

public class Indexer {
    static long startTime, stopTime, timeDifference;

    public static void main(String[] args) throws IOException {

        MongoConnection mongoConnection = new MongoConnection("localhost", 27017);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        IndirectIndex indirectIndexClass = new IndirectIndex();
        DirectIndex directIndexClass = new DirectIndex();
        VectorSearch vectorSearch = new VectorSearch();

        String query;
        SortedSet<HashMap.Entry<String, Double>> vectorSearchResults;
        Scanner queryScanner = new Scanner(System.in);

        do {
            System.out.println();
            System.out.println("1 - Creare index direct + indice tf");
            System.out.println("2 - Memorare index direct + tf in mongo");
            System.out.println();
            System.out.println("3 - Creare index indirect + indice idf");
            System.out.println("4 - Memorare index indirect + idf in mongo");
            System.out.println();
            System.out.println("5 - Creare vectori asociati");
            System.out.println("6 - Memoreaza vectori asociati in mongo");
            System.out.println("7 - Incarca in memorie vectorii asociati");
            System.out.println("8 - Cautare in vectorii asociati");
            System.out.println();

            System.out.println("9 - Iesire");

            System.out.print("Optiune: ");
            Scanner reader = new Scanner(System.in);
            int option = reader.nextInt();

            System.out.println();

            switch (option) {
                case 1:
                    try {
                        System.out.print("Se creeaza indexul direct + tf... ");
                        startTime = System.currentTimeMillis();
                        directIndexClass.directIndex("./test-files/");
                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("OK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        // TODO: handle exception
                        System.out.println("Nu se poate crea indexul direct + tf!");
                        e.printStackTrace();
                    }
                    break;

                case 2:

                    if (directIndexClass.getDirectIndex() == null) {
                        System.out.print("Creati indexul direct + tf!");
                        break;
                    }
                    try {
                        System.out.println("Se memoreaza indexul direct in mongo...");
                        startTime = System.currentTimeMillis();
                        mongoConnection.writeDirectIndexToDatabase("directIndex", directIndexClass.getDirectIndex());
                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("OK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        // TODO: handle exception
                        System.out.println("Nu se poate memora indexul indirect in mongo!");
                        e.printStackTrace();
                    }

                    break;

                case 3:

                    try {
                        System.out.print("Se creeaza indexul indirect + idf... ");
                        startTime = System.currentTimeMillis();

                        indirectIndexClass.indirectIndex("directIndex", mongoConnection.getDatabase());

                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("OK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        // TODO: handle exception
                        System.out.println("Nu se poate crea indexul indirect + idf!");
                        e.printStackTrace();
                    }
                    break;

                case 4:
                    try {
                        if (indirectIndexClass.getIndirectIndex() == null) {
                            System.out.print("Creati indexul indirect + idf!");
                            break;
                        }
                        System.out.println("Se memoreaza indexul indirect + idf in mongo...");
                        startTime = System.currentTimeMillis();
                        mongoConnection.writeIndirectIndexToDatabase("indirectIndex",
                                indirectIndexClass.getIndirectIndex());
                        mongoConnection.writeIdfToDatabase("idf", indirectIndexClass.getIdf());
                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("OK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        System.out.println("Nu se poate scrie indexul indirect + idf in mongo!");
                        e.printStackTrace();
                    }
                    break;

                case 5:
                    try {
                        System.out.print("Se creeaza vectorii asociati documentelor... ");
                        startTime = System.currentTimeMillis();
                        vectorSearch.createAssociatedDocumentVectors(mongoConnection.getDatabase(), "directIndex",
                                "indirectIndex", "idf");
                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("\nOK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        // TODO: handle exception
                        System.out.println("Vectorii asociati documentelor nu pot fi creati!");
                        e.printStackTrace();
                    }

                    break;

                case 6:
                    try {
                        if (vectorSearch.getAssociatedVectors() == null) {
                            System.out.print("Creati indexul indirect + idf!");
                            break;
                        }
                        System.out.println("Se memoreaza vectorii asociati in mongo...");
                        startTime = System.currentTimeMillis();
                        mongoConnection.writeAssociatedVectorsToDatabase("associatedVectors",
                                vectorSearch.getAssociatedVectors());
                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("OK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        System.out.println("Nu se pot scrie vectorii asociati in mongo!");
                        e.printStackTrace();
                    }
                    break;

                case 7:
                    try {
                        System.out.print("Se incarca vectorii asociati in memorie");
                        startTime = System.currentTimeMillis();
                        vectorSearch.loadAssociatedVectors(mongoConnection.getDatabase(), "associatedVectors");
                        vectorSearch.loadIdf(mongoConnection.getDatabase(), "idf");
                        stopTime = System.currentTimeMillis();
                        timeDifference = stopTime - startTime;
                        System.out.println("OK (" + (double) timeDifference / 1000 + " secunde)");
                    } catch (Exception e) {
                        // TODO: handle exception
                        System.out.println("Vectorii asociati nu pot fi incarcati in memorie!");
                        e.printStackTrace();
                    }
                    break;

                case 8:

                    if (vectorSearch.getAssociatedVectors() == null) {
                        System.out.println("Incarcati vectorii asociati in memorie!");
                        break;
                    }
                    System.out.println("Introduceti interogarea:");
                    query = queryScanner.nextLine();

                    startTime = System.currentTimeMillis();
                    vectorSearchResults = vectorSearch.search(query);
                    stopTime = System.currentTimeMillis();
                    timeDifference = stopTime - startTime;
                    if (vectorSearchResults != null && !vectorSearchResults.isEmpty()) {

                        System.out.println("\nRezultatele cautarii:");
                        for (Map.Entry<String, Double> resultDoc : vectorSearchResults) {
                            System.out.println("\t" + resultDoc.getKey() + " (relevanta "
                                    + (double) Math.round(resultDoc.getValue() * 10000.0) / 1.0 + " %..)");
                        }
                        System.out.println("OK (" + vectorSearchResults.size() + " rezultate gasite in ");
                        System.out.println((double) timeDifference / 1000 + " secunde)");

                    } else {
                        System.out.println("niciun rezultat gasit! (" + (double) timeDifference / 1000 + " secunde)");
                    }
                    break;
                case 9:
                    System.exit(0);
                default:
                    System.out.println("\nEROARE: Optiunea nu exista!");
            }
            System.console().flush();

            System.out.print("\nApasati o tasta pentru a continua");
            Scanner cont = new Scanner(System.in);
            cont.nextLine();
        } while (true);
    }
}

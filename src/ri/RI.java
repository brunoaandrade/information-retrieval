/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexandre Brigeiro mec: 42201,
 * @author Bruno Andrade mec: 59506
 */
public class RI {

    /**
     * @param args the command line arguments
     */
    private static final int maxThreads = 10;
    private static long startTime;
    private static File folder;
    private static CorpusReader rf;
    private static InverterIndex t;
    private static Search s;
    private static boolean useStopWord = false;
    private static boolean usePorterStemmer = false;
    private static Map<String, Integer> listTokens;
    private static Map<Integer, Double> rankDoc;
    private static Map<Integer, String> listDocuments;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("Start Program");
        boolean createIndexNotReadFile = askWhatToDo();//preciso criar
        if (createIndexNotReadFile) {
            Scanner in = new Scanner(System.in);
            System.out.println("Write index to File?(y/n)");
            String stop;
            stop = in.nextLine();
            System.out.println("");
            createIndex(args, "y".equals(stop));// preciso  meter a preencher listas

        } else {
            readListsFromFiles();// preciso  meter a preencher listas
        }
        s = new Search(listTokens, usePorterStemmer, rankDoc,listDocuments);
        while (true) {
            search();//preciso criar
        }
    }

    private static void askIfUsePorter() {
        Scanner in = new Scanner(System.in);
        System.out.println("Do you want to use Porter Stemmer?(y/n)");
        String stemmer;
        stemmer = in.nextLine();
        System.out.println("");
        usePorterStemmer = "y".equals(stemmer);
    }

    private static void askIfUseStopWords() {
        Scanner in = new Scanner(System.in);
        if (useStopWord == false) {
            System.out.println("Do you want to use Stop Words?(y/n)");
            String stop;
            stop = in.nextLine();
            System.out.println("");
            useStopWord = "y".equals(stop);
        }
    }

    private static boolean askIfUseDefault() {
        Scanner in = new Scanner(System.in);
        System.out.println("Use default Setings?(y/n)");
        String stop;
        stop = in.nextLine();
        System.out.println("");
        return "y".equals(stop);
    }

    private static void createIndex(String[] args, boolean write) {
        String stopWord;
        if (args.length == 1) {
            useStopWord = true;
            stopWord = args[0];
        } else {
            stopWord = "stopword.txt";
        }
        if (!askIfUseDefault()) {
            askIfUseStopWords();
            askIfUsePorter();
        }

        //path to the home file, here are all the documents
        folder = new File("Files_to_Tokenize");
        //Instance the CorpusReader, to read all files, and the Tokenizer, to filter the terms
        rf = new CorpusReader();
        t = new InverterIndex(stopWord, useStopWord, usePorterStemmer, write);
        System.out.println("Start Searching files");
        //save the time that the program started
        startTime = System.currentTimeMillis();
        //find all files in home folder
        rf.listFilesForFolder(folder);
        rf.writeFileDocId();
        //create a array of threads
        HandlerFile[] testeThread = new HandlerFile[maxThreads];
        System.out.println("Start tokenizing and indexation Files");
        //for all threads
        for (int i = 0; i < maxThreads; i++) {
            //instance the threads
            testeThread[i] = new HandlerFile(t, rf);
            //start the threads
            testeThread[i].start();
        }

        //wait for all threads to be done
        for (int j = 0; j < maxThreads; j++) {
            try {
                testeThread[j].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(RI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //calculate the time of execution of the program
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("End Program");
        System.out.println("Time to Index Documents: " + totalTime / 60000 + " min " + (totalTime % 60000) / 1000 + " s " + (totalTime % 60000) % 1000 + " ms");
        System.out.println("End tokenizing and indexation Files");
        //write to file, the terms ID

        if (write) {
            listDocuments = rf.getListArticlesComments();
            rf.clearHashMaps();
            t.joinHashMaps();
            listTokens = t.getListTokens();
            rankDoc = t.getRankDoc();
            t.clearHashMaps();
            try {
                Gson gson = new GsonBuilder().create();
                String json = gson.toJson(listTokens);
                PrintWriter writer = new PrintWriter("Objects_database/tokens.txt", "UTF-8");
                writer.println(json);
                writer.close();

                gson = new GsonBuilder().create();
                json = gson.toJson(rankDoc);
                writer = new PrintWriter("Objects_database/rankDoc.txt", "UTF-8");
                writer.println(json);
                writer.close();

                gson = new GsonBuilder().create();
                json = gson.toJson(listDocuments);
                writer = new PrintWriter("Objects_database/listDocuments.txt", "UTF-8");
                writer.println(json);
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(RI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        t.writeFileTermId();
    }

    private static boolean askWhatToDo() {
        Scanner in = new Scanner(System.in);
        System.out.println("Read Index from File?(y/n)");
        String stop;
        stop = in.nextLine();
        System.out.println("");
        return "n".equals(stop);
    }

    private static void readListsFromFiles() {
        try {
            Gson gson = new GsonBuilder().create();
            byte[] encoded = Files.readAllBytes(Paths.get("Objects_database/tokens.txt"));
            String json = new String(encoded, "UTF-8");
            Type typeOfHashMap = new TypeToken<Map<String, Integer>>() {
            }.getType();
            listTokens = gson.fromJson(json, typeOfHashMap);

            encoded = Files.readAllBytes(Paths.get("Objects_database/rankDoc.txt"));
            json = new String(encoded, "UTF-8");
            typeOfHashMap = new TypeToken<Map<Integer, Double>>() {
            }.getType();
            rankDoc = gson.fromJson(json, typeOfHashMap);
            
            encoded = Files.readAllBytes(Paths.get("Objects_database/listDocuments.txt"));
            json = new String(encoded, "UTF-8");
            typeOfHashMap = new TypeToken<Map<Integer, String>>() {
            }.getType();
            listDocuments = gson.fromJson(json, typeOfHashMap);
        } catch (FileNotFoundException ex) {
            System.out.println("Files are missing...");
            Logger.getLogger(RI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.out.println("Error loding information from files...");
            Logger.getLogger(RI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void search() {
        long startSearch;
        Scanner in = new Scanner(System.in);
        try {
            System.out.print("Search: ");
            String stop;
            stop = in.nextLine();
            System.out.println("");

            String[] contentFrase;
            int separation = 1;
            String[] results;
            if (stop.contains("\"")) {
                contentFrase = stop.split("\"");
                String word = "";
                for (String x : contentFrase) {
                    System.out.println(x);
                    if (!"".equals(x)) {
                        if (x.contains("~")) {
                            x = x.replace("~", "");
                            x = x.replaceAll("\\s+", "");
                            separation = Integer.parseInt(x);
                        } else {
                            word = word + " " + x;
                        }
                    }

                }
                System.out.println("separation: " + separation + " words: " + word);
                startSearch = System.currentTimeMillis();
                results = s.searchFrase(separation, word);

            } else {
                stop = stop.toLowerCase(Locale.ENGLISH);
                String words = "";
                if ((stop.contains("author") || stop.contains("date") || stop.contains("title")) && stop.contains("=")) {
                    contentFrase = stop.split("[\\+]");
                    String author = "", date = "", title = "";
                    boolean find;
                    for (String contentFrase1 : contentFrase) {
                        find = false;
                        if (contentFrase1.contains("author") && contentFrase1.contains("=")) {
                            author = contentFrase1.replace("author", "").replace("=", "");
                            find = true;
                        }
                        if (contentFrase1.contains("date")) {
                            date = contentFrase1.replace("date", "").replace("=", "");
                            find = true;
                        }
                        if (contentFrase1.contains("title")) {
                            title = contentFrase1.replace("title", "").replace("=", "");
                            find = true;
                        }
                        if (find == false) {
                            words = words + " " + contentFrase1;
                        }
                    }
                    System.out.println("words: " + words + " || title: " + title + " || date: " + date + " || author: " + author);
                    startSearch = System.currentTimeMillis();
                    results = s.searchWords(words, title, date, author);
                } else {
                    System.out.println(stop);
                    startSearch = System.currentTimeMillis();
                    results = s.searchWords(stop);
                }
            }
            int i = 0;
            if(results!=null){
                for (String x : results) {
                    i++;
                    System.out.println(i + " - " + x);
                }
            }
            else{
                System.out.println("No results.");
            }
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startSearch;
            System.out.println("Search time: " + totalTime / 60000 + " min " + (totalTime % 60000) / 1000 + " s " + (totalTime % 60000) % 1000 + " ms");
        } catch (Exception ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Invalide query");
            System.out.println("try again");
        }
    }
}

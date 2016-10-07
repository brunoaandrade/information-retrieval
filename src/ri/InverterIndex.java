/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowballstemmer.EnglishStemmer;

/**
 *
 * @author Alexandre Brigeiro mec: 42201,
 * @author Bruno Andrade mec: 59506
 */
public class InverterIndex {

    private List<String> stopDic;
    private final Map<String, Integer> listTokens;
    private final EnglishStemmer stemmer;
    private final boolean useStopWord;
    private final boolean usePorterStemmer;
    private final Map<String, ArrayList<Integer>> listAuthors;
    private final Map<Date, ArrayList<Integer>> listDates;
    private final Map<String, ArrayList<Integer>> listTitles;
    private final Map<Character, Map<String, Map<Integer, ArrayList<Integer>>>> listPosition;
    private final Map<Integer, Double> rankDoc;
    private final boolean write;
    private int docMRead = 1;

    /**
     * Inicialize the class create the files, and if already exists delete is
     * content
     *
     * @param stopWord
     * @param useStopWord
     * @param usePorterStemmer
     * @param write
     */
    public InverterIndex(String stopWord, boolean useStopWord, boolean usePorterStemmer, boolean write) {

        this.write = write;
        listTokens = new HashMap();
        stopDic = null;
        stemmer = new EnglishStemmer();
        listAuthors = new HashMap();
        listDates = new HashMap();
        listTitles = new HashMap();
        listPosition = new HashMap<>();
        rankDoc = new HashMap();
        this.useStopWord = useStopWord;
        this.usePorterStemmer = usePorterStemmer;
        //read the file of stop word
        File file1 = new File("Results/doc_index.txt");
        // if file doesnt exists, then create it
        if (file1.exists()) {
            file1.delete();
        }

        if (write) {
            File dir = new File("Objects_database");
            deleteFiles(dir);
        }
        if (useStopWord) {
            stopDic = createStopDictionary(stopWord);
        }
    }

    private void deleteFiles(File folder) {

        for (final File fileEntry : folder.listFiles()) {
            //if find a directory, it will list the files in that directory
            if (fileEntry.isDirectory()) {
                deleteFiles(fileEntry);
            } //if i
            else {
                fileEntry.delete();
            }

        }
    }

    //read the stop words from the file
    private List<String> createStopDictionary(String path) {
        List<String> lines = new ArrayList<>();
        try {
            FileInputStream fis;
            File stop = new File(path);
            fis = new FileInputStream(stop);
            InputStreamReader z = new InputStreamReader(fis);
            try (BufferedReader a = new BufferedReader(z)) {
                String line;
                while ((line = a.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CorpusReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CorpusReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lines;
    }

    /**
     * identified Tokens
     *
     * @param s
     * @return
     */
    public synchronized String identifiedTokens(String s) {
        if (useStopWord && usePorterStemmer) {
            if (!listTokens.containsKey(s) && !stopDic.contains(s)) {
                stemmer.setCurrent(s);
                if (stemmer.stem()) {
                    listTokens.put(stemmer.getCurrent(), listTokens.size());
                    return stemmer.getCurrent();
                }
            }
        }
        if (useStopWord && !usePorterStemmer) {

            if (!listTokens.containsKey(s) && !stopDic.contains(s)) {
                listTokens.put(s, listTokens.size());
                return s;
            }
        }
        if (!useStopWord && usePorterStemmer) {

            if (!listTokens.containsKey(s)) {
                stemmer.setCurrent(s);
                if (stemmer.stem()) {
                    listTokens.put(stemmer.getCurrent(), listTokens.size());
                    return stemmer.getCurrent();
                }
            }
        }
        if (!useStopWord && !usePorterStemmer) {
            if (!listTokens.containsKey(s)) {
                listTokens.put(s, listTokens.size());
                return s;
            }
        }
        if (useStopWord && stopDic.contains(s)) {
            return "";
        }
        if (usePorterStemmer) {
            stemmer.setCurrent(s);
            if (stemmer.stem()) {
                return stemmer.getCurrent();
            }
        }
        return s;
    }

    /**
     * Index the document analysing the terms that existes in the document
     *
     * @param s , array of all words in document
     * @param fileID file identification of the document
     */
    public synchronized void indexDocument(String[] s, int fileID) {
        // see all terms in the document
        HashMap<String, ArrayList<Integer>> docIndex = new HashMap();
        for (int i = 0; i < s.length; i++) {

            if (listTokens.get(s[i]) == null) {
                s[i] = identifiedTokens(s[i]);
            }
            // find the term id of the term
            if (!"".equals(s[i]) && (listTokens.get(s[i]) != null)) {
                ArrayList<Integer> docsWithTerm;
                // if is the first time that the term is find in the document
                if (!docIndex.containsKey(s[i])) {
                    //add  nem entry
                    ArrayList<Integer> myCoords = new ArrayList<>();
                    myCoords.add(i);
                    docIndex.put(s[i], myCoords);
                } else {
                    // find the location of the term in the document
                    //remove it
                    docsWithTerm = docIndex.get(s[i]);
                    //add the new location
                    docsWithTerm.add(i);
                    //add the entery to hash map
                    docIndex.put(s[i], docsWithTerm);
                }
            }
        }
        if (write) {
            printIndexToObject(docIndex, fileID);
        }
        tfCalcDocument(fileID, docIndex);
    }

    /**
     *
     * @param positionInFile
     * @param docID
     */
    public synchronized void printIndexToObject(HashMap<String, ArrayList<Integer>> positionInFile, int docID) {
        Set<String> keySet = positionInFile.keySet();
        for (String s : keySet) {
            Map<String, Map<Integer, ArrayList<Integer>>> listByTermID;
            if (Character.isDigit(s.charAt(0))) {
                listByTermID = listPosition.get('0');
            } else {
                listByTermID = listPosition.get(s.charAt(0));
            }
            if (listByTermID == null) {
                listByTermID = new HashMap<>();
            }
            Map<Integer, ArrayList<Integer>> listFiles = listByTermID.get(s);
            if (listFiles == null) {
                listFiles = new HashMap<>();
            }
            listFiles.put(docID, positionInFile.get(s));
            listByTermID.put(s, listFiles);

            if (Character.isDigit(s.charAt(0))) {
                listPosition.put('0', listByTermID);
            } else {
                listPosition.put(s.charAt(0), listByTermID);
            }
        }

        Runtime runtime = Runtime.getRuntime();
        int memMB = 800;
        int docRead = 10000;
        long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemPerc = runtime.maxMemory() * 70 / 100;
        if ((allocatedMemory > 1024 * 1000 * memMB || allocatedMemory > maxMemPerc) && docID / docRead > docMRead) {
            docMRead++;
            writeListPositionToFile();
            writeHashMapsFiles();
            //long memNow = allocatedMemory - (runtime.totalMemory() - runtime.freeMemory());
            //System.out.println("free memory: " + memNow);
        }
    }

    /**
     *
     */
    public synchronized void writeListPositionToFile() {
        File file;
        Set<Character> keySet = listPosition.keySet();
        for (char key : keySet) {
            try {

                int location;
                int value = Character.getNumericValue(key);
                if (value < 10) {
                    location = 0;
                } else {
                    location = value - 9;
                }
                Map<String, Map<Integer, ArrayList<Integer>>> listByTermID;
                if (Character.isDigit(key)) {
                    listByTermID = listPosition.get(key);
                } else {
                    listByTermID = listPosition.get(key);
                }
                int i = 0;
                do {
                    file = new File("Objects_database/index/" + location + "/" + i + ".txt");
                    i++;
                } while (file.exists());
                Gson gson = new GsonBuilder().create();
                String json = gson.toJson(listByTermID);
                try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                    writer.println(json);
                }

                listByTermID.clear();
                listPosition.put(key, listByTermID);

            } catch (FileNotFoundException ex) {
                Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        listPosition.clear();
    }

    private void writeHashMapsFiles() {
        File file;
        try {
            int i = 0;
            do {
                file = new File("Objects_database/index/authors/" + i + ".txt");
                i++;
            } while (file.exists());
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(listAuthors);
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println(json);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        listAuthors.clear();

        try {
            int i = 0;
            do {
                file = new File("Objects_database/index/dates/" + i + ".txt");
                i++;
            } while (file.exists());
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(listDates);
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println(json);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        listDates.clear();

        try {
            int i = 0;
            do {
                file = new File("Objects_database/index/titles/" + i + ".txt");
                i++;
            } while (file.exists());
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(listTitles);
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println(json);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        listTitles.clear();
    }

    /**
     *
     */
    public void joinHashMaps() {
        writeHashMapsFiles();
        writeListPositionToFile();
        joinFilesPositionHashMaps();
        joinStringArrayIntegerHashMaps("Objects_database/index/authors/");
        joinStringArrayIntegerHashMaps("Objects_database/index/titles/");
        joinStringArrayIntegerHashMapsDate();
    }

    private void joinStringArrayIntegerHashMaps(String path) {
        //private HashMap<String, ArrayList<Integer>> listAuthors;
        //private HashMap<String, ArrayList<Integer>> listTitles;
        Map<String, ArrayList<Integer>> mainList;
        Map<String, ArrayList<Integer>> list = null;
        File file = new File(path);
        mainList = new HashMap<>();
        for (final File filestoJoin : file.listFiles()) {
            try {
                Gson gson = new GsonBuilder().create();
                byte[] encoded = Files.readAllBytes(filestoJoin.toPath());
                String json = new String(encoded, "UTF-8");
                Type typeOfHashMap = new TypeToken<Map<String, ArrayList<Integer>>>() {
                }.getType();
                list = gson.fromJson(json, typeOfHashMap);
                filestoJoin.delete();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////-------algoritmo the  two join lists-----/////////////////////////////////////////////////////////////////
            if (mainList == null || mainList.isEmpty()) {
                mainList = list;
            } else {
                for (Map.Entry<String, ArrayList<Integer>> e : list.entrySet()) {
                    ArrayList<Integer> termsTemp = e.getValue();
                    ArrayList<Integer> a = mainList.get(e.getKey());
                    if (a != null) {
                        a.addAll(termsTemp);
                        mainList.put(e.getKey(), a);
                    } else {
                        mainList.put(e.getKey(), termsTemp);
                    }
                }
            }
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        }

        try {
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(mainList);
            try (PrintWriter writer = new PrintWriter("Objects_database/" + file.getName() + ".txt", "UTF-8")) {
                writer.println(json);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void joinStringArrayIntegerHashMapsDate() {
        String path = "Objects_database/index/dates/";
        Map<Date, ArrayList<Integer>> mainList;
        Map<Date, ArrayList<Integer>> list = null;
        File file = new File(path);
        mainList = new HashMap<>();
        for (final File filestoJoin : file.listFiles()) {
            try {
                Gson gson = new GsonBuilder().create();
                byte[] encoded = Files.readAllBytes(filestoJoin.toPath());
                String json = new String(encoded, "UTF-8");
                Type typeOfHashMap = new TypeToken<Map<String, ArrayList<Integer>>>() {
                }.getType();
                list = gson.fromJson(json, typeOfHashMap);
                filestoJoin.delete();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////-------algoritmo the  two join lists-----/////////////////////////////////////////////////////////////////
            if (mainList == null || mainList.isEmpty()) {
                mainList = list;
            } else {
                for (Map.Entry<Date, ArrayList<Integer>> e : list.entrySet()) {
                    ArrayList<Integer> termsTemp = e.getValue();
                    ArrayList<Integer> a = mainList.get(e.getKey());
                    if (a != null) {
                        a.addAll(termsTemp);
                        mainList.put(e.getKey(), a);
                    } else {
                        mainList.put(e.getKey(), termsTemp);
                    }
                }
            }
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        }

        try {
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(mainList);
            try (PrintWriter writer = new PrintWriter("Objects_database/" + file.getName() + ".txt", "UTF-8")) {
                writer.println(json);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void joinFilesPositionHashMaps() {
        //Map<String, HashMap<Integer, ArrayList<Integer>>> listPosition;
        Map<String, HashMap<Integer, ArrayList<Integer>>> mainList;
        Map<String, HashMap<Integer, ArrayList<Integer>>> list = new HashMap<>();
        File file = new File("Objects_database/index/");
        for (final File fileEntry : file.listFiles()) {
            if (((fileEntry.getName().equals("authors") || fileEntry.getName().equals("titles")) || fileEntry.getName().equals("rank") || fileEntry.getName().equals("dates")) || fileEntry.getName().equals("invertedIndex") || fileEntry.getName().equals("tokens")) {
            } else {
                try {
                    int charID = Integer.parseInt(fileEntry.getName());
                    mainList = new HashMap<>();
                    for (File filestoJoin : fileEntry.listFiles()) {
                        try {
                            Gson gson = new GsonBuilder().create();
                            byte[] encoded = Files.readAllBytes(filestoJoin.toPath());
                            String json = new String(encoded, "UTF-8");
                            Type typeOfHashMap = new TypeToken<Map<String, HashMap<Integer, ArrayList<Integer>>>>() {
                            }.getType();
                            list = gson.fromJson(json, typeOfHashMap);
                            filestoJoin.delete();

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        //////////////-------algoritmo the  two join lists-----/////////////////////////////////////////////////////////////////
                        if (mainList == null || mainList.isEmpty()) {
                            mainList = list;
                        } else {
                            for (Map.Entry<String, HashMap<Integer, ArrayList<Integer>>> e : list.entrySet()) {
                                HashMap<Integer, ArrayList<Integer>> termsTemp = e.getValue();
                                HashMap<Integer, ArrayList<Integer>> a = mainList.get(e.getKey());
                                if (a != null) {
                                    a.putAll(termsTemp);
                                    mainList.put(e.getKey(), a);
                                } else {
                                    mainList.put(e.getKey(), termsTemp);
                                }
                            }
                        }
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }
                    Gson gson = new GsonBuilder().create();
                    String json = gson.toJson(mainList);
                    try (PrintWriter writer = new PrintWriter("Objects_database/indexPosition" + charID + ".txt", "UTF-8")) {
                        writer.println(json);
                    }
                    if (mainList != null) {
                        mainList.clear();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Write the terms id to document
     */
    public void writeFileTermId() {
        try {
            FileWriter fw;
            System.out.println("Terms find " + listTokens.size());
            File file = new File("Results/termids.txt");
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter out = new BufferedWriter(fw);
            for (String key : listTokens.keySet()) {

                out.write(listTokens.get(key) + " - " + key);
                out.newLine();
                out.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Print to document the indexation of the document
     *
     * @param fileID
     * @param docToIndex
     */
    public synchronized void printIndexDocument(int fileID, Map<Integer, ArrayList<Integer>> docToIndex) {

        PrintWriter out = null;
        try {
            String fileName = "Results/doc_index.txt";
            File file = new File(fileName);
            out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            Set keys = docToIndex.keySet();
            for (Iterator i = keys.iterator(); i.hasNext();) {
                int key = (int) i.next();
                ArrayList<Integer> value = docToIndex.get(key);
                out.println(fileID + "\t" + key + "\t" + value.toString() + "\n");
                out.println();
                out.flush();

            }
        } catch (IOException ex) {
            Logger.getLogger(InverterIndex.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     *
     * @param internalID
     * @param author
     * @param title
     * @param date
     */
    public synchronized void addAuthorTitleDate(int internalID, String author, String title, Date date) {
        ArrayList<Integer> get;

        if (author != null && author.length() > 0) {
            author = author.replace("\n", "");
            if (listAuthors.containsKey(author)) {
                get = listAuthors.get(author);
            } else {
                get = new ArrayList<>();
            }
            get.add(internalID);
            listAuthors.put(author, get);
        }

        if (date != null && !"".equals(date.toString())) {
            SimpleDateFormat parserSDF = new SimpleDateFormat("EEE MMM d yyyy HH:mm:ss ");
            Date x = new Date(date.getYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes(), date.getSeconds());
            if (listDates.containsKey(x)) {
                get = listDates.get(x);
            } else {
                get = new ArrayList<>();
            }
            get.add(internalID);
            listDates.put(x, get);
        }
        if (title != null && title.length() > 0) {
            title = title.replace("\n", "");
            if (listTitles.containsKey(title)) {
                get = listTitles.get(title);
            } else {
                get = new ArrayList<>();
            }
            get.add(internalID);
            listTitles.put(title, get);
        }
    }

    /**
     *
     * @param fileID
     * @param docIndex
     */
    public synchronized void tfCalcDocument(int fileID, HashMap<String, ArrayList<Integer>> docIndex) {
        Set<String> termFile = docIndex.keySet();
        double normal = 0;
        for (String i : termFile) {
            normal += Math.pow(docIndex.get(i).size(), 2);
        }
        Math.sqrt(normal);
        rankDoc.put(fileID, normal);
    }

    void clearHashMaps() {
        if (stopDic != null) {
            stopDic.clear();
        }
        listAuthors.clear();
        listDates.clear();
        listTitles.clear();
        listPosition.clear();
    }

    /**
     *
     * @param readFile
     * @return
     */
    public synchronized String[] tokenize(String readFile) {
        Tokenizer t = new Tokenizer();
        readFile = t.lowerCase(readFile);
        readFile = t.removeChar(readFile);
        readFile = t.normalizer(readFile);
        readFile = t.smallWords(readFile);
        String[] s = t.whiteSpace(readFile);
        return s;
    }

    /**
     *
     * @return
     */
    public Map<String, ArrayList<Integer>> getListAuthors() {
        return listAuthors;
    }

    /**
     *
     * @return
     */
    public Map<String, ArrayList<Integer>> getListTitles() {
        return listTitles;
    }

    /**
     *
     * @return
     */
    public Map<String, Integer> getListTokens() {
        return listTokens;
    }

    /**
     *
     * @return
     */
    public Map<Integer, Double> getRankDoc() {
        return rankDoc;
    }
}

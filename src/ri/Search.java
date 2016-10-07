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
import java.io.UnsupportedEncodingException;
import static java.lang.Math.abs;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowballstemmer.EnglishStemmer;

/**
 *
 * @author Bruno
 */
class Search {

    private Map<String, ArrayList<Integer>> listAuthors;
    private Map<Date, ArrayList<Integer>> listDates;
    private Map<String, ArrayList<Integer>> listTitles;
    private Map<Integer, String> listDocuments;
    private Map<Integer, ArrayList<Integer>> listPosition;
    private final Map<Integer, Double> rankDoc;
    private final Map<String, Integer> listTokens;
    private final Tokenizer t = new Tokenizer();
    private final boolean usePorterStemmer;
    private String[] tokens;

    Search(Map<String, Integer> listTokens, boolean usePorterStemmer, Map<Integer, Double> rankDoc, Map<Integer, String> listDocuments) {
        this.listTokens = listTokens;
        this.usePorterStemmer = usePorterStemmer;
        this.rankDoc = rankDoc;
        listPosition = new HashMap<>();
        this.listDocuments = listDocuments;
    }

    public String[] searchFrase(int separation, String words) {
        tokens = t.tokenize(words);
        if (removeWordsNotInTokens()) {
            if (usePorterStemmer) {
                porter();
            }
            List<Integer> filesWithWord = new ArrayList<>();
            List<Integer> filesRespectingPosition = new ArrayList<>();
            int count = 0;
            for (String s : tokens) {
                listPosition = new HashMap<>();
                readFile(s);
                List<Integer> keySet = new ArrayList<>();
                keySet.addAll(listPosition.keySet());
                if (count == 0) {
                    filesWithWord.addAll(keySet);
                } else {
                    filesWithWord = intersection(filesWithWord, keySet);
                }
                count++;
            }
            ///por cada ficheiro, ver a localizacao e ver se estao a distancia separation
            //HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> listPosition;
            if (tokens.length > 1) {
                List<Integer> temp;
                for (int i = 0; i < tokens.length - 1; i++) {
                    temp = new ArrayList<>();
                    listPosition = new HashMap<>();
                    readFile(tokens[i]);
                    Map<Integer, ArrayList<Integer>> getFirstWordLocation = listPosition;
                    listPosition = new HashMap<>();
                    readFile(tokens[i + 1]);
                    Map<Integer, ArrayList<Integer>> getSecondWordLocation = listPosition;
                    listPosition = new HashMap<>();
                    for (int f : filesWithWord) {
                        ArrayList<Integer> getFirst = getFirstWordLocation.get(f);
                        ArrayList<Integer> getSecond = getSecondWordLocation.get(f);
                        for (int posA : getFirst) {
                            for (int posB : getSecond) {
                                if (abs(posA - posB) < separation + 1) {
                                    temp.add(f);
                                }
                            }
                        }
                    }
                    if (i > 0) {
                        filesRespectingPosition = intersection(temp, filesRespectingPosition);
                    } else {
                        filesRespectingPosition = temp;
                    }
                }
            } else {
                filesRespectingPosition = filesWithWord;
            }
            return getFilesRanked(tokens, filesRespectingPosition);
        }
        return null;
    }

    public String[] searchWords(String words) {

        tokens = t.tokenize(words);
        if (removeWordsNotInTokens()) {
            if (usePorterStemmer) {
                porter();
            }
            ArrayList<Integer> get = new ArrayList<>();
            for (String term : tokens) {
                listPosition = new HashMap<>();
                readFile(term);
                get.addAll(listPosition.keySet());
            }
            return getFilesRanked(tokens, get);
        }
        return null;
    }

    public String[] searchWords(String words, String title, String date, String author) {
        boolean header = false;
        tokens = t.tokenize(words);
        if (removeWordsNotInTokens()) {
            if (usePorterStemmer) {
                porter();
            }
            List<Integer> filesHeader = new ArrayList<>();
            try {
                if (!"".equals(title)) {
                    Gson gson = new GsonBuilder().create();
                    byte[] encoded = Files.readAllBytes(Paths.get("Objects_database/titles.txt"));
                    String json = new String(encoded, "UTF-8");
                    Type typeOfHashMap = new TypeToken<Map<String, ArrayList<Integer>>>() {
                    }.getType();
                    listTitles = gson.fromJson(json, typeOfHashMap);

                    ArrayList<Integer> get = listTitles.get(title);
                    if (get != null) {
                        header = true;
                        filesHeader = get;
                    }
                }
                if (!"".equals(date)) {
                    Gson gson = new GsonBuilder().create();
                    byte[] encoded = Files.readAllBytes(Paths.get("Objects_database/dates.txt"));
                    String json = new String(encoded, "UTF-8");
                    Type typeOfHashMap = new TypeToken<Map<String, ArrayList<Integer>>>() {
                    }.getType();
                    listDates = gson.fromJson(json, typeOfHashMap);

                    ArrayList<Integer> get;
                    Date dateValue = null;
                    try {
                        dateValue = new SimpleDateFormat("EEE MMM dd yyyy hh:mm aaaa", Locale.ENGLISH).parse(date);
                    } catch (ParseException ex1) {
                        try {
                            dateValue = new SimpleDateFormat("MMM dd yyyy hh:mm:ss aaaa", Locale.ENGLISH).parse(date);
                        } catch (ParseException ex2) {
                            try {
                                dateValue = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aaaa", Locale.ENGLISH).parse(date);
                            } catch (ParseException ex3) {
                                try {
                                    dateValue = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa", Locale.ENGLISH).parse(date);
                                } catch (ParseException ex4) {
                                    try {
                                        dateValue = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss aa", Locale.ENGLISH).parse(date);
                                    } catch (ParseException ex5) {
                                        Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex5);
                                    }
                                }
                            }
                        }
                    }
                    if (dateValue != null) {
                        Date temp = new Date(dateValue.getYear(), dateValue.getMonth(), dateValue.getDay(), dateValue.getHours(), dateValue.getMinutes(), dateValue.getSeconds());
                        get = listDates.get(temp);
                        if (get != null) {
                            if (header) {
                                filesHeader = intersection(filesHeader, get);
                            } else {
                                filesHeader = get;
                            }
                            header = true;
                        }
                    }
                }
                if (!"".equals(author)) {
                    Gson gson = new GsonBuilder().create();
                    byte[] encoded = Files.readAllBytes(Paths.get("Objects_database/authors.txt"));
                    String json = new String(encoded, "UTF-8");
                    Type typeOfHashMap = new TypeToken<Map<String, ArrayList<Integer>>>() {
                    }.getType();
                    listAuthors = gson.fromJson(json, typeOfHashMap);

                    ArrayList<Integer> get = listAuthors.get(author);
                    if (get != null) {
                        if (header) {
                            filesHeader = intersection(filesHeader, get);
                        } else {
                            filesHeader = get;
                        }
                    }
                }
                List<Integer> files = new ArrayList<>();
                for (String term : tokens) {
                    listPosition.clear();
                    readFile(term);
                    files.addAll(listPosition.keySet());
                }
                if (filesHeader != null && !filesHeader.isEmpty()) {
                    filesHeader = intersection(filesHeader, files);
                } else {
                    filesHeader = files;
                }
            } catch (IOException ex) {
                Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            }
            listAuthors.clear();
            listDates.clear();
            listTitles.clear();
            return getFilesRanked(tokens, filesHeader);
        }
        return null;
    }

    private int findFileWithLetter(char charAt) {
        int value = Character.getNumericValue(charAt);
        if (value < 10) {
            return 0;
        } else if (value > 36) {
            System.out.println("Error find UpperCase Letter");
        } else {
            return value - 9;
        }
        return -1;
    }

    private void readFile(String word) {
        int location = findFileWithLetter(word.charAt(0));
        Gson gson = new GsonBuilder().create();
        File file = new File("Objects_database/indexPosition" + location + ".txt");
        //for (final File fileEntry : file.listFiles()) {
        try {

            byte[] encoded = Files.readAllBytes(file.toPath());
            String json = new String(encoded, "UTF-8");
            Type typeOfHashMap = new TypeToken<Map<String, Map<Integer, ArrayList<Integer>>>>() {
            }.getType();
            Map<String, Map<Integer, ArrayList<Integer>>> x = gson.fromJson(json, typeOfHashMap);
            if (x != null && !x.isEmpty()) {
                if (listPosition == null) {
                    listPosition = new HashMap<>();
                }
                if (x.containsKey(word)) {
                    listPosition.putAll(x.get(word));
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
        }
        // }
    }

    private List<Integer> intersection(List<Integer> list1, List<Integer> list2) {
        List<Integer> list = new ArrayList<>();
        for (Integer x : list1) {
            if (list2.contains(x)) {
                list.add(x);
            }
        }
        return list;
    }

    private void porter() {
        EnglishStemmer stemmer = new EnglishStemmer();
        for (int i = 0; i < tokens.length; i++) {
            stemmer.setCurrent(tokens[i]);
            tokens[i] = stemmer.getCurrent();
        }
    }

    private boolean removeWordsNotInTokens() {
        String[] temp = new String[tokens.length];
        int i = 0;
        for (String x : tokens) {
            if (listTokens.containsKey(x)) {
                temp[i] = (x);
            }
            i++;
        }
        tokens = temp;
        return tokens.length != 0 && !"".equals(tokens[0]);
    }

    private synchronized double tfCalc(int fileID) {
        //int docNumberWords number of words in a document   
        //int countnumber of times the term termToCheck appears in document
        Double get = rankDoc.get(fileID);
        int size = 0;
        if (listPosition.containsKey(fileID)) {
            size = listPosition.get(fileID).size();
        }
        return (double) size / get;
    }

    private double idfCalc() {
        double count = listPosition.keySet().size();// number of documents where the term appears
        return Math.log(listDocuments.size() / count);
    }

    public double getRank(int idFile) {

        return tfCalc(idFile) * idfCalc();
    }

    public String[] getFilesRanked(String[] id, List<Integer> filesRespectingPosition) {
        Map<Integer, Double> filesFinal = new HashMap<>();
        Collections.sort(filesRespectingPosition);
        for (int i = 0; i < id.length; i++) {
            int count = 1;
            if (!"".equals(id[i])) {
                for (int j = i + 1; j < id.length; j++) {
                    if (id[i].equals(id[j])) {
                        count++;
                        id[j] = "";
                    }
                }
                readFile(id[i]);
                for (int file : filesRespectingPosition) {
                    double x = Math.log(count);
                    if (!filesFinal.containsKey(file)) {
                        filesFinal.put(file, getRank(file) * x);
                    } else {
                        filesFinal.put(file, getRank(file) * +filesFinal.get(file));
                    }
                }
            }
        }
        //listFilePosition.clear();
        String[] result;
        if (filesFinal.size() > 500) {
            result = new String[500];
        } else {
            result = new String[filesFinal.size()];
        }
        filesFinal = sortByValues(filesFinal);
        Set<Integer> keys = filesFinal.keySet();
        int countf = 0;
        for (Integer key : keys) {
            result[countf] = listDocuments.get(key);
            countf++;
            if (countf == 500) {
                return result;
            }
        }
        filesFinal.clear();
        listPosition.clear();
        return result;
    }

    public static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o2, Map.Entry<K, V> o1) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
}

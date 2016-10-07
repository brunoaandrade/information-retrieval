/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ri;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;

/**
 *
 * @author Alexandre Brigeiro mec: 42201,
 * @author Bruno Andrade mec: 59506
 */
public class CorpusReader {

    //Hash map with files
    private final Map<Integer, String> listFiles = new HashMap();

    private final MapClass mp = new MapClass();
    //variable that stores who many files whre already read
    private int alreadyRead = 0;
    private int lastArticle = 0;

    /**
     * search for a id of a file not yet readed
     *
     * @return the id of the file
     */
    public synchronized int askNewFile() {
        if (alreadyRead != listFiles.size()) {
            alreadyRead++;
            if (alreadyRead % 1000 == 0) {
                System.out.println("Files Handle: " + alreadyRead);
            }
            return alreadyRead - 1;
        }
        return -1;
    }

    /**
     * Retreve the content of a file
     *
     * @param key of the file to be read
     * @return the content of a file
     */
    public synchronized List<FileContent> readNewFile(int key) {
        List<FileContent> fileContents = readFile(listFiles.get(key));
        return fileContents;
    }

    /**
     * Search in the main folder for all files fill the hash map
     *
     * @param folder , main folder where are all documents
     */
    public void listFilesForFolder(File folder) {
        int i = 0;
        //search files in directory
        for (final File fileEntry : folder.listFiles()) {
            //if find a directory, it will list the files in that directory
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } //if is a file
            else {
                //ve os primeiros 50 ficheiros,de cada pasta
                if (i < 5000000) {
                    i++;
                    //add to file list
                    if (!listFiles.containsValue(fileEntry.getAbsolutePath())) {
                        listFiles.put(listFiles.size(), fileEntry.getAbsolutePath());
                    }
                } else {
                    return;
                }
            }
        }

    }

    //read the content of a file
    //receve the path of the file and return is content
    private synchronized List<FileContent> readFile(String path) {
        List<FileContent> fileContents = new ArrayList<>();
        Parser parser = new Parser(fileContents, path, mp,lastArticle);
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            try {
                lines = Files.readAllLines(Paths.get(path), StandardCharsets.ISO_8859_1);
            } catch (IOException ex1) {
                Logger.getLogger(CorpusReader.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        Iterator i = lines.listIterator();
        StringBuilder x = new StringBuilder();
        while (i.hasNext()) {
            x.append((String) i.next());
        }
        lastArticle = parser.parseFile(x.toString());
        return fileContents;
    }

    //Fill the docid file with the id and path of the files

    /**
     *
     */
        public void writeFileDocId() {
        try {
            File file = new File("Results/docids.txt");

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            //for each file            
            try (FileWriter fw = new FileWriter(file.getAbsoluteFile()); BufferedWriter bw = new BufferedWriter(fw)) {
                //for each file
                for (Integer key : listFiles.keySet()) {
                    //write the id and path of the files
                    bw.write(key + " - " + listFiles.get(key));
                    bw.newLine();
                    bw.flush();
                }
                bw.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(CorpusReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    Map<Integer, String> getListArticlesComments() {
        return mp.returnHash(); //To change body of generated methods, choose Tools | Templates.

    }

    void clearHashMaps() {
        listFiles.clear();
    }
}

class Parser {

    private final String filename;
    private final List<FileContent> contentList;
    private String lastArticleID;
    private final MapClass mp;
    int defaultId = 1;
    private int lastArticle;

    public Parser(List<FileContent> contentList, String filename, MapClass mp, int lastArticle) {
        this.filename = filename;
        this.contentList = contentList;
        this.mp = mp;
        this.lastArticle = lastArticle;
    }

    public int parseFile(String input) {

        int articleSIndex = 0, articleEIndex = 0, commentSIndex = 0, commentEIndex = 0;
        articleSIndex = input.indexOf("<article>", articleSIndex);
        articleEIndex = input.indexOf("</article>", articleEIndex);
        commentSIndex = input.indexOf("<comment>", commentSIndex);
        commentEIndex = input.indexOf("</comment>", commentEIndex);
        boolean article;
        do {

            article = false;
            if ((articleSIndex < commentSIndex && articleSIndex != -1) || commentSIndex == -1) {
                article = true;
            }

            if (articleSIndex != -1 && articleEIndex != -1 && article) { //tenho um article é o unico posso assumir que vêm completo
                something(input.substring(articleSIndex + 9, articleEIndex), false);
                articleEIndex += 10;//passo o article a frente e processo o resto da String
                articleSIndex += 9;
                articleSIndex = input.indexOf("<article>", articleSIndex);
                articleEIndex = input.indexOf("</article>", articleEIndex);
            }
            article = false;
            if ((articleSIndex < commentSIndex && articleSIndex != -1) || commentSIndex == -1) {
                article = true;
            }
            if (commentSIndex != -1 && commentEIndex != -1 && !article) {//tenho um comment
                //tenho pelo menon um comment completo
                something(input.substring(commentSIndex + 9, commentEIndex), true);
                commentEIndex += 10;//passo o comment a frente e processo o resto da String                
                commentSIndex += 9;
                commentSIndex = input.indexOf("<comment>", commentSIndex);
                commentEIndex = input.indexOf("</comment>", commentEIndex);
            }
            //System.out.println(" aassa");
        } while (commentEIndex != -1 || articleEIndex != -1);
        return lastArticle;
    }

    private void something(String input, boolean comment) {
        //recebe SEMPRE uma estrutura completa
        int idStartIndex, articleEIndex;
        idStartIndex = input.indexOf("<pid>");
        if (idStartIndex == -1) {
            //impossivel logo não é aid
            idStartIndex = input.indexOf("<cid>");
            if (idStartIndex == -1) {
                //impossivel logo não é cid nem aid só pode ser
                idStartIndex = input.indexOf("<id>");
                articleEIndex = input.indexOf("</id>");
                getInfoHTML(input, comment);
            } else {
                articleEIndex = input.indexOf("</cid>");
                getInfoXMLcomment(input);
            }
        } else {
            articleEIndex = input.indexOf("</pid>");
            getInfoXMLarticle(input);
        }
    }

    private void getInfoHTML(String input, boolean comment) {        
        String id = "";
        if (!input.contains("<id>")) {
            if (comment){
                id = ""+ defaultId;
                defaultId++;
            }else{                
                lastArticle++;
                id = ""+lastArticle;
            }
        } else {
            id = input.substring(input.indexOf("<id>") + 4, input.indexOf("</id>"));
        }
        String title = "";
        if (input.contains("<title>")) {
            title = input.substring(input.indexOf("<title>") + 7, input.indexOf("</title>"));
        }
        String author = input.substring(input.indexOf("<author>") + 8, input.indexOf("</author>"));
        Date date = null;
        if (input.contains("<datestamp>")) {
            date = new Date(Long.valueOf(input.substring(input.indexOf("<datestamp>") + 11, input.indexOf("</datestamp>") - 3)));
        }
        String text = "";
        if (input.contains("<htmltext>")) {
            text = input.substring(input.indexOf("<htmltext>") + 10, input.indexOf("</htmltext>"));
        }
        text = Jsoup.parse(text).text();

        //criar o id que os profs querem primeiro passo
        String[] aux = filename.split("/");
        if (aux.length > 1) { // means it's a windows path            
        } else { //means is a linux path
            aux = filename.split("\\\\");
        }
        String folder = aux[aux.length - 2];
        String name = aux[aux.length - 1].split("\\.")[0];

        id = id.substring(id.indexOf("#") + 1);
        int size;
        if (!comment) {
            lastArticleID = id;
            size = mp.addInfoOnDoc(folder + "_" + name + "_" + id);
        } else {//is a comment
            size = mp.addInfoOnDoc(folder + "_" + name + "_" + lastArticleID + "_" + id);
        }

        //System.out.println("\n"+id+"\n"+title+"\n"+author+"\n"+date+"\n"+text+"\n");
        FileContent tmp = new FileContent(size);
        tmp.setDate(date);
        tmp.setAuthor(author);
        tmp.setTitle(title);
        tmp.setText(text);
        contentList.add(tmp);

    }

    private void getInfoXMLcomment(String input) {
        String id = input.substring(input.indexOf("<cid>") + 5, input.indexOf("</cid>"));
        String author = input.substring(input.indexOf("<author>") + 8, input.indexOf("</author>"));
        Date date = null;
        String str = null;
        try {
            str = input.substring(input.indexOf("<date>") + 6, input.indexOf("</date>"));
            str = str.replace("at", "");
            str = str.replace("PDT", "");
            str = str.replace("PST", "");
            str = str.replace("S ", "");
            str = str.replace("Surday", "Sunday");
            str = str.replaceAll("[^a-zA-Z\\/\\d\\s:-]", "");
            str = str.trim().replaceAll(" +", " ");
            date = new SimpleDateFormat("MMM dd yyyy hh:mm aa", Locale.ENGLISH).parse(str);
        } catch (ParseException ex) {
            try {
                date = new SimpleDateFormat("EEE MMM dd yyyy hh:mm aa", Locale.ENGLISH).parse(str);
            } catch (ParseException ex1) {
                try {
                    date = new SimpleDateFormat("MMM dd yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                } catch (ParseException ex2) {
                    try {
                        date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                    } catch (ParseException ex3) {
                        try {
                            date = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                        } catch (ParseException ex4) {
                            try {
                                date = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                            } catch (ParseException ex5) {
                                Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex5);
                            }
                        }
                    }
                }
            }
        }
        String text = input.substring(input.indexOf("<text>") + 6, input.indexOf("</text>"));
        text = Jsoup.parse(text).text();

        if ("".equals(id)) {
            id = "" + defaultId;
            defaultId++;
        }
        String[] aux = filename.split("/");
        if (aux.length > 1) { // means it's a windows path            
        } else { //means is a linux path
            aux = filename.split("\\\\");
        }
        String folder = aux[aux.length - 2];
        String name = aux[aux.length - 1].split("\\.")[0];
        int size;
        size = mp.addInfoOnDoc(folder + "_" + name + "_" + lastArticleID + "_" + id);

        //System.out.println("\n"+id+"\n"+author+"\n"+date+"\n"+text+"\n");        
        FileContent tmp = new FileContent(size);
        tmp.setDate(date);
        tmp.setAuthor(author);
        tmp.setTitle(null);
        tmp.setText(text);
        contentList.add(tmp);
    }

    private void getInfoXMLarticle(String input) {
        String id = input.substring(input.indexOf("<pid>") + 5, input.indexOf("</pid>"));
        String title = input.substring(input.indexOf("<title>") + 7, input.indexOf("</title>"));
        String author = input.substring(input.indexOf("<author>") + 8, input.indexOf("</author>"));
        Date date = null;
        String str = null;
        try {
            str = input.substring(input.indexOf("<date>") + 6, input.indexOf("</date>"));
            str = str.replace("at", "");
            str = str.replace("PDT", "");
            str = str.replace("PST", "");
            str = str.replace("S ", "");
            str = str.replace("Surday", "Sunday");
            str = str.replaceAll("[^a-zA-Z\\/\\d\\s:-]", "");
            str = str.trim().replaceAll(" +", " ");
            date = new SimpleDateFormat("MMM dd yyyy hh:mm aa", Locale.ENGLISH).parse(str);
        } catch (ParseException ex) {
            try {
                date = new SimpleDateFormat("EEE MMM dd yyyy hh:mm aa", Locale.ENGLISH).parse(str);
            } catch (ParseException ex1) {
                try {
                    date = new SimpleDateFormat("MMM dd yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                } catch (ParseException ex2) {
                    try {
                        date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                    } catch (ParseException ex3) {
                        try {
                            date = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                        } catch (ParseException ex4) {
                            try {
                                date = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss aa", Locale.ENGLISH).parse(str);
                            } catch (ParseException ex5) {
                                Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex5);
                            }
                        }
                    }
                }
            }
        }
        String text = input.substring(input.indexOf("<text>") + 6, input.indexOf("</text>"));
        text = Jsoup.parse(text).text();

        if ("".equals(id)) {
            lastArticle++;
            id = "" + lastArticle;            
        }
        lastArticleID = id;

        String[] aux = filename.split("/");
        if (aux.length > 1) { // means it's a windows path            
        } else { //means is a linux path
            aux = filename.split("\\\\");
        }
        String folder = aux[aux.length - 2];
        String name = aux[aux.length - 1].split("\\.")[0];
        int size;
        size = mp.addInfoOnDoc(folder + "_" + name + "_" + id);

        //System.out.println("\n"+id+"\n"+title+"\n"+author+"\n"+date+"\n"+text+"\n");        
        FileContent tmp = new FileContent(size);
        tmp.setDate(date);
        tmp.setAuthor(author);
        tmp.setTitle(title);
        tmp.setText(text);
        contentList.add(tmp);
    }

}

class MapClass {

    private final Map<Integer, String> listDocuments = new HashMap();

    public synchronized int addInfoOnDoc(String name) {
        int tmp = listDocuments.size();
        listDocuments.put(tmp, name);
        return tmp;
    }

    public synchronized Map<Integer, String> returnHash() {
        return listDocuments;
    }

}

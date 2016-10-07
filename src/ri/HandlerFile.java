/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ri;

import java.util.List;

/**
 *
 * @author Alexandre Brigeiro mec: 42201,
 * @author Bruno Andrade mec: 59506
 */
public class HandlerFile extends Thread {

    private final CorpusReader rf;
    private final InverterIndex t;

    HandlerFile(InverterIndex t, CorpusReader rf) {
        this.t = t;
        this.rf = rf;
    }

    /**
     *
     */
    @Override
    public void run() {
        int fileNumber = 0;
        //while there are files
        while (fileNumber != -1) {
            //ask to CorpusReader the id of a new fike
            fileNumber = rf.askNewFile();
            //if exits yet a file to read
            if (fileNumber != -1) {
                // asks   CorpusReader the content of the file
                List<FileContent> readFile;
                readFile = rf.readNewFile(fileNumber);
                for (FileContent readFile1 : readFile) {
                    String text = readFile1.getText();
                    if (!"".equals(text) || !" ".equals(text) || text != null) {
                        //iniciate the HashMap
                        //apply Tokenizer
                        String[] s = t.tokenize(readFile1.getText());
                        t.addAuthorTitleDate(readFile1.getInternalID(), readFile1.getAuthor(), readFile1.getTitle(), readFile1.getDate());

                        //index the document
                        t.indexDocument(s, readFile1.getInternalID());

                        //print to file the information of indexation of the documents
                        // t.printIndexDocument(readFile1.getInternalID(), docIndex);
                        //docIndex.clear();
                    }
                }
            }
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ri;

import java.text.Normalizer;

/**
 *
 * @author Bruno
 */
public class Tokenizer {

    /**
     * transform a string in a array of string split by white spaces
     *
     * @param readFile, string to split
     * @return array of the "words" splited by white spaces
     */
    public synchronized String[] whiteSpace(String readFile) {
        readFile = readFile.trim();
        String[] s = readFile.split("\\s+");
        return s;
    }

    /**
     * Transform the upper letters to lower
     *
     * @param s, string to transform
     * @return the string s with only lower letters
     */
    public synchronized String lowerCase(String s) {
        return s.toLowerCase();
    }

    /**
     * Remove every char that is not a letter or a number
     *
     * @param s
     * @return
     */
    public synchronized String removeChar(String s) {
        s = s.replaceAll("[^a-zA-Z0-9]+", " ");
        //example: 92.3 → 92 3 but search finds documents with 92 and 3 adjacent
        s = s.replace(".", " ");
        return s;
    }

    /**
     * remove the accents of the letters
     *
     * @param s
     * @return
     */
    public synchronized String normalizer(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        return s;
    }

    /**
     *
     * @param readFile
     * @return
     */
    public synchronized String[] tokenize(String readFile) {
        readFile = lowerCase(readFile);
        readFile = removeChar(readFile);
        readFile = normalizer(readFile);
        //readFile = smallWords(readFile);
        return whiteSpace(readFile);
    }

    /**
     *
     * @param readFile
     * @return
     */
    public synchronized String smallWords(String readFile) {

        return readFile.replaceAll("\\b\\w{1,3}\\b\\s?", "");

    }
}

package app;

import java.util.ArrayList;
import java.util.Stack;

public class QueryString {
    static void parseQueryString(Stack<String> operands, Stack<String> operators, String query) {

        String[] splitQuery = query.split("\\s+");

        int i = splitQuery.length - 1;
        while (i >= 0) {
            String word = splitQuery[i];

            if (ExceptionSet.exceptions.contains(word)) {

                operands.push(word);
                --i;

                if (i >= 0) {
                    operators.push(splitQuery[i--]);
                }
            }

            else if (StopWordSet.stopwords.contains(word)) {

                i -= 2;
            } else {

                Stemmer stemmer = new Stemmer();
                stemmer.add(word.toCharArray(), word.length());
                stemmer.stem();
                word = stemmer.toString();

                operands.push(word);
                --i;

                if (i >= 0) {
                    operators.push(splitQuery[i--]);
                }
            }
        }
    }

    public static ArrayList<String> parseQueryString(String query) {
        String[] splitQuery = query.split("\\s+");
        ArrayList<String> queryWords = new ArrayList<>();

        int i = 0;
        while (i <= splitQuery.length - 1) {
            String word = splitQuery[i].toLowerCase();

            if (ExceptionSet.exceptions.contains(word)) {
                queryWords.add(word);
                ++i;
            }

            else if (StopWordSet.stopwords.contains(word)) {
                ++i;
            } else {
                {
                    Stemmer stemmer = new Stemmer();
                    stemmer.add(word.toCharArray(), word.length());
                    stemmer.stem();
                    word = stemmer.toString();
                    queryWords.add(word);
                    ++i;
                }
            }
        }

        return queryWords;
    }
}
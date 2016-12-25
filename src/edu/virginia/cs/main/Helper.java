/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.utility.SortMap;
import edu.virginia.cs.utility.TextTokenizer;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wua4nw
 */
public class Helper {

    private static final TextTokenizer TOKENIZER = new TextTokenizer(true, true);

    /**
     *
     * @param previousDate
     * @param currentDate
     * @param lastSubmittedQuery
     * @param currentQuery
     * @return
     */
    public static boolean checkSameSession(Date previousDate, Date currentDate, String lastSubmittedQuery, String currentQuery) {
        /**
         * Measuring the time difference between current query and last
         * submitted query in minutes.
         */
        long diffMinutes = -1;
        try {
            //in milliseconds
            long diff = currentDate.getTime() - previousDate.getTime();
            diffMinutes = diff / (60 * 1000);
        } catch (Exception ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
        /**
         * Measuring the similarity between current query and last submitted
         * query in terms of similar tokens they have.
         */

        HashSet<String> currentQuTokens = new HashSet<>(TOKENIZER.TokenizeText(currentQuery));
        HashSet<String> prevQuTokens = new HashSet<>(TOKENIZER.TokenizeText(lastSubmittedQuery));
        boolean isSimilar = false;
        double count = Math.ceil(currentQuTokens.size() / 2.0);
        HashSet<String> intersection = new HashSet<>(currentQuTokens);
        intersection.retainAll(prevQuTokens);
        if (intersection.size() >= count) {
            isSimilar = true;
        }
        /**
         * If time difference is less than 60 minutes between current query and
         * previous query or if both queries have 50% similarity in terms of
         * having similar tokens, they belong to the same session .
         */
        return diffMinutes < 60 || isSimilar;
    }

    private static HashMap<String, Integer> computeTermFreq(List<String> tokens) {
        HashMap<String, Integer> termFreqMap = new HashMap<>();
        // computing term frequency of all the unique terms found in the document
        for (String tok : tokens) {
            if (termFreqMap.containsKey(tok)) {
                termFreqMap.put(tok, termFreqMap.get(tok) + 1);
            } else {
                termFreqMap.put(tok, 1);
            }
        }
        return termFreqMap;
    }

    private static ArrayList<ResultDoc> reRank(HashMap<String, Float> docScoreMap, ArrayList<ResultDoc> relDocs) {
        /**
         * Client side re-ranking using true user profile.
         */
        Map<String, Float> resultedMap = SortMap.sortMapByValue(docScoreMap, false);

        /**
         * Re-rank the documents by giving weight to the search engine rank and
         * the client side rank.
         */
        int i = 0;
        for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
            float score;
            // Giving 50% weight to both search engine and client side rank.
            score = 1.0f * (1.0f / (i + 1)) + 0.0f * (1.0f / (Integer.parseInt(entry.getKey() + 1)));
            docScoreMap.put(entry.getKey(), score);
            i++;
        }

        // sort the documents in descending order according to the new score assigned
        Map<String, Float> result = SortMap.sortMapByValue(docScoreMap, false);
        ArrayList<ResultDoc> retValue = new ArrayList<>();
        for (Map.Entry<String, Float> entry : result.entrySet()) {
            retValue.add(relDocs.get(Integer.parseInt(entry.getKey())));
        }

        return retValue;
    }

    /**
     * Method that re-ranks the result in the client side.
     *
     * @param history
     * @param relDocs all the relevant documents returned by the search engine
     * @return re-ranked resulting documents
     */
    public static ArrayList<ResultDoc> reRankResults(HashMap<String, Integer> history, ArrayList<ResultDoc> relDocs) {
        HashMap<String, Float> docScoreMap = new HashMap<>();
        HashMap<String, Integer> uniqueDocTerms;

        for (int i = 0; i < relDocs.size(); i++) {
            List<String> tokens = TOKENIZER.TokenizeText(relDocs.get(i).getContent());
            uniqueDocTerms = computeTermFreq(tokens);

            float docScore = 0;
            // smoothing parameter for linear interpolation
            float lambda = 0.1f;
            int count_for_normalization = 0;

            for (Map.Entry<String, Integer> entry : history.entrySet()) {
                if (!uniqueDocTerms.containsKey(entry.getKey())) {
                    continue;
                }
                // maximum likelihood calculation
                Float tokenProb = 0.0f;
                if (uniqueDocTerms.containsKey(entry.getKey())) {
                    tokenProb = (uniqueDocTerms.get(entry.getKey()) * 1.0f) / tokens.size();
                }
                // probability from reference model for smoothing purpose
                Double refProb = StaticData.SmoothingReference.get(entry.getKey());
                if (refProb == null) {
                    refProb = 0.0;
                }

                // smoothing token probability using linear interpolation
                Double smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                smoothedTokenProb = smoothedTokenProb / (lambda * refProb);
                docScore = docScore + (entry.getValue() * (float) Math.log(smoothedTokenProb));
                count_for_normalization += entry.getValue();
            }

            if (count_for_normalization != 0) {
                docScore = docScore / count_for_normalization;
            }
            docScoreMap.put(String.valueOf(i), docScore);
        }

        // return re-ranked documents
        return reRank(docScoreMap, relDocs);
    }

    /**
     * Method that generate the id of all users for evaluation.
     *
     * @param folder folder path where all user search log resides
     * @param count
     * @return list of all user id
     */
    public static ArrayList<String> getAllUserId(String folder, int count) {
        ArrayList<String> allUserIds = new ArrayList<>();
        File dir = new File(folder);
        int userCount = 0;
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                String fileName = f.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                allUserIds.add(fileName);
                userCount++;
            }
            if (userCount == count) {
                break;
            }
        }
        return allUserIds;
    }

}

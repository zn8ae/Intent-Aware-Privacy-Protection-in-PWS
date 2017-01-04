/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.extra;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.utility.SortMap;
import edu.virginia.cs.utility.TextTokenizer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

/**
 *
 * @author wua4nw
 */
public class Personalization {

    private static final TextTokenizer TOKENIZER = new TextTokenizer(true, true);

    public static ScoreDoc[] personalizeResults(IndexSearcher indexSearcher, String field, HashMap<String, Integer> history, ScoreDoc[] relDocs) {
        ScoreDoc[] reRankedDocs = new ScoreDoc[relDocs.length];
        try {
            /* Store return document with their personalized score */
            HashMap<String, Float> mapDocToScore = new HashMap<>();
            /* Unique terms found in the returned document */
            HashMap<String, Integer> uniqueDocTerms;

            for (int i = 0; i < relDocs.length; i++) {
                Document doc = indexSearcher.doc(relDocs[i].doc);
                /**
                 * Extract the unique tokens from a relevant document returned
                 * by the lucene index searcher.
                 */
                uniqueDocTerms = new HashMap<>();
                List<String> tokens = TOKENIZER.TokenizeText(doc.getField(field).stringValue());
                // computing term frequency of all the unique terms found in the document
                for (String tok : tokens) {
                    if (uniqueDocTerms.containsKey(tok)) {
                        uniqueDocTerms.put(tok, uniqueDocTerms.get(tok) + 1);
                    } else {
                        uniqueDocTerms.put(tok, 1);
                    }
                }

                /* Score after personalizing result */
                float score = 0;
                /* Smoothing paramter for linear interpolation */
                float lambda = 0.1f;

                /**
                 * Computing score for a returned document based on user profile
                 * maintained by the server side.
                 */
                int count_for_normalization = 0;
                for (Map.Entry<String, Integer> entry : history.entrySet()) {
                    if (!uniqueDocTerms.containsKey(entry.getKey())) {
                        continue;
                    }
                    count_for_normalization += entry.getValue();
                    Double tokenProb = (uniqueDocTerms.get(entry.getKey()) * 1.0) / tokens.size();
                    Double refProb = StaticData.SmoothingReference.get(entry.getKey());
                    if (refProb == null) {
                        refProb = 0.0;
                    }
                    /* Smoothing using linear interpolation */
                    Double smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                    smoothedTokenProb = smoothedTokenProb / (lambda * refProb);
                    score = score + (entry.getValue() * (float) Math.log(smoothedTokenProb));
                }
                if (count_for_normalization != 0) {
                    score = score / count_for_normalization;
                }
                mapDocToScore.put(String.valueOf(i), score);

            }

            /**
             * Re-ranking for personalization using server side user profile.
             */
            Map<String, Float> tempMap = SortMap.sortMapByValue(mapDocToScore, false);

            /**
             * Computing score for documents through a ranking aggregation
             * method called Borda's method.
             */
            int i = 0;
            for (Map.Entry<String, Float> entry : tempMap.entrySet()) {
                float score = 0;
                // Giving 50% weight to personalization and 50% to OkapiBM25.
                score = 0.25f * (1.0f / (i + 1)) + 0.75f * (1.0f / (Integer.parseInt(entry.getKey() + 1)));
                mapDocToScore.put(entry.getKey(), score);
                /**
                 * Storing the final score of documents computed through Borda's
                 * method.
                 */
                relDocs[Integer.parseInt(entry.getKey())].score = score;
                i++;
            }

            /**
             * Final re-ranking through ranking aggregation.
             */
            Map<String, Float> resultedMap = SortMap.sortMapByValue(mapDocToScore, false);
            i = 0;
            for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
                reRankedDocs[i] = relDocs[Integer.parseInt(entry.getKey())];
                i++;
            }
        } catch (IOException ex) {
            Logger.getLogger(Personalization.class.getName()).log(Level.SEVERE, null, ex);
        }

        return reRankedDocs;
    }

}

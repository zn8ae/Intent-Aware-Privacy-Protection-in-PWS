/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.utility.SortMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wua4nw
 */
public class Topic {

    private final int BUCKET_SIZE = 10;
    private final double LAMBDA = 0.9;

    private HashMap<String, Integer> unigramLM;
    private HashMap<String, Integer> bigramLM;
    private HashMap<String, Integer> trigramLM;
    private HashMap<String, Integer> fourgramLM;
    private final Double[] probArrayUnigram;
    private final Double[] probArrayBigram;
    private final Double[] probArrayTrigram;
    private final Double[] probArrayFourgram;

    private int totalUnigrams;
    private int totalUniqueUnigrams;

    private double maxProbUnigram;
    private double minProbUnigram;
    private double maxProbBigram;
    private double minProbBigram;
    private double maxProbTrigram;
    private double minProbTrigram;
    private double maxProbFourgram;
    private double minProbFourgram;

    public Topic(HashMap<String, Float> refModel) {
        this.unigramLM = new HashMap<>();
        this.bigramLM = new HashMap<>();
        this.trigramLM = new HashMap<>();
        this.fourgramLM = new HashMap<>();
        this.probArrayUnigram = new Double[BUCKET_SIZE];
        this.probArrayBigram = new Double[BUCKET_SIZE];
        this.probArrayTrigram = new Double[BUCKET_SIZE];
        this.probArrayFourgram = new Double[BUCKET_SIZE];

    }

    public HashMap<String, Integer> getUnigramLM() {
        return unigramLM;
    }

    public void setUnigramLM(HashMap<String, Integer> unigramLM) {
        this.unigramLM = unigramLM;
    }

    public HashMap<String, Integer> getBigramLM() {
        return bigramLM;
    }

    public void setBigramLM(HashMap<String, Integer> bigramLM) {
        this.bigramLM = bigramLM;
    }

    public HashMap<String, Integer> getTrigramLM() {
        return trigramLM;
    }

    public void setTrigramLM(HashMap<String, Integer> trigramLM) {
        this.trigramLM = trigramLM;
    }

    public HashMap<String, Integer> getFourgramLM() {
        return fourgramLM;
    }

    public void setFourgramLM(HashMap<String, Integer> fourgramLM) {
        this.fourgramLM = fourgramLM;
    }

    public int getTotalUnigrams() {
        return totalUnigrams;
    }

    public void setTotalUnigrams(int totalUnigrams) {
        this.totalUnigrams = totalUnigrams;
    }

    public int getTotalUniqueUnigrams() {
        return totalUniqueUnigrams;
    }

    public void setTotalUniqueUnigrams(int totalUniqueUnigrams) {
        this.totalUniqueUnigrams = totalUniqueUnigrams;
    }

    public double getMaxProbUnigram() {
        return maxProbUnigram;
    }

    public void setMaxProbUnigram(double maxProbUnigram) {
        this.maxProbUnigram = maxProbUnigram;
    }

    public double getMinProbUnigram() {
        return minProbUnigram;
    }

    public void setMinProbUnigram(double minProbUnigram) {
        this.minProbUnigram = minProbUnigram;
    }

    public double getMaxProbBigram() {
        return maxProbBigram;
    }

    public void setMaxProbBigram(double maxProbBigram) {
        this.maxProbBigram = maxProbBigram;
    }

    public double getMinProbBigram() {
        return minProbBigram;
    }

    public void setMinProbBigram(double minProbBigram) {
        this.minProbBigram = minProbBigram;
    }

    public double getMaxProbTrigram() {
        return maxProbTrigram;
    }

    public void setMaxProbTrigram(double maxProbTrigram) {
        this.maxProbTrigram = maxProbTrigram;
    }

    public double getMinProbTrigram() {
        return minProbTrigram;
    }

    public void setMinProbTrigram(double minProbTrigram) {
        this.minProbTrigram = minProbTrigram;
    }

    public double getMaxProbFourgram() {
        return maxProbFourgram;
    }

    public void setMaxProbFourgram(double maxProbFourgram) {
        this.maxProbFourgram = maxProbFourgram;
    }

    public double getMinProbFourgram() {
        return minProbFourgram;
    }

    public void setMinProbFourgram(double minProbFourgram) {
        this.minProbFourgram = minProbFourgram;
    }

    /**
     * Checks whether a topic is empty or not.
     *
     * @return
     */
    public boolean isEmpty() {
        return unigramLM.isEmpty();
    }

    /**
     * Computes probability from reference model for smoothing purpose.
     *
     * @param unigram
     * @return
     */
    public double getReferenceProbability(String unigram) {
        if (StaticData.SmoothingReference.containsKey(unigram)) {
            return StaticData.SmoothingReference.get(unigram);
        }
        return 0.00000001;
    }

    /**
     * Computes joint probability or conditional probability of a unigram.
     *
     * @param unigram
     * @return
     */
    public double getProbabilityUnigram(String unigram) {
        double prob;
        /* Computing probability of a unigram using linear interpolation smoothing */
        if (unigramLM.containsKey(unigram)) {
            prob = (1.0 - LAMBDA) * (unigramLM.get(unigram) / totalUnigrams) + LAMBDA * getReferenceProbability(unigram);
        } else {
            prob = LAMBDA * getReferenceProbability(unigram);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a bigram.
     *
     * @param bigram
     * @param isJoint
     * @return
     */
    public double getProbabilityBigram(String bigram, boolean isJoint) {
        double prob;
        /* Computing probability of a bigram using linear interpolation smoothing */
        String[] split = bigram.split(" ");
        if (bigramLM.containsKey(bigram)) {
            prob = (1.0 - LAMBDA) * (bigramLM.get(bigram) / unigramLM.get(split[0]));
        } else {
            prob = 0.0;
        }
        prob += LAMBDA * getProbabilityUnigram(split[0]);
        if (isJoint) {
            prob *= getProbabilityUnigram(split[0]);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a trigram.
     *
     * @param trigram
     * @param isJoint
     * @return
     */
    public double getProbabilityTrigram(String trigram, boolean isJoint) {
        double prob;
        /* Computing probability of a trigram using linear interpolation smoothing */
        String[] split = trigram.split(" ");
        String prevBigram = split[0] + " " + split[1];
        if (trigramLM.containsKey(trigram)) {
            prob = (1.0 - LAMBDA) * (trigramLM.get(trigram) / bigramLM.get(prevBigram));
        } else {
            prob = 0.0;
        }
        String bigram = split[1] + " " + split[2];
        prob += LAMBDA * getProbabilityBigram(bigram, false);
        if (isJoint) {
            prob *= getProbabilityBigram(prevBigram, false);
            prob *= getProbabilityUnigram(split[0]);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a fourgram.
     *
     * @param fourgram
     * @param isJoint
     * @return if isJoint is true, returns joint probability, otherwise
     * conditional probability
     */
    public double getProbabilityFourgram(String fourgram, boolean isJoint) {
        double prob;
        /* Computing probability of a fourgram using linear interpolation smoothing */
        String[] split = fourgram.split(" ");
        String prevTrigram = split[0] + " " + split[1] + " " + split[2];
        if (fourgramLM.containsKey(fourgram)) {
            prob = (1.0 - LAMBDA) * (fourgramLM.get(fourgram) / trigramLM.get(prevTrigram));
        } else {
            prob = 0.0;
        }
        String trigram = split[1] + " " + split[2] + " " + split[3];
        prob += LAMBDA * getProbabilityTrigram(trigram, false);
        if (isJoint) {
            prob *= getProbabilityBigram(prevTrigram, false);
            String bigram = split[0] + " " + split[1];
            prob *= getProbabilityBigram(bigram, false);
            prob *= getProbabilityUnigram(split[0]);
        }
        return prob;
    }

    /**
     * Computes joint probability of a n-gram where n>4. Suppose n=6, then joint
     * probability formula is, P(w6 w5 w4 w3 w2 w1) = P(w6 | w5 w4 w3) * P(w5 |
     * w4 w3 w2) * P(w4 | w3 w2 w1) * P(w3 | w2 w1) * P(w2 | w1) * P(w1)
     *
     * @param ngram
     * @param n
     * @return
     */
    public double getProbabilityNgram(String ngram, int n) {
        double prob = 1.0;
        /* Computing probability of a n-gram using linear interpolation smoothing */
        String[] split = ngram.split(" ");
        for (int i = split.length - 1; i >= 0; i--) {
            if (i >= 3) {
                String fourgram = split[i - 3] + " " + split[i - 2] + " " + split[i - 1] + " " + split[i];
                prob *= getProbabilityFourgram(fourgram, false);
            } else if (i == 2) {
                String trigram = split[i - 2] + " " + split[i - 1] + " " + split[i];
                prob *= getProbabilityBigram(trigram, false);
            } else if (i == 1) {
                String bigram = split[i - 1] + " " + split[i];
                prob *= getProbabilityBigram(bigram, false);
            } else {
                prob *= getProbabilityUnigram(split[0]);
            }
        }
        return prob;
    }

    /**
     * Set the Maximum and Minimum probability of the language model.
     *
     * @param param either unigram or bigram or trigram or fourgram
     */
    public void setMaxMinProb(String param) {
        double max = -1.0;
        double min = -1.0;
        HashMap<String, Double> tempMap = new HashMap<>();
        if (param.equals("unigram")) {
            if (unigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : unigramLM.entrySet()) {
                double prob = getProbabilityUnigram(entry.getKey());
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbUnigram = max;
            minProbUnigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayUnigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayUnigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("bigram")) {
            if (bigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : bigramLM.entrySet()) {
                double prob = getProbabilityBigram(entry.getKey(), true);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbBigram = max;
            minProbBigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayBigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayBigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("trigram")) {
            if (trigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : trigramLM.entrySet()) {
                double prob = getProbabilityTrigram(entry.getKey(), true);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbTrigram = max;
            minProbTrigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayTrigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayTrigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("fourgram")) {
            if (fourgramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : fourgramLM.entrySet()) {
                double prob = getProbabilityFourgram(entry.getKey(), true);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbFourgram = max;
            minProbFourgram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayFourgram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayFourgram[i - 1] = list.get(index).getValue();
                }
            }
        } else {
            System.err.println("Unknown Parameter...!");
        }
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.user;

import edu.cs.virginia.config.StaticData;
import edu.virginia.cs.interfaces.Tree;
import edu.virginia.cs.interfaces.TreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wua4nw
 */
public class Profile implements Tree {

    private final int userId;
    private ArrayList<TreeNode> intents;
    private int totalTokenCount;

    public Profile(int id) {
        this.userId = id;
        this.intents = new ArrayList<>();
    }

    @Override
    public List<TreeNode> getListOfNodes() {
        return intents;
    }

    @Override
    public void addNode(TreeNode node) {
        intents.add(node);
    }

    @Override
    public void setNodes(List<TreeNode> nodes) {
        intents = new ArrayList<>(nodes);
    }

    public int getUserId() {
        return userId;
    }

    public int getTotalTokenCount() {
        return totalTokenCount;
    }

    public HashMap<String, Integer> getCompleteHistory() {
        HashMap<String, Integer> completeHistory = new HashMap<>();
        totalTokenCount = 0;
        for (TreeNode node : intents) {
            for (Map.Entry<String, Integer> entry : ((Intent) node).getHistory().getSelectedQueryTerms().entrySet()) {
                if (completeHistory.containsKey(entry.getKey())) {
                    completeHistory.put(entry.getKey(), completeHistory.get(entry.getKey()) + entry.getValue());
                } else {
                    completeHistory.put(entry.getKey(), entry.getValue());
                }
                totalTokenCount += entry.getValue();
            }
            for (Map.Entry<String, Integer> entry : ((Intent) node).getHistory().getSelectedDocTerms().entrySet()) {
                if (completeHistory.containsKey(entry.getKey())) {
                    completeHistory.put(entry.getKey(), completeHistory.get(entry.getKey()) + entry.getValue());
                } else {
                    completeHistory.put(entry.getKey(), entry.getValue());
                }
                totalTokenCount += entry.getValue();
            }
        }
        return completeHistory;
    }

    /**
     * Computes KL-Divergence between two different user profiles.
     *
     * @param comparedTo
     * @return KL-Divergence value
     */
    public float calculateKLDivergence(Profile comparedTo) {
        float klDiv = 0;
        float lambda = 0.1f;
        HashMap<String, Integer> thisProfile = getCompleteHistory();
        HashMap<String, Integer> profileCompared = comparedTo.getCompleteHistory();

        HashSet<String> keySet = new HashSet<>();
        keySet.addAll(thisProfile.keySet());
        keySet.addAll(profileCompared.keySet());

        for (String name : keySet) {
            Integer value = profileCompared.get(name);
            if (value == null) {
                value = 0;
            }
            Double tokenProb = (value * 1.0) / comparedTo.getTotalTokenCount();
            Double refProb = StaticData.SmoothingReference.get(name);
            if (refProb == null) {
                refProb = 0.0;
            }
            Double smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
            Double p1 = smoothedTokenProb;

            Integer value2 = thisProfile.get(name);
            if (value2 == null) {
                value2 = 0;
            }
            Double tokenProb2 = (value2 * 1.0) / getTotalTokenCount();
            Double refProb2 = refProb;

            Double smoothedTokenProb2 = (1 - lambda) * tokenProb2 + lambda * refProb2;
            Double p2 = smoothedTokenProb2;
            if (p1 == 0) {
                continue;
            }
            if (p2 == 0) {
                continue;
            }
            klDiv = (float) (klDiv + p1 * Math.log(p1 / p2));
        }
        return klDiv;
    }

}

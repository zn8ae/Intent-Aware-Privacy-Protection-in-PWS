/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.user;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.interfaces.Tree;
import edu.virginia.cs.interfaces.TreeNode;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.object.UserQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author wua4nw
 */
public class Profile implements Tree {

    private final String userId;
    private HashMap<String, TreeNode> intents;
    private final ArrayList<UserQuery> submittedQueries;
    private int totalTokenCount;
    private final TreeNode root;

    public Profile(String id) {
        this.submittedQueries = new ArrayList<>();
        this.userId = id;
        this.intents = new HashMap<>();
        this.root = new Intent("Top");
        this.root.setNodeLevel(0);
        this.root.setParent(null);
    }

    public boolean isEmpty() {
        return submittedQueries.isEmpty();
    }

    public ArrayList<UserQuery> getSubmittedQueries() {
        return submittedQueries;
    }

    public UserQuery getQuery(int index) {
        if (index < submittedQueries.size()) {
            return submittedQueries.get(index);
        } else {
            return null;
        }
    }

    public void addQuery(UserQuery query) {
        this.submittedQueries.add(query);
        query.getQuery_intent().updateUsingSubmittedQuery(query.getQuery_text());
        for (ResultDoc doc : query.getRelevant_documents()) {
            if (doc.isClicked()) {
                query.getQuery_intent().updateUsingClickedDoc(doc.getContent());
            }
        }
    }

    public int checkRepeatInCurrentSession(UserQuery query) {
        int query_index = -1;
        for (int i = submittedQueries.size() - 1; i >= 0; i--) {
            if (submittedQueries.get(i).getQuery_session().getSession_id() == query.getQuery_session().getSession_id()) {
                if (submittedQueries.get(i).getQuery_text().equals(query.getQuery_text())) {
                    query_index = i;
                    break;
                }
            } else {
                break;
            }
        }
        return query_index;
    }

    public void addBranch(String path) {
        if (!branchExists(path)) {
            String[] nodes = path.split("/");
            String temp = nodes[0];
            TreeNode parent = root;
            for (int i = 1; i < nodes.length; i++) {
                temp += "/" + nodes[i];
                if (!branchExists(temp)) {
                    TreeNode node = new Intent(temp);
                    node.setParent(parent);
                    node.setNodeLevel(parent.getNodeLevel() + 1);
                    parent.addChildrens(node);
                    parent = node;
                    intents.put(temp, node);
                }
            }
        }
    }

    public boolean branchExists(String path) {
        return intents.containsKey(path);
    }

    @Override
    public HashMap<String, TreeNode> getNodeMap() {
        return this.intents;
    }

    @Override
    public void addNode(String nodePath, TreeNode node) {
        this.intents.put(nodePath, node);
    }

    @Override
    public void setNodes(HashMap<String, TreeNode> nodeMap) {
        this.intents = nodeMap;
    }

    public String getUserId() {
        return userId;
    }

    public int getTotalTokenCount() {
        return totalTokenCount;
    }

    public HashMap<String, Integer> getCompleteHistory() {
        HashMap<String, Integer> completeHistory = new HashMap<>();
        totalTokenCount = 0;
        for (String key : intents.keySet()) {
            TreeNode node = intents.get(key);
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

    public HashMap<String, Integer> getBranchHistory(Intent intent) {
        HashMap<String, Integer> branchHistory = new HashMap<>();
        Intent target_intent = (Intent) intents.get(intent.getName());
        totalTokenCount = 0;
        while (target_intent != null) {
            for (Map.Entry<String, Integer> entry : target_intent.getHistory().getSelectedQueryTerms().entrySet()) {
                if (branchHistory.containsKey(entry.getKey())) {
                    branchHistory.put(entry.getKey(), branchHistory.get(entry.getKey()) + entry.getValue());
                } else {
                    branchHistory.put(entry.getKey(), entry.getValue());
                }
                totalTokenCount += entry.getValue();
            }
            for (Map.Entry<String, Integer> entry : target_intent.getHistory().getSelectedDocTerms().entrySet()) {
                if (branchHistory.containsKey(entry.getKey())) {
                    branchHistory.put(entry.getKey(), branchHistory.get(entry.getKey()) + entry.getValue());
                } else {
                    branchHistory.put(entry.getKey(), entry.getValue());
                }
                totalTokenCount += entry.getValue();
            }
            target_intent = (Intent) target_intent.getParent();
        }
        return branchHistory;
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

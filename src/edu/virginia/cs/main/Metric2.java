/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.SortMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wua4nw
 */
public class Metric2 {

    private final int interval_in_hours;
    private final HashMap<String, Double> transitionProbMap;
    private String[] previous_topic_path;
    private final HashMap<String, Integer> map;

    public Metric2(int param) {
        this.interval_in_hours = param;
        this.transitionProbMap = new HashMap<>();
        doInitialization();

        map = new HashMap<>();
        map.put("initial_state", 0);
        map.put("up1", 1);
        map.put("up2", 2);
        map.put("down1", 3);
        map.put("down2", 4);
        map.put("same_state", 5);
        map.put("same_parent", 6);
        map.put("same_grand_parent", 7);
        map.put("others", 8);
    }

    private void doInitialization() {
        BufferedReader br = null;
        try {
            /* load background knowledge from file */
            br = new BufferedReader(new FileReader(new File(DeploymentConfig.TransitionProbability)));
            String line;
            double totalQuery = Double.valueOf(br.readLine());
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] split = line.split("\\s+");
                double prob = Double.valueOf(split[3]) / totalQuery;
                transitionProbMap.put(split[0] + "/" + split[2], prob);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Metric2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Metric2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public double evaluateTransitions(Profile userProf) {
        long difference_in_hours = 0;
        Date lastQueryTime = null;
        ArrayList<UserQuery> queries = new ArrayList<>();
        double totalRankingScore = 0;
        int totalScoreCount = 0;
        for (UserQuery query : userProf.getSubmittedQueries()) {
            if (lastQueryTime != null) {
                difference_in_hours = getDateDiff(lastQueryTime, query.getQuery_time(), TimeUnit.HOURS);
                if (difference_in_hours > interval_in_hours) {
                    /* compute goodness of alignment */
                    if (!queries.isEmpty()) {
                        double rankingScore = computeRanking(queries);
                        totalRankingScore += rankingScore;
                        totalScoreCount++;
                    }
                    /* start of a new interval, reset everything */
                    lastQueryTime = query.getQuery_time();
                    queries = new ArrayList<>();
                    previous_topic_path = new String[]{"Top"};
                }
                queries.add(query);
            } else {
                lastQueryTime = query.getQuery_time();
                previous_topic_path = new String[]{"Top"};
            }
        }

        if (totalScoreCount > 0) {
            return totalRankingScore / totalScoreCount;
        } else {
            return totalRankingScore;
        }
    }

    private double computeRanking(ArrayList<UserQuery> userQueries) {
        double rankingScore = 0.0;
        HashMap<String, Double> scoreMap = score(userQueries);
        Map<String, Double> sortedScoreMap = SortMap.sortMapByValue(scoreMap, false);

        /* compute ranking score */
        int maxScore = RunTimeConfig.NumberOfCoverQuery;

        int coverSeqFound = 0;
        int trueSeqFound = 0;
        for (Map.Entry<String, Double> entry : sortedScoreMap.entrySet()) {
            if (entry.getKey().contains("cover_sequence")) {
                coverSeqFound++;
            } else if (entry.getKey().contains("true_sequence")) {
                rankingScore += coverSeqFound - trueSeqFound;
                trueSeqFound++;
            }
        }

        if (maxScore != 0) {
            rankingScore = rankingScore / maxScore;
        }

        if (userQueries.isEmpty()) {
            return rankingScore;
        } else {
            return rankingScore / userQueries.size();
        }
    }

    private HashMap<String, Double> score(ArrayList<UserQuery> userQueries) {
        HashMap<String, Double> scoreMap = new HashMap<>();
        String previous_transition_status = "initial_state";
        ArrayList<String> previous_cover_status = new ArrayList<>();
        for (UserQuery query : userQueries) {
            String transition_status = getTransitionStatus(query);
            if (transition_status == null) {
                continue;
            }
            if (query.getCover_queries().size() != RunTimeConfig.NumberOfCoverQuery) {
                continue;
            }

            Double refProb = transitionProbMap.get(previous_transition_status + "/" + transition_status);
            double score = 0;
            if (refProb == null) {
                continue;
            } else if (refProb != 0) {
                score = Math.log(refProb);
            }

            if (scoreMap.containsKey("true_sequence")) {
                scoreMap.put("true_sequence", scoreMap.get("true_sequence") + score);
            } else {
                scoreMap.put("true_sequence", score);
            }

            ArrayList<String> current_cover_status = new ArrayList<>();
            int index = 0;
            for (UserQuery coverQuery : query.getCover_queries()) {
                String cover_status = getTransitionStatus(coverQuery);

                double cover_score;
                if (previous_cover_status.isEmpty()) {
                    cover_score = Math.log(transitionProbMap.get("initial_state" + "/" + cover_status));
                } else {
                    cover_score = Math.log(transitionProbMap.get(previous_cover_status.get(index) + "/" + cover_status));
                }
                if (scoreMap.containsKey("cover_sequence_" + index)) {
                    scoreMap.put("cover_sequence_" + index, scoreMap.get("cover_sequence_" + index) + cover_score);
                } else {
                    scoreMap.put("cover_sequence_" + index, cover_score);
                }

                current_cover_status.add(cover_status);
                index++;
            }

            previous_transition_status = transition_status;
            previous_cover_status = new ArrayList<>(current_cover_status);
        }
        return scoreMap;
    }

    private String getTransitionStatus(UserQuery query) {
        if (query.getQuery_intent() == null) {
            return null;
        }
        String[] split = query.getQuery_intent().getName().split("/");
        String transition_status;

        if (Arrays.equals(split, previous_topic_path)) {
            transition_status = "same_state";
        } else if (split.length == previous_topic_path.length) { // same level, 3 cases possible
            if (split.length > 1 && split[split.length - 2].equals(previous_topic_path[split.length - 2])) { // share same parent
                transition_status = "same_parent";
            } else if (split.length > 2 && split[split.length - 3].equals(previous_topic_path[split.length - 3])) { // share same grand parent
                transition_status = "same_grand_parent";
            } else { // others
                transition_status = "others";
            }
        } else if (split.length == previous_topic_path.length - 1 && Arrays.equals(split, Arrays.copyOfRange(previous_topic_path, 0, previous_topic_path.length - 1))) { // up 1 step
            transition_status = "up1";
        } else if (split.length == previous_topic_path.length - 2 && Arrays.equals(split, Arrays.copyOfRange(previous_topic_path, 0, previous_topic_path.length - 2))) { // up 2 step
            transition_status = "up2";
        } else if (split.length - 1 == previous_topic_path.length && Arrays.equals(previous_topic_path, Arrays.copyOfRange(split, 0, split.length - 1))) { // down 1 step
            transition_status = "down1";
        } else if (split.length - 2 == previous_topic_path.length && Arrays.equals(previous_topic_path, Arrays.copyOfRange(split, 0, split.length - 2))) { // down 2 step
            transition_status = "down2";
        } else { // others
            transition_status = "others";
        }

        previous_topic_path = split;
        return transition_status;
    }

}

package edu.virginia.cs.main;

import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.engine.OkapiBM25;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.model.GenerateCoverQuery;
import edu.virginia.cs.model.TopicTree;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.object.Session;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.Converter;
import java.io.IOException;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.FileWriter;
import java.util.Date;

public class Evaluate {

    private Searcher _searcher = null;
    /* Storing a specific user's queries and corresponding all clicked documents */
    private List<UserQuery> userQueries;
    /* Storing a specific user's all queries */
    private ArrayList<String> listOfUserQuery;
    /* Storing a specific user's all queries */
    private ArrayList<String> listOfCoverQuery;
    /* Object to generate cover queries for a specific user query */
    private final GenerateCoverQuery gCoverQuery;
    /* User profile which is constructed and maintained in the client side */
    private Profile profile;
    /* Total MAP for 'n' users that we are evaluating, ex. in our case, n = 250 */
    private double totalMAP = 0.0;
    /* Total number of queries evaluated for 'n' users, ex. in our case, n = 250 */
    private double totalQueries = 0.0;
    /* Total KL-Divergence for 'n' users that we are evaluating, ex. in our case, n = 250 */
    private double totalKL = 0;
    /* Total mutual information for 'n' users that we are evaluating, ex. in our case, n = 250 */
    private double totalMI = 0;

    private final NMICalculation computeNMI;

    public Evaluate(TopicTree tree) {
        _searcher = new Searcher(DeploymentConfig.AolIndexPath);
        _searcher.setSimilarity(new OkapiBM25());
        // setting the flag to enable personalization
        _searcher.activatePersonalization(true);
        computeNMI = new NMICalculation(DeploymentConfig.AolIndexPath);
    }

    /**
     * Main method that executes the entire pipeline.
     *
     * @param allUserId
     * @param threadId
     * @return
     * @throws java.lang.Throwable
     */
    public String startEvaluation(ArrayList<String> allUserId, String threadId) throws Throwable {
        int countUsers = 0;
        FileWriter writer = new FileWriter("model-output-files/" + threadId + ".txt");
        for (String userId : allUserId) {
            countUsers++;
            // initializing user profile of the server side and setting the reference model
            _searcher.initializeUserProfile(userId);

            // required for calculating mutual information between user queris and cover queries
            listOfUserQuery = new ArrayList<>();
            listOfCoverQuery = new ArrayList<>();

            // generate the clicked urls for evaluation
            loadUserJudgements(userId);
            // initialization for client side user profile
            profile = new Profile(userId);

            double meanAvgPrec = 0.0;
            // Number of queries evaluated
            int numQueries = 0;

            /**
             * query contains query plus the timestamp when the query was
             * submitted.
             */
            Date lastQuerySubmitTime = null;
            UserQuery lastSubmittedQuery = null;
            int session_id = 0;

            for (UserQuery query : userQueries) {
                /**
                 * If a user query has at least one corresponding clicked
                 * document, then we evaluate it, otherwise not.
                 *
                 */
                if (!query.getRelevant_documents().isEmpty()) {
                    if (lastQuerySubmitTime != null && lastSubmittedQuery != null) {
                        boolean isSame = Helper.checkSameSession(lastQuerySubmitTime, query.getQuery_time(), lastSubmittedQuery.getQuery_text(), query.getQuery_text());
                        // current query and previous query are from different session
                        if (!isSame) {
                            // set end time of previous session
                            lastSubmittedQuery.getQuery_session().setEndTime(lastQuerySubmitTime);
                            // start of a new user session
                            Session session = new Session(session_id);
                            session.setStartTime(query.getQuery_time());
                            session_id++;
                            query.setQuery_session(session);
                        }
                    }
                    listOfUserQuery.add(query.getQuery_text());
                    // computing average precision for a query
                    double avgPrec = AvgPrec(query);
                    meanAvgPrec += avgPrec;
                    ++numQueries;
                    lastQuerySubmitTime = query.getQuery_time();
                    lastSubmittedQuery = query;
                }
            }

            // totalMAP = sum of all MAP computed for queries of 'n' users
            totalMAP += meanAvgPrec;
            // totalQueries = total number of queries for 'n' users
            totalQueries += numQueries;
            /**
             * computing KL-Divergence from true user profile to noisy user
             * profile.
             */
            double klDivergence = (double) _searcher.getUserProfile().calculateKLDivergence(profile);
            totalKL += klDivergence;
            // compute MAP for the current user
            double MAP = meanAvgPrec / numQueries;
            // compute mutual information for the current user
            double mutualInfo = computeNMI.calculateNMI(listOfUserQuery, listOfCoverQuery);
            // totalMI = sum of all MI computed for 'n' users
            totalMI += mutualInfo;
            writer.write(countUsers + "\t" + Integer.parseInt(userId) + "\t" + MAP + "\t" + klDivergence + "\t" + mutualInfo + "\n");
            writer.flush();
            System.out.printf("%-8d\t%-8d\t%-8f\t%.8f\t%.8f\n", countUsers, Integer.parseInt(userId), MAP, klDivergence, mutualInfo);
        }

        double avgKL = 0;
        double avgMI = 0;
        double finalMAP = totalMAP / totalQueries;
        if (countUsers > 0) {
            avgKL = totalKL / countUsers;
            avgMI = totalMI / countUsers;
        }

        writer.write("\n************Result after full pipeline execution for n users**************" + "\n");
        writer.write("\nTotal number of users : " + countUsers + "\n");
        writer.write("Total number of quries tested : " + totalQueries + "\n");
        writer.write("MAP : " + finalMAP + "\n");
        writer.write("Average KL : " + avgKL + "\n");
        writer.write("Average MI : " + avgMI + "\n");
        writer.flush();
        writer.close();

        String retValue = countUsers + "\t" + totalQueries + "\t" + totalMAP + "\t" + totalKL + "\t" + totalMI;
        return retValue;
    }

    /**
     * Method that computes average precision of a user submitted query.
     *
     * @param query user's original query
     * @param clickedDocs clicked documents for the true user query
     * @return average precision
     */
    private double AvgPrec(UserQuery query) throws Throwable {
        // generating the cover queries
        double avgp = 0.0;
        if (RunTimeConfig.NumberOfCoverQuery == 0) {
            // if no cover query is required, just submit the original query
            avgp = submitOriginalQuery(query);
        } else {
            ArrayList<UserQuery> coverQueries;
            /**
             * If the user is repeating a query in the same session, same set of
             * cover queries will be submitted to the search engine.
             */
            int query_index = profile.checkRepeatInCurrentSession(query);
            if (query_index == -1) {
                coverQueries = gCoverQuery.generateNQueries(profile, query);
            } else {
                /* User has repeated a query in the same session */
                coverQueries = profile.getQuery(query_index).getCover_queries();
            }
            /**
             * if for some reason cover queries are not generated properly, no
             * query will be submitted to the search engine.
             */
            if (coverQueries == null) {
                return avgp;
            }

            int randNum = (int) (Math.random() * coverQueries.size());
            for (int k = 0; k < coverQueries.size(); k++) {
                listOfCoverQuery.add(coverQueries.get(k).getQuery_text());
                // submitting cover query to the search engine
                ArrayList<ResultDoc> searchResults = _searcher.search(coverQueries.get(k)).getDocs();
                // generating fake clicks for the cover queries, one click per cover query
                if (!searchResults.isEmpty()) {
                    int rand = (int) (Math.random() * searchResults.size());
                    ResultDoc rdoc = searchResults.get(rand);
                    // user clicked a document
                    _searcher.clickDocument(coverQueries.get(k), rdoc);
                }
                // submitting the original user query to the search engine
                if (k == randNum) {
                    avgp = submitOriginalQuery(query);
                }
                query.addCover_query(coverQueries.get(k));
            }

        }
        return avgp;
    }

    /**
     * Submit the original query to search engine and computes average precision
     * of the search results.
     *
     * @param query
     * @param clickedDocs
     * @return
     * @throws IOException
     */
    private double submitOriginalQuery(UserQuery query) throws IOException {
        double avgp = 0.0;
        ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
        if (results.isEmpty()) {
            return avgp;
        }
        // re-rank the results based on the user profile kept in client side
        if (RunTimeConfig.ClientSideRanking) {
            results = Helper.reRankResults(profile.getCompleteHistory(), results);
//            results = Helper.reRankResults(profile.getBranchHistory(query.getQuery_intent()), results);
        }
        int i = 1;
        double numRel = 0;
        for (ResultDoc rdoc : results) {
            ResultDoc relDoc = query.relDoc_Contains(rdoc);
            if (relDoc != null) {
                relDoc.setClicked();
                numRel++;
                avgp = avgp + (numRel / i);
                // update user profile kept in the server side
                _searcher.clickDocument(query, rdoc);
            }
            ++i;
        }
        avgp = avgp / query.getRelevant_documents().size();
        // updating user profile kept in client side
        profile.addQuery(query);
        return avgp;
    }

    /**
     * Method that generates a mapping between each user query and corresponding
     * clicked documents.
     *
     * @param userId
     * @throws java.lang.Throwable
     */
    private void loadUserJudgements(String userId) {
        userQueries = new ArrayList<>();
        String judgeFile = DeploymentConfig.UserSearchLogPath + userId + ".txt";
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(judgeFile));
            String line;
            boolean isQuery = false;
            UserQuery uq = null;
            int query_id = 0;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    isQuery = false;
                    continue;
                }
                if (!isQuery) {
                    isQuery = true;
                    String[] terms = line.split("\t");
                    uq = new UserQuery(query_id, terms[0]);
                    uq.setQuery_time(Converter.convertStringToDate(terms[1]));
                    userQueries.add(uq);
                    query_id++;
                } else {
                    ResultDoc doc = new ResultDoc();
                    doc.setUrl(line);
                    if (uq != null) {
                        uq.addRelevant_document(doc);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Evaluate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

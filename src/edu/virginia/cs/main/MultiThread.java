/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.extra.Helper;
import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.model.TopicTree;
import edu.virginia.cs.model.TopicTreeNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Wasi
 */
public class MultiThread {

    public static void main(String[] args) throws Exception {
        MultiThread ml = new MultiThread();
        ml.loadParameters();
        ml.doInitialization();
        ml.createThreads();
    }

    /**
     * Load all parameters.
     */
    private void loadParameters() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("settings.txt")));
            RunTimeConfig.NumberOfCoverQuery = Integer.parseInt(br.readLine().replace("number of cover queries =", "").trim());
            RunTimeConfig.ClientSideRanking = br.readLine().replace("client side re-ranking =", "").trim().equals("on");
            RunTimeConfig.NumberOfThreads = Integer.parseInt(br.readLine().replace("number of threads =", "").trim());
            RunTimeConfig.TotalDocInWeb = Integer.parseInt(br.readLine().replace("total documents in AOL index =", "").trim());
            RunTimeConfig.removeStopWordsInCQ = br.readLine().replace("remove stopwords from cover query =", "").trim().equals("yes");
            RunTimeConfig.doStemmingInCQ = br.readLine().replace("generate stemmed cover query =", "").trim().equals("yes");

            DeploymentConfig.AolIndexPath = br.readLine().replace("lucene AOL index directory =", "").trim();
            DeploymentConfig.OdpIndexPath = br.readLine().replace("lucene ODP index directory =", "").trim();
            DeploymentConfig.UserSearchLogPath = br.readLine().replace("users search log directory =", "").trim();
            DeploymentConfig.ReferenceModelPath = br.readLine().replace("reference model file =", "").trim();
            DeploymentConfig.AolDocFreqRecord = br.readLine().replace("AOL document frequency record =", "").trim();
            DeploymentConfig.OdpHierarchyRecord = br.readLine().replace("ODP hierarchy file =", "").trim();
            DeploymentConfig.BackgroundKnowledge = br.readLine().replace("background knowledge file =", "").trim();
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Do initialization.
     */
    private void doInitialization() {
        File file = new File("model-output-files/");
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * Loads language models up to level 'param' from all language models of
     * DMOZ categories.
     *
     * @param filename
     * @param depth depth of the hierarchy
     * @return list of language models
     */
    private TopicTree createTopicTree(int depth) {
        TopicTree topicTree = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            ArrayList<String> topics = null;
            try {
                FileInputStream fis = new FileInputStream(DeploymentConfig.OdpHierarchyRecord);
                ODPCategoryReader odpCatReader = new ODPCategoryReader(depth);
                saxParser.parse(fis, odpCatReader);
                topics = odpCatReader.getTopics();
            } catch (SAXException | IOException ex) {
                Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (topics == null) {
                return topicTree;
            }

            topicTree = new TopicTree();
            int node_id = 0;

            for (String currentTopic : topics) {
                String[] split = currentTopic.split("/");
                int level = split.length - 1;

                TopicTreeNode node = new TopicTreeNode(currentTopic, node_id);
                node.setNodeLevel(level);

                if (split.length >= 2) {
                    String parent = currentTopic.substring(0, currentTopic.lastIndexOf("/"));
                    if (topicTree.exists(parent)) {
                        node.setParent(topicTree.getTreeNode(parent));
                        topicTree.getTreeNode(parent).addChildren(node);
                    } else {
                        System.err.println("Problem while loading ODP topic hierarchy...");
                        System.exit(1);
                    }
                } else {
                    node.setParent(null);
                }

                topicTree.addNode(currentTopic, node);
                node_id++;
            }

        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        return topicTree;
    }

    /**
     * The main method that creates and starts threads.
     *
     * @param count number of threads need to be created and started.
     * @return
     */
    private void createThreads() throws InterruptedException {
        try {
            MyThread[] myT = new MyThread[RunTimeConfig.NumberOfThreads];
            /* Loading the reference model, idf record and all user ids */
            ArrayList<String> allUserId = Helper.getAllUserId(DeploymentConfig.UserSearchLogPath, -1);
            StaticData.loadRefModel(DeploymentConfig.ReferenceModelPath);
            StaticData.loadIDFRecord(DeploymentConfig.AolDocFreqRecord);

            TopicTree tree = createTopicTree(4);
            if (tree == null) {
                System.err.println("Failed to load ODP category hierarchy");
                System.exit(1);
            }
            System.out.println("Topic tree loaded... " + tree.getNodesOfLevel(3).size());

            int limit = allUserId.size() / RunTimeConfig.NumberOfThreads;
            for (int i = 0; i < RunTimeConfig.NumberOfThreads; i++) {
                int start = i * limit;
                ArrayList<String> list;
                if (i == RunTimeConfig.NumberOfThreads - 1) {
                    list = new ArrayList<>(allUserId.subList(start, allUserId.size()));
                } else {
                    list = new ArrayList<>(allUserId.subList(start, start + limit));
                }
                myT[i] = new MyThread(list, "thread_" + i, tree);
                myT[i].start();
            }
            for (int i = 0; i < RunTimeConfig.NumberOfThreads; i++) {
                myT[i].getThread().join();
            }

            /* When all threads finished its execution, generate final result */
            double totalKLDivergence = 0.0;
            double totalMI = 0.0;
            double totalMAP = 0.0;
            double totalGoA = 0.0;
            int totalUsers = 0;
            double totalQueries = 0;
            for (int i = 0; i < RunTimeConfig.NumberOfThreads; i++) {
                String[] result = myT[i].getResult().split("\t");
                totalUsers += Integer.parseInt(result[0]);
                totalQueries += Double.parseDouble(result[1]);
                totalMAP += Double.valueOf(result[2]);
                totalKLDivergence += Double.valueOf(result[3]);
                totalMI += Double.valueOf(result[4]);
                totalGoA += Double.valueOf(result[5]);
            }
            double finalKL = totalKLDivergence / totalUsers;
            double finalMI = totalMI / totalUsers;
            double finalMAP = totalMAP / totalQueries;
            double finalGoA = totalGoA / totalQueries;
            FileWriter fw = new FileWriter("model-output-files/final_output.txt");
            fw.write("**************Parameter Settings**************\n");
            fw.write("Number of cover queries = " + RunTimeConfig.NumberOfCoverQuery + "\n");
            fw.write("**********************************************\n");
            fw.write("Total Number of users = " + totalUsers + "\n");
            fw.write("Total Number of queries tested = " + totalQueries + "\n");
            fw.write("Averge MAP = " + finalMAP + "\n");
            fw.write("Average KL-Divergence = " + finalKL + "\n");
            fw.write("Average Mutual Information = " + finalMI + "\n");
            fw.write("Average Goodness of Alignment Score = " + finalGoA + "\n");
            fw.close();
        } catch (InterruptedException | NumberFormatException | IOException ex) {
            Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class MyThread implements Runnable {

    private Thread t = null;
    private final ArrayList<String> userIds;
    private final String threadId;
    private String result;
    private final TopicTree topicTree;

    public MyThread(ArrayList<String> listUsers, String id, TopicTree tree) {
        this.userIds = listUsers;
        this.threadId = id;
        this.topicTree = tree;
    }

    /**
     * Overriding the run method of the Thread class.
     */
    @Override
    public void run() {
        try {
            Evaluate evaluate = new Evaluate(topicTree);
            result = evaluate.startEvaluation(userIds, threadId);
        } catch (Throwable ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method to start the thread.
     */
    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    public String getResult() {
        return result;
    }

    /**
     * Method to return the thread object.
     *
     * @return thread object
     */
    public Thread getThread() {
        return t;
    }
}

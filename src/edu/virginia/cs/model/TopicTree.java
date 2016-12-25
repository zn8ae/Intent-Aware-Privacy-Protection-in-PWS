/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.interfaces.Tree;
import edu.virginia.cs.interfaces.TreeNode;
import java.util.HashMap;

/**
 *
 * @author wua4nw
 */
public class TopicTree implements Tree {

    private HashMap<String, TreeNode> nodeMap;

    @Override
    public void setNodes(HashMap<String, TreeNode> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public TreeNode getTreeNode(String name) {
        return this.nodeMap.get(name);
    }

    @Override
    public HashMap<String, TreeNode> getNodeMap() {
        return this.nodeMap;
    }

    @Override
    public void addNode(String nodePath, TreeNode node) {
        this.nodeMap.put(nodePath, node);
    }

}

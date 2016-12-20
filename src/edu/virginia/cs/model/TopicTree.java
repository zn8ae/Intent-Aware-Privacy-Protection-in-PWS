/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import java.util.ArrayList;
import edu.virginia.cs.interfaces.Tree;
import edu.virginia.cs.interfaces.TreeNode;
import java.util.List;

/**
 *
 * @author wua4nw
 */
public class TopicTree implements Tree {

    ArrayList<TreeNode> nodes;

    @Override
    public void setNodes(List<TreeNode> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    public TreeNode getTopicByIndex(int index) {
        if (nodes != null && nodes.size() > index) {
            return nodes.get(index);
        }
        return null;
    }

    @Override
    public List<TreeNode> getListOfNodes() {
        return this.nodes;
    }

    @Override
    public void addNode(TreeNode node) {
        this.nodes.add(node);
    }

}

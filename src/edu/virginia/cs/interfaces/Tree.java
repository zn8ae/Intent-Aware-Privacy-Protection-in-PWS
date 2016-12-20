/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.interfaces;

import java.util.List;

/**
 *
 * @author wua4nw
 */
public interface Tree {

    public List<TreeNode> getListOfNodes();

    public void addNode(TreeNode node);

    public void setNodes(List<TreeNode> nodes);
}

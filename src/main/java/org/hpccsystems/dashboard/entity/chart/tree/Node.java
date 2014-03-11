package org.hpccsystems.dashboard.entity.chart.tree;

import java.util.List;

public class Node {
	private String name;
	private List<Node> children;
	
	public Node() {
	}
	
	public Node(String name) {
		this.setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}
}

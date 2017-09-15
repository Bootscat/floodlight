package veriflow;

import java.util.ArrayList;

public class Node {
	public Node zeroBranch = null;
	public Node oneBranch = null;
	public Node starBranch = null;
	public Node parent = null;
	public Tree nextTree = null;
	public ArrayList<RuleObject> ruleSet = new ArrayList<RuleObject>();
}

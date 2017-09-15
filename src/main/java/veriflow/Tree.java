package veriflow;

public class Tree {
	public Node root = null;
	public String fieldName = null;
	
	public Tree(String name) {
		root = new Node();
		fieldName = name;
	}
	
	public Node insert(String fieldCode) {
		Node current = root;
		for(char bit : fieldCode.toCharArray()) {
			switch(bit) {
				case '0':
					if(current.zeroBranch != null) {
						current = current.zeroBranch;
					} else {
						current.zeroBranch = new Node();
						current.zeroBranch.parent = current;
						current = current.zeroBranch;
					}
					break;
				case '1':
					if(current.oneBranch != null) {
						current = current.oneBranch;
					} else {
						current.oneBranch = new Node();
						current.oneBranch.parent = current;
						current = current.oneBranch;
					}
					break;
				case '*':
					if(current.starBranch != null) {
						current = current.starBranch;
					} else {
						current.starBranch = new Node();
						current.starBranch.parent = current;
						current = current.starBranch;
					}
					break;
			}
		}
		return current;
	}
}

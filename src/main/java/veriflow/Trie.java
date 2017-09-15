package veriflow;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trie {
	private String[] matchFields = new String[] {"eth_type", "eth_src", "eth_dst", "ipv4_src", "ipv4_dst"};
	private JSONObject fieldLenth = new JSONObject();
	public Tree rootTree = null;
	public Logger log = LoggerFactory.getLogger(VeriFlow.class);
	
	public Trie() {
		rootTree = new Tree("eth_type"); 
		try {
			fieldLenth.put("eth_type", 16);
			fieldLenth.put("eth_src", 48);
			fieldLenth.put("eth_dst", 48);
			fieldLenth.put("ipv4_src", 32);
			fieldLenth.put("ipv4_dst", 32);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Node insert(JSONObject matchObj) {
		Tree currentTree = rootTree;
		Node currentNode = null;
		
		for(int index=0; index<5; index++) {
			String field = matchFields[index];
			String fieldValue = null;
			try {
				if(matchObj.has(field)){
					fieldValue = matchObj.get(field).toString();
					fieldValue = toBinaryString(field, fieldValue);
					currentNode = currentTree.insert(fieldValue);
				} else {
					String tmpValue = "";
					for(int i=0; i < Integer.valueOf(fieldLenth.get(field).toString()); i++) {
						tmpValue += "*";
					}
					currentNode = currentTree.insert(tmpValue);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(index == 4) {
				// insert rule to current Node
				return currentNode;
			} else {
				if(currentNode.nextTree != null) {
					currentTree = currentNode.nextTree;
				} else {
					currentNode.nextTree = new Tree(matchFields[index+1]);
					currentNode.nextTree.root.parent = currentNode;
					currentTree = currentNode.nextTree;
				}
			}
		}
		return null;
	}
	
	private String toBinaryString(String fieldName,String fieldValue) {
		String ret = "";
		String[] tmp;
		switch(fieldName) {
			case "eth_type":
				tmp = fieldValue.split("x");
				ret = Integer.toBinaryString(Integer.parseInt(tmp[1], 16));
				while(ret.length() < 16) {
					ret = "0" + ret;
				}
				break;
			case "eth_src":
			case "eth_dst":
				tmp = fieldValue.split(":");
				for(int i=0; i<6; i++) {
					String hex2Int = Integer.toBinaryString(Integer.parseInt(tmp[i], 16));
					while(hex2Int.length()<8) {
						hex2Int = "0" + hex2Int;
					}
					ret += hex2Int;
				}
				break;
			case "ipv4_src":
			case "ipv4_dst":
				tmp = fieldValue.split("\\.");
				for(int i=0; i<4; i++) {
					String ten2Int = Integer.toBinaryString(Integer.parseInt(tmp[i]));
					while(ten2Int.length()<8) {
						ten2Int = "0" + ten2Int;
					}
					ret += ten2Int;
				}
				break;
		}
		return ret;
		
	}
}

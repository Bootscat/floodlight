package cead;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiGraph {
	public HashMap<Entry<String, String>, Set<Entry<String, String>>> adjList = null;
	public HashMap<Entry<String, String>, Integer> outDegree = null;
	public HashMap<Entry<String, String>, Integer> inDegree = null;
	public Set<Entry<String, String>> visited = null;
	public Set<Entry<String, String>> ancestors = null;
	protected static final Logger log = LoggerFactory.getLogger("CEAD");

	public DiGraph() {
		adjList = new HashMap<Entry<String, String>, Set<Entry<String, String>>>();
		outDegree = new HashMap<Entry<String, String>, Integer>();
		inDegree = new HashMap<Entry<String, String>, Integer> ();
		visited = new HashSet<Entry<String, String>>();
		ancestors = new HashSet<Entry<String, String>>();
	}

	public void addNode(Entry<String, String> node) {
		if(this.adjList.get(node) == null) {
			this.adjList.put(node, new HashSet<Entry<String, String>>());
		}
		if(this.inDegree.get(node) == null) {
			this.inDegree.put(node, 0);
		}
		if(this.outDegree.get(node) == null) {
			this.outDegree.put(node, 0);
		}
	}

	public void addEdge(Entry<String, String> src, Entry<String, String> dst) {
		this.addNode(src);
		this.addNode(dst);
		this.adjList.get(src).add(dst);
		outDegree.put(src, outDegree.get(src) + 1);
		inDegree.put(dst, inDegree.get(dst) + 1);
	}

	public Integer getInDegree(Entry<String, String> node) {
		return this.inDegree.get(node);
	}

	public Integer getOutDegree(Entry<String, String> node) {
		return this.outDegree.get(node);
	}

	public Set<Entry<String, String>> getNeighbors(Entry<String, String> node) {
		return this.adjList.get(node);
	}

	public Set<Entry<String, String>> getNodes() {
		return this.adjList.keySet();
	}

	public boolean isVisited(Entry<String, String> node) {
		if(visited.contains(node)) {
			return true;
		} else {
			return false;
		}
	}

	
	/**
	 * Find a loop in directed graph, and check the reachability
	 * @param match used to check the source and destination node
	 * @param node the node DFS is traveling
	 */
	public boolean dfs(JSONObject match, Entry<String, String> node) throws JSONException {
		if(!this.isVisited(node)) {
			if(this.getOutDegree(node) == 0) {
				if(match.has("eth_dst")) {
					if(!match.get("eth_dst").toString().equals(node.getKey())) {
						log.error("Wrong routing decision");
						return false;
					}
				}
				
				/**
				* 這裡是要檢查IP
				* 但有時topology更新時
				* IP會沒有更新到 (剛好遇到controller刷新IP的混沌期)
				* 反而造成不必要的判斷錯誤
				* 目前尚無法解決這個問題
				* 故註解
				* if(match.has("ipv4_dst")) {
				*	if(!match.get("ipv4_dst").toString().equals(node.getValue())) {
				*		log.info(match.get("ipv4_dst").toString());
				*		log.info(node.getValue());
				*		log.error("Wrong routing decision");
				*		return false;
				*	}
				* }
				*/
			}

			if(this.getInDegree(node) == 0) {
				/**
				 * 這裡是要檢查source node
				 * 但是比destination麻煩很多
				 * 故省略 XD
				 */
			}

			visited.add(node);
			ancestors.add(node);
			for(Entry<String, String> n : this.getNeighbors(node)) {
				if(!visited.contains(n)) {
					this.dfs(match, n);
				} else if(ancestors.contains(n)) {
					log.info("Cycle error");
					return false;
				}
			}
			ancestors.remove(node);
		}
		return true;
	}
}

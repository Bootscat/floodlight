package cead;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.floodlightcontroller.core.IOFSwitch;


public class AnomalyDetector {
	protected static final Logger log = LoggerFactory.getLogger("CEAD");
	
	public static void handleTransaction(ArrayList<Entry<IOFSwitch, OFMessage>> list, JSONObject topology) throws JSONException {
		HashMap<String, DiGraph> graphs = drawFlowGraph(list, topology);
		anomalyDetect(graphs);
	}
	
	public static HashMap<String, DiGraph> drawFlowGraph(ArrayList<Entry<IOFSwitch, OFMessage>> list, JSONObject topology) throws JSONException {
		HashMap<String, DiGraph> graphs = new HashMap<String, DiGraph>();

		for(Entry<IOFSwitch, OFMessage> entry : list) {
			OFFlowMod fm = (OFFlowMod) entry.getValue(); // value is OFMessage originally
			IOFSwitch sw = entry.getKey();
			JSONObject matchObj =  new JSONObject();
			String in_port = null;
			try {
				for(MatchField<?> field : fm.getMatch().getMatchFields()) {
					if(field.getName().equals("in_port")) {
						in_port = fm.getMatch().get(field).toString();
						continue;
					}
					matchObj.put(field.getName(), fm.getMatch().get(field).toString());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			DiGraph graph = graphs.get(matchObj.toString());
			if(graph == null){
				graph = new DiGraph();
				graphs.put(matchObj.toString(), graph);
			}

			String target = sw.getId().toString();
			String[] outputs = fm.getOutPort().toString().split(",");
			if(in_port != null) {
				Entry<String, String> in_start = new SimpleEntry<String, String>(target, in_port);
				for(String port : outputs) {
					Entry<String, String> in_end = new SimpleEntry<String, String>(target, port);
					graph.addEdge(in_start, in_end);
					JSONArray tuple = (JSONArray) ((JSONObject) topology.get(target)).get(port);
					Entry<String, String> out_end = new SimpleEntry<String, String>(tuple.get(0).toString(), tuple.get(1).toString());
					graph.addEdge(in_end, out_end);
				}
			} else {
				Entry<String, String> in_start = new SimpleEntry<String, String>(target, "");
				for(String port : outputs) {
					Entry<String, String> in_end = new SimpleEntry<String, String>(target, port);
					graph.addEdge(in_start, in_end);
					JSONArray tuple = (JSONArray) ((JSONObject) topology.get(target)).get(port);
					String out_switch = tuple.get(0).toString();
					if(out_switch != null) {
						Entry<String, String> out_end = new SimpleEntry<String, String>(out_switch, "");
						graph.addEdge(in_end, out_end);
					}
				}
			}
		}
		
		return graphs;
	}
	
	public static void anomalyDetect(HashMap<String, DiGraph> graphs) throws JSONException {
		boolean isAnomalous = false;
		for(String match : graphs.keySet()) {
			DiGraph graph = graphs.get(match);
			JSONObject matchObj = new JSONObject(match);
			isAnomalous = violateRoutingPolicies(matchObj, graph);
			
			if(isAnomalous) {
				log.info("Transaction error");
				break;
			}
		}
		Long tid = Thread.currentThread().getId();
		TransactionClassifier.installTransaction(tid);
	}
	
	public static boolean violateRoutingPolicies(JSONObject match, DiGraph graph) throws JSONException {
		for(Entry<String, String> node : graph.getNodes()) {
			if(!graph.dfs(match, node)){
				return true;
			}
		}
		return false;
	}
}

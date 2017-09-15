package veriflow;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.floodlightcontroller.core.IOFSwitch;

public class VeriFlow {
	public static Socket socket = null;
	public static Trie flowTrie = new Trie();
	public static JSONObject topology = null;
	protected static final Logger log = LoggerFactory.getLogger("VeriFlow");
	public static boolean start = false;
	
	public static void init() {
		if(socket != null) {
			return;
		}
	    
		try {
			socket = IO.socket("http://140.113.203.233:5000");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
		  @Override
		  public void call(Object... args) {}
		}).on("start", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				if(!start) {
					log.info("VeriFlow start");
				} else {
					log.info("VeriFlow stop");
				}
				start = !start;
			}
		}).on("topo", new Emitter.Listener() {
			public void call(Object... args) {
				topology = (JSONObject)args[0];
				log.info("Topology update");
			}
		}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
			
			@Override
			public void call(Object... args) {
				socket.close();
			}
		});
		socket.connect();
	}
	
	public static boolean handleFlowMod(IOFSwitch sw, OFMessage m) {
		if (!start || !m.getType().name().equals("FLOW_MOD")) {
			return true;
		}
		
		String appName = getMessageSender();
		
		if(!appName.equals("net.floodlightcontroller.experimentApp.ExperimentApp")) {
			return true;
		}
		
		OFFlowMod fm = (OFFlowMod) m;	
		JSONObject matchObj = new JSONObject();
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
		
		String[] outputs = fm.getOutPort().toString().split(",");
		RuleObject currentFlow = new RuleObject(sw, in_port , outputs);
		
		Node leafNode = flowTrie.insert(matchObj);
		// build graph
		DiGraph graph = new DiGraph();
		// insert current flow
		Entry<String, String> dst = null;
		try {
			dst = addGraph(graph, currentFlow);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// insert existing flow
		if(!leafNode.ruleSet.isEmpty()) {
			for(RuleObject rule : leafNode.ruleSet) {
				try {
					addGraph(graph, rule);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return true;
//		
//		for(Entry<String, String> node : graph.getNodes()) {
//			try {
//				if(!graph.dfs(matchObj, node)){
//					return false;
//				}
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		if(dst != null) {
//			/**
//			* This part are not implemented in related work, so I comment it.
//			* try {
//			*	if(matchObj.has("eth_dst")) {
//			*		if(!matchObj.get("eth_dst").toString().equals(dst.getKey())) {
//			*			log.error(matchObj.get("eth_dst").toString());
//			*			log.error(dst.getKey());
//			*			log.error("Destination error");
//			*			return false;
//			*		}
//			*	}
//			*	
//			*	if(matchObj.has("ipv4_dst")) {
//			*		if(!matchObj.get("ipv4_dst").toString().equals(dst.getValue())) {
//			*			log.error(matchObj.get("ipv4_dst").toString());
//			*			log.error(dst.getValue());
//			*			log.error("Destination error");
//			*			return false;
//			*		}
//			*	}
//			* } catch (JSONException e) {
//			*	// TODO Auto-generated catch block
//			*	e.printStackTrace();
//			* }
// 			*/
//			sw.write(m);
//			leafNode.ruleSet.clear();
//		} else {
//			sw.write(m);
//			leafNode.ruleSet.add(currentFlow);
//		}
//		return false;
	}
	
	
	/**
	* find the App who sends the Flow-Mod or Packet-Out
	* @return the App who sends the Flow-Mod or Packet-Out
	*/
	public static String getMessageSender() {
		String function;
		String sender = "";
		int cnt = 0;
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			function = ste.toString().split("\\(")[0];
			if(function.equals("veriflow.VeriFlow.handleFlowMod")) {
				cnt += 1;
			}
			int cutPoint = function.indexOf(".receive");
			if(cutPoint != -1) {
				sender = function.substring(0, cutPoint);
			}
		}
		return cnt > 1 ? "VeriFlow" : sender;
	}
	
	
	public static Entry<String, String> addGraph(DiGraph graph, RuleObject rule) throws JSONException {
		String target = rule.sw.getId().toString();
		String in_port = rule.in_port;
		String [] outputs = rule.outputs;
		
		if(in_port != null) {
			Entry<String, String> in_start = new SimpleEntry<String, String>(target, in_port);
			for(String port : outputs) {
				Entry<String, String> in_end = new SimpleEntry<String, String>(target, port);
				graph.addEdge(in_start, in_end);
				JSONArray tuple = (JSONArray) ((JSONObject) topology.get(target)).get(port);
				Entry<String, String> out_end = new SimpleEntry<String, String>(tuple.get(0).toString(), tuple.get(1).toString());
				graph.addEdge(in_end, out_end);
				
				String tmp = out_end.getValue();
				if(!tmp.matches("[-+]?\\d*\\.?\\d+")){
					return out_end;
				};
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
					
					String tmp = out_end.getValue();
					if(!tmp.matches("[-+]?\\d*\\.?\\d+")){
						return out_end;
					};
				}
			}
		}
		return null;
		
	}
}


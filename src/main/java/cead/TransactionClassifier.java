package cead;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.FileHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.EthType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Ethernet;

/**
* Monitor the message transfer between App and Controller
*/
public class TransactionClassifier {
	public static Socket socket = null;
	public static HashMap<Long, Entry<String, JSONObject>> packetIn = new HashMap<Long, Entry<String, JSONObject>>();
	public static HashMap<Long, Entry<IOFSwitch, OFMessage>> packetOut = new HashMap<Long, Entry<IOFSwitch, OFMessage>>();
	public static HashMap<Long, ArrayList<Entry<IOFSwitch, OFMessage>>> flowMod = new HashMap<Long, ArrayList<Entry<IOFSwitch, OFMessage>>>();
	/** topology information */
	public static JSONObject topology = null;
	/** CEAD active or not */
	public static boolean start = false;
	protected static final Logger log = LoggerFactory.getLogger("CEAD");




	public static void init() {
		if(socket != null) {
			return;
		}

		try {
			socket = IO.socket("http://140.113.203.233:5000");
//			socket = IO.socket("http://127.0.0.1:5000");
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
					log.info("CEAD start");
				} else {
					log.info("CEAD stop");
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


	/**
	* Monitor and store the Packet-In message
	* @param sw the switch receive the Packet-In message
	* @param m the Packet-In message
	*/
	public static void handlePacketIn(IOFSwitch sw, OFMessage m) {
		if (start == false) {
			return;
		}
		Long tid = Thread.currentThread().getId();
		JSONObject headerObj = new JSONObject();
		OFPacketIn pi = (OFPacketIn)m;
		Ethernet eth = new Ethernet();
        eth.deserialize(pi.getData(), 0, pi.getData().length);

		/**
		* Parse and store the Packet-In message
		String[] ethArray = eth.toString().split("\n");
		for(String line : ethArray) {
			if(line.indexOf(":") >= 0) {
				String[] lineArray = line.split(": ", 2);
				try {
					headerObj.put(lineArray[0], lineArray[1]);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		try {
			headerObj.put("eth_type", eth.getEtherType().toString());
			headerObj.put("dpid", sw.getId());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/

		Entry<String, JSONObject> tmp = new SimpleEntry<String, JSONObject>(sw.getId().toString(), headerObj);
		packetIn.put(tid, tmp);
	}


	/**
	* Monitor and store the Flow-Mod or Packet-Out message
	* @param sw the switch is going to get this message
	* @param m the Flow-Mod or Packet-Out message
	* @return if this message should be sent to data-plane right now
	*/
	public static boolean handleFlowMod(IOFSwitch sw, OFMessage m) {
		if (start == false) {
			return true;
		}

		Long tid = Thread.currentThread().getId();
		String appName = getMessageSender();

		switch (m.getType().name()) {
			case "FLOW_MOD":
				OFFlowMod fm = (OFFlowMod) m;
				if(fm.getMatch().get(MatchField.ETH_TYPE) != null) {
					if (fm.getMatch().get(MatchField.ETH_TYPE).toString().equals("0x88cc")) {
						log.error("Service pollution error");
						return false;
					}
				}

				switch (appName) {
					case "net.floodlightcontroller.routing.ForwardingBase":
						break;
					case "net.floodlightcontroller.myapp.MyApp":
						Entry<IOFSwitch, OFMessage> tmp = new SimpleEntry<IOFSwitch, OFMessage>(sw, m);
						ArrayList<Entry<IOFSwitch, OFMessage>> list = flowMod.get(tid);
						if(list != null){
							list.add(tmp);
						} else {
							flowMod.put(tid, new ArrayList<Entry<IOFSwitch, OFMessage>>());
							list = flowMod.get(tid);
							list.add(tmp);
						}
						flowMod.put(tid, list);
						break;
					default:
						return true;
				}
				break;
			case "PACKET_OUT":
				/**
				* Intercept the Packet-Out message
				Entry<IOFSwitch, OFMessage> tmp = new SimpleEntry<IOFSwitch, OFMessage>(sw, m);
				packetOut.put(tid, tmp);
				*/
				break;
			default:
				return true;
		}

		return false;
	}

	/**
	* find the App who sends the Flow-Mod or Packet-Out
	* @return the App who sends the Flow-Mod or Packet-Out
	*/
	public static String getMessageSender() {
		String function;
		String sender = "";
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			function = ste.toString().split("\\(")[0];
			int cutPoint = function.indexOf(".receive");
			if(cutPoint != -1) {
				sender = function.substring(0, cutPoint);
			}
		}
		return sender;
	}


	// return value: false if there is not a flowMod
	public static boolean handleTransaction() throws JSONException {
		if (start == false) {
			return false;
		}
		Long tid = Thread.currentThread().getId();
		ArrayList<Entry<IOFSwitch, OFMessage>> list = flowMod.get(tid);

		if(list == null) {
//			installTransaction(tid);
			packetIn.remove(tid);
			packetOut.remove(tid);
			flowMod.remove(tid);
			return false;
		} else {
			packetIn.remove(tid);
			packetOut.remove(tid);
			flowMod.remove(tid);
			return true;
		}

//		HashMap<String, DiGraph> fmClass = new HashMap<String, DiGraph>();

//		for(Entry<IOFSwitch, OFMessage> entry : list) {
//			OFFlowMod fm = (OFFlowMod) entry.getValue(); // value is OFMessage originally
//			IOFSwitch sw = entry.getKey();
//			JSONObject matchObj =  new JSONObject();
//			String in_port = null;
//			try {
//				for(MatchField<?> field : fm.getMatch().getMatchFields()) {
//					if(field.getName().equals("in_port")) {
//						in_port = fm.getMatch().get(field).toString();
//						continue;
//					}
//					matchObj.put(field.getName(), fm.getMatch().get(field).toString());
//				}
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			DiGraph graph = fmClass.get(matchObj.toString());
//			if(graph == null){
//				graph = new DiGraph();
//				fmClass.put(matchObj.toString(), graph);
//			}
//
//			String target = sw.getId().toString();
//			String[] outputs = fm.getOutPort().toString().split(",");
//			if(in_port != null) {
//				Entry<String, String> in_start = new SimpleEntry<String, String>(target, in_port);
//				for(String port : outputs) {
//					Entry<String, String> in_end = new SimpleEntry<String, String>(target, port);
//					graph.addEdge(in_start, in_end);
//					JSONArray tuple = (JSONArray) ((JSONObject) topology.get(target)).get(port);
//					Entry<String, String> out_end = new SimpleEntry<String, String>(tuple.get(0).toString(), tuple.get(1).toString());
//					graph.addEdge(in_end, out_end);
//				}
//			} else {
//				Entry<String, String> in_start = new SimpleEntry<String, String>(target, "");
//				for(String port : outputs) {
//					Entry<String, String> in_end = new SimpleEntry<String, String>(target, port);
//					graph.addEdge(in_start, in_end);
//					JSONArray tuple = (JSONArray) ((JSONObject) topology.get(target)).get(port);
//					String out_switch = tuple.get(0).toString();
//					if(out_switch != null) {
//						Entry<String, String> out_end = new SimpleEntry<String, String>(out_switch, "");
//						graph.addEdge(in_end, out_end);
//					}
//				}
//			}
//		}
//		boolean flag = false;
//		for(String match : fmClass.keySet()) {
//			DiGraph graph = fmClass.get(match);
//			JSONObject matchObj = new JSONObject(match);
//			flag = verify(matchObj, graph);
//			if(!flag) {
//				log.info("Transaction error");
//				break;
//			}
//		}
//
//		if(flag) {
//			installTransaction(tid);
//		}
////		log.info(String.valueOf(list.size()) + " " + String.valueOf(System.nanoTime() - startStamp));
//		packetIn.remove(tid);
//		packetOut.remove(tid);
//		flowMod.remove(tid);
//		return true;
	}

	public static void installTransaction(Long tid) {
		ArrayList<Entry<IOFSwitch, OFMessage>> list = flowMod.get(tid);
		if(list != null) {
			for(Entry<IOFSwitch, OFMessage> entry : list) {
				IOFSwitch sw = entry.getKey();
				OFMessage m = entry.getValue();
				sw.write(m);
			}
		}
		Entry<IOFSwitch, OFMessage> tmp = packetOut.get(tid);
		if(tmp != null) {
			IOFSwitch sw = tmp.getKey();
			OFMessage m = tmp.getValue();
			sw.write(m);
		}
	}

	public static boolean verify(JSONObject match, DiGraph graph) throws JSONException {

		for(Entry<String, String> node : graph.getNodes()) {
			if(!graph.dfs(match, node, log)){
				return false;
			}
		}
		return true;
	}

	public static void recordTime(long front, long end) {
		if(!start && end != 0) {
			return;
		}
		String tmp1, tmp2;
		tmp1 = Thread.currentThread().getId() + " timestamp: " + String.valueOf(front) + "\n";
		tmp2 = Thread.currentThread().getId() + " duration: " + String.valueOf(end -front) + "\n";
		try {
			Files.write(Paths.get("overhead.log"), tmp1.getBytes(), StandardOpenOption.APPEND);
			Files.write(Paths.get("overhead.log"), tmp2.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void recordPacketIn(OFMessage m) {
		OFPacketIn pi = (OFPacketIn)m;
		Ethernet eth = new Ethernet();
        eth.deserialize(pi.getData(), 0, pi.getData().length);

		String msg = "PacketIn " + Thread.currentThread().getId() + eth.toString() + "\nover\n";
		try {
			Files.write(Paths.get("OFmessage.log"), msg.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void recordFlowMod(OFFlowMod fm, String swid) {
		String function;
		String caller = "";
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			function = ste.toString().split("\\(")[0];
			int cutPoint = function.indexOf(".receive");
			if(cutPoint != -1) {
				caller = function.substring(0, cutPoint);
			}
		}

		if(!caller.equals("net.floodlightcontroller.routing.ForwardingBase")) {
			return;
		}

		String msg = "FlowMod " + Thread.currentThread().getId() + " " + swid + "\n" + fm.getMatch() + " " + fm.getOutPort() + "\n";
		try {
			Files.write(Paths.get("OFmessage.log"), msg.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void recordTransaction() {
		String msg = "Transaction " + Thread.currentThread().getId() + "\n";
		try {
			Files.write(Paths.get("OFmessage.log"), msg.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

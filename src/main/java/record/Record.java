package record;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;

import net.floodlightcontroller.packet.Ethernet;

public class Record {
	
	/**
	 * Used to compute the detection time of CEAD
	 * @param start the time CEAD start to detection
	 * @param end the time CEAD complete the detection
	 */
	public static void recordTime(long start, long end) {
		String tmp1, tmp2;
		tmp1 = Thread.currentThread().getId() + " timestamp: " + String.valueOf(start) + "\n";
		tmp2 = Thread.currentThread().getId() + " duration: " + String.valueOf(end -start) + "\n";
		Path path = Paths.get("custom/overhead.log");
		if (Files.exists(path)) {
			try {
				Files.write(Paths.get("custom/overhead.log"), tmp1.getBytes(), StandardOpenOption.APPEND);
				Files.write(Paths.get("custom/overhead.log"), tmp2.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				Files.write(Paths.get("custom/overhead.log"), tmp1.getBytes(), StandardOpenOption.CREATE);
				Files.write(Paths.get("custom/overhead.log"), tmp2.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

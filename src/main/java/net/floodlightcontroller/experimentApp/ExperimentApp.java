/**
 * 
 */
package net.floodlightcontroller.experimentApp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.util.FlowModUtils;

/**
 * @author bootscat
 *
 */
public class ExperimentApp implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected static JSONParser parser = new JSONParser();
	protected static JSONObject rule; 
	protected IOFSwitchService switchService;
	

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IListener#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return ExperimentApp.class.getSimpleName();
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IListener#isCallbackOrderingPrereq(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return (type.equals(OFType.PACKET_IN) && (name.equals("forwarding")));
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IListener#isCallbackOrderingPostreq(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#getModuleServices()
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#getServiceImpls()
	 */
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#getModuleDependencies()
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;

	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#init(net.floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
	    macAddresses = new ConcurrentSkipListSet<Long>();
	    logger = LoggerFactory.getLogger(ExperimentApp.class);
	    try {
			rule = (JSONObject) parser.parse(new FileReader("custom/rule.json"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#startUp(net.floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFMessageListener#receive(net.floodlightcontroller.core.IOFSwitch, org.projectfloodlight.openflow.protocol.OFMessage, net.floodlightcontroller.core.FloodlightContext)
	 */
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		// TODO Auto-generated method stub
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		String key = eth.getEtherType().toString() + eth.getSourceMACAddress().toString() + eth.getDestinationMACAddress().toString();

		JSONArray flows = (JSONArray) rule.get(key);
		if (flows == null) {
			return Command.CONTINUE;
		}
		for (int i = 0, size=flows.size(); i<size; i++) {
			JSONObject flow = (JSONObject) flows.get(i);
			OFFactory OF13 = OFFactories.getFactory(OFVersion.OF_13);
			Match.Builder mb = OF13.buildMatch();
			if (eth.getEtherType().toString().equals("0x806")) {
				mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
				
			} else {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				mb.setExact(MatchField.IPV4_SRC, IPv4Address.of(flow.get("ipv4_src").toString()));
				mb.setExact(MatchField.IPV4_DST, IPv4Address.of(flow.get("ipv4_dst").toString()));
			}
			mb.setExact(MatchField.ETH_SRC, eth.getSourceMACAddress());
			mb.setExact(MatchField.ETH_DST, eth.getDestinationMACAddress());
			mb.setExact(MatchField.IN_PORT, OFPort.of(Integer.valueOf(flow.get("in_port").toString())));
			
			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
			OFActions actions = OF13.actions();
			OFActionOutput output = actions.buildOutput()
					.setMaxLen(Integer.MAX_VALUE)
					.setPort(OFPort.of(Integer.valueOf(flow.get("output").toString())))
					.build();
			actionList.add(output);
			OFFlowMod.Builder fmb = OF13.buildFlowAdd();
			fmb.setMatch(mb.build())
			.setIdleTimeout(5)
			.setHardTimeout(0)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(U64.of(10))
			.setOutPort(OFPort.of(Integer.valueOf(flow.get("output").toString())))
			.setPriority(1);
			FlowModUtils.setActions(fmb, actionList, sw);
			fmb.setTableId(TableId.ZERO);
			IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(flow.get("dpid").toString()));
			mySwitch.write(fmb.build());
		}
		return Command.CONTINUE;
	}

}

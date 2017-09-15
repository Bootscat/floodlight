package veriflow;

import net.floodlightcontroller.core.IOFSwitch;

public class RuleObject {
	public IOFSwitch sw = null;
	public String[] outputs = null;
	public String in_port = null;
	
	public RuleObject(IOFSwitch ofSwitch, String in, String[] outs) {
		sw = ofSwitch;
		in_port = in;
		outputs = outs;
	}
}

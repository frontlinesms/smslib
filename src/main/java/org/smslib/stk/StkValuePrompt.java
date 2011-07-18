package org.smslib.stk;

public class StkValuePrompt extends StkPrompt {
	public static final StkRequest REQUEST = new StkRequest() {};

	@Override
	public StkRequest getRequest() {
		return REQUEST;
	}
}

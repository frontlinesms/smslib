package org.smslib.stk;

public class StkValuePrompt extends StkPrompt {
	public static final StkRequest REQUEST = new StkRequest() {};
	
	public StkValuePrompt(String promptText) {
		super(promptText);
	}

	@Override
	public StkRequest getRequest() {
		return REQUEST;
	}
}

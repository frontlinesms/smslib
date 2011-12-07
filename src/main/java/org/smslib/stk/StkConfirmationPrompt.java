package org.smslib.stk;

public class StkConfirmationPrompt extends StkPrompt {
	public static final StkRequest REQUEST = new StkRequest() {};
	
	public StkConfirmationPrompt(String promptText) {
		super(promptText);
	}
	
	@Override
	public StkRequest getRequest() {
		return REQUEST;
	}
}

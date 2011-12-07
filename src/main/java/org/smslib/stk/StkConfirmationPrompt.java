package org.smslib.stk;

public class StkConfirmationPrompt extends StkPrompt {
	public StkConfirmationPrompt(String promptText) {
		super(promptText);
	}
	
	@Override
	public StkRequest getRequest() {
		return new StkRequest() {};
	}
}

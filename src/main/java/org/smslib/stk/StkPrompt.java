package org.smslib.stk;

public abstract class StkPrompt implements StkResponse {
	private final String text;
	
	public StkPrompt(String promptText) {
		this.text = promptText;
	}

	public abstract StkRequest getRequest();

	public String getText() {
		return this.text;
	}
}

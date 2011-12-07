package org.smslib.stk;

public abstract class StkPrompt implements StkResponse {
	private final String promptText;

	public StkPrompt() {
		this("<NOT SET>");
	}
	
	public StkPrompt(String promptText) {
		this.promptText = promptText;
	}

	public abstract StkRequest getRequest();

	public String getPromptText() {
		return this.promptText;
	}
}

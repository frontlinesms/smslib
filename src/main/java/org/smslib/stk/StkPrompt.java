package org.smslib.stk;

public abstract class StkPrompt implements StkResponse {
	private String promptText;

	public abstract StkRequest getRequest();

	public String getPromptText() {
		return this.promptText;
	}
}

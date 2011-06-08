package org.smslib.stk;

public class StkMenuItem extends StkRequest {
	private final String text;

	public StkMenuItem(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}

	public StkRequest getRequest() {
		return new StkRequest();
	}
}

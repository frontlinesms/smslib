package org.smslib.stk;

public class StkNotification implements StkResponse {
	private final String text;
	
	public StkNotification(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}

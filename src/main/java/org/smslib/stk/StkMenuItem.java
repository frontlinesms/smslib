package org.smslib.stk;

public class StkMenuItem {
	private final String text;

	public StkMenuItem(String text) {
		this.text = text;
	}
	
	public StkMenuItem(String text, int i, int j) {
		this(text);
	}
	
	public String getText() {
		return text;
	}

	public StkRequest getRequest() {
		// TODO Auto-generated method stub
		return null;
	}
}

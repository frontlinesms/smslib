package org.smslib.ussd;

public class ActionableUssdResponse implements UssdResponse {
	private final String text;
	
	public ActionableUssdResponse(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
}

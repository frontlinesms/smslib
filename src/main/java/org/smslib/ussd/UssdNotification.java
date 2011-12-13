package org.smslib.ussd;

public class UssdNotification implements UssdResponse {
	private String text;

	public UssdNotification(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
}

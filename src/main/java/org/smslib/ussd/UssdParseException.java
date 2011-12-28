package org.smslib.ussd;

import org.apache.commons.lang3.StringEscapeUtils;

@SuppressWarnings("serial")
public class UssdParseException extends Exception {
	public UssdParseException(String response) {
		super(createMessage(response));
	}

	public UssdParseException(String response, Throwable cause) {
		super(createMessage(response), cause);
	}

	private static String createMessage(String response) {
		return "Error parsing response: " + StringEscapeUtils.escapeJava(response);
	}
}

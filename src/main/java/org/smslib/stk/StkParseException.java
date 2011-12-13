package org.smslib.stk;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

@SuppressWarnings("serial")
public class StkParseException extends Exception {
	public StkParseException(String parsingResponse) {
		super("Could not parse response: " + escapeJava(parsingResponse));
	}
}

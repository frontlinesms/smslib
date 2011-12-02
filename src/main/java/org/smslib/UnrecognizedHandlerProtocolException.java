package org.smslib;

import org.smslib.service.Protocol;

@SuppressWarnings("serial")
public class UnrecognizedHandlerProtocolException extends SMSLibDeviceException {
	public UnrecognizedHandlerProtocolException(Protocol protocol) {
		super("Unrecognized message protocol: " + protocol.name());
	}
}

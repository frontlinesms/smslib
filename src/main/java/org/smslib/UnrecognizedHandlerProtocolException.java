package org.smslib;

@SuppressWarnings("serial")
public class UnrecognizedHandlerProtocolException extends SMSLibDeviceException {
	public UnrecognizedHandlerProtocolException(CService.Protocol protocol) {
		super("Unrecognized message protocol: " + protocol.name());
	}
}

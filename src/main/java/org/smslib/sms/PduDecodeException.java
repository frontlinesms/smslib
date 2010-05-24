/**
 * 
 */
package org.smslib.sms;

/**
 * Exception thrown while trying to decode a {@link PduComponent} or {@link SmsDeliverPdu} itself.
 * @author Alex
 */
@SuppressWarnings("serial")
public class PduDecodeException extends Exception {

//> STATIC CONSTANTS

//> INSTANCE PROPERTIES

//> CONSTRUCTORS
	/** @see Exception#Exception(Throwable) */
	public PduDecodeException(Throwable cause, String... pdus) {
		super("PDUs: " + prettify(pdus), cause);
	}

	/** @see Exception#Exception(String) */
	public PduDecodeException(String message) {
		super(message);
	}

//> ACCESSORS

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS
	private static final String prettify(String... pdus) {
		StringBuilder pduList = new StringBuilder();
		for(String pdu : pdus) {
			pduList.append(", ");
			pduList.append(pdu);
		}
		return "{" + pduList.toString().substring(2) + "}";
	}
}

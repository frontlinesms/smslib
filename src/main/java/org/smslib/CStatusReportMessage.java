// SMSLib for Java
// An open-source API Library for sending and receiving SMS via a GSM modem.
// Copyright (C) 2002-2007, Thanasis Delenikas, Athens/GREECE
// Web Site: http://www.smslib.org
//
// SMSLib is distributed under the LGPL license.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

package org.smslib;

import org.smslib.sms.PduInputStream;

public class CStatusReportMessage extends CIncomingMessage {
	/**
	 * Holds values representing the delivery status of a previously sent message.
	 */
	public static class DeliveryStatus {
		/** Unknown. */
		public static final int Unknown = 0;
		/** Message was delivered. */
		public static final int Delivered = 1;
		/** Message was not delivered, but the SMSC will keep trying. */
		public static final int KeepTrying = 2;
		/** Message was not delivered and the SMSC will abort it. */
		public static final int Aborted = 3;
	}
	
//> INSTANCE PROPERTIES
	/** (Presumably) the date the original message this refers to was sent */
	private long dateOriginal;
	/** The date this status report was received/or when it was generated? */
	private long dateReceived;
	/** Status that this report represents */
	private int status;

//> CONSTRUCTORS
	protected CStatusReportMessage(int refNo, int memIndex, String memLocation, long dateOriginal, long dateReceived)
	{
		super(MessageType.StatusReport, memIndex, memLocation);

		this.refNo = refNo;
		this.dateOriginal = dateOriginal;
		this.dateReceived = dateReceived;
		messageText = "";
		status = DeliveryStatus.Unknown;
	}
	
	/**
	 * Decodes the supplied SMS PDU to create a status report.
	 * @param pdu
	 * @param memIndex
	 * @param memLocation
	 * @param checkForSmsc sometimes the PDU does not start with SMSC number.  Not sure if the
	 *   spec allows for this or not, but it certainly happens (August 2012) on Safaricom
	 *   Kenya, so it seems sensible to allow for it.
	 * @throws MessageDecodeException 
	 */
	protected CStatusReportMessage(String pdu, int memIndex, String memLocation, boolean checkForSmsc) throws MessageDecodeException {
		super(MessageType.StatusReport, memIndex, memLocation);

		try {
			PduInputStream in = new PduInputStream(pdu);
			
			// The SMSC address.  Not used here.
			if(checkForSmsc) in.readSmscAddress();
			
			// This byte contains header information etc. and TODO should be decoded
			in.readByte();
			
			// TP-Message-Reference (TP-MR):
			// Parameter identifying the previously submitted SMS-SUBMIT or SMS-COMMAND
			refNo = in.read();
			
			// TP-Recipient-Address (TP-RA):
			// Address of the recipient of the previously submitted mobile originated short message
			recipient = in.readAddress();

			// TP-Service-Centre-Time-Stamp (TP-SCTS):
			// Parameter identifying time when the SC received the previously sent SMS-SUBMIT
			// 7 octets
			dateOriginal = in.readTimestamp();
			
			// TP-Discharge-Time (TP-DT):
			// Parameter identifying the time associated with a particular TP-ST outcome
			// 7 octets
			dateReceived = in.readTimestamp();
			
			// TP-Status (TS-ST):
			// Parameter identifying the status of the previously sent mobile originated short message
			int i = in.readByte();
			
			switch((i >> 5) & 3) {
				case 0:
					messageText = "00 - Succesful Delivery.";
					status = DeliveryStatus.Delivered;
					break;
				case 1:
					messageText = "01 - Errors, will retry dispatch.";
					status = DeliveryStatus.KeepTrying;
					break;
				case 2:
					messageText = "02 - Errors, stopped retrying dispatch.";
					status = DeliveryStatus.Aborted;
					break;
				case 3:
					messageText = "03 - Errors, stopped retrying dispatch.";
					status = DeliveryStatus.Aborted;
					break;
			}
			
			// TODO check if TP-UD is expected and if so read it
			// TODO in.checkEmpty(); // N.B. this check is not applicable if TP-UD is expected
		} catch(Exception ex) {
			throw new MessageDecodeException("Error decoding PDU: " + pdu, ex);
		}
	}
	
//> ACCESSORS
	/**
	 * FIXME why does {@link #getOriginator()} return {@link CMessage#recipient}???
	 * @return the MSISDN this report refers to
	 */
	public String getOriginator() {
		return recipient;
	}

	/**
	 * Returns the delivery status flag.
	 * 
	 * @return The status flag.
	 */
	public int getDeliveryStatus() {
		return status;
	}

	/**
	 * Returns the date of the original SMS message for which a status report was requested.
	 * 
	 * @return The date of the original SMS message.
	 */
	public long getDateOriginal() {
		return this.dateOriginal;
	}

	/**
	 * Returns the date when the original SMS message was received by recipient.
	 * 
	 * @return The date when the message was received.
	 */
	public long getDateReceived() {
		return this.dateReceived;
	}
}

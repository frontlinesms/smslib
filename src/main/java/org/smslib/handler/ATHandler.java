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

package org.smslib.handler;

import java.io.IOException;

import org.smslib.CService;
import org.smslib.service.MessageClass;
import org.smslib.service.Protocol;
import org.smslib.NoResponseException;
import org.smslib.SMSLibDeviceException;
import org.smslib.UnrecognizedHandlerProtocolException;

import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;

public interface ATHandler {
	public abstract class SynchronizedWorkflow<T> {
		public abstract T run() throws SMSLibDeviceException, IOException;
	}

	void setStorageLocations(String loc);

	boolean dataAvailable() throws IOException;

	void sync() throws IOException;

	void reset() throws IOException;

	void echoOff() throws IOException;

	void init() throws IOException;

	boolean isAlive() throws IOException;

	/**
	 * Issues the AT Command to check if the device is waiting for a PIN or PUK
	 * @return The response from the AT Handler, verbatim
	 * @throws IOException
	 */
	String getPinResponse() throws IOException;

	/**
	 * Check the supplied response to the PIN AT command to see if a PIN is required.
	 * @param commandResponse
	 * @return <code>true</code> if a PIN is being waited for; <code>false</code> otherwise
	 */
	boolean isWaitingForPin(String commandResponse);
	/**
	 * Check the supplied response to the PIN AT command to see if a PIN2 is required.
	 * @param commandResponse
	 * @return <code>true</code> if a PIN2 is being waited for; <code>false</code> otherwise
	 */
	boolean isWaitingForPin2(String commandResponse);

	/**
	 * Check the supplied response to the PIN AT command to see if a PUK is required.
	 * @param commandResponse
	 * @return <code>true</code> if a PIN is being waited for; <code>false</code> otherwise
	 */	
	boolean isWaitingForPuk(String commandResponse);

	boolean enterPin(String pin) throws IOException;

	boolean setVerboseErrors() throws IOException;

	boolean setPduMode() throws IOException;

	boolean setTextMode() throws IOException;

	boolean enableIndications() throws IOException;

	boolean disableIndications() throws IOException;

	String getManufacturer() throws IOException;

	String getModel() throws IOException;
	
	String getMsisdn() throws IOException;

	String getSerialNo() throws IOException;

	String getImsi() throws IOException;

	String getSwVersion() throws IOException;

	String getBatteryLevel() throws IOException;

	String getSignalLevel() throws IOException;

	boolean setMemoryLocation(String mem) throws IOException;

	/**
	 * Switches the serial communication mode from data mode to command mode.  Command
	 * mode allows the sending of AT commands.
	 * @throws IOException if there was a problem with the serial connection
	 */
	void switchToCmdMode() throws IOException;

	boolean keepGsmLinkOpen() throws IOException;

	/**
	 * This method is called to send an SMS.  It can be used with {@link CService.Protocol#PDU} or
	 * {@link CService.Protocol#TEXT}, but nothing else.
	 * @param size the "size" of the message when using {@link CService.Protocol#PDU}
	 * @param pdu the PDU of the message when using {@link CService.Protocol#PDU}
	 * @param phone the target phone number for the message when using {@link CService.Protocol#TEXT}
	 * @param text the textual content of the message when using {@link CService.Protocol#TEXT}
	 * @return the SMSC reference number for the message, or {@link #SMSC_REF_NUMBER_SEND_FAILED} if there was a problem sending the message.
	 * @throws IOException
	 * @throws NoResponseException
	 * @throws UnrecognizedHandlerProtocolException
	 */
	int sendMessage(int size, String pdu, String phone, String text) throws IOException, NoResponseException, UnrecognizedHandlerProtocolException;

	String listMessages(MessageClass messageClass) throws IOException, UnrecognizedHandlerProtocolException, SMSLibDeviceException;

	boolean deleteMessage(int memIndex, String memLocation) throws IOException;

	String getGprsStatus() throws IOException;

	String getNetworkRegistration() throws IOException;

	String getStorageLocations();
	void initStorageLocations() throws IOException;

	/**
	 * Checks whether this AT Handler has support for receiving SMS messages
	 * @return true if this AT handler supports receiving of SMS messages.
	 */
	boolean supportsReceive();

	/**
	 * Checks whether this AT Handler has support for sending SMS binary messages
	 * @return true if this AT handler supports sending of SMS binary message.
	 */
	boolean supportsBinarySmsSending();

	/**
	 * Checks whether this AT Handler has support for sending UCS-2 encoded text messages.
	 * @return true if this AT handler supports sending UCS-2 encoded text messages.
	 */
	boolean supportsUcs2SmsSending();
	
	Protocol getProtocol();

	boolean supportsStk();
	
	void stkInit() throws SMSLibDeviceException, IOException;
	
	StkResponse stkRequest(StkRequest request, String... variables) throws SMSLibDeviceException, IOException;

	void configureModem() throws SMSLibDeviceException, IOException;
}

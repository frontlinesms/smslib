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

import java.io.IOException;

import org.smslib.CService.MessageClass;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;

abstract public class AbstractATHandler {
	abstract protected void setStorageLocations(String loc);

	abstract protected boolean dataAvailable() throws IOException;

	abstract protected void sync() throws IOException;

	abstract protected void reset() throws IOException;

	abstract protected void echoOff() throws IOException;

	abstract protected void init() throws IOException;

	abstract protected boolean isAlive() throws IOException;

	/**
	 * Issues the AT Command to check if the device is waiting for a PIN or PUK
	 * @return The response from the AT Handler, verbatim
	 * @throws IOException
	 */
	protected abstract String getPinResponse() throws IOException;

	/**
	 * Check the supplied response to the PIN AT command to see if a PIN is required.
	 * @param commandResponse
	 * @return <code>true</code> if a PIN is being waited for; <code>false</code> otherwise
	 */
	protected abstract boolean isWaitingForPin(String commandResponse);
	/**
	 * Check the supplied response to the PIN AT command to see if a PIN2 is required.
	 * @param commandResponse
	 * @return <code>true</code> if a PIN2 is being waited for; <code>false</code> otherwise
	 */
	protected abstract boolean isWaitingForPin2(String commandResponse);

	/**
	 * Check the supplied response to the PIN AT command to see if a PUK is required.
	 * @param commandResponse
	 * @return <code>true</code> if a PIN is being waited for; <code>false</code> otherwise
	 */	
	protected abstract boolean isWaitingForPuk(String commandResponse);

	abstract protected boolean enterPin(String pin) throws IOException;

	abstract protected boolean setVerboseErrors() throws IOException;

	abstract protected boolean setPduMode() throws IOException;

	abstract protected boolean setTextMode() throws IOException;

	abstract protected boolean enableIndications() throws IOException;

	abstract protected boolean disableIndications() throws IOException;

	abstract protected String getManufacturer() throws IOException;

	abstract protected String getModel() throws IOException;
	
	abstract protected String getMsisdn() throws IOException;

	abstract protected String getSerialNo() throws IOException;

	abstract protected String getImsi() throws IOException;

	abstract protected String getSwVersion() throws IOException;

	abstract protected String getBatteryLevel() throws IOException;

	abstract protected String getSignalLevel() throws IOException;

	abstract protected boolean setMemoryLocation(String mem) throws IOException;

	/**
	 * Switches the serial communication mode from data mode to command mode.  Command
	 * mode allows the sending of AT commands.
	 * @throws IOException if there was a problem with the serial connection
	 */
	abstract protected void switchToCmdMode() throws IOException;

	abstract protected boolean keepGsmLinkOpen() throws IOException;

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
	abstract protected int sendMessage(int size, String pdu, String phone, String text) throws IOException, NoResponseException, UnrecognizedHandlerProtocolException;

	abstract protected String listMessages(MessageClass messageClass) throws IOException, UnrecognizedHandlerProtocolException, SMSLibDeviceException;

	abstract protected boolean deleteMessage(int memIndex, String memLocation) throws IOException;

	abstract protected String getGprsStatus() throws IOException;

	abstract protected String getNetworkRegistration() throws IOException;

	abstract protected String getStorageLocations();
	abstract protected void initStorageLocations() throws IOException;

	/**
	 * Checks whether this AT Handler has support for receiving SMS messages
	 * @return true if this AT handler supports receiving of SMS messages.
	 */
	abstract protected boolean supportsReceive();

	/**
	 * Checks whether this AT Handler has support for sending SMS binary messages
	 * @return true if this AT handler supports sending of SMS binary message.
	 */
	protected boolean supportsBinarySmsSending() {
		return true;
	}

	/**
	 * Checks whether this AT Handler has support for sending UCS-2 encoded text messages.
	 * @return true if this AT handler supports sending UCS-2 encoded text messages.
	 */
	public abstract boolean supportsUcs2SmsSending();
	
	protected abstract CService.Protocol getProtocol();

	public abstract boolean supportsStk();
	
	public abstract StkResponse stkRequest(StkRequest request, String... variables) throws SMSLibDeviceException;
}

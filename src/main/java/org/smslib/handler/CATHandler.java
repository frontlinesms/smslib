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
import java.util.*;

import org.smslib.*;
import org.smslib.CService.MessageClass;
import org.smslib.stk.NoStkSupportException;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;
import org.apache.log4j.*;

public class CATHandler implements ATHandler {
	/**
	 * Exceptionally fine logging level used when troubleshooting low-level problems with AT devices.
	 * FIXME remove this and disable it
	 */
	//private static final boolean TRACE = false;
	private static final boolean TRACE = true;
	/** The value returned by {@link #sendMessage(int, String, String, String)} instead of a valid
	 * SMSC reference number when sending a message failed. */
	protected static final int SMSC_REF_NUMBER_SEND_FAILED = -1;
	
	protected static final int DELAY_AT = 200;
	protected static final int DELAY_RESET = 20000;
	protected static final int DELAY_PIN = 12000;
	protected static final int DELAY_CMD_MODE = 1000;
	protected static final int DELAY_CMGS = 300;
	
	/** Character used to terminate a line after an AT command */
	protected static final String END_OF_LINE = "\r";
	
	/** AT Command for switching echo off */
	private static final String AT_ECHO_OFF = "ATE0";
	/** AT Command for retrieving the IMSI number of the connected device */
	private static final String AT_IMSI = "AT+CIMI";
	/** AT Command for retrieving the network registration status */
	private static final String AT_NETWORK_REGISTRATION = "AT+CREG?";
	/** AT Command for retrieving the MSISDN of the connected device */
	private static final String AT_GET_MSISDN = "CNUM";
	/** AT Command to retrieve the GPRS status */
	private static final String AT_GPRS_STATUS = "AT+CGATT?";
	/** AT Command to retrieve the battery level */
	private static final String AT_BATTERY = "AT+CBC";

//> INSTANCE VARIABLES
	protected CSerialDriver serialDriver;
	protected Logger log;
	protected String storageLocations = "";
	protected CService srv;

//> CONSTRUCTORS
	public CATHandler(CSerialDriver serialDriver, Logger log, CService srv) {
		this.serialDriver = serialDriver;
		this.log = log;
		this.srv = srv;
	}
	
	public String getStorageLocations() {
		return this.storageLocations;
	}

	public void setStorageLocations(String loc) {
		storageLocations = loc;
	}

	public boolean dataAvailable() throws IOException {
		return serialDriver.dataAvailable();
	}

	public void sync() throws IOException {
		for(int i=0; i<4; ++i) {
			sleepWithoutInterruption(DELAY_AT);
			serialDriver.send("AT\r");
		}
		sleepWithoutInterruption(DELAY_AT);
	}

	public void reset() throws IOException {}

	public void echoOff() throws IOException {
		serialSendReceive(AT_ECHO_OFF);
	}

	public void init() throws IOException {
		serialSendReceive("AT+CLIP=1");
		serialSendReceive("AT+COPS=0");
		// No need for the following, until full GPRS functionality is implemented.
		//serialSendReceive("AT+CGATT=1");
		serialDriver.emptyBuffer();
	}

	public boolean isAlive() throws IOException {
		String response = serialSendReceive("AT");
		return response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s");
	}
	
	public String getPinResponse() throws IOException {
		return serialSendReceive("AT+CPIN?");
	}
	
	public boolean isWaitingForPin(String commandResponse) {
		return commandResponse.contains("SIM PIN");
	}
	
	/**
	 * Added due to confusion about the implementation of PIN checking in {@link CService}
	 * @author alex@frontlinesms.com
	 */
	public boolean isWaitingForPin2(String commandResponse) {
		return commandResponse.contains("SIM PIN2");
	}

	public boolean isWaitingForPuk(String commandResponse) {
		return commandResponse.contains("SIM PUK");
	}

	public boolean enterPin(String pin) throws IOException {
		serialDriver.send(CUtils.replace("AT+CPIN=\"{1}\"\r", "{1}", pin));
		sleepWithoutInterruption(DELAY_PIN);
		if(serialDriver.getResponse().contains("OK")) {
			sleepWithoutInterruption(DELAY_PIN);
			return true;
		} else return false;
	}

	public boolean setVerboseErrors() throws IOException {
		String response = serialSendReceive("AT+CMEE=1");
		return response.matches("\\s+OK\\s+");
	}

	public boolean setPduMode() throws IOException {
		String response = serialSendReceive("AT+CMGF=0");
		return response.matches("\\s+OK\\s+");
	}

	public boolean setTextMode() throws IOException {
		String response = serialSendReceive("AT+CMGF=1");
		if(response.matches("\\s+OK\\s+")) {
			response = serialSendReceive("AT+CSCS=\"HEX\"");
			return response.matches("\\s+OK\\s+");
		} else return false;
	}

	public boolean enableIndications() throws IOException {
		String response = serialSendReceive("AT+CNMI=1,1,0,0,0");
		return response.matches("\\s+OK\\s+");
	}

	public boolean disableIndications() throws IOException {
		String response = serialSendReceive("AT+CNMI=0,0,0,0,0");
		return response.matches("\\s+OK\\s+");
	}

	public String getManufacturer() throws IOException {
		return executeATCommand("CGMI", true);
	}

	public String getModel() throws IOException {
		return executeATCommand("CGMM", true);
	}
	
	public String getMsisdn() throws IOException {
		return executeATCommand(AT_GET_MSISDN, true);
	}

	public String getSerialNo() throws IOException {
		return executeATCommand("CGSN", true);
	}

	public String getImsi() throws IOException {
		return serialSendReceive(AT_IMSI);
	}

	public String getSwVersion() throws IOException {
		return serialSendReceive("AT+CGMR");
	}

	public String getBatteryLevel() throws IOException {
		return serialSendReceive(AT_BATTERY);
	}

	public String getSignalLevel() throws IOException {
		return serialSendReceive("AT+CSQ");
	}

	public boolean setMemoryLocation(String mem) throws IOException {
		String response = serialSendReceive("AT+CPMS=\"" + mem + "\"");
		return response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s");
	}

	public void switchToCmdMode() throws IOException {
		serialDriver.send("+++" + END_OF_LINE);
		sleepWithoutInterruption(DELAY_CMD_MODE);
	}

	public boolean keepGsmLinkOpen() throws IOException {
		String response = serialSendReceive("AT+CMMS=1");
		return response.matches("\\s+OK\\s+");
	}
	
	private int sendMessage_PDU(int size, String pdu) throws IOException, NoResponseException {
		int smscReferenceNumber;
		int errorRetries = 0;
		while (true) {
			int responseRetries = 0;
			serialDriver.send(CUtils.replace("AT+CMGS=\"{1}\"\r", "\"{1}\"", "" + size));
			sleepWithoutInterruption(DELAY_CMGS);
			while (!serialDriver.dataAvailable()) {
				responseRetries++;
				if (responseRetries == srv.getRetriesNoResponse()) throw new NoResponseException();
				if (log != null) log.warn("ATHandler().SendMessage(): Still waiting for response (I) (" + responseRetries + ")...");
				sleepWithoutInterruption(srv.getDelayNoResponse());
			}
			responseRetries = 0;
			serialDriver.clearBuffer();
			serialDriver.send(pdu);
			serialDriver.send((char) 26);
			String response = serialDriver.getResponse();
			while(response.length() == 0) {
				responseRetries++;
				if (responseRetries == srv.getRetriesNoResponse()) throw new NoResponseException();
				if (log != null) log.warn("ATHandler().SendMessage(): Still waiting for response (II) (" + responseRetries + ")...");
				sleepWithoutInterruption(srv.getDelayNoResponse());
				response = serialDriver.getResponse();
			}
			if (response.indexOf("OK\r") >= 0) {
				smscReferenceNumber = getMessageReferenceNumberFromResponse(response);
				break;
			} else {
				if (response.toUpperCase().indexOf("ERROR") >= 0) {
					String err = response.replaceAll("\\s+", "");
					AtCmsError.log(log, err, pdu);
				} else {
					log.info("Could not understand response to AT+CMGS; treating as error: " + response);
				}
				
				if (++errorRetries >= srv.getRetriesCmsErrors()) {
					if (log != null) log.error("Quit retrying, message lost...");
					smscReferenceNumber = SMSC_REF_NUMBER_SEND_FAILED;
					break;
				} else {
					if (log != null) log.warn("Retrying...");
					sleepWithoutInterruption(srv.getDelayCmsErrors());
				}
			}
		}
		return smscReferenceNumber;
	}
	
	private int sendMessage_TEXT(String phone, String text) throws IOException {
		String cmd1 = CUtils.replace("AT+CMGS=\"{1}\"\r", "{1}", phone);
		serialDriver.send(cmd1);
		serialDriver.emptyBuffer();
		serialDriver.send(text);
		sleepWithoutInterruption(DELAY_CMGS);
		serialDriver.send((byte) 26);
		String response = serialDriver.getResponse();
		if (response.indexOf("OK\r") >= 0) {
			return getMessageReferenceNumberFromResponse(response);
		} else {
			return SMSC_REF_NUMBER_SEND_FAILED;
		}
	}

	/** Sends an SMS message and retrieves the SMSC reference number assigned to it. */
	public int sendMessage(int size, String pdu, String phone, String text) throws IOException, NoResponseException, UnrecognizedHandlerProtocolException {
		int smscReferenceNumber;
		CService.Protocol messageProtocol = srv.getProtocol();
		switch(messageProtocol) {
			case PDU:
				smscReferenceNumber = sendMessage_PDU(size, pdu);
				break;
			case TEXT:
				smscReferenceNumber = sendMessage_TEXT(phone, text);
				break;
			default:
				throw new UnrecognizedHandlerProtocolException(messageProtocol);
		}
		return smscReferenceNumber;
	}
	
	/**
	 * Helper method for retrieving the SMSC reference number of a message from a serial response from the phone after sending.
	 * @param response
	 * @return
	 */
	private static int getMessageReferenceNumberFromResponse(String response) {
		StringBuilder bob = new StringBuilder(4);
		int i = response.indexOf(":");
		while (!Character.isDigit(response.charAt(i)))
			++i;
		while (Character.isDigit(response.charAt(i))) {
			bob.append(response.charAt(i));
			++i;
		}
		return Integer.parseInt(bob.toString());		
	}

	public String listMessages(MessageClass messageClass) throws IOException, UnrecognizedHandlerProtocolException, SMSLibDeviceException {
		if(TRACE) System.out.println("CATHandler.listMessages() : " + this.getClass().getSimpleName());
		
		CService.Protocol messageProtocol = srv.getProtocol();
		switch (messageProtocol) {
			case PDU:
				return serialSendReceive("AT+CMGL=" + messageClass.getPduModeId());
			case TEXT:
				return serialSendReceive("AT+CMGL=\"" + messageClass.getTextId() + "\"");
			default:
				throw new UnrecognizedHandlerProtocolException(messageProtocol);
		}
	}

	public boolean deleteMessage(int memIndex, String memLocation) throws IOException {
		if (!setMemoryLocation(memLocation)) throw new RuntimeException("CATHandler.deleteMessage() : Memory Location not found!!!");
		String response = serialSendReceive(CUtils.replace("AT+CMGD={1}", "{1}", "" + memIndex));
		return response.matches("\\s+OK\\s+");
	}

	public String getGprsStatus() throws IOException {
		return serialSendReceive(AT_GPRS_STATUS);
	}

	public String getNetworkRegistration() throws IOException {
		return serialSendReceive(AT_NETWORK_REGISTRATION);
	}

	/** @see ATHandler#getStorageLocations() */
	public void initStorageLocations() throws IOException {
		String response = serialSendReceive("AT+CPMS?");
		if(response.contains("+CPMS:")) {
			response = response.replaceAll("\\s*\\+CPMS:\\s*", "");
			StringTokenizer tokens = new StringTokenizer(response, ",");
			while(tokens.hasMoreTokens()) {
				String loc = tokens.nextToken().replace("\"", "");
				if(!storageLocations.contains(loc)) storageLocations += loc;
				tokens.nextToken();
				tokens.nextToken();
			}
		}
	}
	
	/**
	 * Writes a string to the serial driver, appends a {@link #END_OF_LINE}, and retrieves the response.
	 * @param command The string to send to the serial device
	 * @return The response to the issued command, verbatim
	 * @throws IOException if access to {@link ATHandler#serialDriver} throws an {@link IOException}
	 */
	public String serialSendReceive(String command) throws IOException {
		if(TRACE) log.info("ISSUING COMMAND: " + command);
		if(TRACE) System.out.println("[" + Thread.currentThread().getName() + "] ISSUING COMMAND: " + command);
		serialDriver.send(command + END_OF_LINE);
		String response = serialDriver.getResponse();
		if(TRACE) log.info("RECEIVED RESPONSE: " + response);
		if(TRACE) System.out.println("[" + Thread.currentThread().getName() + "] RECEIVED RESPONSE: " + response);
		return response;
	}

	/**
	 * Writes an AT command to the serial driver and retrieves the response.  The supplied
	 * command will be prepended with "AT+" and appended with a \r.  If requested, any
	 * presence of the command in the response will be removed.
	 * @param command
	 * @param removeCommand If set true, the command is removed from the response.
	 * @return the response to the issued command
	 * @throws IOException If there was an issue contacting the serial port
	 */
	private String executeATCommand(String command, boolean removeCommand) throws IOException {
		// Issue the command
		String response = serialSendReceive("AT+"+command);
		
		// If requested, remove the command we issued from the response string
		if(removeCommand) {
			response = response.replaceAll("\\s*(AT)?\\+" + command + "\\s*", "");
		}
		
		return response;
	}

	/**
	 * Make the thread sleep until it's slept the requested amount.
	 * TODO this seems a dangerous practice.  If the thread's been interrupted, it should probably be allowed to die.
	 * @param millis
	 */
	public static void sleepWithoutInterruption(long millis) {
		while(millis > 0) {
			long startTime = System.currentTimeMillis();
			try {
				Thread.sleep(millis);
			} catch(InterruptedException ex) {}
			millis -= (System.currentTimeMillis() - startTime);
		}
	}
	
	/** This method should be overridden by AT Handlers that only support sending. */
	public boolean supportsReceive() {
		return true;
	}

	public boolean supportsUcs2SmsSending() {
		return true;
	}
	
	public boolean supportsBinarySmsSending() {
		return true;
	}

	public CService.Protocol getProtocol() {
		return CService.Protocol.PDU;
	}

	public boolean supportsStk() {
		return false;
	}
	
	public void stkInit() throws SMSLibDeviceException, IOException {
		if(!supportsStk()) throw new IllegalStateException("Cannot initialise STK if not supported.");
	}

	public StkResponse stkRequest(StkRequest request, String... variables) throws SMSLibDeviceException, IOException {
		throw new NoStkSupportException();
	}
}

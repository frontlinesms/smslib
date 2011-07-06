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

import serial.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CSerialDriver implements SerialPortEventListener {
	/** Prints to console a selection of full lines read and written to the serial port. */
	private static final boolean TRACE_IO = false;
	private static final boolean DEBUG = false;
	
	private static final int DELAY = 1000;

	private static final int DELAY_AFTER_WRITE = 1000;

	private static final int RECV_TIMEOUT = 30 * 1000;

	private static final int BUFFER_SIZE = 16384;
	
	/** The name of the serial port this conencts to. */
	private String port;
	
	private String lastAtCommand;
	
	private int baud;

	private CommPortIdentifier commPortIdentifier;
	/** The serial port this connects to. */
	public SerialPort serialPort;
	/** Input stream of the serial port this connects to. */
	private InputStream inStream;
	/** Output stream of the serial port this connects to. */
	private OutputStream outStream;
	
	private CNewMsgMonitor newMsgMonitor;
	/** Set HIGH to stop current operations. */
	private volatile boolean stopFlag;
	/** The logger for this driver. */
	private Logger log;

	private CService srv;

	public CSerialDriver(String port, int baud, CService srv) {
		if(DEBUG) System.out.println("CSerialDriver.CSerialDriver() : ENTRY");
		this.port = port;
		this.baud = baud;
		this.srv = srv;
		this.log = Logger.getLogger(CSerialDriver.class);
		if(DEBUG) System.out.println("CSerialDriver.CSerialDriver() : EXIT");
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPort() {
		return port;
	}

	public int getBaud() {
		return baud;
	}

	public void setNewMsgMonitor(CNewMsgMonitor monitor) {
		this.newMsgMonitor = monitor;
	}

	public void killMe() {
		stopFlag = true;
	}

	public void open() throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException, TooManyListenersException {
		if (log != null) log.info("Connecting to serial port: " + port + " @ " + baud);

		commPortIdentifier = CommPortIdentifier.getPortIdentifier(getPort());
		serialPort = (SerialPort) commPortIdentifier.open("FrontlineSMS", 1971);
		serialPort.addEventListener(this);
		serialPort.notifyOnDataAvailable(true);
		/*
		 * There is no handling for these notifications except for logging, so I have
		 * removed setting them.  Also, some combination of these notifications cause
		 * my machine to lock up if certain devices are attached.
			serialPort.notifyOnOutputEmpty(true);
			serialPort.notifyOnBreakInterrupt(true);
			serialPort.notifyOnFramingError(true);
			serialPort.notifyOnOverrunError(true);
			serialPort.notifyOnParityError(true);
		 */
		serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
		serialPort.setSerialPortParams(getBaud(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		serialPort.setInputBufferSize(BUFFER_SIZE);
		serialPort.setOutputBufferSize(BUFFER_SIZE);
		serialPort.enableReceiveTimeout(RECV_TIMEOUT);
		inStream = serialPort.getInputStream();
		outStream = serialPort.getOutputStream();
		
		//bjdw added to try and catch "WaitCommEvent: Error 5" when usb port is disconnected
		serialPort.notifyOnCTS(true);
	}

	public void close() {
		if (log != null) log.info("Disconnecting from serial port: " + port);
		// TODO is this check necessary?  Possibly not...
		if(serialPort!=null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	public void serialEvent(SerialPortEvent event) {
		int eventType = event.getEventType();
		if(eventType == SerialPortEvent.BI) {
			return;
		}
		if(eventType == SerialPortEvent.OE) {
			if (log != null) log.error("COMM-ERROR: Overrun Error!");
			return;
		}
		if(eventType == SerialPortEvent.FE) {
			if (log != null) log.error("COMM-ERROR: Framing Error!");
			return;
		}
		if(eventType == SerialPortEvent.PE) {
			if (log != null) log.error("COMM-ERROR: Parity Error!");
			return;
		}
		if(eventType == SerialPortEvent.CD) {
			return;
		}
		if(eventType == SerialPortEvent.CTS) {
			if(DEBUG) System.out.println("CSERIAL DRIVER ->> CTS event:"+event.getNewValue()+ "on "+port);
			//numberOfCTSevents++;
			if (/*(numberOfCTSevents>=MAX_CTS_EVENTS_BEFORE_CLOSE) &&*/ !event.getNewValue()) {
				//try disconnect
				if(DEBUG) System.out.println("CSERIAL DRIVER ->> CTS event: closing port");
				close();
			}
			return;
		}
		if(eventType == SerialPortEvent.DSR) {
			return;
		}
		if(eventType == SerialPortEvent.RI) {
			return;
		}
		if(eventType == SerialPortEvent.OUTPUT_BUFFER_EMPTY) {
			return;
		}
		if(eventType == SerialPortEvent.DATA_AVAILABLE) {
			//System.out.println("\tRaising...");
			if (newMsgMonitor != null) newMsgMonitor.raise(CNewMsgMonitor.DATA);
			return;
		}
	}

	public void clearBufferCheckCMTI() throws IOException {
		StringBuilder buffer = new StringBuilder(BUFFER_SIZE);

		if (log != null) log.debug("SerialDriver(): clearBufferCheckCMTI() called");
		while (dataAvailable()) {
			int c = inStream.read();
			if (c == -1) break;
			buffer.append((char) c);
		}
		if (log != null) log.debug("ME(CL): " + formatLog(buffer));
		if (newMsgMonitor != null && newMsgMonitor.getState() != CNewMsgMonitor.CMTI) {
			final String txt = buffer.toString();
			newMsgMonitor.raise((txt.indexOf("+CMTI:") >= 0 || txt.indexOf("+CDSI:") >= 0) ? CNewMsgMonitor.CMTI : CNewMsgMonitor.IDLE);
		}
	}

	public void emptyBuffer() throws IOException {
		if (log != null) log.debug("SerialDriver(): emptyBuffer() called");
		sleep_ignoreInterrupts(DELAY);
		while(dataAvailable()) inStream.read();
	}

	public String readAll() throws IOException {
		StringBuilder bob = new StringBuilder();
		int c;
		while(!stopFlag && (c = inStream.read())!=-1) {
			bob.append(c);
		}
		return bob.toString();
	}

	public void clearBuffer() throws IOException {
		sleep_ignoreInterrupts(DELAY);
		clearBufferCheckCMTI();
	}

	public void send(String s) throws IOException {
		this.lastAtCommand = s;
		if (log != null) log.debug("TE: " + formatLog(new StringBuilder(s)));
		if(TRACE_IO) System.out.println("> " + s);
		
		if (s.startsWith("AT+STGR=0,1,128") || s.startsWith("AT+STGR=0,1,1")){
			System.out.println();
		}
		
		for (int i = 0; i < s.length(); i++) {
			outStream.write((byte) s.charAt(i));
		}
		outStream.flush();
		
		sleep_ignoreInterrupts(DELAY_AFTER_WRITE);
	}
	
	public void send(String s, byte eF) throws IOException {
		this.lastAtCommand = s;
		if (log != null) log.debug("TE: " + formatLog(new StringBuilder(s)));
		if(TRACE_IO) System.out.println("> " + s);
		
		if (s.startsWith("AT+STGR=0,1,128") || s.startsWith("AT+STGR=0,1,1")){
			System.out.println();
		}
		
		for (int i = 0; i < s.length(); i++) {
			outStream.write((byte) s.charAt(i));
		}
		outStream.write(eF);
		outStream.flush();
		
		sleep_ignoreInterrupts(DELAY_AFTER_WRITE);
	}

	public void send(char c) throws IOException {
		outStream.write((byte) c);
		outStream.flush();
		sleep_ignoreInterrupts(DELAY_AFTER_WRITE);
	}

	public void send(byte c) throws IOException {
		outStream.write(c);
		outStream.flush();
		sleep_ignoreInterrupts(DELAY_AFTER_WRITE);
	}

	public void skipBytes(int numOfBytes) throws IOException {
		int c, count = 0;
		while (count < numOfBytes) {
			c = inStream.read();
			// Looks dodgy - if c IS -1, then we've reached the end of the stream, and should get out of here.
			if (c != -1) count++;
		}
	}

	public boolean dataAvailable() throws IOException {
		int available = inStream.available();
		return !stopFlag && available>0;
	}

	public String getResponse() throws IOException {
		final int MAX_RETRIES = 3;
		int retry = 0;
		final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);

		while (retry < MAX_RETRIES) {
			try {
				while (true) {
					while (true) {
						if (stopFlag) return "+ERROR:\r\n";
						int c = inStream.read();
						if (c == -1) {
							buffer.delete(0, buffer.length());
							break;
						}
						buffer.append((char) c);
						if ((c == '\r') || (c == '\n')) break;
					}
					String response = buffer.toString();

					if(response.length() == 0
							//|| response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s")
							
							//|| response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s")
							// if (response.matches("\\s*[\\p{ASCII}]*\\s+READY\\s+OK\\s")
							|| response.matches("\\s*[\\p{ASCII}]*\\s+READY\\s+")
							|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
							|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")
							|| response.matches("\\s*[\\p{ASCII}]*\\s+SIM PIN\\s")) break;
					else if(response.matches("\\s*[+]((CMTI)|(CDSI))[:][^\r\n]*[\r\n]")) {
						if (log != null) log.debug("ME: " + formatLog(buffer));
						buffer.delete(0, buffer.length());
						if (newMsgMonitor != null) newMsgMonitor.raise(CNewMsgMonitor.CMTI);
						continue;
					}
					Matcher matcher = Pattern.compile("AT[+]STGR[=|,|\\d]").matcher(this.lastAtCommand);
					if(matcher.find()){
						matcher = Pattern.compile("\\s*[\\p{ASCII}]*\\s*+STIN: \\d+\\s*").matcher(response);
						
						if (matcher.find()
						|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
						|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")) break;
					}else{
						matcher = Pattern.compile("AT").matcher(this.lastAtCommand);
						if(matcher.find()){
							matcher = Pattern.compile("\\s*[\\p{ASCII}]*\\s*+STIN: \\d+\\s*").matcher(response);
							
							if (matcher.find()
							|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
							|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")) break;
						}else{
							if(response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s")){
								break;
							}
						}
					}
				}
				retry = MAX_RETRIES;
			} catch (IOException e) {
				e.printStackTrace();
				if (retry < MAX_RETRIES)
				{
					try { Thread.sleep(DELAY); } catch(InterruptedException ex) {}
					++retry;
				} else throw e;
			}
		}
		if (log != null) log.debug("ME: " + formatLog(buffer));
		clearBufferCheckCMTI();

		if (buffer.indexOf("RING") > 0) {
			if (srv.isConnected()) {
				Pattern p = Pattern.compile("\\+?\\d+");
				Matcher m = p.matcher(buffer.toString());
				m.find();
				String phone = buffer.toString().substring(m.start(), m.end());

				if (srv.getCallHandler() != null) srv.getCallHandler().received(srv, new CIncomingCall(phone, new java.util.Date()));

				String response = buffer.toString();
				response = response.replaceAll("\\s*RING\\s+[\\p{ASCII}]CLIP[[\\p{Alnum}][\\p{Punct}] ]+\\s\\s", "");
				return response;
			} else return buffer.toString();
		} else {
			String response = buffer.toString();
			if(TRACE_IO) System.out.println("< " + response);
			return response;
		}
	}

	private String formatLog(StringBuilder s) {
		StringBuffer response = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
				case 13:
					response.append("(cr)");
					break;
				case 10:
					response.append("(lf)");
					break;
				case 9:
					response.append("(tab)");
					break;
				default:
					response.append("(" + (int) s.charAt(i) + ")");
					break;
			}
		}
		response.append("  Text:[");
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
				case 13:
					response.append("(cr)");
					break;
				case 10:
					response.append("(lf)");
					break;
				case 9:
					response.append("(tab)");
					break;
				default:
					response.append(s.charAt(i));
					break;
			}
		}
		response.append("]");
		return response.toString();
	}

	public void ownershipChange(int type) {
		log.info("CSerialDriver.ownershipChange() : " + type);
	}
	
	/**
	 * Make the thread sleep; ignore InterruptedExceptions.
	 * @param millis
	 */
	public static void sleep_ignoreInterrupts(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException ex) {}
	}
}

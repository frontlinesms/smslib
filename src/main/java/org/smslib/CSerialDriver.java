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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.smslib.CNewMsgMonitor.State;

public class CSerialDriver implements SerialPortEventListener {
	/** Prints to console a selection of full lines read and written to the serial port. */
	private static final boolean TRACE_IO = false;
	private static final boolean DEBUG = false;
	
	private static final int DELAY = 50;

	private static final int DELAY_AFTER_WRITE = 50;

	private static final int RECV_TIMEOUT = 30 * 1000;

	private static final int BUFFER_SIZE = 16384;
	
	/** The name of the serial port this conencts to. */
	private String port;
	
	String lastAtCommand;
	
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
		
		// FIXME this line should obviously NOT be committed:
		String modemName = port.replace('/', '_');
		PrintStream fileLog;
		try {
			File f = new File("/Users/alex/temp/frontlinesmsmodemlogs/" + modemName + ".log");
			f.getParentFile().mkdirs();
			fileLog = new PrintStream(new FileOutputStream(f));
//			fileLog = System.out;
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		inStream = new LoggingInputStream(serialPort.getInputStream(), fileLog, ">>${thread}>>TX>>${stack}>>", '\r', '\n');
		outStream = new LoggingOutputStream(serialPort.getOutputStream(), fileLog, ">>${thread}>>RX>>${stack}>>", '\r', '\n');
		
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
			if (newMsgMonitor != null) newMsgMonitor.raise(State.DATA);
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
		if (newMsgMonitor != null && newMsgMonitor.getState() != State.CMTI) {
			final String txt = buffer.toString();
			newMsgMonitor.raise((txt.indexOf("+CMTI:") >= 0 || txt.indexOf("+CDSI:") >= 0) ? State.CMTI : State.IDLE);
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
		
		for (int i = 0; i < s.length(); i++) {
			outStream.write((byte) s.charAt(i));
		}
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
				readResponseToBuffer(buffer);
				retry = MAX_RETRIES;
			} catch (ServiceDisconnectedException e) {
				return "+ERROR:\r\n";
			} catch (IOException e) {
				if (++retry <= MAX_RETRIES) {
					if(log!=null) log.info("IOException reading from serial port.  Will retry.", e);
					try { Thread.sleep(DELAY); } catch(InterruptedException ex) {}
				} else throw e;
			}
		}
		if (log != null) log.debug("ME: " + formatLog(buffer));
		clearBufferCheckCMTI();
		
		if(TRACE_IO) System.out.println("< " + buffer.toString());
		
		// check to see if any phone call alerts have been triggered
		if (buffer.indexOf("RING") > 0) { // FIXME should this actually read ">= 0"?!
			if (srv.isConnected()) {
				if (srv.getCallHandler() != null) {
					Pattern p = Pattern.compile("\\+?\\d+");
					Matcher m = p.matcher(buffer);
					String phone = m.find()? m.group(): "";
					srv.getCallHandler().received(srv, new CIncomingCall(phone, new java.util.Date()));
				}

				// strip all content relating to RING, and return the rest of the response
				return buffer.toString().replaceAll("\\s*RING\\s+[\\p{ASCII}]CLIP[[\\p{Alnum}][\\p{Punct}] ]+\\s\\s", "");
			} else return buffer.toString();
		} else {
			return buffer.toString();
		}
	}
	
	void readToBuffer(StringBuilder buffer) throws IOException, ServiceDisconnectedException {
		while (true) {
			if (stopFlag) throw new ServiceDisconnectedException();
			int c = inStream.read();
			if (c == -1) {
				if(TRACE_IO) System.out.println("< " + buffer.toString());
				buffer.delete(0, buffer.length());
				break;
			}
			buffer.append((char) c);
			
			if ((c == '\r') || (c == '\n') || (c == '>')) return;
		}
	}
	
	/** this is the new version of the method which we want to adopt once the old version is properly unit tested. */
	void readResponseToBuffer(StringBuilder buffer) throws IOException, ServiceDisconnectedException {
		while (true) {
			readToBuffer(buffer);
			String response = buffer.toString();

			if(response.length() == 0
					|| response.matches("\\s*[\\p{ASCII}]*\\s+READY\\s+")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+SIM PIN\\s")
					|| response.matches("\\s*\\	>\\s*")) {
				// When the '>' character starts a line, it shows that the device
				// is waiting for input.  We should return quickly from this. // FIXME "return quickly" is vague, and this '>' prompt is not unit tested.  Please clarify before merging with master. 
				return;
			} else if(response.matches("\\s*[+]((CMTI)|(CDSI))[:][^\r\n]*[\r\n]")) {
				if (log != null) log.debug("ME: " + formatLog(buffer));
				if(TRACE_IO) System.out.println("< " + buffer.toString());
				buffer.delete(0, buffer.length());
				if (newMsgMonitor != null) newMsgMonitor.raise(State.CMTI);
				continue;
			}
			// TODO Why is this matcher not applied in the same way as previous match tests in the previous if statement?
			Matcher matcher = Pattern.compile("AT[+]STGR[=|,|\\d]").matcher(this.lastAtCommand);
			if(matcher.find()) {
				matcher = Pattern.compile("\\s*[\\p{ASCII}]*\\s*+STIN: \\d+\\s*").matcher(response);
				
				if (matcher.find()
						|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
						|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")) return;
			} else {
				matcher = Pattern.compile("AT").matcher(this.lastAtCommand);
				if(matcher.find()) {
					matcher = Pattern.compile("\\s*[\\p{ASCII}]*\\s+OK\\s").matcher(response);
					if(matcher.find()) {
						if(response.contains("Sending...")) { // FIXME this looks suspiciously like it's MPESA-specific
							matcher = Pattern.compile("\\s*[\\p{ASCII}]*\\s*+STIN: \\d+\\s*").matcher(response);
			
							if (matcher.find()
									|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
									|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")) return;
						} else {
							return;
						}
					}
				} else {
					matcher = Pattern.compile("\\s*[\\p{ASCII}]*\\s*+STIN: \\d+\\s*").matcher(response);
					if (matcher.find()
							|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
							|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")
							|| response.matches("\\s*[+]CMGS[:] \\d+[\\r\\n]*OK\\s*")) return;
				}
			}
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

@SuppressWarnings("serial")
class ServiceDisconnectedException extends Exception {}

interface StreamLogger {
	PrintStream getLog();
	StringBuilder getBuffer();
	String getLogPrefix();
	char[] getTerminators();
}

class LoggingUtils {
	private static final String THREAD = "${thread}";
	private static final String STACK = "${stack}";
	
	static void log(StreamLogger logger) {
		StringBuilder buffer = logger.getBuffer();
		logger.getLog().println(logger.getLogPrefix() + translate(buffer));
		buffer.delete(0, Integer.MAX_VALUE);
	}
	
	private static String translate(CharSequence s) {
		return StringEscapeUtils.escapeJava(s.toString());
	}
	
	static boolean isTerminator(StreamLogger logger, int i) {
		for(char c : logger.getTerminators()) if(c == i) return true;
		return false;
	}

	public static String formatLogPrefix(String logPrefix) {
		String log = logPrefix.replace(THREAD, Thread.currentThread().getName());
		if(log.contains(STACK)) {
			log = getLogPrefixWithStack(log);
		}
		return log;
	}

	private static String getLogPrefixWithStack(String logPrefix) {
		// build stack
		StringBuilder s = new StringBuilder();
		for(StackTraceElement trace : Thread.currentThread().getStackTrace()) {
			if(!trace.getClassName().equals("java.lang.Thread") &&
					!trace.getClassName().equals("org.smslib.LoggingUtils") &&
					!trace.getClassName().equals("org.smslib.LoggingInputStream") &&
					!trace.getClassName().equals("org.smslib.LoggingOutputStream")) {
				if(s.length() > 0) s.append('>');
				s.append(trace.getClassName() + "." + trace.getMethodName());
			}
		}
		return logPrefix.replace(STACK, s.toString());
	}
}

class LoggingInputStream extends InputStream implements StreamLogger {
	private InputStream in;
	private PrintStream log;
	private StringBuilder buffer = new StringBuilder();
	private String logPrefix;
	private char[] terminators;

	public LoggingInputStream(InputStream in, PrintStream log, String logPrefix, char...terminators) {
		this.in = in;
		this.log = log;
		this.logPrefix = logPrefix;
		this.terminators = terminators;
	}
	
	public char[] getTerminators() {
		return terminators;
	}
	public StringBuilder getBuffer() {
		return buffer;
	}
	public PrintStream getLog() {
		return log;
	}
	public String getLogPrefix() {
		return LoggingUtils.formatLogPrefix(logPrefix);
	}

	@Override
	public synchronized int read() throws IOException {
		int read = in.read();
		buffer.append((char) read);
		if(isTerminator(read)) {
			LoggingUtils.log(this);
			buffer.delete(0, Integer.MAX_VALUE);
		}
		return read;
	}
	
	private boolean isTerminator(int i) {
		return LoggingUtils.isTerminator(this, i);
	}
}

class LoggingOutputStream extends OutputStream implements StreamLogger {
	private OutputStream out;
	private PrintStream log;
	private StringBuilder buffer = new StringBuilder();
	private String logPrefix;
	private char[] terminators;
	
	public LoggingOutputStream(OutputStream out, PrintStream log, String logPrefix, char...terminators) {
		this.out = out;
		this.log = log;
		this.logPrefix = logPrefix;
		this.terminators = terminators;
	}
	
	public char[] getTerminators() {
		return terminators;
	}
	public StringBuilder getBuffer() {
		return buffer;
	}
	public PrintStream getLog() {
		return log;
	}
	public String getLogPrefix() {
		return LoggingUtils.formatLogPrefix(logPrefix);
	}

	@Override
	public synchronized void write(int c) throws IOException {
		buffer.append((char) c);
		if(isTerminator(c)) {
			LoggingUtils.log(this);
			buffer.delete(0, Integer.MAX_VALUE);
		}
		out.write(c);
	}
	
	private boolean isTerminator(int i) {
		return LoggingUtils.isTerminator(this, i);
	}
}
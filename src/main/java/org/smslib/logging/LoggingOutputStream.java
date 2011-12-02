package org.smslib.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LoggingOutputStream extends OutputStream implements StreamLogger {
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
	
	@Override
	public void flush() throws IOException {
		out.flush();
	}
	
	private boolean isTerminator(int i) {
		return LoggingUtils.isTerminator(this, i);
	}
}
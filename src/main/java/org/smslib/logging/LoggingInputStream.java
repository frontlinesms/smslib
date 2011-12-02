package org.smslib.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class LoggingInputStream extends InputStream implements StreamLogger {
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
	
	@Override
	public int available() throws IOException {
		return in.available();
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}
	
	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
	
	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}
	
	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}
	
	private boolean isTerminator(int i) {
		return LoggingUtils.isTerminator(this, i);
	}
}



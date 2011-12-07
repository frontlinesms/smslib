package org.smslib.test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.smslib.test.SmsLibTestUtils.toUtf8;

public class MultipleStringInputStream extends InputStream {
	private static final long TIMEOUT = 1000;
	private ByteArrayInputStream in;
	private final Queue<String> strings;
	private int available;
	private long readLastChar;
	
	public MultipleStringInputStream(String... strings) {
		this.strings = new LinkedList<String>(Arrays.asList(strings));
	}
	
	@Override
	public int read() throws IOException {
		if(in == null || available == 0) {
			String s = strings.poll();
			if(s == null) return -1;
			updateString(s);
		}
		available = Math.max(available-1, 0);
		if(available == 0) {
			readLastChar = System.currentTimeMillis();
		}
		return in.read();
	}
	
	private void updateString(String s) {
		if(s != null) {
			byte[] bytes = toUtf8(s);
			available = bytes.length;
			in = new ByteArrayInputStream(bytes);
		}
	}

	@Override
	public int available() throws IOException {
		if(available == 0 &&
				readLastChar + TIMEOUT < System.currentTimeMillis()) {
			String s = strings.poll();
			if(s == null) throw new EOFException("Waiting on an empty stream!");
			else updateString(s);
		}
		return available;
	}
}

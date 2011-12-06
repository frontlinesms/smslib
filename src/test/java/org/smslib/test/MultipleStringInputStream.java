package org.smslib.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.smslib.test.SmsLibTestUtils.toUtf8;

public class MultipleStringInputStream extends InputStream {
	private ByteArrayInputStream in;
	private final Queue<String> strings;
	private int available;
	
	public MultipleStringInputStream(String... strings) {
		this.strings = new LinkedList<String>(Arrays.asList(strings));
	}
	
	@Override
	public int read() throws IOException {
		if(in == null || available == 0) {
			String s = strings.poll();
			if(s == null) return -1;
			byte[] bytes = toUtf8(s);
			available = bytes.length;
			in = new ByteArrayInputStream(bytes);
		}
		available = Math.max(available-1, 0);
		return in.read();
	}
	
	@Override
	public int available() throws IOException {
		return available;
	}
}

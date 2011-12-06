package org.smslib.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class StringInputStream extends InputStream {
	private ByteArrayInputStream in;
	
	public void setString(String content) {
		try {
			in = new ByteArrayInputStream(content.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("JVM doesn't support UTF???", ex);
		}
	}

	@Override
	public int read() throws IOException {
		assertStringSet();
		return in.read();
	}

	private void assertStringSet() {
		if(in == null) {
			throw new RuntimeException("Test not set up property - must set content for input stream.");
		}
	}
}

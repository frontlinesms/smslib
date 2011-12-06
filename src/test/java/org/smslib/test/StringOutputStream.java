package org.smslib.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class StringOutputStream extends OutputStream {
	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	@Override
	public void write(int b) throws IOException {
		buffer.write(b);
	}

	public String getBufferText() {
		try {
			return new String(buffer.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("No UTF-8?", e);
		}
	}
	
	public void clearBuffer() {
		this.buffer.reset();
	}
}

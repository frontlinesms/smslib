package org.smslib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CSerialDriver}
 * @author alex
 */
public class CSerialDriverTest extends BaseTestCase {
	/** Instance of {@link CSerialDriver} under test. */
	private CSerialDriver csd;
	/** A mock {@link CService} */
	private CService cService;
	/** input stream that {@link #csd} will read from */
	private StringInputStream in;
	
	@Override
	protected void setUp() throws Exception {
		this.cService = mock(CService.class);
		this.csd = new CSerialDriver("TEST-PORT", 123456, cService);
		
		// Set up input stream
		this.in = new StringInputStream();
		Field inField = CSerialDriver.class.getDeclaredField("inStream");
		inField.setAccessible(true);
		inField.set(this.csd, this.in);
	}
	
	public void testGetResponse_good() throws IOException {
		final String[] modemResponses = {
				"\r\n+CMGS: 98\r\n\r\nOK\r"
		};
		for(String expectedResponse : modemResponses) {
			// given
			in.setString(expectedResponse);
			
			// when
			String actualResponse = csd.getResponse();
			
			assertEquals(expectedResponse, actualResponse);
		}
	}
	
	public void testGetResponse_bad() throws IOException {
		final String[] modemResponses = {
				
		};
		for(String modemResponse : modemResponses) {
			// given
			in.setString(modemResponse);
			
			// when
			String actualResponse = csd.getResponse();
			
			assertEquals("", actualResponse);
		}
	}
}

class StringInputStream extends InputStream {
	private ByteArrayInputStream in;
	
	public void setString(String expectedResponse) {
		try {
			in = new ByteArrayInputStream(expectedResponse.getBytes("UTF-8"));
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
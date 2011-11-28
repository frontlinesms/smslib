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
		
		// set the last AT command to blank to prevent null pointer exceptions
		setPreviousAtCommandOnTheInstanceOfCSerialDriver("");
	}
	
	public void testReadResponseToBuffer() throws IOException, ServiceDisconnectedException {
		// TODO need to build up this test with examples that would have worked before the STK/MPESA
		// code was added, and then fix the current code so that the tests still pass.
		String[][] goodInputsAndOutputs = {
				{ "OK\r", "OK\r" },
				{"> ", "> "},
		};
		
		for(String[] inputAndOutput: goodInputsAndOutputs) {
			// given
			in.setString(inputAndOutput[0]);
			StringBuilder buffer = new StringBuilder();
			
			// when
			csd.readResponseToBuffer(buffer);
			
			// then
			assertEquals(inputAndOutput[1], buffer.toString());
		}
	}
	
	public void testReadResponseToBufferWithRinging() throws IOException, ServiceDisconnectedException {
		/* Test data of the form {streamContent, expectedBufferContent, ringCount=1, ringNumber=any} */
		Object[][] goodInputsAndOutputs = {
		};
		
		for(Object[] data: goodInputsAndOutputs) {
			// setup
			String streamContent = (String) data[0];
			String expectedBufferContent = (String) data[1];
			int ringCount = data.length>2? (Integer) data[2]: 1;
			final String ringNumber = data.length>3? (String) data[3]: "";
			CIncomingCall call = new CIncomingCall(ringNumber, null) {
				@Override
				public boolean equals(Object o) {
					CIncomingCall that = (CIncomingCall) o;
					return ringNumber == null ||
							ringNumber.equals(that.getPhoneNumber());
				}
			};
			ICallListener callListener = mock(ICallListener.class);
			
			// given
			in.setString(streamContent);
			StringBuilder buffer = new StringBuilder();
			
			// when
			csd.readResponseToBuffer(buffer);
			
			// then
			assertEquals(expectedBufferContent, buffer.toString());
			verify(callListener, times(ringCount)).received(cService, call);
		}
	}
	
	public void testReadToBuffer() throws IOException, ServiceDisconnectedException {
		String[][] goodInputsAndOutputs = {
				// line terminators
				{"\n", "\n"}, {"\r", "\r"}, {"> ", ">"},
				// a number of lines - should get the first only
				{"ABCDEFGHIJKLMNOP\r1234567890", "ABCDEFGHIJKLMNOP\r"},
				// an empty input stream should cause the method to return an empty buffer
				{"ABC", ""},
		};
		
		for(String[] inputAndOutput: goodInputsAndOutputs) {
			// given
			in.setString(inputAndOutput[0]);
			StringBuilder buffer = new StringBuilder();
			
			// when
			csd.readToBuffer(buffer);
			
			// then
			assertEquals(inputAndOutput[1], buffer.toString());
		}
	}
	
	public void testReadToBufferWhenKilled() throws IOException, ServiceDisconnectedException {
		// given
		StringBuilder buffer = new StringBuilder();
		in.setString("This half of the string will be read.\nThis half of the string will never be read.\n");
		
		// when
		csd.readToBuffer(buffer);
		
		// then
		assertEquals("This half of the string will be read.\n", buffer.toString());
		
		// when
		csd.killMe();
		
		// then
		try {
			csd.readToBuffer(buffer);
			assertEquals("", buffer.toString());
			fail("Exception should have been thrown when reading from killed CSD.");
		} catch(ServiceDisconnectedException ex) {
			// expected
		}
	}
	
	public void testGetResponseWithPreviousAtCommand_good() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		final ModemResponse[] modemResponses = {
				new ModemResponse("013213513513513518468468486841350", "\r\n+CMGS: 98\r\n\r\nOK\r")
		};
		for(ModemResponse modemResponse : modemResponses) {
			// given
			in.setString(modemResponse.getResponse());
			setPreviousAtCommandOnTheInstanceOfCSerialDriver(modemResponse.getPreviousAtCommand());
			
			// when
			String actualResponse = csd.getResponse();
			
			// then
			assertEquals(modemResponse.getResponse(), actualResponse);
		}
	}
	
	public void testGetResponseWithPreviousAtCommand_bad() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		final ModemResponse[] modemResponses = {
				
		};
		for(ModemResponse modemResponse : modemResponses) {
			// given
			in.setString(modemResponse.getResponse());
			setPreviousAtCommandOnTheInstanceOfCSerialDriver(modemResponse.getPreviousAtCommand());
			
			// when
			String actualResponse = csd.getResponse();
			
			// then
			assertEquals("", actualResponse);
		}
	}

	private void setPreviousAtCommandOnTheInstanceOfCSerialDriver(Object previousAtCommand) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Class<?> c = CSerialDriver.class;
		Field fld = c.getDeclaredField("lastAtCommand");
		fld.set(csd, previousAtCommand);
	}
}

class StringInputStream extends InputStream {
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

class ModemResponse {
	private String response;
	private String previousAtCommand;
	public ModemResponse(String previousAtCommand, String response) {
		this.response = response;
		this.previousAtCommand = previousAtCommand;
	}

	public Object getPreviousAtCommand() {
		return previousAtCommand;
	}

	public String getResponse() {
		return response;
	}
	
}
package org.smslib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

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
	/** input stream that {@link #refImplementation} will read from */
	private StringInputStream refIn;
	
	private CSerialDriverReferenceImplementation refImplementation;
	
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
		
		// set up reference implementation
		refIn = new StringInputStream();
		refImplementation = new CSerialDriverReferenceImplementation();
		refImplementation.setInStream(this.refIn);
	}
	
	/**
	 * Testing inputs and outputs that have changed from the original implementation of {@link CSerialDriver#readResponseToBuffer(StringBuilder)}.
	 * @throws Exception
	 */
	public void testReadResponseToBuffer_changed() throws Exception {
		// { INPUT, ORIGINAL_OUTPUT, NEW_OUTPUT }
		String[][] goodInputsAndOutputs = {
				{ "\n\r\n+CME ERROR: SIM PIN required\r\n", "", "\n\r\n+CME ERROR: SIM PIN required\r\n" },
				{ "\uFFFFAT+CBC\r\r\n+CME ERROR: SIM PIN required\r\n", "", "\uFFFFAT+CBC\r\r\n+CME ERROR: SIM PIN required\r\n" },
				{ "\r\n+CME ERROR: SIM PIN required\r\n", "", "\r\n+CME ERROR: SIM PIN required\r\n" },
				{ "", "" },
				{ "", "" },
				{ "", "" },
		};	
		
		for(String[] inputAndOutput: goodInputsAndOutputs) {
			// given
			in.setString(inputAndOutput[0]);
			refIn.setString(inputAndOutput[0]);
			StringBuilder buffer = new StringBuilder();
			StringBuilder refBuffer = new StringBuilder();
			
			// when
			csd.readResponseToBuffer(buffer);
			refImplementation.readResponseToBuffer(refBuffer);
			
			// then
			assertEquals(inputAndOutput[2], buffer.toString());
			assertEquals(inputAndOutput[1], refBuffer.toString());
		}
	}
	
	/**
	 * Testing inputs and outputs that worked with the original implementation of {@link CSerialDriver#readResponseToBuffer(StringBuilder)}.
	 * This test should pass in spite of any modifications made to the method in order to ensure backwards compatibility.
	 * @throws Exception
	 */
	public void testReadResponseToBuffer_original() throws Exception {
		// { INPUT, OUTPUT }
		String[][] goodInputsAndOutputs = {
				{ "", "" },
				{ "OK\r", "" },
				{ " OK\r", " OK\r" },
				{ "\n\r\nOK\r", "\n\r\nOK\r" },
				{ "\nAT\r\r\nOK\r", "\nAT\r\r\nOK\r" },
				{ "+CMGS:123\rOK", "" },
				{ "+CMGS:123\rOK\r", "+CMGS:123\rOK\r" },
				{ "+CMGF: (0,1)\r\rOK\r", "+CMGF: (0,1)\r\rOK\r" },
				{ " +CMGF: (0,1)\r\rOK\r", " +CMGF: (0,1)\r\rOK\r" },
				{ "+CMGS: 12\r\rOK\r", "+CMGS: 12\r\rOK\r" },
				{ "+CIND: (\"Voice Mail\",(0,1)),(\"service\",(0,1)),(\"call\",(0,1)),(\"Roam\",(0-2)),(\"signal\",(0-5)),(\"callsetup\",(0-3)),(\"smsfull\",(0,1))\"", "" },
				{ "+CIND: (\"Voice Mail\",(0,1)),(\"service\",(0,1)),(\"call\",(0,1)),(\"Roam\",(0-2)),(\"signal\",(0-5)),(\"callsetup\",(0-3)),(\"smsfull\",(0,1))\"\r", "" },
				{ "+CIND: (\"Voice Mail\",(0,1)),(\"service\",(0,1)),(\"call\",(0,1)),(\"Roam\",(0-2)),(\"signal\",(0-5)),(\"callsetup\",(0-3)),(\"smsfull\",(0,1))\"\rOK", "" },
				{ "+CIND: (\"Voice Mail\",(0,1)),(\"service\",(0,1)),(\"call\",(0,1)),(\"Roam\",(0-2)),(\"signal\",(0-5)),(\"callsetup\",(0-3)),(\"smsfull\",(0,1))\"\rOK\r", "+CIND: (\"Voice Mail\",(0,1)),(\"service\",(0,1)),(\"call\",(0,1)),(\"Roam\",(0-2)),(\"signal\",(0-5)),(\"callsetup\",(0-3)),(\"smsfull\",(0,1))\"\rOK\r" },
				{ "+MBAN: Copyright 2000-2004 Motorola, Inc.\rOK\r", "+MBAN: Copyright 2000-2004 Motorola, Inc.\rOK\r" },
				{ "+CPMS: 2,28,2,28,2,28\r\rOK\r", "+CPMS: 2,28,2,28,2,28\r\rOK\r" },
				{ "ERROR\r", "" },
				{ "\nERROR\r", "\nERROR\r" },
				{ "\n\r\n+CME ERROR: SIM PIN required\r\n", "" },
				{ "\nAT^CURC=0\r\r\nOK\r", "\nAT^CURC=0\r\r\nOK\r" },
				{ "\n\r\n+CME ERROR: 11\r", "\n\r\n+CME ERROR: 11\r" },
				{ "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nATE0\r\r\nOK\r", "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nATE0\r\r\nOK\r" },
				{ "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nAT+COPS=0\r\r\n+CME ERROR: SIM PIN required\r\nAT^CURC=0\r\r\nOK\r", "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nAT+COPS=0\r\r\n+CME ERROR: SIM PIN required\r\nAT^CURC=0\r\r\nOK\r" },
				{ "\nAT+CPIN?\r\r\n+CPIN: SIM PIN\r", "\nAT+CPIN?\r\r\n+CPIN: SIM PIN\r" },
				{ "\nAT+CPIN?\r\r\n+CPIN: READY\r", "\nAT+CPIN?\r\r\n+CPIN: READY\r" },
				{ "\nAT+CLIP=1\r\r\nOK\r", "\nAT+CLIP=1\r\r\nOK\r" },
				{ "\uFFFFAT+CBC\r\r\n+CME ERROR: SIM PIN required\r\n", "" },
				{ "\r\n+CME ERROR: SIM PIN required\r\n", "" },
				{ "", "" },
				{ "", "" },
				{ "", "" },
		};
		
		for(String[] inputAndOutput: goodInputsAndOutputs) {
			// given
			in.setString(inputAndOutput[0]);
			refIn.setString(inputAndOutput[0]);
			StringBuilder buffer = new StringBuilder();
			StringBuilder refBuffer = new StringBuilder();
			
			// when
			csd.readResponseToBuffer(buffer);
			refImplementation.readResponseToBuffer(refBuffer);
			
			// then
			assertEquals(inputAndOutput[1], buffer.toString());
			assertEquals(inputAndOutput[1], refBuffer.toString());
		}
	}
	
	public void testReadResponseToBufferWithRinging() throws IOException, ServiceDisconnectedException {
		/* Test data of the form {streamContent, expectedBufferContent, ringCount=1, ringNumber=any} */
		Object[][] goodInputsAndOutputs = {
		};
		
		if(true) throw new RuntimeException("There is no test data here.  Please supply.");
		
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

class CSerialDriverReferenceImplementation {
	private CNewMsgMonitor newMsgMonitor;
	private boolean stopFlag;
	private InputStream inStream;
	private Logger log;
	
	void readResponseToBuffer(StringBuilder buffer) throws IOException, ServiceDisconnectedException {
		while (true) {
			while (true) {
				if (stopFlag) throw new ServiceDisconnectedException();
				int c = inStream.read();
				if (c == -1) {
					buffer.delete(0, buffer.length());
					break;
				}
				buffer.append((char) c);
				if ((c == 0x0a) || (c == 0x0d)) break;
			}
			String response = buffer.toString();

			if (response.length() == 0
					|| response.matches("\\s*[\\p{ASCII}]*\\s+OK\\s")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+READY\\s+")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR\\s")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+ERROR: \\d+\\s")
					|| response.matches("\\s*[\\p{ASCII}]*\\s+SIM PIN\\s"))
				return;
			else if (response.matches("\\s*[+]((CMTI)|(CDSI))[:][^\r\n]*[\r\n]")) {
				if (log != null) log.debug("ME: " + formatLog(buffer));
				buffer.delete(0, buffer.length());
				if (newMsgMonitor != null) newMsgMonitor.raise(CNewMsgMonitor.State.CMTI);
			}
		}
	}
	
	public void setInStream(InputStream in) {
		this.inStream = in;
	}
	
	private String formatLog(CharSequence s) {
		return StringEscapeUtils.escapeJava(s.toString());
	}
}
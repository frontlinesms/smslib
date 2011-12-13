package org.smslib;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.smslib.test.StringInputStream;

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
				{ "\n\r\n+CME ERROR: SIM PIN required\r\n", "", "\n\r\n+CME ERROR: SIM PIN required\r" },
				{ "AT+CBC\r\r\n+CME ERROR: SIM PIN required\r\n", "", "AT+CBC\r\r\n+CME ERROR: SIM PIN required\r" },
				{ "\r\n+CME ERROR: SIM PIN required\r\n", "", "\r\n+CME ERROR: SIM PIN required\r" },
				{ "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nATE0\r\r\nOK\r", "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nATE0\r\r\nOK\r", "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r" },
				{ "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nAT+COPS=0\r\r\n+CME ERROR: SIM PIN required\r\nAT^CURC=0\r\r\nOK\r", "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r\nAT+COPS=0\r\r\n+CME ERROR: SIM PIN required\r\nAT^CURC=0\r\r\nOK\r", "\nAT+CLIP=1\r\r\n+CME ERROR: SIM PIN required\r" },
				{ "AT+CBC\r\r\n+CME ERROR: SIM PIN required\r\n", "", "AT+CBC\r\r\n+CME ERROR: SIM PIN required\r" },
				{ "\r\n+CME ERROR: SIM PIN required\r\n", "", "\r\n+CME ERROR: SIM PIN required\r" },
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
			assertEquals(inputAndOutput[1], refBuffer.toString());
			assertEquals(inputAndOutput[2], buffer.toString());
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
				/* { IN, OUT } */
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
				{ "\nAT^CURC=0\r\r\nOK\r", "\nAT^CURC=0\r\r\nOK\r" },
				{ "\n\r\n+CME ERROR: 11\r", "\n\r\n+CME ERROR: 11\r" },
				{ "\nAT+CPIN?\r\r\n+CPIN: SIM PIN\r", "\nAT+CPIN?\r\r\n+CPIN: SIM PIN\r" },
				{ "\nAT+CPIN?\r\r\n+CPIN: READY\r", "\nAT+CPIN?\r\r\n+CPIN: READY\r" },
				{ "\nAT+CLIP=1\r\r\nOK\r", "\nAT+CLIP=1\r\r\nOK\r" },
				{ "\r\n+CPMS: 25,25,25,25,25,25\r\n\r\nOK\r", "\r\n+CPMS: 25,25,25,25,25,25\r\n\r\nOK\r" },
				{ "\r\n+CBC: 2,0\r\n\r\nOK\r", "\r\n+CBC: 2,0\r\n\r\nOK\r" },
				{ "\n\r\nOK\r\n", "\n\r\nOK\r" },
				{ " \r\nOK\r\n\r\n+STIN: 3\r", " \r\nOK\r" },
				{ "\r\n+CMGL: 25,0,,130\r\n0791527422050000240AD04D68711A0400001121102142752180426BEDF67CDA6039D0F0ED36A7E5ED32D9055ACED136980B0603CDCB6E3A88FE060599456CD0492C4A414127B1289D3E9DA095AC46BBC164B5DCED168B81DE6E50EC1593BD623150980E8AC974321A08DA0439CB7750B3052D4E832071981D768FCBA0F41CB49CA36737568C2673C160\r\n\r\nOK\r", "\r\n+CMGL: 25,0,,130\r\n0791527422050000240AD04D68711A0400001121102142752180426BEDF67CDA6039D0F0ED36A7E5ED32D9055ACED136980B0603CDCB6E3A88FE060599456CD0492C4A414127B1289D3E9DA095AC46BBC164B5DCED168B81DE6E50EC1593BD623150980E8AC974321A08DA0439CB7750B3052D4E832071981D768FCBA0F41CB49CA36737568C2673C160\r\n\r\nOK\r" },
				{ "\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter phone no.\",0\r\n\r\nOK\r", "\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter phone no.\",0\r\n\r\nOK\r" },
				{ "\r\nOK\r\n\r\n+STIN: 6\r", "\r\nOK\r" },
				{ "\r\n+STGI: 0,1,0,20,0,\"Enter phone no.\"\r\n\r\nOK\r", "\r\n+STGI: 0,1,0,20,0,\"Enter phone no.\"\r\n\r\nOK\r" },
				{ "\r\n+CREG: 0,1\r\n\r\nOK\r", "\r\n+CREG: 0,1\r\n\r\nOK\r" },
				{ "\r\n+CPMS: \"SM\",3,25,\"SM\",3,25,\"SM\",3,25\r\n\r\nOK\r", "\r\n+CPMS: \"SM\",3,25,\"SM\",3,25,\"SM\",3,25\r\n\r\nOK\r" },
				{ "\r\n+CME ERROR: 3\r", "\r\n+CME ERROR: 3\r" },
				{ "\r\n639029400593656\r\n\r\nOK\r", "\r\n639029400593656\r\n\r\nOK\r" },
				{ "\r\n+STGI: 0,0,0,\"M-PESA\"\r\n+STGI: 1,7,\"Send money\",0\r\n+STGI: 2,7,\"Withdraw cash\",0\r\n+STGI: 3,7,\"Buy airtime\",0\r\n+STGI: 4,7,\"Pay Bill\",0\r\n+STGI: 5,7,\"Buy Goods\",0\r\n+STGI: 6,7,\"ATM Withdrawal\",0\r\n+STGI: 7,7,\"My account\",0\r\n\r\nOK\r", "\r\n+STGI: 0,0,0,\"M-PESA\"\r\n+STGI: 1,7,\"Send money\",0\r\n+STGI: 2,7,\"Withdraw cash\",0\r\n+STGI: 3,7,\"Buy airtime\",0\r\n+STGI: 4,7,\"Pay Bill\",0\r\n+STGI: 5,7,\"Buy Goods\",0\r\n+STGI: 6,7,\"ATM Withdrawal\",0\r\n+STGI: 7,7,\"My account\",0\r\n\r\nOK\r" },
				{ " \r\nOK\r\n\r\n+STIN: 1\r", " \r\nOK\r" },
				{ "\n", "" },
				{ "\r\n+STGI: 0,0,4,4,0,\"Enter PIN\"\r\n\r\nOK\r", "\r\n+STGI: 0,0,4,4,0,\"Enter PIN\"\r\n\r\nOK\r" },
				{ "\r\n+CSQ: 22,0\r\n\r\nOK\r", "\r\n+CSQ: 22,0\r\n\r\nOK\r" },
				{ "\r\nE160\r\n\r\nOK\r", "\r\nE160\r\n\r\nOK\r" },
				{ "\r\n+CMGL: 25,0,,120\r\n0791527422050000240AD04D68711A0400001121102103622174C6709A5D26BB40CD16B4380D82C661B7FB4D0781E0E13C683947C7602E180C447F8396456736E89C828C4F2968597466832E90F12D07B5DFF23228ED36BFE5ED303DFD7683C661361BF49683A6CD29685C9FD3DFEDB21C342FCBEDE971790E7ABB41B219CD05\r\n\r\nOK\r", "\r\n+CMGL: 25,0,,120\r\n0791527422050000240AD04D68711A0400001121102103622174C6709A5D26BB40CD16B4380D82C661B7FB4D0781E0E13C683947C7602E180C447F8396456736E89C828C4F2968597466832E90F12D07B5DFF23228ED36BFE5ED303DFD7683C661361BF49683A6CD29685C9FD3DFEDB21C342FCBEDE971790E7ABB41B219CD05\r\n\r\nOK\r" },
				{ "\r\n MULTIBAND  900E  1800 \r\n\r\nOK\r", "\r\n MULTIBAND  900E  1800 \r\n\r\nOK\r" },
				{ "\r\n+STGI: \"Sending...\"\r\n\r\nOK\r\n\r\n+STIN: 1\r", "\r\n+STGI: \"Sending...\"\r\n\r\nOK\r" },
				{ "\n\r\n+STIN: 99\r\n", "" },
				{ "\r\n WAVECOM WIRELESS CPU\r\n\r\nOK\r", "\r\n WAVECOM WIRELESS CPU\r\n\r\nOK\r" },
				{ "\r\nR7.42.0.201003050914.GL6110 2131816 030510 09:14\r\n\r\nOK\r", "\r\nR7.42.0.201003050914.GL6110 2131816 030510 09:14\r\n\r\nOK\r" },
				{ "\r\n+CBC: 0,0\r\n\r\nOK\r", "\r\n+CBC: 0,0\r\n\r\nOK\r" },
				{ "\r\n+CNUM: \"flsms test no\",\"254704593656\",161\r\n\r\nOK\r", "\r\n+CNUM: \"flsms test no\",\"254704593656\",161\r\n\r\nOK\r" },
				{ "\r\n+CPMS: 3,3,24,25,24,25\r\n\r\nOK\r", "\r\n+CPMS: 3,3,24,25,24,25\r\n\r\nOK\r" },
				{ "\r\n+CGATT: 1\r\n\r\nOK\r", "\r\n+CGATT: 1\r\n\r\nOK\r" },
				{ "\r\n+STIN: 99\r\n\r\nOK\r", "\r\n+STIN: 99\r\n\r\nOK\r" },
				{ "\r\nOK\r\n\r\nOK\r\n\r\nOK\r\n\r\nOK\r\n", "\r\nOK\r" },
				{ "\r\n+STGI: 1,\"Send money to +254702597711\nKsh60\",1\r\n\r\nOK\r", "\r\n+STGI: 1,\"Send money to +254702597711\nKsh60\",1\r\n\r\nOK\r" },
				{ "\r\n+STGI: 0,1,0,8,0,\"Enter amount\"\r\n\r\nOK\r", "\r\n+STGI: 0,1,0,8,0,\"Enter amount\"\r\n\r\nOK\r" },
				{ "\r\nhuawei\r\n\r\nOK\r", "\r\nhuawei\r\n\r\nOK\r" },
				{ "\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter account no.\",0\r\n\r\nOK\r", "\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter account no.\",0\r\n\r\nOK\r" },
				{ "\r\n639029400629385\r\n\r\nOK\r", "\r\n639029400629385\r\n\r\nOK\r" },
				{ "\r\n+STGI: \"Safaricom\"\r\n+STGI: 1,2,\"Safaricom+\",0,0\r\n+STGI: 128,2,\"M-PESA\",0,21\r\n\r\nOK\r", "\r\n+STGI: \"Safaricom\"\r\n+STGI: 1,2,\"Safaricom+\",0,0\r\n+STGI: 128,2,\"M-PESA\",0,21\r\n\r\nOK\r" },
				{ "\r\n+CPMS: 3,3,25,25,25,25\r\n\r\nOK\r", "\r\n+CPMS: 3,3,25,25,25,25\r\n\r\nOK\r" },
				{ "\r\n+CPMS: 24,25,24,25,24,25\r\n\r\nOK\r", "\r\n+CPMS: 24,25,24,25,24,25\r\n\r\nOK\r" },
				{ "\r\n+STGI: 1,1,0,20,0,\"Enter account no.\"\r\n\r\nOK\r", "\r\n+STGI: 1,1,0,20,0,\"Enter account no.\"\r\n\r\nOK\r" },
				{ "\r\n+STGI: 0,1,0,20,0,\"Enter business no.\"\r\n\r\nOK\r", "\r\n+STGI: 0,1,0,20,0,\"Enter business no.\"\r\n\r\nOK\r" },
				{ "\r\n", "" },
				{ "\r\nOK\r\n\r\n+STIN: 3\r", "\r\nOK\r" },
				{ "\r\n359126030100432\r\n\r\nOK\r", "\r\n359126030100432\r\n\r\nOK\r" },
				{ "\r\nOK\r\n\r\n+STIN: 99\r", "\r\nOK\r" },
				{ "\r\n+CPIN: READY\r", "\r\n+CPIN: READY\r" },
				{ "\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter business no.\",0\r\n\r\nOK\r", "\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter business no.\",0\r\n\r\nOK\r" },
				{ "\r\n11.608.02.00.94\r\n\r\nOK\r", "\r\n11.608.02.00.94\r\n\r\nOK\r" },
				{ "\r\n+STGI: 1,\"Pay Bill 111111 Account 111111\nKsh10\",1\r\n\r\nOK\r", "\r\n+STGI: 1,\"Pay Bill 111111 Account 111111\nKsh10\",1\r\n\r\nOK\r" },
				{ "\r\n351596033790603\r\n\r\nOK\r", "\r\n351596033790603\r\n\r\nOK\r" },
				{ "\r\nOK\r", "\r\nOK\r" },
				{ "\r\n+CSQ: 14,99\r\n\r\nOK\r", "\r\n+CSQ: 14,99\r\n\r\nOK\r" },
				{ "\n\r\nOK\r", "\n\r\nOK\r" },
				{ "\r\nOK\r\n\r\n+STIN: 9\r", "\r\nOK\r" },
				{ "\r\n+CGATT: 0\r\n\r\nOK\r", "\r\n+CGATT: 0\r\n\r\nOK\r" },
				{ "\r\n+STGI: 1,\"Sent\nWait for M-PESA to reply\",0\r\n\r\nOK\r", "\r\n+STGI: 1,\"Sent\nWait for M-PESA to reply\",0\r\n\r\nOK\r" },
				{ " \r\nOK\r\n\r\n+STIN: 6\r", " \r\nOK\r" },
		};
		
		for(int i=0; i<goodInputsAndOutputs.length; ++i) {
			String[] inputAndOutput = goodInputsAndOutputs[i];
			
			// given
			in.setString(inputAndOutput[0]);
			refIn.setString(inputAndOutput[0]);
			StringBuilder buffer = new StringBuilder();
			StringBuilder refBuffer = new StringBuilder();
			
			// when
			csd.readResponseToBuffer(buffer);
			refImplementation.readResponseToBuffer(refBuffer);
			
			// then
			assertEquals("Reference implementation broken for entry " + i, inputAndOutput[1], refBuffer.toString());
			assertEquals("New implementation broken for entry " + i, inputAndOutput[1], buffer.toString());
		}
	}
	
	public void testReadResponseToBufferWithRinging() throws IOException, ServiceDisconnectedException {
		/* Test data of the form {streamContent, expectedBufferContent, ringCount=1, ringNumber=any} */
		Object[][] goodInputsAndOutputs = {
		};
		
		// TODO get test cases and implement this test
		
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
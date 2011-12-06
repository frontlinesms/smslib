package org.smslib.handler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mockito.InOrder;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.stk.StkConfirmationPrompt;
import org.smslib.stk.StkConfirmationPromptResponse;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkMenuItem;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;
import org.smslib.stk.StkValuePrompt;
import org.smslib.test.MultipleStringInputStream;
import org.smslib.test.SmsLibTestUtils;
import org.smslib.test.StringInputStream;
import org.smslib.test.StringOutputStream;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CATHandler_Wavecom_Stk}
 */
public class CATHandler_Wavecom_StkTest extends BaseTestCase {
	/** ASCII value 26 - control-z/EOF */
	private static final char CTRL_Z = 0x1a;
	private static final String[] VALID_CONFIRMATION_PROMPTS = {
		"+STGI: 1,\"Sure?\",1\nOK",
		"+STGI: 1,\"Send money to 0704593656 Ksh50\",1\nOK",
		"+STGI: 1,\"Send money to 0704593656\nKsh50\",1\nOK",
		"\r\n+STGI: 1,\"Send money to +254725452345\nKsh40000\",1\r\n\rOK\r"
	};
	private static final String[] VALID_VALUE_PROMPTS = {
		"+STGI: 0,0,4,4,0,\"Enter PIN\"",
		"+STGI: 0,1,0,20,0,\"Enter phone no.\"",
		"+STGI: 0,1,0,20,0,\"Enter phone no.\"\rOK",
		"\r\n+STGI: 0,1,0,20,0,\"Enter phone no.\"\r\n\r\nOK\r",
	};
	private CATHandler_Wavecom_Stk h;
	private CSerialDriver d;
	private StringInputStream in;
	private StringOutputStream out;
	private Logger l;
	private CService s;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		d = new CSerialDriver("COM1", 9600, s);
		
		in = new StringInputStream();
		inject(d, "inStream", in);
		
		out = new StringOutputStream();
		inject(d, "outStream", out);
		
		l = mock(Logger.class);
		s = SmsLibTestUtils.mockCService();
		h = new CATHandler_Wavecom_Stk(d, l, s);
	}

	public void testStkRootMenuRequest() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("\r\nOK\r\n\r\n+STIN: 99\r\n",
				"\r\n+STGI: \"Safaricom\"\r\n+STGI: 1,2,\"Safaricom+\",0,0\r\n+STGI: 128,2,\"M-PESA\",0,21\r\n\r\nOK\r");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		
		// then
		verifySentToModem("AT+STGR=99", "AT+STGI=0");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Safaricom", rootMenu.getTitle());
		assertNotNull(rootMenu.getRequest("Safaricom+"));
		assertNotNull(rootMenu.getRequest("M-PESA"));
	}
	
	
	public void testStkRootMenuRequestWithCmeError() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("\r\n+CME ERROR: 3\r\n",
				"\r\n+STGI: \"Safaricom\"\r\n+STGI: 1,2,\"Safaricom+\",0,0\r\n+STGI: 128,2,\"M-PESA\",0,21\r\n\r\nOK\r");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		
		// then
		verifySentToModem("AT+STGR=99", "AT+STGI=0");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Safaricom", rootMenu.getTitle());
		rootMenu.getRequest("Safaricom+");
		rootMenu.getRequest("M-PESA");
	}
	
	public void testStkConfirmationPrompt() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("\r\n> \r\nOK\r\n\r\n+STIN: 1\r\n",
				"\r\n+STGI: 1,\"Send money to +254702597711\nKsh60\",1\r\n\r\nOK\r\n",
				"\r\nOK\r\n\r\n+STIN: 9\r\n",
				"\r\n+STGI: \"Sending...\"\r\n\r\nOK\r\n\r\n+STIN: 1\r\n",
				"\r\n+STGI: 1,\"Sent\nWait for M-PESA to reply\",0\r\n\r\nOK\r\n");
	
		StkRequest pinEntrySubmitRequest = new StkValuePrompt().getRequest();
		
		// when the confirmation prompt should be triggered by the previous action
		StkResponse pinEntryResponse = h.stkRequest(pinEntrySubmitRequest, "1234");
		
		// then
		verifySentToModem("AT+STGR=3,1,1",
				"1234" + CTRL_Z + "AT+STGI=1");
		out.clearBuffer();
		assertTrue(pinEntryResponse instanceof StkConfirmationPrompt);
		
		// when the confirmation is sent
		StkResponse confirmationResponse = h.stkRequest(((StkConfirmationPrompt) pinEntryResponse).getRequest());
		
		// then
		verifySentToModem("AT+STGR=1,1,1",
				"AT+STGI=9","AT+STGI=1");
		assertTrue(confirmationResponse instanceof StkConfirmationPromptResponse);
		assertTrue(((StkConfirmationPromptResponse) confirmationResponse).isOk());
	}
	
	public void testStkValuePromptRegex_valid() {
		for(String validPrompt : VALID_VALUE_PROMPTS) {
			assertTrue(CATHandler_Wavecom_Stk.isValuePrompt(validPrompt));
		}
	}
	
	public void testStkValuePromptRegex_invalid() {
		for(String invalidPrompt : VALID_CONFIRMATION_PROMPTS) {
			assertFalse("Regex fails for value: " + invalidPrompt, CATHandler_Wavecom_Stk.isValuePrompt(invalidPrompt));
		}
	}
	
	public void testStkConfirmationPromptRegex_valid() {
		for(String validPrompt : VALID_CONFIRMATION_PROMPTS) {
			assertTrue("Regex fails for value: " + validPrompt, CATHandler_Wavecom_Stk.isConfirmationPrompt(validPrompt));
		}
	}
	
	public void testStkConfirmationPromptRegex_invalid() {
		for(String invalidPrompt : VALID_VALUE_PROMPTS) {
			assertFalse("Regex fails for value: " + invalidPrompt, CATHandler_Wavecom_Stk.isConfirmationPrompt(invalidPrompt));
		}
	}
	
	public void testStkValuePrompt() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("\r\nOK\r\n\r\n+STIN: 3\r\n",
				"\r\n+STGI: 0,1,0,20,0,\"Enter phone no.\"\r\n\r\nOK\r\n",
				"\r\n> \r\nOK\r\n\r\n+STIN: 3\r\n",
				"\r\n+STGI: 0,1,0,8,0,\"Enter amount\"\r\n\r\nOK\r\n");
		StkMenuItem enterPhoneNumber = new StkMenuItem("Enter phone no.", "6", "2");
		
		// when we trigger a relevant menu item
		StkResponse menuItemResponse = h.stkRequest(enterPhoneNumber.getRequest());
		
		// then we are given a prompt for a value
		verifySentToModem("AT+STGR=6,1,2",
				"AT+STGI=3");
		assertTrue("Unexpected response class: " + menuItemResponse.getClass(), menuItemResponse instanceof StkValuePrompt);
		
		// when we submit the value
		StkResponse phoneNumberSubmitResponse = h.stkRequest(((StkValuePrompt) menuItemResponse).getRequest(), "+12345678");
		
		// we are given a success message
		verifySentToModem("AT+STGR=3,1,1",
				"+12345678" + CTRL_Z + "AT+STGI=3");
		assertTrue(phoneNumberSubmitResponse instanceof StkResponse);
	}
	
	public void testStkSubmenuRequest() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("OK\n+STIN: 6",
				"+STGI: 0,0,0,\"M-PESA\"\n+STGI: 1,7,\"Send money\",0\n+STGI: 2,7,\"Withdraw cash\",0\n+STGI: 3,7,\"Buy airtime\",0\n+STGI: 4,7,\"Pay Bill\",0\n+STGI: 5,7,\"Buy Goods\",0\n+STGI: 6,7,\"ATM Withdrawal\",0\n+STGI: 7,7,\"My account\",0\nOK\n+STIN: 6",
				"OK\r\n\r\n+STIN: 6\r",
				"\r\n+STGI: 0,0,0\r\n+STGI: 1,2,\"Search SIM Contacts\",0\r\n+STGI: 2,2,\"Enter phone no.\",0\r\n\r\nOK\r");
		StkRequest submenuRequest = new StkMenuItem("M-PESA", "0", "1");
		
		// when we request a submenu
		StkResponse submenuResponse = h.stkRequest(submenuRequest);
		
		// then we are given the items in that menu
		verifySentToModem("AT+STGR=0,1,1",
				"AT+STGI=6");
		assertTrue(submenuResponse instanceof StkMenu);
		
		// when we request an item from the submenu
		h.stkRequest(((StkMenu) submenuResponse).getRequest("Send money"));
		
		// then correct menu item is corrected FIXME not sure this says what it means
		verifySentToModem("AT+STGR=6,1,1",
				"AT+STGI=6");
	}
	
	public void testStkErrorRequest() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses();
		
		// when
		try {
			h.stkRequest(StkRequest.GET_ROOT_MENU);
			fail();
		} catch(SMSLibDeviceException ex) {
			// expected
		}
		
		// then the handler should have tried to start the session
		verifySentToModem("AT+STGR=99");
	}
	
	public void testInitStkWithoutPin() throws Exception {
		// when
		mockModemResponses("OK", "OK", "+CPIN: READY");
		
		// when
		h.stkInit();
		
		// then
		verifySentToModem("AT+CMEE=1",
				"AT+STSF=1",
				"AT+CPIN?");
	}
	
	public void testInitStkWithPin() throws Exception {
		// when
		mockModemResponses("OK", "OK", "+CPIN: SIM PIN", "OK");
		when(s.getSimPin()).thenReturn("1234");
		
		// when
		h.stkInit();
		
		// then
		verifySentToModem("AT+CMEE=1",
				"AT+STSF=1",
				"AT+CPIN?",
				"AT+CPIN=\"1234\"");
	}

	/** Verifies that a list of serial commands were sent to the modem in a specific order
	 * and that no other commands were sent. */
	private void verifySentToModem(String... commands) throws IOException {
		StringBuilder expectedBuffer = new StringBuilder();
		for(String c : commands) expectedBuffer.append(c + '\r');
		String bufferText = out.getBufferText();
		out.clearBuffer();
		assertEquals(expectedBuffer.toString(), bufferText);
	}
	
	private void mockModemResponses(String... responses) throws IOException {
		for (int i = 0; i < responses.length; i++) {
			responses[i] = '\r' + responses[i] + "\r\n";
		}
		inject(d, "inStream", new MultipleStringInputStream(responses));
	}

	// TODO test cases where PIN is supplied incorrectly?
	// TODO test cases where PIN2 or PUK are required
}

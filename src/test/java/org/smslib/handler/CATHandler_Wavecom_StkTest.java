package org.smslib.handler;

import java.io.IOException;
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

import net.frontlinesms.junit.BaseTestCase;
import net.frontlinesms.test.smslib.SmsLibTestUtils;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CATHandler_Wavecom_Stk}
 */
public class CATHandler_Wavecom_StkTest extends BaseTestCase {
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
	private Logger l;
	private CService s;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		d = mock(CSerialDriver.class);
		l = mock(Logger.class);
		s = SmsLibTestUtils.mockCService();
		h = new CATHandler_Wavecom_Stk(d, l, s);
	}
	
	public void testStkRootMenuRequest() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("+STIN: 99",
				"\r+STGI: \"Safaricom\"\r+STGI: 1,2,\"Safaricom\",0,0\r+STGI: 129,2,\"M-PESA\",0,21\r\rOK");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		
		// then
		verifySentToModem("AT+STGR=99",
				"AT+STGI=0");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Safaricom", rootMenu.getTitle());
		rootMenu.getRequest("Safaricom");
		rootMenu.getRequest("M-PESA");
	}
	
	public void testStkConfirmationPrompt() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses(">",
				"OK\n+STIN: 1",
				"+STGI: 1,\"Send money to 0704593656\nKsh50\",1\nOK",
				"OK\r+STIN: 9",
				"\r+STGI: \"Sending...\"\r\n\rOK\n+STIN: 1",
				"\r+STGI: 1,\"Sent Wait for M-PESA to reply\",0\nOK");
	
		StkRequest pinEntrySubmitRequest = new StkValuePrompt().getRequest();
		
		// when the confirmation prompt should be triggered by the previous action
		StkResponse pinEntryResponse = h.stkRequest(pinEntrySubmitRequest, "1234");
		
		// then
		verifySentToModem("AT+STGR=3,1,1",
				"1234" + (char)0x1a,
				"AT+STGI=1");
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
		mockModemResponses("OK\n+STIN: 3",
				"+STGI: 0,1,0,20,0,\"Enter phone no.\"\nOK",
				">",
				"+STIN: 3",
				"+STGI: 0,1,0,8,0,\"Enter amount\"\nOK"/*final response is anything but an error*/);
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
				"+12345678" + ((char) 0x1A),
				"AT+STGI=3");
		assertTrue(phoneNumberSubmitResponse instanceof StkResponse);
	}
	
	public void testStkSubmenuRequest() throws SMSLibDeviceException, IOException {
		// given
		mockModemResponses("OK\n+STIN: 6",
				"+STGI: 0,0,0,\"M-PESA\"\n+STGI: 1,7,\"Send money\",0\n+STGI: 2,7,\"Withdraw cash\",0\n+STGI: 3,7,\"Buy airtime\",0\n+STGI: 4,7,\"Pay Bill\",0\n+STGI: 5,7,\"Buy Goods\",0\n+STGI: 6,7,\"ATM Withdrawal\",0\n+STGI: 7,7,\"My account\",0\nOK",
				"+STIN: 6");
		StkRequest submenuRequest = new StkMenuItem("M-PESA", "0", "1");
		
		// when we request a submenu
		StkResponse submenuResponse = h.stkRequest(submenuRequest);
		
		// then we are given the items in that menu
		verifySentToModem("AT+STGR=0,1,1",
				"AT+STGI=6");
		assertTrue(submenuResponse instanceof StkMenu);
		
		// when we request an item from the submenu
		h.stkRequest(((StkMenu) submenuResponse).getRequest("Send money"));
		
		// then correct menu item is corrected
		verifySentToModem("AT+STGR=6,1,1",
				"AT+STGI=6");
	}
	
	public void testStkErrorRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("\rERROR\r");
		
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
				"AT+CPIN=1234");
	}

	/** Verifies that a list of serial commands were sent to the modem in a specific order
	 * and that no other commands were sent. */
	private void verifySentToModem(String... commands) throws IOException {
		InOrder inOrder = inOrder(d);
		for(String command: commands) {
			inOrder.verify(d).send(command + '\r');
		}
		inOrder.verify(d, never()).send(anyString());
	}
	
	private void mockModemResponses(String response1, String... responseArray) throws IOException {
		List<String> responses = new LinkedList<String>(Arrays.asList(responseArray));
		responses.add("ERROR");
		when(d.getResponse()).thenReturn(response1, responses.toArray(new String[0]));
	}

	// TODO test cases where PIN is supplied incorrectly?
	// TODO test cases where PIN2 or PUK are required
}

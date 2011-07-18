package org.smslib.handler;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.handler.ATHandler.SynchronizedWorkflow;
import org.smslib.stk.StkConfirmationPrompt;
import org.smslib.stk.StkConfirmationPromptResponse;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkMenuItem;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;
import org.smslib.stk.StkValuePrompt;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CATHandler_Wavecom_Stk}
 */
public class CATHandler_Wavecom_StkTest extends BaseTestCase {
	private static final String[] VALID_CONFIRMATION_PROMPTS = {
		"+STGI: 1,\"Sure?\",1\nOK",
		"+STGI: 1,\"Send money to 0704593656 Ksh50\",1\nOK",
		"+STGI: 1,\"Send money to 0704593656\nKsh50\",1\nOK",
	};
	private static final String[] VALID_VALUE_PROMPTS = {
		"+STGI: 0,0,4,4,0,\"Enter PIN\"",
		"+STGI: 0,1,0,20,0,\"Enter phone no.\"",
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
		s = mock(CService.class);
		// Make sure that synchronized jobs run on the CService actually get executed - 
		// otherwise the mock will just return null!
		when(s.doSynchronized(any(SynchronizedWorkflow.class))).thenAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return ((SynchronizedWorkflow<?>) invocation.getArguments()[0]).run();
			}
		});
		h = new CATHandler_Wavecom_Stk(d, l, s);
	}
	
	public void testStkRootMenuRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("\r+STGI: \"Safaricom\"\r+STGI: 1,2,\"Safaricom\",0,0\r+STGI: 129,2,\"M-PESA\",0,21\r\rOK");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		// TODO wait for synchronised job to complete (how?)
		
		// then
		verify(d).send("AT+STGI=0\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Safaricom", rootMenu.getTitle());
		rootMenu.getRequest("Safaricom");
		rootMenu.getRequest("M-PESA");
	}
	
	public void testStkConfirmationPrompt() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("OK\n+STIN: 1",
				"+STGI: 1,\"Send money to 0704593656\nKsh50\",1\nOK",
				"OK");
		
		StkRequest pinEntrySubmitRequest = new StkValuePrompt().getRequest();
		
		// when
		// the confirmation prompt should be triggered by the previous action
		StkResponse pinEntryResponse = h.stkRequest(pinEntrySubmitRequest, "1234");
		
		// then
		assertTrue(pinEntryResponse instanceof StkConfirmationPrompt);
		
		// when
		// the confirmation is sent
		StkResponse confirmationResponse = h.stkRequest(((StkConfirmationPrompt) pinEntryResponse).getRequest());
		
		// then
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
		when(d.getResponse()).thenReturn("OK\n+STIN: 3",
				"+STGI: 0,1,0,20,0,\"Enter phone no.\"",
				">");
		StkMenuItem enterPhoneNumber = new StkMenuItem("Enter phone no.", "6", "2");
		
		// when we trigger a relevant menu item
		StkResponse menuItemResponse = h.stkRequest(enterPhoneNumber.getRequest());
		
		// then we are given a prompt for a value
		// FIXME these should be verified inorder
		verify(d).send("AT+STGR=6,1,2\r");
		verify(d).send("AT+STGI=3\r");
		assertTrue("Unexpected response class: " + menuItemResponse.getClass(), menuItemResponse instanceof StkValuePrompt);
		
		// when we submit the value
		StkResponse phoneNumberSubmitResponse = h.stkRequest(((StkValuePrompt) menuItemResponse).getRequest(), "+12345678");
		
		// we are given a success message
		verify(d).send("+12345678" + ((char) 0x1A) + '\r');
		assertTrue(phoneNumberSubmitResponse instanceof StkResponse);
	}
	
	public void testStkSubmenuRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("OK\n+STIN: 6",
				"+STGI: 0,0,0,\"M-PESA\"\n+STGI: 1,7,\"Send money\",0\n+STGI: 2,7,\"Withdraw cash\",0\n+STGI: 3,7,\"Buy airtime\",0\n+STGI: 4,7,\"Pay Bill\",0\n+STGI: 5,7,\"Buy Goods\",0\n+STGI: 6,7,\"ATM Withdrawal\",0\n+STGI: 7,7,\"My account\",0\nOK");
		StkRequest submenuRequest = new StkMenuItem("M-PESA", "0", "1");
		
		// when we request a submenu
		StkResponse submenuResponse = h.stkRequest(submenuRequest);
		
		// then we are given the items in that menu
		verify(d).send("AT+STGR=0,1,1\r");
		verify(d).send("AT+STGI=6\r");
		assertTrue(submenuResponse instanceof StkMenu);
		
		// when we request an item from the submenu
		h.stkRequest(((StkMenu) submenuResponse).getRequest("Send money"));
		
		// then correct menu item is corrected
		// FIXME following verify's should be inorder
		verify(d).send("AT+STGR=6,1,1\r");
		verify(d).send("AT+STGI=6\r");
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
		verify(d).send("AT+STGR=99\r");
	}
	
	public void testInitStkWithoutPin() throws Exception {
		// when
		when(d.getResponse()).thenReturn("OK", "OK", "+CPIN: READY");
		
		// when
		h.stkInit();
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+CMEE=1\r");
		inOrder.verify(d).send("AT+STSF=1\r");
		inOrder.verify(d).send("AT+CPIN?\r");
		inOrder.verify(d, never()).send(anyString());
	}
	
	public void testInitStkWithPin() throws Exception {
		// when
		when(d.getResponse()).thenReturn("OK", "OK", "+CPIN: SIM PIN", "OK");
		when(s.getSimPin()).thenReturn("1234");
		
		// when
		h.stkInit();
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+CMEE=1\r");
		inOrder.verify(d).send("AT+STSF=1\r");
		inOrder.verify(d).send("AT+CPIN?\r");
		inOrder.verify(d).send("AT+CPIN=1234\r");
		inOrder.verify(d, never()).send(anyString());
	}

	// TODO test cases where PIN is supplied incorrectly?
	// TODO test cases where PIN2 or PUK are required
}

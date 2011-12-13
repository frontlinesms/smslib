/**
 * 
 */
package org.smslib.handler;

import org.apache.log4j.Logger;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.handler.ATHandler.SynchronizedWorkflow;
import org.smslib.service.Protocol;
import org.smslib.ussd.ActionableUssdResponse;
import org.smslib.ussd.UssdNotification;
import org.smslib.ussd.UssdOperationNotSupportedResponse;
import org.smslib.ussd.UssdResponse;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CATHandler}.
 * @author Alex <alex@frontlinesms.com>
 */
public class CATHandlerTest extends BaseTestCase {
	/** Instance of {@link CATHandler} under test. */
	CATHandler h;
	CSerialDriver serialDriver;
	CService cService;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void setUp() throws Exception {
		Logger log = mock(Logger.class);
		serialDriver = mock(CSerialDriver.class);
		cService = mock(CService.class);
		when(cService.doSynchronized(any(SynchronizedWorkflow.class))).thenAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Exception {
				SynchronizedWorkflow<?> w = (SynchronizedWorkflow<?>) invocation.getArguments()[0];
				return w.run();
			}
		});
		h = new CATHandler(serialDriver, log, cService);
	}
	
	public void testSendMessage_PDU() throws Exception {
		testSendMessage_PDU_ok(42, "07915892000000F001000B915892214365F7000021493A283D0795C3F33C88FE06CDCB6E32885EC6D341EDF27C1E3E97E72E", 20);
		testSendMessage_PDU(42, "07915892000000F001000B915892214365F7000021493A283D0795C3F33C88FE06CDCB6E32885EC6D341EDF27C1E3E97E72E", 123,
				"", "AT+CMGS:123\r\nOK\r", null);
		testSendMessage_PDU(42, "07915892000000F001000B915892214365F7000021493A283D0795C3F33C88FE06CDCB6E32885EC6D341EDF27C1E3E97E72E", -1,
				"+CMSERROR:500");
		testSendMessage_PDU(0, "", -1, "+CMSERROR:500");
		testSendMessage_PDU(0, "", -1, "+cmserror:500");
		testSendMessage_PDU(0, "", -1, "RANDOM ERROR");
		testSendMessage_PDU(0, "", -1, "GOBBLEDEGOOK");
	}
	
	private void testSendMessage_PDU_ok(int size, String pdu, int expectedSmscReferenceNumber) throws Exception {
		testSendMessage_PDU(size, pdu, expectedSmscReferenceNumber, "AT+CMGS:" + expectedSmscReferenceNumber + "\r\n>\rOK\r", new String[0]);
	}
	
	private void testSendMessage_PDU(int size, String pdu, int expectedSmscReferenceNumber, String response1, String... responses) throws Exception {
		reset(serialDriver, cService);
		when(serialDriver.dataAvailable()).thenReturn(true);
		when(serialDriver.getResponse()).thenReturn(response1, responses);
		
		when(cService.getProtocol()).thenReturn(Protocol.PDU);
		
		int smscRefNumber = h.sendMessage(size, pdu, null, null);
		assertEquals(expectedSmscReferenceNumber, smscRefNumber);
		
		// Check the size, PDU and SUB character were sent
		verify(serialDriver).send("AT+CMGS=" + size + '\r');
		verify(serialDriver).send(pdu);
		verify(serialDriver).send((char)26);
	}
	
	public void testInitUssdAlreadySet() throws Exception {
		// given
		when(serialDriver.getResponse()).thenReturn("\r\n+CUSD: 1\r\n\r\nOK\r");
		
		// when
		h.initUssd();
		
		// then
		InOrder inOrder = inOrder(serialDriver);
		inOrder.verify(serialDriver).send("AT+CUSD?\r");
		inOrder.verify(serialDriver, never()).send(anyString());
	}
	
	public void testInitUssdNeedsSetting() throws Exception {
		// given
		when(serialDriver.getResponse()).thenReturn("\r\n+CUSD: 2\r\n\r\nOK\r", "\r\nOK\r");
		
		// when
		h.initUssd();
		
		// then
		InOrder inOrder = inOrder(serialDriver);
		inOrder.verify(serialDriver).send("AT+CUSD?\r");
		inOrder.verify(serialDriver).send("AT+CUSD=1\r");
		inOrder.verify(serialDriver, never()).send(anyString());
	}
	
	public void testInitUssdError1() throws Exception {
		// given
		when(serialDriver.getResponse()).thenReturn("\r\nERROR\r");
		
		// when
		try {
			h.initUssd();
			fail("Should have thrown exception.");
		} catch(SMSLibDeviceException ex) {
			// expected
		}
		
		// then
		InOrder inOrder = inOrder(serialDriver);
		inOrder.verify(serialDriver).send("AT+CUSD?\r");
		inOrder.verify(serialDriver, never()).send(anyString());
	}
	
	public void testInitUssdError2() throws Exception {
		// given
		when(serialDriver.getResponse()).thenReturn("\r\n+CUSD: 2\r\n\r\nOK\r", "\r\nERROR\r");
		
		// when
		try {
			h.initUssd();
			fail("Should have thrown exception.");
		} catch(SMSLibDeviceException ex) {
			// expected
		}
		
		// then
		InOrder inOrder = inOrder(serialDriver);
		inOrder.verify(serialDriver).send("AT+CUSD?\r");
		inOrder.verify(serialDriver).send("AT+CUSD=1\r");
		inOrder.verify(serialDriver, never()).send(anyString());
	}
	
	public void testActionableUssdResponse() throws Exception {
		final String[] ACTIONABLE_RESPONSES = {
				"\r\n+CUSD: 1,\"Internet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n",
				"\r\n+CUSD: 1,\"Please select:\n1:To Check Balance\n2:50 MB (at Ksh 100)\n3:200 MB (at Ksh 250)\n4:500 MB (at Ksh 499)\n5:1.5 GB (at Ksh 999)\n6:3 GB (at Ksh 1,999)\n98:MORE\",15\r\n",
				"\n\r\n+CUSD: 1,\"Internet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n",
				"\r\n+CUSD: 1,\"Invalid choice. Try again.\nInternet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n",
				"\r\n+CUSD: 1,\"Invalid choice. Try again.\n1:Soccer Updates\n2:GMail\n3:Facebook\n4:Twitter\n5:DSTV Mobile\n6:Safaricom Live\n7:You Tube\",15\r\n",
		};
		for(String s : ACTIONABLE_RESPONSES) {
			testUssdRequest(s, ActionableUssdResponse.class);
		}
	}
	
	public void testUssdNotificationResponse() throws Exception {
		final String[] NOTIFICATION_RESPONSES = {
				"\r\n+CUSD: 0,\"The service you requested is currently not available.\",15\r\n",
				"\r\n+CUSD: 2,\"Current balance is #1.40\", 0\r\n",
				"\r\n+CUSD: 2,\"Max Number of Menu Retries is Reached\",15\r\n",
				"\r\n+CUSD: 2,\"You will receive an SMS confirmation message shortly\",15\r\n",
				"\r\n+CUSD: 2,\"We have received the request. Please wait while we check your balance. Thank you for choosing Safaricom the better option.\",15\r\n",
		};
		for(String s : NOTIFICATION_RESPONSES) {
			testUssdRequest(s, UssdNotification.class);
		}
	}
	
	public void testUssdOperationNotSupportedResponse() throws Exception {
		testUssdRequest(	"\r\n+CUSD: 4\r\n", UssdOperationNotSupportedResponse.class);
	}
	
	private void testUssdRequest(String responseString, Class<?> expectedResponseClass) throws Exception {
		// given
		when(serialDriver.getResponse()).thenReturn("\r\n+CUSD: 1\r\n\r\nOK\r",
				"\r\nOK\r");
		when(serialDriver.getLastClearedBuffer()).thenReturn(responseString);
		
		// when
		UssdResponse response = h.ussdRequest("*544#");
		
		// then
		assertInstanceOf("Incorrect USSD response.", expectedResponseClass, response);
	}
	
	public void testUssdRequest2() throws Exception {
		// given
		when(serialDriver.getResponse()).thenReturn("\r\n+CUSD: 1\r\n\r\nOK\r",
				"\r\n+CUSD: 4\r\n\r\nOK\r");
		when(serialDriver.getLastClearedBuffer()).thenReturn("\n\r\n+CUSD: 1,\"Internet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n");
		
		// when
		UssdResponse response = h.ussdRequest("*544#");
		
		// then
		assertInstanceOf("Incorrect USSD response.", ActionableUssdResponse.class, response);
	}
	
	public void testIsValidCusdResponse() {
		final String[] VALID_CUSD_RESPONSES = {
				"\r\n+CUSD: 1,\"Internet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n",
				"\r\n+CUSD: 1,\"Invalid choice. Try again.\nInternet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n",
				"\r\n+CUSD: 0,\"The service you requested is currently not available.\",15\r\n",
				"\r\n+CUSD: 2,\"Current balance is #1.40\", 0\r\n",
				"\r\n+CUSD: 2,\"Max Number of Menu Retries is Reached\",15\r\n",
				"\r\n+CUSD: 1,\"Invalid choice. Try again.\n1:Soccer Updates\n2:GMail\n3:Facebook\n4:Twitter\n5:DSTV Mobile\n6:Safaricom Live\n7:You Tube\",15\r\n",
				"\r\n+CUSD: 4\r\n",
				"\r\n+CUSD: 2,\"You will receive an SMS confirmation message shortly\",15\r\n",
		};
		for(String r : VALID_CUSD_RESPONSES) {
			assertTrue("Failed for " + r, h.isValidCusdResponse(r));
		}
	}
	
	public void testIsNotValidCusdResponse() {
		final String[] INVALID_CUSD_RESPONSES = {
				"\nERROR\r\n",
		};
		for(String r : INVALID_CUSD_RESPONSES) {
			assertFalse(h.isValidCusdResponse(r));
		}
	}
	
	public void testParseUssdResponse() throws Exception {
		final Object[][] USSD_RESPONSES = {
				{"\r\n+CUSD: 1,\"Internet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE\",15\r\n",
					"Internet Options:\n1:KulaHappy\n2:Quick Links\n3:Daily Internet Bundles\n4:Data Bundles\n5:Unlimited Bundles\n6:Browse at 2/- \n98:MORE"},
		};
		for(Object[] r : USSD_RESPONSES) {
			// when
			UssdResponse u = h.parseUssdResponse((String) r[0]);
			
			// then
			assertTrue(u instanceof ActionableUssdResponse);
			ActionableUssdResponse aur = (ActionableUssdResponse) u;
			assertEquals(r[1], aur.getText());
		}
	}
}
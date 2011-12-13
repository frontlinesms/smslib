/**
 * 
 */
package org.smslib.handler;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mockito.InOrder;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.service.Protocol;

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
	
	@Override
	protected void setUp() throws Exception {
		Logger log = mock(Logger.class);
		serialDriver = mock(CSerialDriver.class);
		cService = mock(CService.class);
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
}

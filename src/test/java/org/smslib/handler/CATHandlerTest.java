/**
 * 
 */
package org.smslib.handler;

import org.apache.log4j.Logger;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.service.Protocol;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CATHandler}.
 * @author Alex <alex@frontlinesms.com>
 */
public class CATHandlerTest extends BaseTestCase {
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
		CSerialDriver serialDriver = mock(CSerialDriver.class);
		when(serialDriver.dataAvailable()).thenReturn(true);
		when(serialDriver.getResponse()).thenReturn(response1, responses);
		
		CService cService = mock(CService.class);
		when(cService.getProtocol()).thenReturn(Protocol.PDU);
		
		Logger log = mock(Logger.class);
		
		CATHandler handler = new CATHandler(serialDriver, log, cService);
		int smscRefNumber = handler.sendMessage(size, pdu, null, null);
		assertEquals(expectedSmscReferenceNumber, smscRefNumber);
		
		// Check the size, PDU and SUB character were sent
		verify(serialDriver).send("AT+CMGS=" + '"' + size + '"' + '\r');
		verify(serialDriver).send(pdu);
		verify(serialDriver).send((char)26);
	}
}

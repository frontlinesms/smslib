package org.smslib;

import org.smslib.CMessage.MessageType;
import org.smslib.CStatusReportMessage.DeliveryStatus;
import org.smslib.sms.SmsMessageEncoding;

import net.frontlinesms.junit.BaseTestCase;

public class CStatusReportMessageTest extends BaseTestCase {
	public void testConstruction0() throws Exception {
		// given
		String pdu = "06130C91527420121670110172111332E11101721113322100";

		// when
		CStatusReportMessage m = new CStatusReportMessage(pdu, 0, "SM", false);
		
		// then
		assertEquals(DeliveryStatus.Delivered, m.getDeliveryStatus());
	}
	
	public void testConstruction1() throws Exception {
		// given
		String pdu = "079152742205000006070A817020957711112112113391E11121121133912100";
		
		// when
		CStatusReportMessage m = new CStatusReportMessage(pdu, 0, "SM", true);
		
		// then
		assertDateEquals("dateOriginal", 1324467199000L, m.getDateOriginal());
		assertDateEquals("dateReceived", 1324456399000L, m.getDateReceived());
		assertEquals(0, m.getDcsByte());
		assertEquals(DeliveryStatus.Delivered, m.getDeliveryStatus());
		assertEquals(0, m.getDestinationPort());
		assertEquals(0, m.getMemIndex());
		assertEquals(0, m.getMpMaxNo());
		try { m.getMpRefNo(); fail(); } catch(NullPointerException _) {}
		assertEquals(0, m.getMpSeqNo());
		assertEquals(0, m.getPid());
		assertEquals(7, m.getRefNo());
		assertEquals(0, m.getSourcePort());
		assertEquals(MessageType.StatusReport, m.getType());
		assertTrue(m.getDate().longValue() <= System.currentTimeMillis());
		assertNull(m.getBinary());
		assertEquals(CStatusReportMessage.class, m.getClass());
		assertNull(m.getId());
		assertEquals("SM", m.getMemLocation());
		assertEquals(SmsMessageEncoding.GSM_7BIT, m.getMessageEncoding());
		assertEquals("Unexpected value for mem index.", new String[]{}, m.getMpMemIndex());
		assertEquals("00 - Succesful Delivery.", m.getText());
		assertEquals("0702597711", m.getOriginator());
	}
	
	public void testConstruction2() throws Exception {
		// given
		String pdu = "079152742205000006080A817020957711112112118374E11121121183742100";
		
		// when
		CStatusReportMessage m = new CStatusReportMessage(pdu, 0, "SM", true);
		
		// then
		assertDateEquals("dateOriginal", 1324467527000L, m.getDateOriginal());
		assertDateEquals("dateReceived", 1324456727000L, m.getDateReceived());
		assertEquals(0, m.getDcsByte());
		assertEquals(DeliveryStatus.Delivered, m.getDeliveryStatus());
		assertEquals(0, m.getDestinationPort());
		assertEquals(0, m.getMemIndex());
		assertEquals(0, m.getMpMaxNo());
		try { m.getMpRefNo(); fail(); } catch(NullPointerException _) {}
		assertEquals(0, m.getMpSeqNo());
		assertEquals(0, m.getPid());
		assertEquals(8, m.getRefNo());
		assertEquals(0, m.getSourcePort());
		assertEquals(MessageType.StatusReport, m.getType());
		assertTrue(m.getDate().longValue() <= System.currentTimeMillis());
		assertNull(m.getBinary());
		assertEquals(CStatusReportMessage.class, m.getClass());
		assertNull(m.getId());
		assertEquals("SM", m.getMemLocation());
		assertEquals(SmsMessageEncoding.GSM_7BIT, m.getMessageEncoding());
		assertEquals("Unexpected value for mem index.", new String[]{}, m.getMpMemIndex());
		assertEquals("00 - Succesful Delivery.", m.getText());
		assertEquals("0702597711", m.getOriginator());
	}
	
	public void testConstruction3() throws Exception {
		// given
		String pdu = "0791527422050000060911818967454365878967F5112112213012E11121122130122140";
		
		// when
		CStatusReportMessage m = new CStatusReportMessage(pdu, 0, "SM", true);
		
		// then
		assertDateEquals("dateOriginal", 1324469001000L, m.getDateOriginal());
		assertDateEquals("dateReceived", 1324458201000L, m.getDateReceived());
		assertEquals(0, m.getDcsByte());
		assertEquals(DeliveryStatus.Aborted, m.getDeliveryStatus());
		assertEquals(0, m.getDestinationPort());
		assertEquals(0, m.getMemIndex());
		assertEquals(0, m.getMpMaxNo());
		try { m.getMpRefNo(); fail(); } catch(NullPointerException _) {}
		assertEquals(0, m.getMpSeqNo());
		assertEquals(0, m.getPid());
		assertEquals(9, m.getRefNo());
		assertEquals(0, m.getSourcePort());
		assertEquals(MessageType.StatusReport, m.getType());
		assertTrue(m.getDate().longValue() <= System.currentTimeMillis());
		assertNull(m.getBinary());
		assertEquals(CStatusReportMessage.class, m.getClass());
		assertNull(m.getId());
		assertEquals("SM", m.getMemLocation());
		assertEquals(SmsMessageEncoding.GSM_7BIT, m.getMessageEncoding());
		assertEquals("Unexpected value for mem index.", new String[]{}, m.getMpMemIndex());
		assertEquals("02 - Errors, stopped retrying dispatch.", m.getText());
		assertEquals("98765434567898765", m.getOriginator());
	}
	
	public void testConstruction4() throws Exception {
		// given
		String pdu = "0006D60B911326880736F4111011719551401110117195714000";
		
		// when
		CStatusReportMessage m = new CStatusReportMessage(pdu, 0, "SM", true);
		
		// then
		assertDateEquals("dateOriginal", 1294765155000L, m.getDateOriginal());
		assertDateEquals("dateReceived", 1294765157000L, m.getDateReceived());
		assertEquals(0, m.getDcsByte());
		assertEquals(DeliveryStatus.Delivered, m.getDeliveryStatus());
		assertEquals(0, m.getDestinationPort());
		assertEquals(0, m.getMemIndex());
		assertEquals(0, m.getMpMaxNo());
		try { m.getMpRefNo(); fail(); } catch(NullPointerException _) {}
		assertEquals(0, m.getMpSeqNo());
		assertEquals(0, m.getPid());
		assertEquals(214, m.getRefNo());
		assertEquals(0, m.getSourcePort());
		assertEquals(MessageType.StatusReport, m.getType());
		assertTrue(m.getDate().longValue() <= System.currentTimeMillis());
		assertNull(m.getBinary());
		assertEquals(CStatusReportMessage.class, m.getClass());
		assertNull(m.getId());
		assertEquals("SM", m.getMemLocation());
		assertEquals(SmsMessageEncoding.GSM_7BIT, m.getMessageEncoding());
		assertEquals("Unexpected value for mem index.", new String[]{}, m.getMpMemIndex());
		assertEquals("00 - Succesful Delivery.", m.getText());
		assertEquals("+31628870634", m.getOriginator());
	}
}

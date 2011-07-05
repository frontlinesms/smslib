package org.smslib.handler;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mockito.InOrder;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkMenuItem;
import org.smslib.stk.StkMenuItemNotFoundException;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CATHandler_Wavecom_Stk}
 */
public class CATHandler_Wavecom_StkTest extends BaseTestCase {
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
		h = new CATHandler_Wavecom_Stk(d, l, s);
	}
	
	public void testStkRootMenuRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("+STGI: \"Safaricom\"\r+STGI: 1,2,\"Safaricom\",0,0\r+STGI: 129,2,\"M-PESA\",0,21");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		
		// then
		verify(d).send("AT+STGI=0\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Safaricom", rootMenu.getTitle());
		
		rootMenu.getRequest("Safaricom");
		rootMenu.getRequest("M-PESA");
	}
	
	public void testStkSubmenuWithTitleRequest() throws SMSLibDeviceException, IOException {
		// given	
		when(d.getResponse()).thenReturn("\rOK\r+STIN: 6\r","+STGI: 0,0,0,\"M-PESA\"\r+STGI: 1,7,\"Send money\",0\r+STGI: 2,7,\"Withdraw cash\"" +
				",0\r+STGI: 3,7,\"Buy airtime\",0\r+STGI: 4,7,\"Pay Bill\",0\r+STGI: 5,7,\"Buy Goods\",0\r+STGI: 6,7,\"ATM Withdrawal\",0\r" +
				"+STGI: 7,7,\"My account\",0");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(new StkMenuItem("M-PESA","0","129"));
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+STGR=0,1,129\r");
		inOrder.verify(d).send("AT+STGI=6\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "M-PESA", rootMenu.getTitle());

		rootMenu.getRequest("Send money");
		rootMenu.getRequest("My account");
	}
	
	
	public void testStkSubmenuWithoutTitleRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("\rOK\r+STIN: 6\r","+STGI: 0,0,0\r+STGI: 1,2,\"Search SIM Contacts\",0" +
		"\r+STGI: 2,2,\"Enter phone no.\",0");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(new StkMenuItem("Send Money","6","1"));
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+STGR=6,1,1\r");
		inOrder.verify(d).send("AT+STGI=6\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "", rootMenu.getTitle());
		
		rootMenu.getRequest("Enter phone no.");
	}
	
	public void testStkInputRequirementRequest() throws SMSLibDeviceException, IOException {
		String phoneNumber="0711640000";
		// given
		when(d.getResponse()).thenReturn("\rOK\r+STIN: 3\r","+STGI: 0,1,0,8,0,\"Enter amount\"\rOK");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(new StkMenuItem("Enter phone no.","3",""),phoneNumber);
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+STGR=3,1,1\r> 0711640000<ctrl+z>\r");
		inOrder.verify(d).send("AT+STGI=3\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Item", rootMenu.getTitle());
		
		rootMenu.getRequest("Enter amount");
	}
	
	public void testStkSubmenuConfirmRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("\rOK\r+STIN: 9\r","+STGI: \"Sending...\"\r");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(new StkMenuItem("Send money to 0711640000 Ksh1000","1",""));
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+STGR=1,1,1\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Sending...", rootMenu.getMenuItems().get(0).getText());
		
	}
	
	public void testStkErrorRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("\rERROR\r");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+STGI=0\r");
		
		assertTrue(rootMenuResponse.equals(StkResponse.ERROR));
	}
	
}

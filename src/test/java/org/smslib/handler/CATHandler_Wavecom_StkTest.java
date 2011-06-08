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
		// mock response for root menu
		//when(d.getResponse()).thenReturn("\r+STGI: \"Random STK Thingy\"\r+STGI: 1,2,\"Section 1\",0,0\r+STGI: 129,2,\"Section 2\",0,21\r\rOK");
		when(d.getResponse()).thenReturn("\r+STGI: \"Random STK Thingy\"\r+STGI: 1,2,\"Section 1\",0,0\r+STGI: 129,2,\"Section 2\",0,21\r\rOK");
		
		// when
		StkResponse rootMenuResponse = h.stkRequest(StkRequest.GET_ROOT_MENU);
		
		// then
		verify(d).send("AT+STGI=0\r");
		
		assertTrue(rootMenuResponse instanceof StkMenu);
		StkMenu rootMenu = (StkMenu) rootMenuResponse;

		assertEquals("Menu title was incorrect.", "Random STK Thingy", rootMenu.getTitle());
		try {
			rootMenu.getRequest("Section 1");
			rootMenu.getRequest("Section 2");
		} catch(StkMenuItemNotFoundException ex) {
			throw ex;
		}
	}
	
	public void testStkSubmenuRequest() throws SMSLibDeviceException, IOException {
		// given
		when(d.getResponse()).thenReturn("\r+STGI: \"Random STK Thingy\"\r+STGI: 1,2,\"Section 1\",0,0\r+STGI: 129,2,\"Section 2\",0,21\r\rOK");
		
		// when
		StkResponse stkResponse = h.stkRequest(new StkMenuItem("Section 2"));
		
		// then
		InOrder inOrder = inOrder(d);
		inOrder.verify(d).send("AT+STGR=0,1,129\r");
		inOrder.verify(d).send("AT+STGI=6\r");
	}
}

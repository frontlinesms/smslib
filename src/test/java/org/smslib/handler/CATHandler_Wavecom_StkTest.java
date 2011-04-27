package org.smslib.handler;

import org.apache.log4j.Logger;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.stk.StkRequest;

import net.frontlinesms.junit.BaseTestCase;

import static org.mockito.Mockito.*;

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
	
	public void testStkRootMenuRequest() throws SMSLibDeviceException {
		h.stkRequest(StkRequest.GET_ROOT_MENU);
		fail("Check that the expected AT commands were sent to the device.");
	}
}
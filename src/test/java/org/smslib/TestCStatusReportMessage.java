package org.smslib;

import net.frontlinesms.junit.BaseTestCase;

public class TestCStatusReportMessage extends BaseTestCase {
	private static final String[] TEST_PDUS = {
		"079144973770939906270C91449777621047012021216043000120212160930000",
	};
	
	public void testPduDecode() throws Exception {
		for(String pdu : TEST_PDUS) {
			CStatusReportMessage m = new CStatusReportMessage(pdu, 0, "", true);
			
		}
	}
}

/**
 * 
 */
package org.smslib.handler;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.smslib.CSerialDriver;
import org.smslib.CService;

/**
 * @author lupinus
 *
 */
public class CATHandler_Simcom_SIMCOM_SIM300 extends CATHandler {
	public CATHandler_Simcom_SIMCOM_SIM300(CSerialDriver serialDriver,
			Logger log, CService srv) {
		super(serialDriver, log, srv);
	}

	@Override
	public void switchToCmdMode() throws IOException {
		// this modem doesn't seem to need to switch to command mode, and
		// the usual +++ command causes it to return ERROR.  Consequently,
		// doing nothing here is the safest thing to do :-)
	}
}


/**
 * 
 */
package org.smslib;

import net.frontlinesms.junit.BaseTestCase;

import org.apache.log4j.Logger;
import org.smslib.handler.CATHandler;
import org.smslib.handler.CATHandler_SonyEricsson;
import org.smslib.handler.CATHandler_Huawei;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AbstractATHandler} class.
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class AbstractATHandlerTest extends BaseTestCase {
	/**
	 * Test the loading of {@link CATHandler}s by {@link AbstractATHandler#load(CSerialDriver, Logger, CService, String, String, String)}.
	 */
	public void testLoad() {
		// N.B. The manufacturer names used here ARE case-sensitive.
		testLoad(CATHandler.class, "", "");
		testLoad(CATHandler_SonyEricsson.class, "SonyEricsson", "K800i");
		testLoad(CATHandler_Huawei.class, "Huawei", "E1550");
		// More mapping tests should be added here
	}
	
	private void testLoad(Class<? extends AbstractATHandler> expectedHandler, String gsmDeviceManufacturer, String gsmDeviceModel) {
		CSerialDriver serialDriver = mock(CSerialDriver.class);
		Logger log = mock(Logger.class);
		CService srv = mock(CService.class);
		AbstractATHandler loadedHandler = AbstractATHandler.load(serialDriver, log, srv, gsmDeviceManufacturer, gsmDeviceModel, null);
		assertEquals(expectedHandler, loadedHandler.getClass());
	}
}

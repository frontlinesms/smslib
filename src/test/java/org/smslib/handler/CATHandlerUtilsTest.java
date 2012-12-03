/**
 * 
 */
package org.smslib.handler;

import net.frontlinesms.junit.BaseTestCase;

import org.apache.log4j.Logger;
import org.smslib.CSerialDriver;
import org.smslib.CService;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ATHandler} class.
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class CATHandlerUtilsTest extends BaseTestCase {
	/**
	 * Test the loading of {@link CATHandler}s by {@link ATHandler#load(CSerialDriver, Logger, CService, String, String, String)}.
	 */
	public void testLoad() {
		// N.B. The manufacturer names used here ARE case-sensitive.
		testLoad(CATHandler.class, null, null);
		testLoad(CATHandler.class, "", "");
		testLoad(CATHandler_SonyEricsson.class, "SonyEricsson", "K800i");
		testLoad(CATHandler_Huawei.class, "Huawei", "E1550");
		// More mapping tests should be added here
	}
	
	private void testLoad(Class<? extends ATHandler> expectedHandler, String gsmDeviceManufacturer, String gsmDeviceModel) {
		CSerialDriver serialDriver = mock(CSerialDriver.class);
		Logger log = mock(Logger.class);
		CService srv = mock(CService.class);
		ATHandler loadedHandler = CATHandlerUtils.load(serialDriver, log, srv, gsmDeviceManufacturer, gsmDeviceModel, null);
		assertEquals(expectedHandler, loadedHandler.getClass());
	}

	public void testCaseInsensitiveLoad() {
		// N.B. The manufacturer names used here ARE case-sensitive.
		testCaseInsensitiveLoad(CATHandler.class, null, null);
		testCaseInsensitiveLoad(CATHandler.class, "", "");
		testCaseInsensitiveLoad(CATHandler_SonyEricsson.class, "SONYERICSSON", "k800i");
		testCaseInsensitiveLoad(CATHandler_Huawei.class, "huawei", "e1550");
	}

	private void testCaseInsensitiveLoad(Class<? extends ATHandler> expectedHandler, String gsmDeviceManufacturer, String gsmDeviceModel) {
		CSerialDriver serialDriver = mock(CSerialDriver.class);
		Logger log = mock(Logger.class);
		CService srv = mock(CService.class);
		ATHandler loadedHandler = CATHandlerUtils.caseInsensitiveLoad(serialDriver, log, srv, gsmDeviceManufacturer, gsmDeviceModel, null);
		assertEquals(expectedHandler, loadedHandler.getClass());
	}
}

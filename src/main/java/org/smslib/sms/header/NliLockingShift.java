/**
 * 
 */
package org.smslib.sms.header;

import java.io.IOException;

import org.smslib.sms.PduInputStream;
import org.smslib.util.TpduUtils;

/**
 * @author Alex Anderson
 */
public class NliLockingShift extends NationalLanguageIndicator {
	private NliLockingShift(int nli) {
		super(TpduUtils.TP_UDH_IEI_NATIONAL_LANGUAGE_LOCKING_SHIFT, nli);
	}

	public static final NliLockingShift getFromStream(PduInputStream in) throws IOException {
		int nli = in.read();
		return new NliLockingShift(nli);
	}
}

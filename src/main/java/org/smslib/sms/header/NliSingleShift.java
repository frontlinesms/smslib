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
public class NliSingleShift extends NationalLanguageIndicator {
	private NliSingleShift(int nli) {
		super(TpduUtils.TP_UDH_IEI_NATIONAL_LANGUAGE_SINGLE_SHIFT, nli);
	}

	public static final NliSingleShift getFromStream(PduInputStream in) throws IOException {
		int nli = in.read();
		return new NliSingleShift(nli);
	}
}
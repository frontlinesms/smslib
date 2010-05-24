/**
 * 
 */
package org.smslib.sms.header;

import org.smslib.sms.UserDataHeaderPart;

/**
 * @author Alex Anderson
 */
public abstract class NationalLanguageIndicator implements UserDataHeaderPart {
	private final int iEId;
	private final int nli;
	
	protected NationalLanguageIndicator(int iEId, int nli) {
		this.iEId = iEId;
		this.nli = nli;
	}

	public int getIEId() {
		return this.iEId;
	}
	
	public int getNli() {
		return nli;
	}

	public int getLength() {
		return 1;
	}

	public byte[] toBinary() {
		throw new IllegalStateException("NYI");
	}
}

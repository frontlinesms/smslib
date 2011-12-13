package org.smslib.service;

import org.smslib.CService;

/**
 * Holds values representing receive mode.
 * @see CService#setReceiveMode(int)
 * @see CService#getReceiveMode()
 */
public enum ReceiveMode {
	/** Synchronous reading. */
	SYNC,
	/** Asynchronous reading - CMTI indications. */
	ASYNC_CNMI,
	/** Asynchronous reading - polling. */
	ASYNC_POLL;
}

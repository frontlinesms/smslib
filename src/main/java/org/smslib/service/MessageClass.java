package org.smslib.service;

/** Holds values representing the message class of the message to be read from the GSM device. */
public enum MessageClass {
	/** Read all messages. */
	ALL(4, "ALL"),
	/** Read unread messages. After reading, all returned messages will be marked as read. */
	UNREAD(0, "REC UNREAD"),
	/** Read already-read messages. */
	READ(1, "REC READ");

//> INSTANCE PROPERTIES
	/** text ID for this {@link MessageClass} when listing messages on a device in TEXT mode */
	private final String textModeId;
	/** integer ID for this {@link MessageClass} when listing messages on a device in PDU mode */
	private final int pduModeId;

//> CONSTRUCTORS
	/**
	 * Create a new {@link MessageClass}
	 * @param pduModeId value for {@link #pduModeId}
	 * @param textModeId value for {@link #textModeId}
	 */
	MessageClass(int pduModeId, String textModeId) {
		this.pduModeId = pduModeId;
		this.textModeId = textModeId;
	}

	/** @return the text ID for this {@link MessageClass} when listing messages on a device in TEXT mode. */
	public String getTextId() {
		return this.textModeId;
	}

	/** @return the integer ID for this {@link MessageClass} when listing messages on a device in PDU mode. */
	public int getPduModeId() {
		return this.pduModeId;
	}
}
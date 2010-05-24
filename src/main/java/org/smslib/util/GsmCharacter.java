/**
 * 
 */
package org.smslib.util;

/**
 * The details of how a GSM character is encoded in its table.
 * @author Alex Anderson
 */
class GsmCharacter {
	/** The byte value of this character, as found in a PDU */
	private final int byteValue;
	/** The character itself */
	private final char character;
	
	public GsmCharacter(int byteValue, char character) {
		assert(byteValue >= 0x00 && byteValue <= 0xFF) : "Byte value out of range: " + Integer.toString(byteValue, 16);
		this.byteValue = byteValue;
		this.character = character;
	}
	
	public int getByteValue() {
		return byteValue;
	}
	
	public char getCharacter() {
		return character;
	}
}

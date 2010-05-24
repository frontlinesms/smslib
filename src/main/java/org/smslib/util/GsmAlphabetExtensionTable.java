/**
 * 
 */
package org.smslib.util;

import java.util.HashSet;
import java.util.Set;

/**
 * The GSM National Alphabet Single Shift tables, as defined in 3GPP TS 23.038
 * @author Alex Anderson
 */
public enum GsmAlphabetExtensionTable implements GsmCharacterTable {
	DEFAULT(TpduUtils.TP_UDH_IEI_NLI_DEFAULT,
			new GsmCharacter(0x0a, '\u000c'), // FORM FEED
			new GsmCharacter(0x14, '\u005e'), // CIRCUMFLEX ACCENT 
			new GsmCharacter(0x28, '\u007b'), // LEFT CURLY BRACKET 
			new GsmCharacter(0x29, '\u007d'), // RIGHT CURLY BRACKET 
			new GsmCharacter(0x2f, '\\'), 
			new GsmCharacter(0x3c, '['), 
			new GsmCharacter(0x3d, '~'), 
			new GsmCharacter(0x3e, ']'), 
			new GsmCharacter(0x40, '\u007c'), // VERTICAL LINES 
			new GsmCharacter(0x65, '\u20ac') // EURO SIGN 
		),
	
	TURKISH(TpduUtils.TP_UDH_IEI_NLI_TURKISH,
			new GsmCharacter(0x14, '^'),
			new GsmCharacter(0x28, '{'),
			new GsmCharacter(0x29, '}'),
			new GsmCharacter(0x2f, '\\'),
			new GsmCharacter(0x3c, '['),
			new GsmCharacter(0x3d, '~'),
			new GsmCharacter(0x3e, ']'),
			new GsmCharacter(0x40, '|'), 
			new GsmCharacter(0x47, '\u011E'), // G-breve, uppercase
			new GsmCharacter(0x49, '\u0130'), // I-dotted, uppercase 
			new GsmCharacter(0x53, '\u015E'), // S+cedilla, upper-case
			new GsmCharacter(0x63, '\u00E7'), // C+cedilla, lower-case
			new GsmCharacter(0x65, '\u20AC'), // euro sign
			new GsmCharacter(0x67, '\u011F'), // G-breve, lower-case
			new GsmCharacter(0x69, '\u0131'), // I-dotless, lower-case 
			new GsmCharacter(0x73, '\u015F') // S+cedilla, lower-case
		),
	;
	
//> PROPERTIES
	/** The National Language Identifier for the language encoded in this table. */
	private final int nli;
	/** The characters found in this character table.  This is an array to ensure that it is
	 * accessed in a thread-safe manner. */
	private final GsmCharacter[] characters;
	
//> CONSTRUCTORS
	private GsmAlphabetExtensionTable(int nli, GsmCharacter... characters) {
		this.nli = nli;

		// Check that there are no duplicate byte values in the characters provided
		Set<Integer> usedByteValues = new HashSet<Integer>();
		for(GsmCharacter c : characters) {
			boolean added = usedByteValues.add(c.getByteValue());
			assert(added) :  "There is a duplicate definition for character value: " + Integer.toString(c.getByteValue(), 16);
		}
		this.characters = characters;
	}
	
//> ACCESSORS
	public int getNli() {
		return nli;
	}
	
	public char getCharacter(int byteValue) {
		for (int i = 0; i < this.characters.length; i++) {
			GsmCharacter c = this.characters[i];
			if(c.getByteValue() == byteValue) {
				return c.getCharacter();
			}
		}
		// TODO what should this return if there is no match?
		return ' ';
	}
	
	public int getByteValue(char character) {
		for (int i = 0; i < this.characters.length; i++) {
			GsmCharacter c = this.characters[i];
			if(c.getCharacter() == character) {
				return c.getByteValue();
			}
		}
		return -1;
	}
	
//> STATIC HELPERS
	/** @throws UnsupportedLanguageException if the supplied NLI is not supported */
	public static final GsmAlphabetExtensionTable getFromNli(int nli) {
		for(GsmAlphabetExtensionTable table : values()) {
			if(table.getNli() == nli) {
				return table;
			}
		}
		throw new UnsupportedLanguageException(nli);
	}

	/** @return <code>true</code> iff the indicated language is supported */
	public static boolean isNliSupported(int nli) {
		for(GsmAlphabetExtensionTable table : values()) {
			if(table.getNli() == nli) {
				return true;
			}
		}
		return false;
	}
}
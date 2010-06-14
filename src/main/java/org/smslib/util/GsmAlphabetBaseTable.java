/**
 * 
 */
package org.smslib.util;

/**
 * @author Alex Anderson
 */
public enum GsmAlphabetBaseTable implements GsmCharacterTable {
	/*
	 * It's important to unicode-encode characters here to make the build portable between
	 * different computers who assume different encodings when reading the source files. 
	 */
	
	/** The default GSM 7bit alphabet base table. */
	DEFAULT(TpduUtils.TP_UDH_IEI_NLI_DEFAULT,
			// Codes: 0x00-0x0f
			'@',
			'\u00A3', // POUND SIGN
			     '$', 
					'\u00A5', // YEN SIGN
					'\u00E8', // LATIN SMALL LETTER E WITH GRAVE
					'\u00E9', // LATIN SMALL LETTER E WITH ACUTE
					'\u00F9', // LATIN SMALL LETTER U WITH GRAVE
					'\u00EC', // LATIN SMALL LETTER I WITH GRAVE
					'\u00F2', // LATIN SMALL LETTER O WITH GRAVE
					'\u00C7', // LATIN CAPITAL LETTER C WITH CEDILLA
			                                   '\n',
	       			'\u00D8', // LATIN CAPITAL LETTER O WITH STROKE
	       			'\u00F8', // LATIN SMALL LETTER O WITH STROKE
				                                               '\r',
	       			'\u00C5', // LATIN CAPITAL LETTER A WITH RING ABOVE
	    			'\u00E5', // LATIN SMALL LETTER A WITH RING ABOVE
			// Codes: 0x10-0x1f
			'\u0394', // GREEK CAPITAL LETTER DELTA
					'_',
					'\u03A6', // GREEK CAPITAL LETTER PHI
					'\u0393', // GREEK CAPITAL LETTER GAMMA
					'\u039B', // GREEK CAPITAL LETTER LAMDA
					'\u03A9', // GREEK CAPITAL LETTER OMEGA
					'\u03A0', // GREEK CAPITAL LETTER PI
					'\u03A8', // GREEK CAPITAL LETTER PSI
					'\u03A3', // GREEK CAPITAL LETTER SIGMA
					'\u0398', // GREEK CAPITAL LETTER THETA
					'\u039E', // GREEK CAPITAL LETTER XI
					'\u00A0', // Escape to alphabet extension or national language single shift table.  "A receiving entity which does not understand the meaning of this escape mechanism shall display it as a space character."
					'\u00C6', // LATIN CAPITAL LETTER AE
					'\u00E6', // LATIN SMALL LETTER AE
					'\u00DF', // LATIN SMALL LETTER SHARP S (German)
					'\u00C9', // LATIN CAPITAL LETTER E WITH ACUTE
			// Codes: 0x20-0x2f                                                                      
			' ', '!', '"', '#',
					'\u00A4', // CURRENCY SIGN
			                         '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
			// Codes: 0x30-0x3f
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?',
			// Codes: 0x40-0x4f
			'\u00A1', // INVERTED EXCLAMATION MARK
			     'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
			// Codes: 0x50-0x5f
			'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
					'\u00C4', // LATIN CAPITAL LETTER A WITH DIAERESIS
					'\u00D6', // LATIN CAPITAL LETTER O WITH DIAERESIS
					'\u00D1', // LATIN CAPITAL LETTER N WITH TILDE
					'\u00DC', // LATIN CAPITAL LETTER U WITH DIAERESIS
					'\u00A7', // SECTION SIGN
			// Codes: 0x60-0x6f
			'\u00BF', // INVERTED QUESTION MARK
			     'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
			// Codes: 0x70-0x7f     
			'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
					'\u00E4', // LATIN SMALL LETTER A WITH DIAERESIS
					'\u00F6', // LATIN SMALL LETTER O WITH DIAERESIS
					'\u00F1', // LATIN SMALL LETTER N WITH TILDE
					'\u00FC', // LATIN SMALL LETTER U WITH DIAERESIS
					'\u00E0' // LATIN SMALL LETTER A WITH GRAVE
		);
	
//> PROPERTIES
	/** The National Language Identifier for the language encoded in this table. */
	private final int nli;
	/** The characters available in this alphabet.  The position in this array indicates the byte value
	 * of the character when encoded in the alphabet. */
	private char[] characters;
	
//> CONSTRUCTORS
	private GsmAlphabetBaseTable(int nli, char... characters) {
		assert(characters.length == 128) : "Character array was incorrect size.  Expected 128, but was: " + characters.length;
		
		this.nli = nli;
		this.characters = characters;
	}
	
//> ACCESSORS
	public int getNli() {
		return nli;
	}
	
	public char getCharacter(int byteValue) {
		if(byteValue < 0 || byteValue >= characters.length) {
			// Should return SPACE character for characters we can't cope with according to SPEC.  Ideally we wouldn't be here
			// if there were unrecognized characters, but you never know.
			return ' ';
		} else {
			return characters[byteValue];
		}
	}
	
	public int getByteValue(char character) {
		for (int i = 0; i < characters.length; i++) {
			if(characters[i] == character) {
				return i;
			}
		}
		return -1;
	}
	
//> STATIC HELPERS
	public static final GsmAlphabetBaseTable getFromNli(int nli) {
		for(GsmAlphabetBaseTable table : values()) {
			if(table.getNli() == nli) {
				return table;
			}
		}
		return null;
	}

	/** @return <code>true</code> iff the indicated language is supported */
	public static boolean isNliSupported(int nli) {
		for(GsmAlphabetBaseTable table : values()) {
			if(table.getNli() == nli) {
				return true;
			}
		}
		return false;
	}
}

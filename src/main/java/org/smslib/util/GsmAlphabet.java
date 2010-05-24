// SMSLib for Java
// An open-source API Library for sending and receiving SMS via a GSM modem.
// Copyright (C) 2002-2007, Thanasis Delenikas, Athens/GREECE
// Copyright (C) 2009, Alex Anderson, Masabi, Kiwanja
// Web Site: http://www.smslib.org
//
// SMSLib is distributed under the LGPL license.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

package org.smslib.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;



/**
 * Following this spec: ETSI TS 123 038 V8.2.0 (2008-10)
 * TODO Add detection to check if shift tables are required rather than using UCS-2 encoding on a message. // using shift tables is not necessarily a good idea, as it is not guaranteed supported by all devices receiving messages.
 * @author Alex
 */
public class GsmAlphabet {
	/**
	 * 7-bit GSM Alphabet value which escapes to alphabet extension or national language single shift table.
	 * "A receiving entity which does not understand the meaning of this escape mechanism shall display it as a space character."
	 */
	private static final byte ESCAPE_SHIFT = 0x1B;
	
	/**
	 * Checks a String to see if the characters in it are all valid 7-bit GSM characters.
	 * @param text
	 * @return <code>true</code> if all characters in the supplied text are valid 7-bit GSM; <code>false</code> otherwise.
	 */
	public static boolean areAllCharactersValidGSM(String text) {
		return areAllCharactersValidGsm(text, GsmAlphabetBaseTable.DEFAULT, GsmAlphabetExtensionTable.DEFAULT);
	}
	
	public static boolean areAllCharactersValidGsm(String text, GsmAlphabetBaseTable baseTable, GsmAlphabetExtensionTable shiftTable) {
		for(char c : text.toCharArray()) {
			if(baseTable.getByteValue(c) == -1 && shiftTable.getByteValue(c) == -1)
				return false;
		}
		return true;
	}


	/**
	 * FIXME rename this method gsmSeptets2string or similar
	 * Converts a byte[] containing 7-bit GSM alphabet values (1 value per byte) into a String
	 * containing the characters that the supplied byte[] represents.
	 * @param bytes
	 * @return
	 */
	public static String bytesToString(byte[] bytes) {
		return bytesToString(bytes, GsmAlphabetBaseTable.DEFAULT, GsmAlphabetExtensionTable.DEFAULT);
	}
	
	public static String bytesToString(byte[] bytes, int baseNli, int extensionNli) {
		GsmAlphabetBaseTable baseTable = GsmAlphabetBaseTable.getFromNli(baseNli);
		GsmAlphabetExtensionTable shiftTable = GsmAlphabetExtensionTable.getFromNli(extensionNli);
		
		return bytesToString(bytes, baseTable, shiftTable);
	}
	
	public static String bytesToString(byte[] bytes, GsmAlphabetBaseTable baseTable, GsmAlphabetExtensionTable shiftTable) {
		StringBuffer text = new StringBuffer(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			if(bytes[i] == ESCAPE_SHIFT) {
				text.append(shiftTable.getCharacter(bytes[++i]));
			} else text.append(baseTable.getCharacter(bytes[i]));
		}
		return text.toString();
	}

	
	/**
	 * FIXME rename this method octetStream2septetStream or similar
	 * Convert a stream of septets read as octets into a byte array containing the 7-bit
	 * values from the octet stream.
	 * FIXME old method, please remove
	 * @param encodedOctetStream octet stream encoded as a hexadecimal string.
	 * @return
	 */
	static byte[] pduToText(String encodedOctetStream) {
		if(encodedOctetStream.length() == 0) return new byte[0];
		return octetStream2septetStream(HexUtils.decode(encodedOctetStream), 0);
	}
	
	/**
	 * Convert a stream of septets read as octets into a byte array containing the 7-bit
	 * values from the octet stream.
	 * @param octets
	 * @param bitSkip
	 * FIXME pass the septet length in here, so if there is a spare septet at the end of the octet, we can ignore that
	 * @return
	 */
	static byte[] octetStream2septetStream(byte[] octets, int bitSkip) {
		return octetStream2septetStream(octets, bitSkip, ((8 * octets.length) - bitSkip) / 7);
	}
	
	public static byte[] octetStream2septetStream(byte[] octets, int bitSkip, int septetCount) {
		byte[] septets = new byte[septetCount];
		for(int newIndex=septets.length-1; newIndex>=0; --newIndex) {
			for(int bit=6; bit>=0; --bit) {
				int oldBitIndex = ((newIndex * 7) + bit) + bitSkip;
				if((octets[oldBitIndex >>> 3] & (1 << (oldBitIndex & 7))) != 0)
					septets[newIndex] |= (1 << bit);
			}
		}
		
		return septets;		
	}

	/**
	 * Encodes the supplied text with the 7-bit GSM character set, and places
	 * the encoded text in the supplied byte array.  Each septet takes
	 * up one byte of the generated array.
	 * @param text
	 * @return
	 */
	public static byte[] stringToBytes(String text) {
		return stringToBytes(text, GsmAlphabetBaseTable.DEFAULT, GsmAlphabetExtensionTable.DEFAULT);
	}
	
	public static byte[] stringToBytes(String text, int nliBase, int nliExtended) {
		GsmAlphabetBaseTable baseTable = GsmAlphabetBaseTable.getFromNli(nliBase);
		GsmAlphabetExtensionTable shiftTable = GsmAlphabetExtensionTable.getFromNli(nliExtended);
		
		return stringToBytes(text, baseTable, shiftTable);
	}
	
	public static byte[] stringToBytes(String text, GsmAlphabetBaseTable baseTable, GsmAlphabetExtensionTable shiftTable) {
		int textLength = text.length();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(textLength);
		for (int i = 0; i < textLength; i++) {
			char ch = text.charAt(i);
			// Try finding this character in the standard 7-bit GSM alphabet
			int idx = baseTable.getByteValue(ch);
			if(idx != -1) {
				// We've found the character, so write it.
				baos.write(idx);
				continue;
			}
			
			// Try finding this character in the extended 7-bit GSM alphabet
			idx = shiftTable.getByteValue(ch);
			if(idx != -1) {
				baos.write(ESCAPE_SHIFT);
				baos.write(idx);
				continue;
			}
			
			// We don't recognise this character, so just write a space!
			baos.write(' ');
		}
		return baos.toByteArray();
	}

	/**
	 * Splits text into the parts that can fit into each section of a GSM message.
	 * @param messageText The text we would like to split up.
	 * @param udhLength The length of the UDH.  Usually this method is called twice
	 *                   - first assuming no concat needed (so UDH length does not include it),
	 *                    and then again if concat is actually needed with the longer UDH length. 
	 * @return The message, split appropriately for this UDH length, making sure no escaped characters split over message boundaries.
	 */
	public static String[] splitText(String messageText, boolean isPorted)
	{
		GsmAlphabetBaseTable baseTable = GsmAlphabetBaseTable.DEFAULT;
		GsmAlphabetExtensionTable shiftTable = GsmAlphabetExtensionTable.DEFAULT;
		
		// hope we don't actuaslly need to concatenate, so split with a UDH that has no concat block
		String[] messageParts = splitText(messageText, TpduUtils.getUDHSize(true, isPorted, false), baseTable, shiftTable);
		// re-split the message with a header that includes concat info, if we do actually need it
		if(messageParts.length > 1)
			messageParts = GsmAlphabet.splitText(messageText, TpduUtils.getUDHSize(true, isPorted, true), baseTable, shiftTable);
		return messageParts;
	}
	
	/**
	 * Splits text into the parts that can fit into each section of a GSM message.
	 * @param text The text we would like to split up.
	 * @param udhLength The length of the UDH.  Usually this method is called twice (eg. by {@link #splitText(String, boolean)})
	 *                   - first assuming no concat needed (so UDH length does not include it),
	 *                    and then again if concat is actually needed with the longer UDH length. 
	 * @return The message, split appropriately for this UDH length, making sure no escaped characters split over message boundaries.  Splitting an empty string returns one empty part.
	 */
	static String[] splitText(String text, int udhLength, GsmAlphabetBaseTable baseTable, GsmAlphabetExtensionTable shiftTable) {
		if(text.length() == 0) return new String[]{""};
		
		ArrayList<String> strings = new ArrayList<String>();
		
		/** The number of bits at the start of this text that will be left blank. */
		int skipBits = calculateBitSkip(udhLength);
		/** Maximum length of the ecoded text, in bits. */
		int maxLength = (TpduUtils.MAX_PDU_SIZE - udhLength) * 8;
		/** Bits we've used so far. */
		int bitsUsed = skipBits;
		
		StringBuilder bob = new StringBuilder();
		
		// Iterate over every character in the string, calculating its character
		// code and making sure there is enough space to add it to the current string.
		// If there is not enough space, we must shunt it into the next string.
		for (int charIndex = 0; charIndex < text.length(); charIndex++) {
			char ch = text.charAt(charIndex);
			
			int characterSize = GsmAlphabet.getCharSize(ch, baseTable, shiftTable);
			
			// Check we have enough space 
			if(bitsUsed + characterSize > maxLength) {
				strings.add(bob.toString());
				bob.delete(0, Integer.MAX_VALUE);
				bitsUsed = skipBits;
			}
			
			bob.append(ch);
			bitsUsed += characterSize;
		}
		if(bob.length() > 0) strings.add(bob.toString());
		
		return strings.toArray(new String[strings.size()]);
	}
	
	private static int getCharSize(char ch, GsmAlphabetBaseTable baseTable, GsmAlphabetExtensionTable shiftTable) {
		// Try finding this character in the standard 7-bit GSM alphabet
		int idx = baseTable.getCharacter(ch);
		if(idx != -1) {
			// Standard characters are a single septet
			return 7;
		}
		
		// Try finding this character in the extended 7-bit GSM alphabet
		idx = shiftTable.getCharacter(ch);
		if(idx != -1) {
			// Extended characters take up 2 septets.
			return 14;
		}
		
		// Unknown characters are encoded as a space character, which is found in the standard alphabet
		return 7;		
	}
	
	/**
	 * Encodes the supplied text with the 7-bit GSM character set, and then packs this data into
	 * an octet stream.
	 * @param text
	 * @param udhLength The length in octets of this message's UDH, including the UDH's length octet.
	 * @return
	 */
	public static byte[] encode(String text, int udhLength) {
		if(text.length() == 0) return new byte[0];
		int skipBits = calculateBitSkip(udhLength);
		byte[] septets = stringToBytes(text);
		return septetStream2octetStream(septets, skipBits);
	}
	
	/**
	 * Convert a list of septet values into an octet stream, with a number of empty bits at the start.
	 * @param septets
	 * @param skipBits
	 * @return
	 */
	static byte[] septetStream2octetStream(byte[] septets, int skipBits) {
		int octetLength = (int) Math.ceil(((septets.length * 7) + skipBits) / 8.0);
		byte[] octets = new byte[octetLength];
		
		for (int i = 0; i < septets.length; i++) {
			for (int j = 0; j < 7; j++) {
				if ((septets[i] & (1 << j)) != 0) {
					int bitIndex = (i * 7) + j + skipBits;
					octets[bitIndex >>> 3] |= 1 << (bitIndex & 7);
				}
			}
		}
		
		return octets;
		
	}
	
	/**
	 * Calculates the number of empty bits to prefix encoded 7-bit GSM text
	 * The position of the MS data actually depends on the length of the UDH, for some reason.
	 * This number should be between 0 and 6.
	 * TODO rename this fill bits
	 * @param udhLength Length in octets of the User-Data-Header
	 * @return
	 */
	public static int calculateBitSkip(int udhLength) {
		int skip = ((udhLength << 3) % 7);
		if(skip != 0) {
			skip = 7 - skip;
		}
		return skip;
	}
}
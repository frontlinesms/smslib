/**
 * 
 */
package org.smslib.util;

/**
 * Exception thrown when a language is not supported.
 * @author Alex Anderson
 */
@SuppressWarnings("serial")
class UnsupportedLanguageException extends RuntimeException {
	/**
	 * @param nli the National Language Identifier that is not supported.
	 */
	UnsupportedLanguageException(int nli) {
		super("Language not supported for NLI: " + nli);
	}
}

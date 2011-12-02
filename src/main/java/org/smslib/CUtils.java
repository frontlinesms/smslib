// SMSLib for Java
// An open-source API Library for sending and receiving SMS via a GSM modem.
// Copyright (C) 2002-2007, Thanasis Delenikas, Athens/GREECE
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

package org.smslib;

public class CUtils
{
	public static String replace(String text, String symbol, String value)
	{
		StringBuilder buffer;

		while (text.indexOf(symbol) >= 0)
		{
			buffer = new StringBuilder(text);
			buffer.replace(text.indexOf(symbol), text.indexOf(symbol) + symbol.length(), value);
			text = buffer.toString();
		}
		return text;
	}

	/**
	 * Make the thread sleep; ignore InterruptedExceptions.
	 * @param millis
	 * @return the number of milliseconds actually slept for
	 */
	public static long sleep_ignoreInterrupts(long millis) {
		long startTime = System.currentTimeMillis();
		try {
			Thread.sleep(millis);
		} catch(InterruptedException ex) {}
		return System.currentTimeMillis() - startTime;
	}
}

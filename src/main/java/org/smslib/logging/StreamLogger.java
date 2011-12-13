package org.smslib.logging;

import java.io.PrintStream;

interface StreamLogger {
	PrintStream getLog();
	StringBuilder getBuffer();
	String getLogPrefix();
	char[] getTerminators();
}
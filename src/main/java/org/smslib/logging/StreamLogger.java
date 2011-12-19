package org.smslib.logging;

import java.io.PrintWriter;

interface StreamLogger {
	PrintWriter getLog();
	StringBuilder getBuffer();
	String getLogPrefix();
	char[] getTerminators();
}
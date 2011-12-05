package org.smslib.logging;

import org.apache.commons.lang3.StringEscapeUtils;

class LoggingUtils {
	private static final String THREAD = "${thread}";
	private static final String STACK = "${stack}";
	
	static void log(StreamLogger logger) {
		StringBuilder buffer = logger.getBuffer();
		logger.getLog().println(logger.getLogPrefix() + translate(buffer));
		buffer.delete(0, Integer.MAX_VALUE);
	}
	
	private static String translate(CharSequence s) {
		return StringEscapeUtils.escapeJava(s.toString());
	}
	
	static boolean isTerminator(StreamLogger logger, int i) {
		for(char c : logger.getTerminators()) if(c == i) return true;
		return false;
	}

	public static String formatLogPrefix(String logPrefix) {
		String log = logPrefix.replace(THREAD, Thread.currentThread().getName());
		if(log.contains(STACK)) {
			log = getLogPrefixWithStack(log);
		}
		return log;
	}

	private static String getLogPrefixWithStack(String logPrefix) {
		// build stack
		StringBuilder s = new StringBuilder();
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for(int i=stackTrace.length-1; i>=0; --i) {
			StackTraceElement trace = stackTrace[i];
			if(!trace.getClassName().equals("java.lang.Thread") &&
					!trace.getClassName().startsWith("org.smslib.logging.")) {
				if(s.length() > 0) {
					s.append('>');
				}
				s.append(trace.getClassName() + "." + trace.getMethodName() + ':' + trace.getLineNumber());
			}
		}
		return logPrefix.replace(STACK, s.toString());
	}
}
package org.smslib.stk;

public abstract class StkConfirmationPromptResponse implements StkResponse {
	public static final StkConfirmationPromptResponse OK = new StkConfirmationPromptResponse() {
		@Override
		public boolean isOk() {
			return true;
		}
	};
	
	public abstract boolean isOk();

	public static StkResponse createError(String response) {
		return new StkConfirmationPromptResponse() {
			@Override
			public boolean isOk() {
				return false;
			}
		};
	}
}

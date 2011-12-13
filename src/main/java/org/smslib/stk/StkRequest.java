package org.smslib.stk;

public interface StkRequest {
	public static final StkRequest GET_ROOT_MENU = new StkRequest() {
		public String toString() {
			return "StkRequest: get root menu";
		};
	};
}

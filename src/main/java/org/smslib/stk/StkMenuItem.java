package org.smslib.stk;

public class StkMenuItem implements StkRequest {
	/** ID of this item */
	private final String id;
	private final String text;
	/** ID of the menu this item belongs to */
	private final String menuId;

	public StkMenuItem(String id, String text, String menuId) {
		this.id = id;
		this.text = text;
		this.menuId = menuId;
	}

	public String getText() {
		return text;
	}
	
	public String getMenuId() {
		return menuId;
	}

	public String getId() {
		return id;
	}

	public StkRequest getRequest() {
		return this;
	}
}

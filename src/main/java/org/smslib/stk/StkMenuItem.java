package org.smslib.stk;

public class StkMenuItem implements StkRequest {
	private final String text;
	private final String menuId; //MenuId of the menu where the item belongs to
	private final String menuItemId; //Id of the item itself

	public StkMenuItem(String text, String menuId, String menuItemId) {
		this.text = text;
		this.menuId = menuId;
		this.menuItemId = menuItemId;
	}

	public String getText() {
		return text;
	}
	
	public String getMenuId() {
		return menuId;
	}

	public String getMenuItemId() {
		return menuItemId;
	}

	public StkRequest getRequest() {
		return this;
	}
}

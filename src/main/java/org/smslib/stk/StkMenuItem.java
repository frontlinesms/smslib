package org.smslib.stk;

public class StkMenuItem implements StkRequest {
	private String text;
	private String menuId; //MenuId of the menu where the item belongs to
	private String menuItemId; //Id of the item itself

	public StkMenuItem(String text, String menuId, String menuItemId) {
		super();
		this.text = text;
		this.menuId = menuId;
		this.menuItemId = menuItemId;
	}
	
	public StkMenuItem() {
		// TODO Auto-generated constructor stub
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
	
	public StkRequest getRequest(String string) {
		return this;
	}
}

package org.smslib.stk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StkMenu extends StkResponse {
	private final String title;
	private final List<StkMenuItem> menuItems;

	public StkMenu(String title, Object... menuItems) {
		this.title = title;
		
		List<StkMenuItem> tempMenuItems = new ArrayList<StkMenuItem>(menuItems.length);
		for(Object m : menuItems) {
			if(m instanceof String) {
				tempMenuItems.add(new StkMenuItem((String) m,"",""));
			} else if(m instanceof StkMenuItem) {
				tempMenuItems.add((StkMenuItem) m);
			} else throw new IllegalArgumentException();
		}
		this.menuItems = Collections.unmodifiableList(tempMenuItems);
	}
	
	public String getTitle() {
		return title;
	}

	public StkRequest getRequest(String menuOption) throws StkMenuItemNotFoundException {
		for(StkMenuItem m : this.menuItems) {
			if(m.getText().equals(menuOption)) {
				return m;
			}
		}
		throw new StkMenuItemNotFoundException();
	}
	
	public StkMenuItem getMenuItem(String menuItemText){
		if (!this.menuItems.isEmpty()){
			int i=0;
			while(i<this.menuItems.size() && !this.menuItems.get(i).getText().contains(menuItemText)){
				i++;
			}
			return this.menuItems.get(i);
		}
		return StkMenuItem.ERROR;
	}
	public List<StkMenuItem> getMenuItems(){
	  return this.menuItems;
	 }
}

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

package org.smslib.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.smslib.CSerialDriver;
import org.smslib.CService;
import org.smslib.SMSLibDeviceException;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkMenuItem;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;

public class CATHandler_Wavecom_Stk extends CATHandler_Wavecom {
	public String regexNumberComma = "([\\d])+(,)+";
	public String regexNumber = "([\\d])+";
	
	
	public CATHandler_Wavecom_Stk(CSerialDriver serialDriver, Logger log, CService srv) {
		super(serialDriver, log, srv);
	}
	
	@Override
	public boolean supportsStk() {
		return true;
	}
	
	@Override
	public StkResponse stkRequest(StkRequest request, String... variables)
			throws SMSLibDeviceException, IOException {
		String initResponse="";
		
		if(request.equals(StkRequest.GET_ROOT_MENU)) {
			initResponse = serialSendReceive("AT+STGI=0");
			if (notOk(initResponse)){
				return StkResponse.ERROR;
			} else {
				return parseMenu(initResponse,"0");
			}
		} else if(request instanceof StkMenuItem) {
			return doMenuRequest((StkMenuItem) request, variables);
		} else return null;		
 	}

	private StkResponse doMenuRequest(StkMenuItem request, String... variables) throws IOException {
		String menuId;
		String variable="";
		String initResponse="";
	
		// test if Item or menuItem
		if ( request.getMenuItemId().equals("")){
			//Item: add variables if any needed
			if ( variables.length==1 ){
				variable = "\r> "+ variables[0] + "<ctrl+z>";
			}
			
			if ( !request.getText().contains("Send money")){
//				System.out.println("KIM: ITEM => get MenuItemId => AT+STGR="+ request.getMenuId()+",1");
				initResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1"+variable);
				if (notOk(initResponse)){
					return StkResponse.ERROR;
				} else {
					menuId = getMenuId(initResponse);
					//System.out.println("AT+STGI="+menuId);
					initResponse = serialSendReceive("AT+STGI="+menuId);
					if (notOk(initResponse)){
						return StkResponse.ERROR;
					} else {
						return (parseMenu(initResponse,menuId));
					}
				}
			} else {
				initResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1");
				if (notOk(initResponse)){
					return StkResponse.ERROR;
				} else {
					//System.out.println("AT+STGR="+ request.getMenuId()+",1");
					return (parseMenu(initResponse,""));
				}
			}
			
		} else {
			//MenuItem: retrieve next menu
			initResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1,"+request.getMenuItemId());
			if (notOk(initResponse)){
				return StkResponse.ERROR;

			} else {
				menuId = getMenuId(initResponse);
				initResponse = serialSendReceive("AT+STGI="+menuId);
				if (notOk(initResponse)){
					return StkResponse.ERROR;
				} else {
//					System.out.println("KIM: MENUITEM => get MenuItemId for next Menu => AT+STGR="+ request.getMenuId()+",1,"+request.getMenuItemId());
//					System.out.println("AT+STGI="+menuId);
					return (parseMenu(initResponse,menuId));
				}
			}
		}
	}

	private StkResponse parseMenu(String serialSendReceive, String menuId) {
		String title = parseMenuTitle(serialSendReceive);
		List<StkMenuItem> menuItems = parseMenuItems(serialSendReceive, menuId);
		return new StkMenu(title, menuItems.toArray());
	}
	
	private String parseMenuTitle(String serialSendReceive) {
		Matcher matcher = Pattern.compile("\\+STGI: (([0],)+)?\\\"([\\w](.)*)+\\\"").matcher(serialSendReceive);
		if (matcher.find()){
			String uncleanTitle = matcher.group();
			uncleanTitle = uncleanTitle.replace("+STGI: ", "");
			uncleanTitle = uncleanTitle.replace("\"", "");
			uncleanTitle = uncleanTitle.replaceAll(regexNumberComma, "");
//			System.out.println("KIM CLEANTITLE: "+uncleanTitle);
			return uncleanTitle;
		} else {
//			System.out.println("KIM CLEANTITLE: Item");
	        return "Item";	
		}
	}

	private List<StkMenuItem> parseMenuItems(String serialSendReceive, String menuId) {
		System.out.println("parseMenuItems");
		ArrayList<StkMenuItem> items = new ArrayList<StkMenuItem>();
		String uncleanTitle = "";
		String menuItemId = "";

		Matcher matcher = Pattern.compile("\\+STGI: ((([\\d])+,)(([\\d])+,)+)+\\\"([\\w](.)*)+\\\"").matcher(serialSendReceive);
		while (matcher.find() ){
			uncleanTitle = matcher.group();
			// retrieve menuItemId
			Matcher matcherMenuItemId = Pattern.compile(regexNumber).matcher(uncleanTitle);
			if (matcherMenuItemId.find() && !matcherMenuItemId.group().equals("0")){
				menuItemId = matcherMenuItemId.group();
			}
			// clean the title
			uncleanTitle = uncleanTitle.replace("+STGI: ", "");
			uncleanTitle = uncleanTitle.replace("\"", "");
			uncleanTitle = uncleanTitle.replaceAll(regexNumberComma, "");

			System.out.println("parseMenuItems cleanItemMenu:"+ uncleanTitle+"||menuId:"+menuId+"||MenuItemId:"+menuItemId);
			items.add(new StkMenuItem(uncleanTitle,menuId,menuItemId));
		}
		return items;
	}

	private StkResponse getMenu(String menuResp) {
		Object[] menu = menuResp.split("\n");
		StkMenu m = new StkMenu(menuResp);
		return m;
	}

	// Function which is getting the menuId in the response.
	private String getMenuId(String serialSendReceive) {
		Matcher matcher = Pattern.compile("\\d").matcher(serialSendReceive);
		if (matcher.find()){
			return matcher.group();
		}
		return null;
	}

	private boolean notOk(String initResponse) {
		// TODO Auto-generated method stub
		return initResponse.contains("ERROR");
	}
	
	
}

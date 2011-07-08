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
import org.smslib.stk.StkInputRequiremnent;

public class CATHandler_Wavecom_Stk extends CATHandler_Wavecom {
	public String regexNumberComma = "([\\d])+(,)+";
	public String regexNumber = "([\\d])+";
	private String getNewSessionId = "";
	
	
	public CATHandler_Wavecom_Stk(CSerialDriver serialDriver, Logger log, CService srv) {
		super(serialDriver, log, srv);
	}
	
	@Override
	public void initStorageLocations() throws IOException {
		storageLocations = "SMME";
	}
	
	@Override
	public boolean supportsStk() {
		return true;
	}
	
	@Override
	public void stkInit() throws SMSLibDeviceException, IOException {
		srv.doSynchronized(new SynchronizedWorkflow<Object>() {
			public Object run() throws IOException {
				serialSendReceive("AT+CMEE=1");
		 		serialSendReceive("AT+STSF=1");
				String pinResponse = serialSendReceive("AT+CPIN?");
				if(isWaitingForPin(pinResponse)) {
					serialSendReceive("AT+CPIN="+srv.getSimPin());
				}
				return null;
			}
		});
	}

	public boolean stkStartNewSession() throws SMSLibDeviceException, IOException {
		String initResponse = serialSendReceive("AT+STGR=99");
		if (notOk(initResponse)){
			if(initResponse.contains("+CME ERROR: 3")){
				return true;
			}else{
				return false;
			}
		} else {
			String menuId = getMenuId(initResponse);
			getNewSessionId = menuId;
			if(menuId.equals("99")){
				return true;
			}else if(menuId.equals("6")){
				initResponse = serialSendReceive("AT+STGR=99");
				if (notOk(initResponse)){
				} else {
					return true;
				}
				//return false;
			}
		}
		return true;
	}
	
	@Override
	public StkResponse stkRequest(final StkRequest request, final String... variables)
			throws SMSLibDeviceException, IOException {
		return srv.doSynchronized(new SynchronizedWorkflow<StkResponse>() {
			public StkResponse run() throws IOException, SMSLibDeviceException {	
				if(request.equals(StkRequest.GET_ROOT_MENU)) {
					String initResponse = "";

					if(stkStartNewSession()){
						initResponse = serialSendReceive("AT+STGI=0");
						if (notOk(initResponse)){
							return StkResponse.ERROR;
						} else {
							while(initResponse.contains("+STIN")) {
								try {
									stkRequest(StkRequest.GET_ROOT_MENU);
								} catch (SMSLibDeviceException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							return parseMenu(initResponse, "0");
						}	
					} else {
						return doMenuRequest((StkMenuItem) request, getNewSessionId);
					}
				} else if(request instanceof StkMenuItem) {
					return doMenuRequest((StkMenuItem) request, variables);
				} else return null;	
			}
		});
 	}

	private StkResponse doMenuRequest(StkMenuItem request, String... variables) throws IOException {
		// test if Item or menuItem
		if (request.getMenuItemId().equals("")) {
			if (!request.getText().contains("Send money")) { // FIXME DEFINITELY SHOULD NOT BE SPECIAL HANDLING HERE FOR SEND MONEY
				String variable = variables.length==1? variables[0] : "";
				
				// have to read all as pertinent information is supplied AFTER the "OK"
				String stgrResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1"+request.getMenuItemId());
				// TODO wait a bit...
				if (notOk(stgrResponse)) {
					return StkResponse.ERROR;
				} else {
					stgrResponse = serialSendReceive(variable);
					if (notOk(stgrResponse)) {
						return StkResponse.ERROR;
					} else {
						String menuId = getMenuId(stgrResponse);
						String stgiResponse = serialSendReceive("AT+STGI="+menuId);
						if (notOk(stgiResponse)){
							return StkResponse.ERROR;
						} else {
							while(stgiResponse.contains("+STIN")) {
								stgiResponse = serialSendReceive("AT+STGI="+menuId);
							}
							if(menuId.equals("1")){
								String stgrRequest = serialSendReceive("AT+STGR=1,1,1");
								if (notOk(stgrRequest)) {
									return StkResponse.ERROR;
								} else {
									menuId = getMenuId(stgrRequest);
									stgiResponse = serialSendReceive("AT+STGI="+menuId);
									if (notOk(stgiResponse)) {
										return StkResponse.ERROR;
									} else {
										menuId = getMenuId(stgiResponse);
										return parseMenu(stgiResponse, menuId);
									}
								}
							}
							return parseMenu(stgiResponse, menuId);
						}
					}
				}
			} else {
				String initResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1");
				if (notOk(initResponse)) {
					return StkResponse.ERROR;
				} else {
					return (parseMenu(initResponse,""));
				}
			}
			
		} else {
			//MenuItem: retrieve next menu
			String initResponse="";
			initResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1,"+request.getMenuItemId());
			if (notOk(initResponse)){
				return StkResponse.ERROR;
			} else {
				String menuId = getMenuId(initResponse);
				initResponse = serialSendReceive("AT+STGI="+menuId);
				if (notOk(initResponse)){
					return StkResponse.ERROR;
				} else {
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
		  Matcher matcher = Pattern.compile("\\+STGI: ((([\\d])+,)+)?(\\\"[\\w -.]+\\\")?").matcher(serialSendReceive);
		  //Test to find out if the string is an inputRequirement
		  String[] splitSerialSendReceive = serialSendReceive.split("\\+STGI");
		  if (splitSerialSendReceive.length > 2){
		   if (matcher.find()){
		    String uncleanTitle = matcher.group();
		    uncleanTitle = uncleanTitle.replace("+STGI: ", "");
		    uncleanTitle = uncleanTitle.replace("\"", "");
		    uncleanTitle = uncleanTitle.replaceAll(regexNumberComma, "");
		    return uncleanTitle;
		   } else {
		    //TODO
		    return "ERROR TITLE";
		   }
		  }
		   else {
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

	private String getMenuId(String response) {
		Matcher matcher = Pattern.compile("\\d+").matcher(response);
		if (matcher.find()){
			return matcher.group();
		}
		return null;
	}

	private boolean notOk(String initResponse) {
		return initResponse.contains("ERROR");
	}
}

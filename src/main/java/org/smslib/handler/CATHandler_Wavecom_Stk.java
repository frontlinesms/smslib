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
import org.smslib.stk.StkConfirmationPrompt;
import org.smslib.stk.StkConfirmationPromptResponse;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkMenuItem;
import org.smslib.stk.StkRequest;
import org.smslib.stk.StkResponse;
import org.smslib.stk.StkValuePrompt;

public class CATHandler_Wavecom_Stk extends CATHandler_Wavecom {
	private static final String VALUE_PROMPT_REGEX = "\\s*\\+STGI: (\\d+,){5}\".*\"(\\s+OK\\s*)?";
	private static final Pattern CONFIRMATION_PROMPT_REGEX = Pattern.compile("\\s*\\+STGI: \\d+,\".*\",\\d+\\s+OK\\s*", Pattern.DOTALL);
	public String regexNumberComma = "([\\d])+(,)+";
	public String regexNumber = "([\\d])+";
	
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

	/** Starts a new STK session if required. */
	public void stkStartNewSession() throws IOException, SMSLibDeviceException {
		String initResponse = serialSendReceive("AT+STGR=99");
		if (notOk(initResponse)) {
			if(!initResponse.contains("+CME ERROR: 3")){
				throw new SMSLibDeviceException("Unable to start new session: " + initResponse);
			}
		} else {
			String menuId = getMenuId(initResponse);
			if(menuId.equals("99")) {
			} else if(menuId.equals("6")) {
				initResponse = serialSendReceive("AT+STGR=99");
			}
		}
	}
	
	@Override
	public StkResponse stkRequest(final StkRequest request, final String... variables)
			throws SMSLibDeviceException, IOException {
		return srv.doSynchronized(new SynchronizedWorkflow<StkResponse>() {
			public StkResponse run() throws IOException, SMSLibDeviceException {
				System.out.println("class: " + request.getClass());
				System.out.println("crct:  " + (request instanceof StkConfirmationPrompt));
				if(request.equals(StkRequest.GET_ROOT_MENU)) {
					stkStartNewSession();
					String initResponse = serialSendReceive("AT+STGI=0");
					if (notOk(initResponse)){
						return StkResponse.ERROR;
					} else {
						while(initResponse.contains("+STIN")) {
							try {
								stkRequest(StkRequest.GET_ROOT_MENU);
							} catch (SMSLibDeviceException e) {
								log.warn(e);
								e.printStackTrace(); // FIXME handle this properly
							}
						}
						return parseMenu(initResponse, "0");
					}	
				} else if(request instanceof StkMenuItem) {
					return doMenuRequest((StkMenuItem) request);
				} else if(request.equals(StkValuePrompt.REQUEST)) {
					return handleValuePromptRequest(request, variables);
				} else /*if(request instanceof StkConfirmationPrompt)*/ {
					// 1[confirm],1[dunno, but always there],1[optional it seems]
					String stgrResponse = serialSendReceive("AT+STGR=1,1,1");
					if(stgrResponse.contains("OK")) {
						String stgiResponse = serialSendReceive("AT+STGI=" + extractNumber(stgrResponse, 1));
						if(stgiResponse.contains("OK")) {
							return StkConfirmationPromptResponse.OK;
						}
					}
					return StkConfirmationPromptResponse.createError(stgrResponse);
				} /*else return null;	*/
			}
		});
 	}

	private StkResponse handleValuePromptRequest(StkRequest request, String... variables) throws IOException {
		// 3[mode=input],1[not sure],1[this seems to be optional]
		serialSendReceive("AT+STGR=3,1,1");
		// Suffix variable with "EOF"/"End of file"/"Ctrl-z"
		String submitResponse = serialSendReceive(variables[0] + (char)0x1A);
		String menuId = getMenuId(submitResponse);
		String next = serialSendReceive("AT+STGI=" + menuId);
		return parseStkResponse(next, menuId);
	}
	
	private StkResponse parseStkResponse(String serialResponse, String menuId) {
		if(isValuePrompt(serialResponse)) {
			return new StkValuePrompt();
		} else if(isConfirmationPrompt(serialResponse)) {
			return new StkConfirmationPrompt();
		} else {
			return parseMenu(serialResponse, menuId);
		}
	}
	
	static boolean isConfirmationPrompt(String serialResponse) {
		return CONFIRMATION_PROMPT_REGEX.matcher(serialResponse).matches();
	}

	static boolean isValuePrompt(String serialResponse) {
		return serialResponse.matches(VALUE_PROMPT_REGEX);
	}

	private StkResponse doMenuRequest(StkMenuItem request) throws IOException {
		String initialResponse = serialSendReceive("AT+STGR=" + request.getMenuId() + ",1," + request.getMenuItemId());
		String newMenuId = extractNumber(initialResponse, 1);
		String secondaryResponse = serialSendReceive("AT+STGI=" + newMenuId);
		return parseStkResponse(secondaryResponse, newMenuId);
	}

	/** Extract the nth set of digits from the supplied string */
	private static String extractNumber(String s, int n) {
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(s);
		while(n-- > 0) m.find();
		return m.group();
	}

	private StkResponse doMenuRequest(StkMenuItem request, String[] variables) throws IOException {
		// test if Item or menuItem
		if (request.getMenuItemId().equals("")) {
			if (!request.getText().contains("Send money")) { // FIXME DEFINITELY SHOULD NOT BE SPECIAL HANDLING HERE FOR SEND MONEY
				String variable = variables.length==1? variables[0] : "";
				
				// have to read all as pertinent information is supplied AFTER the "OK"
				String stgrResponse = serialSendReceive("AT+STGR="+ request.getMenuId()+",1,"+request.getMenuItemId());
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
			items.add(new StkMenuItem(uncleanTitle, menuId, menuItemId));
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

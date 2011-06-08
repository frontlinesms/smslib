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
		
		if(request.equals(StkRequest.GET_ROOT_MENU)) {
			return parseMenu(serialSendReceive("AT+STGI=0"));
		} else if(request instanceof StkMenuItem) {
			return doMenuRequest((StkMenuItem) request);
		} else return null;
		
//		// if the request is get_root_menu
//		String initResponse = "";
//		try {
//			initResponse = serialSendReceive("AT+STSF=1");
//			System.out.println("KIM - stkRequest:" + initResponse);
//			initResponse = serialSendReceive("AT+STGI=0");
//			System.out.println("KIM - stkRequest:" + initResponse);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if(notOk(initResponse)) {
//			return StkResponse.ERROR;
//		}
//		return null;
//			else {
//			return new StkResponse(initResponse);
//		}
		
//		String menuResp = null;
//		try {
//			menuResp = serialSendReceive("AT+STGI=0" + getMenuId(initResponse));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return getMenu(menuResp);
		
 	}

	private StkResponse doMenuRequest(StkMenuItem request) throws IOException {
		// FIXME implement generation of request from StkMenuItem
		// FIXME parse response from first request
		// FIXME implement generation of second request
		// TODO implement parsing of second response and creation of StkResponse (probably not necessary for this test)
		serialSendReceive("AT+STGR=0,1,129"); // FIXME this is nonsense
		serialSendReceive("AT+STGI=6"); // FIXME this is nonsense
		return null; // FIXME this is nonsense
	}

	private StkResponse parseMenu(String serialSendReceive) {
		String title = parseMenuTitle(serialSendReceive);
		List<StkMenuItem> menuItems = parseMenuItems(serialSendReceive);
		return new StkMenu(title, menuItems.toArray());
	}

	private List<StkMenuItem> parseMenuItems(String serialSendReceive) { // FIXME implement parsing properly
		Matcher matcher = Pattern.compile("\\+STGI: ((([\\d])+,)+)?\\\"[\\w ]+\\\"").matcher(serialSendReceive);
		if (matcher.find() ){
			for(int i=0;i<matcher.groupCount();i++){
				System.out.println(matcher.group(i));
			}
		}
		ArrayList<StkMenuItem> items = new ArrayList<StkMenuItem>();
		items.add(new StkMenuItem("Section 1")); // FIXME this is nonsense
		items.add(new StkMenuItem("Section 2")); // FIXME this is nonsense
		return items;
	}
	
	private String parseMenuTitle(String serialSendReceive) { // FIXME implement parsing properly
		Matcher matcher = Pattern.compile("\\+STGI: (([0],)+)?\\\"[\\w ]+\\\"").matcher(serialSendReceive);
		matcher.find();
		String uncleanTitle = matcher.group();
		uncleanTitle = uncleanTitle.replace("+STGI: ", "");
		uncleanTitle = uncleanTitle.replace("\"", "");
		return uncleanTitle;
	}

	private StkResponse getMenu(String menuResp) {
		Object[] menu = menuResp.split("\n");
		StkMenu m = new StkMenu(menuResp, menu);
		return m;
	}

	private String getMenuId(String initResponse) {
		// TODO Auto-generated method stub
		return initResponse.substring(12);
	}

	private boolean notOk(String initResponse) {
		// TODO Auto-generated method stub
		return initResponse.contains("ERROR");
	}
	
	
}

// SMSLib for Java v3
// A Java API library for sending and receiving SMS via a GSM modem
// or other supported gateways.
// Web Site: http://www.smslib.org
//
// Copyright (C) 2002-2010, Thanasis Delenikas, Athens/GREECE.
// SMSLib is distributed under the terms of the Apache License version 2.0
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.smslib.handler;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.smslib.CSerialDriver;
import org.smslib.CService;

/**
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 * CAT Handler for Huawei modems.
 */
public class CATHandler_Huawei extends CATHandler
{
	/** @see CATHandler#CATHandler(CSerialDriver, Logger, CService) */
	public CATHandler_Huawei (CSerialDriver serialDriver, Logger log, CService srv) {
		super(serialDriver, log, srv);
	}

	@Override
	public void init() throws IOException {
		super.init();

		// Enable full functionality.  Without this, message receiving may randomly stop working.
		serialSendReceive("AT+CFUN=1");
		sleepWithoutInterruption(DELAY_RESET);
		
		serialSendReceive("AT^CURC=0");
		serialSendReceive("AT+CLIP=1");
	}
}


/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.batch.oneM2M.mgmtCmd;

import java.util.concurrent.atomic.AtomicInteger;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.MgmtCmdService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * mgmtCmd Task.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Component
public class MgmtCmdTask implements Runnable {

	private static final Log logger = LogFactory.getLog(MgmtCmdService.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private MgmtCmdRunnableService mgmtCmdRunnableService;

	@Autowired
	private MgmtCmdService mgmtCmdService;

	private MgmtCmd mgmtCmdProfile;

	private AtomicInteger runCount;

	/**
	 * mgmtCmd run function
	 */
	@Override
	public void run() {
		try {
			if (runCount.get() > 0) {
				mongoLogService.log(logger, LEVEL.ERROR, "MgmtCmdTask " + mgmtCmdProfile.getResourceID() + " Start!! runCount = " + runCount);
				mgmtCmdService.mgmtCmdControl("", mgmtCmdProfile);
			} else {
				mongoLogService.log(logger, LEVEL.ERROR, "MgmtCmdTask " + mgmtCmdProfile.getResourceID() + " stop!!  runCount = " + runCount);
				mgmtCmdRunnableService.stopSchedule(mgmtCmdProfile.getResourceID());
			}

			runCount.decrementAndGet();

		} catch (RSCException e) {
			e.printStackTrace();
		}
	}

	public MgmtCmd getMgmtCmdProfile() {
		return mgmtCmdProfile;
	}

	public void setMgmtCmdProfile(MgmtCmd mgmtCmdProfile) {
		this.mgmtCmdProfile = mgmtCmdProfile;
	}

	public void setMgmtCmdRunnableService(MgmtCmdRunnableService mgmtCmdRunnableService) {
		this.mgmtCmdRunnableService = mgmtCmdRunnableService;
	}

	public void setMgmtCmdService(MgmtCmdService mgmtCmdService) {
		this.mgmtCmdService = mgmtCmdService;
	}

	public void setMongoLogService(MongoLogService mongoLogService) {
		this.mongoLogService = mongoLogService;
	}

	public int getRunCount() {
		return runCount.get();
	}

	public void setRunCount(int runCount) {
		this.runCount = new AtomicInteger(runCount);
	}

}

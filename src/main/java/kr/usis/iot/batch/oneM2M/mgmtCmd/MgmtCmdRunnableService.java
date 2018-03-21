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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.Duration;

import kr.usis.iot.domain.common.IotException;
import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.EXEC_MODE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * mgmtCmd runnable service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Service
public class MgmtCmdRunnableService {

	private static final Log logger = LogFactory.getLog(MgmtCmdRunnableService.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private MgmtCmdService mgmtCmdService;
	

	private final Map<String, Future<?>> futuresMapping = new HashMap<String, Future<?>>();
	private static ScheduledExecutorService exec = Executors.newScheduledThreadPool(100);
	
	/**
	 * mgmtCmd start schedule
	 * @param mgmtCmdProfile
	 * @throws IotException
	 */
	public void startSchedule(MgmtCmd mgmtCmdProfile) throws RSCException {
			
		MgmtCmd findMgmtCmdItem = null;
		if (CommonUtil.isEmpty((findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(mgmtCmdProfile.getParentID(), mgmtCmdProfile.getResourceName())))) {
			throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
		}
		
		long initialDelay =  0;
		long period       =  0;
		int  runCount     =  1;
		
		BigInteger execMode 	= findMgmtCmdItem.getExecMode();
		Duration execFrequency	= findMgmtCmdItem.getExecFrequency();
		Duration execDelay      = findMgmtCmdItem.getExecDelay();
		BigInteger execNumber	= findMgmtCmdItem.getExecNumber();
		if(execMode.equals(EXEC_MODE.IMMEDIATE_ONCE.getValue())) {
			
			
		} else if(execMode.equals(EXEC_MODE.IMMEDIATE_AND_REPEATEDLY.getValue())) {			
			runCount = execNumber.intValue();
			
		} else if(execMode.equals(EXEC_MODE.RANDOM_ONCE.getValue())) {
			//initialDelay = execDelay;
			
		} else if(execMode.equals(EXEC_MODE.RANDOM_AND_REPEATEDLY.getValue())) {
			//initialDelay = Integer.parseInt(execDelay);
			//period   = (long)(Integer.parseInt(execFrequency));
			//runCount = Integer.parseInt(execNumber);
		}
		
		mgmtCmdProfile.setResourceID(findMgmtCmdItem.getResourceID());
		MgmtCmdTask task = new MgmtCmdTask();
		task.setMgmtCmdRunnableService(this);
		task.setMongoLogService(mongoLogService);
		task.setMgmtCmdService(mgmtCmdService);
		task.setMgmtCmdProfile(mgmtCmdProfile);
		task.setRunCount(runCount);
		try {
			if (!CommonUtil.isEmpty(futuresMapping.get(mgmtCmdProfile.getResourceID()))) {
				this.stopSchedule(mgmtCmdProfile.getResourceID());
			}

			if(runCount == 1) {
				exec.schedule(task, initialDelay, TimeUnit.SECONDS);
				
			} else {
				Future<?> future = exec.scheduleWithFixedDelay(task, initialDelay, period, TimeUnit.SECONDS);
				futuresMapping.put(mgmtCmdProfile.getResourceID(), future);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, "MgmtCmdTask schedule Error");
		}
	}

	/**
	 * mgmtCmd stop schedule
	 * @param resourceId
	 * @throws IotException
	 */
	public void stopSchedule(String resourceId) throws RSCException {
		
		mongoLogService.log(logger, LEVEL.ERROR, "MgmtCmdTask " + resourceId + " Schedule stop");
		Future<?> future = futuresMapping.get(resourceId);
		try {
			if (!CommonUtil.isEmpty(future) && !future.isDone()) {
				future.cancel(true);
				futuresMapping.remove(resourceId);
			}
		} catch (Exception e) {
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, "MgmtCmdTask schedule cancle Error");
		}
	}

}

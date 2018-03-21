/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.batch.oneM2M.subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kr.usis.iot.domain.common.IotException;
import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.Subscription;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.SubscriptionService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * subscription runnable service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Service
public class SubscriptionRunnableService {

	private static final Log logger = LogFactory.getLog(SubscriptionRunnableService.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private SubscriptionService subscriptionService;
	

	private final Map<String, Future<?>> futuresMapping = new HashMap<String, Future<?>>();
	private static ScheduledExecutorService exec = Executors.newScheduledThreadPool(100);
	
	/**
	 * subscription start schedule
	 * @param subscriptionProfile
	 * @param objectEntity
	 * @throws IotException
	 */
	public void startSchedule(Subscription subscriptionProfile, Object objectEntity) throws RSCException {
	
		long initialDelay =  0;
		int  runCount     =  1;
	
		SubscriptionTask task = new SubscriptionTask();
		task.setMgmtCmdRunnableService(this);
		task.setMongoLogService(mongoLogService);
		task.setSubscriptionService(subscriptionService);
		task.setSubscriptionProfile(subscriptionProfile);
		task.setObjectEntity(objectEntity);
		task.setRunCount(runCount);
		
		try {
			if (!CommonUtil.isEmpty(futuresMapping.get(subscriptionProfile.getResourceID()))) {
				this.stopSchedule(subscriptionProfile.getResourceID());
			}

			exec.schedule(task, initialDelay, TimeUnit.SECONDS);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, "SubscriptionTask schedule Error");
		}
	}

	/**
	 * subscription stop schedule
	 * @param subscriptionProfile
	 * @param objectEntity
	 * @throws IotException
	 */
	public void stopSchedule(String resourceId) throws RSCException {
		
		mongoLogService.log(logger, LEVEL.ERROR, "SubscriptionTask " + resourceId + " Schedule stop");
		Future<?> future = futuresMapping.get(resourceId);
		try {
			if (!CommonUtil.isEmpty(future) && !future.isDone()) {
				future.cancel(true);
				futuresMapping.remove(resourceId);
			}
		} catch (Exception e) {
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, "SubscriptionTask schedule cancle Error");
		}
	}

}

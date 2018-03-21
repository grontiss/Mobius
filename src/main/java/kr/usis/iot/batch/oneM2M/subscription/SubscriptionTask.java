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

import java.util.concurrent.atomic.AtomicInteger;

import kr.usis.iot.domain.oneM2M.Subscription;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
import kr.usis.iot.service.oneM2M.SubscriptionService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * subscription Task.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Component
public class SubscriptionTask implements Runnable {

	private static final Log logger = LogFactory.getLog(MgmtCmdService.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private SubscriptionRunnableService subscriptionRunnableService;

	@Autowired
	private SubscriptionService subscriptionService;

	private Subscription subscriptionProfile;
	
	private Object objectEntity; 

	private AtomicInteger runCount;

	/**
	 * subscription run function
	 */
	@Override
	public void run() {
		try {
			if (runCount.get() > 0) {
				mongoLogService.log(logger, LEVEL.ERROR, "SubscriptionTask " + subscriptionProfile.getResourceID() + " Start!! runCount = " + runCount);
//				subscriptionService.sendSubscription(subscriptionProfile.getNotificationURI(), stringEntity);
				subscriptionService.sendSubscription(subscriptionProfile, objectEntity);
				
			} else {
				mongoLogService.log(logger, LEVEL.ERROR, "SubscriptionTask " + subscriptionProfile.getResourceID() + " stop!!  runCount = " + runCount);
				subscriptionRunnableService.stopSchedule(subscriptionProfile.getResourceID());
			}

			runCount.decrementAndGet();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public Subscription getSubscriptionProfile() {
		return subscriptionProfile;
	}

	public void setSubscriptionProfile(Subscription subscriptionProfile) {
		this.subscriptionProfile = subscriptionProfile;
	}

	public void setMgmtCmdRunnableService(SubscriptionRunnableService mgmtCmdRunnableService) {
		this.subscriptionRunnableService = mgmtCmdRunnableService;
	}

	public void setSubscriptionService(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}
	
	public void setObjectEntity(Object objectEntity) {
		this.objectEntity = objectEntity;
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

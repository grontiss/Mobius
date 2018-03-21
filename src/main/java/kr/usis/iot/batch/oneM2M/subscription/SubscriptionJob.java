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

import java.util.List;

import kr.usis.iot.domain.oneM2M.SubscriptionPending;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.SubscriptionService;
import kr.usis.iot.util.CommonUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;


/**
 * subscription panding mgmt batch
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Controller
public class SubscriptionJob {
	
	private static final Log logger = LogFactory.getLog(SubscriptionJob.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private SubscriptionService subscriptionService;

	/**
	 * subscription panding retransmission
	 */
	@Scheduled(fixedDelay = 60000)
	private void subscriptionPending() {
		
		Long runTime = System.currentTimeMillis();
		try {
			//mongoLogService.log(logger, LEVEL.DEBUG, "subscriptionPending scheduler start!!!");
			
			List<SubscriptionPending> pendingList = subscriptionService.findSubscriptionPending();
			if(!CommonUtil.isEmpty(pendingList) && pendingList.size() > 0) {
				this.sendScriptionPending(pendingList);
			}
			

		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			e.printStackTrace();
		} finally {
			//mongoLogService.log(logger, LEVEL.DEBUG, "subscriptionPending scheduler end!!! (runTime : " + (System.currentTimeMillis() - runTime) + "ms)");
		}

	}
	
	/**
	 * retransmission success data delete
	 * @param pendingList
	 */
	private void sendScriptionPending(List<SubscriptionPending> pendingList) {
		for(SubscriptionPending subscriptionPendingItem : pendingList) {
			try {
				subscriptionService.sendSubscription(subscriptionPendingItem.getNotificationUri(), subscriptionPendingItem.getContent());
				subscriptionService.deleteSubscriptionPending(subscriptionPendingItem.getPadingKey());
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}

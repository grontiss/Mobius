/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.batch.oneM2M.contentInstance;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.mdao.oneM2M.ContentInstanceDao;
import kr.usis.iot.service.apilog.ApiLogService;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.SubscriptionService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.OPERATION_MONITOR;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_STATUS;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RedisToMongoDB Bulk Insert service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Service
public class ContentInstanceDeviceToMongoRegService {

	private static final Log logger = LogFactory.getLog(ContentInstanceDeviceToMongoRegService.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private ContentInstanceDao contentInstanceDao;
	
	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private ApiLogService apiLogService;
	
	/**
	 * RedisToMongoDB Bulk Insert.
	 * @throws Exception
	 */
	public void moveRedis2Mongo() throws Exception {
		
		long runTime = System.currentTimeMillis();
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "moveRedis2Mongo scheduler start!!!");
		
		try {
			
			List<String> getObjectsKeys = contentInstanceDao.getObjectsKeys(0, -1);
			
			if(!CommonUtil.isEmpty(getObjectsKeys) && getObjectsKeys.size() > 0) {
				
				Map<String, BigInteger> listMap = new LinkedHashMap<String, BigInteger>();
				List<ContentInstance> contentInstanceList = contentInstanceDao.getObjectsByKeys(getObjectsKeys);

				mongoLogService.log(logger, LEVEL.DEBUG, "Redis contentInstance Data List start");
				for(ContentInstance contentInstanceItem : contentInstanceList) {
					
					if(!listMap.containsKey(contentInstanceItem.getParentID())) {
						Container findContainerItem = containerService.findOneContainer(contentInstanceItem.getParentID());
						BigInteger beforeStateTag = findContainerItem.getStateTag();
						BigInteger afterStateTag = beforeStateTag.add(BigInteger.ONE);
						
						listMap.put(contentInstanceItem.getParentID(), afterStateTag);
					} else {
						BigInteger beforeStateTag = listMap.get(contentInstanceItem.getParentID());
						BigInteger afterStateTag = beforeStateTag.add(BigInteger.ONE);
						
						listMap.put(contentInstanceItem.getParentID(), afterStateTag);
					}
					
					contentInstanceItem.setStateTag(listMap.get(contentInstanceItem.getParentID()));
					
					mongoLogService.log(logger, LEVEL.DEBUG, ToStringBuilder.reflectionToString(contentInstanceItem, ToStringStyle.DEFAULT_STYLE));
				}
				mongoLogService.log(logger, LEVEL.DEBUG, "Redis contentInstance Data List end");
				
				contentInstanceDao.multi_insert(contentInstanceList);
				
				for(ContentInstance contentInstanceItem : contentInstanceList) {
					
					subscriptionService.sendSubscription(contentInstanceItem.getParentID(), contentInstanceItem.getResourceID(), RESOURCE_STATUS.CHILD_CREATED, OPERATION_MONITOR.CREATE, ContentInstance.class, contentInstanceItem);
				}
				
				contentInstanceDao.redisDeleteObjectsByKeys(getObjectsKeys);
				
				containerService.updateContainerCurrentInstanceValue(contentInstanceList);
				
			}

		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR,  e.getMessage());
			throw new Exception(e);
		} 
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "moveRedis2Mongo scheduler end!!! (runTime : " + (System.currentTimeMillis() - runTime) + "ms)");
	}

}

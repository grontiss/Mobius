/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.service.oneM2M;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kr.usis.iot.batch.oneM2M.subscription.SubscriptionRunnableService;
import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.BatchNotify;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.domain.oneM2M.EventNotificationCriteria;
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.RateLimit;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.Resource;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.Subscription;
import kr.usis.iot.domain.oneM2M.SubscriptionPending;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.ContainerDao;
import kr.usis.iot.mdao.oneM2M.ContentInstanceDao;
import kr.usis.iot.mdao.oneM2M.ExecInstanceDao;
import kr.usis.iot.mdao.oneM2M.MgmtCmdDao;
import kr.usis.iot.mdao.oneM2M.SubscriptionDao;
import kr.usis.iot.mdao.oneM2M.SubscriptionPendingDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.NOTIFICATION_CONTENT_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.OPERATION_MONITOR;
import kr.usis.iot.util.oneM2M.CommonCode.PENDING_NOTIFICATION;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_STATUS;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;

@Service
public class SubscriptionService {

	private static final Log logger = LogFactory.getLog(SubscriptionService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private ContentInstanceService contentInstanceService;
	
	@Autowired
	private ExecInstanceService execInstanceService;
	
	@Autowired
	private SubscriptionRunnableService subscriptionRunnableService;
	
	@Autowired
	private SequenceDao seqDao;

	@Autowired
	private SubscriptionDao subscriptionDao;
	
	@Autowired
	private ContainerDao containerDao;
	
	@Autowired
	private MgmtCmdDao mgmtCmdDao;
	
	@Autowired
	private ContentInstanceDao contentInstanceDao;
	
	@Autowired
	private ExecInstanceDao execInstanceDao;
	
	@Autowired
	private SubscriptionPendingDao subscriptionPendingDao;
	
	
	/**
	 * subscription retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Subscription findOneSubscription(String resourceID) throws RSCException {
		Subscription findSubscriptionItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try {
			findSubscriptionItem = (Subscription) subscriptionDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}

		return findSubscriptionItem;
	}
	
	/**
	 * subscription retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Subscription findOneSubscriptionByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Subscription findSubscriptionItem = null;
		Subscription findNoFilterSubscriptionItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findSubscriptionItem = (Subscription) subscriptionDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findSubscriptionItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterSubscriptionItem = findOneSubscriptionResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findSubscriptionItem)) findSubscriptionItem = findNoFilterSubscriptionItem;

		return findSubscriptionItem;
	}
	
	/**
	 * subscription References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Subscription findOneSubscriptionResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Subscription findSubscriptionResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findSubscriptionResourceRefItem = (Subscription) subscriptionDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}
		
		return findSubscriptionResourceRefItem;
	}

	/**
	 * subscription retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public List<Subscription> findSubscription(String key, String value) throws RSCException {
		List<Subscription> findSubscriptionList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {
			findSubscriptionList = (List<Subscription>) subscriptionDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}

		return findSubscriptionList;
	}
	
	/**
	 * subscription retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Subscription> findSubscription(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Subscription>();
		
		List<Subscription> findSubscriptionList = null;
		List<Subscription> findSubscriptionNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findSubscriptionList = (List<Subscription>) subscriptionDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findSubscriptionList);
			
			if (filterCriteria.isFilterCriteria()) findSubscriptionNoFilterResourceRefList = findSubscriptionResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findSubscriptionNoFilterResourceRefList.size(); i++) {
				Subscription noFilterSubscription = findSubscriptionNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findSubscriptionList.size(); t++) {
					Subscription subscription = findSubscriptionList.get(t);
					if (noFilterSubscription.getResourceRef().getResourceID().equals(subscription.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findSubscriptionList.add(noFilterSubscription);
			}
		}

		return findSubscriptionList;
	}
	
	/**
	 * subscription References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Subscription> findSubscriptionResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Subscription> findSubscriptionResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findSubscriptionResourceRefList = (List<Subscription>) subscriptionDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}
		
		return findSubscriptionResourceRefList;
	}
	
	/**
	 * subscription References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Subscription> findSubscriptionResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Subscription>();
		
		String includeField = "resourceRef";
		List<Subscription> findSubscriptionResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findSubscriptionResourceRefList = (List<Subscription>) subscriptionDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findSubscriptionResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}
		
		return findSubscriptionResourceRefList;
	}
	
	/**
	 * subscription retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Subscription findOneSubscriptionByResourceName(String parentID, String resourceName) throws RSCException {
		Subscription findSubscriptionItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findSubscriptionItem = (Subscription) subscriptionDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
		}

		return findSubscriptionItem;
	}

	/**
	 * subscription create
	 * @param url
	 * @param subscriptionProfile
	 * @return
	 * @throws RSCException
	 */
	public Subscription createSubscription(String url, Subscription subscriptionProfile) throws RSCException {
		Subscription subscriptionItem = new Subscription();
		
		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.SUBSCRIPTION);
		
		String expirationTime = subscriptionProfile.getExpirationTime();

		subscriptionItem.setResourceType(RESOURCE_TYPE.SUBSCRIPTION.getValue());
		subscriptionItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.SUBSCRIPTION.getValue(), seqID));
		subscriptionItem.setResourceName(!CommonUtil.isEmpty(subscriptionProfile.getResourceName()) ? subscriptionProfile.getResourceName() : subscriptionItem.getResourceID());
		subscriptionItem.setParentID(subscriptionProfile.getParentID());
		subscriptionItem.setExpirationTime(expirationTime);
		subscriptionItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		subscriptionItem.setCreationTime(currentTime);
		subscriptionItem.setLastModifiedTime(currentTime);
		subscriptionItem.getLabels().addAll(subscriptionProfile.getLabels());
		//subscriptionItem.getAccessControlPolicyIDs().addAll(subscriptionProfile.getAccessControlPolicyIDs());
		subscriptionItem.setEventNotificationCriteria(subscriptionProfile.getEventNotificationCriteria());
		subscriptionItem.setExpirationCounter(subscriptionProfile.getExpirationCounter());
		subscriptionItem.getNotificationURI().addAll(subscriptionProfile.getNotificationURI());
		subscriptionItem.setGroupID(subscriptionProfile.getGroupID());
		subscriptionItem.setNotificationForwardingURI(subscriptionProfile.getNotificationForwardingURI());
		subscriptionItem.setBatchNotify(subscriptionProfile.getBatchNotify());
		subscriptionItem.setRateLimit(subscriptionProfile.getRateLimit());
		subscriptionItem.setPreSubscriptionNotify(subscriptionProfile.getPreSubscriptionNotify());
		subscriptionItem.setPendingNotification(subscriptionProfile.getPendingNotification());
		subscriptionItem.setNotificationStoragePriority(subscriptionProfile.getNotificationStoragePriority());
		subscriptionItem.setLatestNotify(subscriptionProfile.isLatestNotify());
		subscriptionItem.setNotificationContentType(subscriptionProfile.getNotificationContentType());
		subscriptionItem.setNotificationEventCat(subscriptionProfile.getNotificationEventCat());
		subscriptionItem.setCreator(subscriptionProfile.getCreator());
		subscriptionItem.setSubscriberURI(subscriptionProfile.getSubscriberURI());
		subscriptionItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, subscriptionItem), subscriptionItem.getResourceName(), RESOURCE_TYPE.SUBSCRIPTION.getValue(), subscriptionItem.getResourceID(), null));
		
		try {
			subscriptionDao.insert(subscriptionItem);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "subscription create success");
		
		return this.findOneSubscription(subscriptionItem.getResourceID());

	}

	/**
	 * subscription update
	 * @param subscriptionItem
	 * @return
	 * @throws RSCException
	 */
	public Subscription updateSubscription(Subscription subscriptionItem) throws RSCException {
		
		String currentTime 									= CommonUtil.getNowTimestamp();
		
		String resourceID									= subscriptionItem.getResourceID();
		String expirationTime								= subscriptionItem.getExpirationTime();
		//List<String> accessControlPolicyIDs 				= subscriptionItem.getAccessControlPolicyIDs();
		List<String> labels									= subscriptionItem.getLabels();
		EventNotificationCriteria eventNotificationCriteria = subscriptionItem.getEventNotificationCriteria();
		BigInteger expirationCounter 						= subscriptionItem.getExpirationCounter();
		List<String> notificationURI 						= subscriptionItem.getNotificationURI();
		String groupID										= subscriptionItem.getGroupID();
		String notificationForwardingURI 					= subscriptionItem.getNotificationForwardingURI();
		BatchNotify batchNotify 							= subscriptionItem.getBatchNotify();
		RateLimit rateLimit 								= subscriptionItem.getRateLimit();
		BigInteger pendingNotification 						= subscriptionItem.getPendingNotification();
		BigInteger notificationStoragePriority 				= subscriptionItem.getNotificationStoragePriority();
		Boolean latestNotify 								= subscriptionItem.isLatestNotify();
		BigInteger notificationContentType 					= subscriptionItem.getNotificationContentType();
		String notificationEventCat 						= subscriptionItem.getNotificationEventCat();
		
		Update update = new Update();
		if(!CommonUtil.isEmpty(expirationTime))				update.set("expirationTime", expirationTime);
		if(!CommonUtil.isEmpty(expirationTime))				update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))		update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isEmpty(labels))						update.set("labels", labels);
		if(!CommonUtil.isEmpty(eventNotificationCriteria))	update.set("eventNotificationCriteria", eventNotificationCriteria);
		if(!CommonUtil.isNull(expirationCounter))			update.set("expirationCounter", expirationCounter);
		if(!CommonUtil.isNull(notificationURI))				update.set("notificationURI", notificationURI);
		if(!CommonUtil.isEmpty(groupID))					update.set("groupID", groupID);
		if(!CommonUtil.isEmpty(notificationForwardingURI))	update.set("notificationForwardingURI", notificationForwardingURI);
		if(!CommonUtil.isEmpty(batchNotify))				update.set("batchNotify", batchNotify);
		if(!CommonUtil.isEmpty(rateLimit))					update.set("rateLimit", rateLimit);
		if(!CommonUtil.isNull(pendingNotification))			update.set("pendingNotification", pendingNotification);
		if(!CommonUtil.isEmpty(notificationStoragePriority))update.set("notificationStoragePriority", notificationStoragePriority);
		if(!CommonUtil.isEmpty(latestNotify))				update.set("latestNotify", latestNotify);
		if(!CommonUtil.isNull(notificationContentType))		update.set("notificationContentType", notificationContentType);
		if(!CommonUtil.isNull(notificationEventCat))		update.set("notificationEventCat", notificationEventCat);
															update.set("lastModifiedTime", currentTime);

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try {
			subscriptionDao.update(query, update);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "subscription update success");

		return this.findOneSubscription(resourceID);
	}

	/**
	 * subscription delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteSubscription(String resourceID) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {
			subscriptionDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "subscription delete success");

	}
	
	/**
	 * subscription delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteSubscription(String key, String value) throws RSCException {
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			subscriptionDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.subscription.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "subscription delete success");

	}
	
	/**
	 * subscription check
	 * @param subscriptionItem
	 * @throws RSCException
	 */
	public void checkSubscriptionValidation(Subscription subscriptionItem) throws RSCException{
		
		// 필수항목
		if(CommonUtil.isEmpty(subscriptionItem.getNotificationURI()) || !CommonUtil.checkUrl(subscriptionItem.getNotificationURI())) {
			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "notificationURI " + CommonUtil.getMessage("device.error.invalid.pocurl"));
		}
			
		if(CommonUtil.isEmpty(subscriptionItem.getNotificationContentType()) || CommonUtil.isEmpty(NOTIFICATION_CONTENT_TYPE.isValidation(subscriptionItem.getNotificationContentType()))) {
			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "notificationContentType " + CommonUtil.getMessage("msg.device.subscription.type.nosupport"));
		}
		
		// 옵션
		if(!CommonUtil.isEmpty(subscriptionItem.getSubscriberURI()) && !CommonUtil.checkUrl(subscriptionItem.getSubscriberURI())) {
			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "subscriberURI " + CommonUtil.getMessage("device.error.invalid.pocurl"));
		}
		
		if(!CommonUtil.isEmpty(subscriptionItem.getExpirationCounter()) && !CommonUtil.isInteger(subscriptionItem.getExpirationCounter().toString())) {
			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "expirationCounter " + CommonUtil.getMessage("msg.device.number.text"));
		}
		
		if(!CommonUtil.isEmpty(subscriptionItem.getPendingNotification()) && CommonUtil.isEmpty(PENDING_NOTIFICATION.isValidation(subscriptionItem.getPendingNotification()))) {
			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "pendingNotification " + CommonUtil.getMessage("msg.device.subscription.type.nosupport"));
		}
		
//		EventNotificationCriteria criteriaVO = subscriptionItem.getEventNotificationCriteria();
//		if(CommonUtil.isEmpty(subscriptionItem.getEventNotificationCriteria())) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "eventNotificationCriteria " + CommonUtil.getMessage("msg.input.empty.text"));
//		}
//			
//		if(!CommonUtil.isEmpty(criteriaVO.getResourceStatus()) && CommonUtil.isEmpty(RESOURCE_STATUS.isValidation(criteriaVO.getResourceStatus()))) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "resourceStatus " + CommonUtil.getMessage("msg.device.subscription.type.nosupport"));
//		}
//		
//		if(!CommonUtil.isEmpty(criteriaVO.getModifiedSince()) && CommonUtil.isEmpty(CommonUtil.parseDate(criteriaVO.getModifiedSince(), "yyyy-MM-dd'T'HH:mm:ssXXX"))) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "modifiedSince " + CommonUtil.getMessage("msg.device.date.text"));
//		}
//		
//		if(!CommonUtil.isEmpty(criteriaVO.getUnmodifiedSince()) && CommonUtil.isEmpty(CommonUtil.parseDate(criteriaVO.getUnmodifiedSince(), "yyyy-MM-dd'T'HH:mm:ssXXX"))) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "unmodifiedSince " + CommonUtil.getMessage("msg.device.date.text"));
//		}
//		
//		if(!CommonUtil.isEmpty(criteriaVO.getStateTagSmaller()) && !CommonUtil.isInteger(criteriaVO.getStateTagSmaller().toString())) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "stateTagSmaller " + CommonUtil.getMessage("msg.device.number.text"));
//		}
//		
//		if(!CommonUtil.isEmpty(criteriaVO.getStateTagBigger()) && !CommonUtil.isInteger(criteriaVO.getStateTagBigger().toString())) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "stateTagBigger " + CommonUtil.getMessage("msg.device.number.text"));
//		}
//		
//		if(!CommonUtil.isEmpty(criteriaVO.getSizeAbove()) && !CommonUtil.isInteger(criteriaVO.getSizeAbove().toString())) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "sizeAbove " + CommonUtil.getMessage("msg.device.number.text"));
//		}
//		
//		if(!CommonUtil.isEmpty(criteriaVO.getSizeBelow()) && !CommonUtil.isInteger(criteriaVO.getSizeBelow().toString())) {
//			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, "sizeBelow " + CommonUtil.getMessage("msg.device.number.text"));
//		}
	}
	
	/**
	 * subscription send
	 * @param parentID
	 * @param resourceID
	 * @param resourceStatus
	 * @param clazz
	 * @param objectEntity
	 * @throws RSCException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void sendSubscription(String parentID, String resourceID, RESOURCE_STATUS resourceStatus, OPERATION_MONITOR operationMonitor, Class clazz, Object objectEntity) throws RSCException {
		mongoLogService.log(logger, LEVEL.INFO, "subscriptionService sendSubscrition call");
		
		try {
			
			if(objectEntity.getClass().getSimpleName().indexOf("Update") > -1) {
				BasicDBObject dbObject = new BasicDBObject();
				HashMap<String, Object> setMap = (HashMap<String, Object>) ((Update) objectEntity).getUpdateObject().get("$set");
				for(String key : setMap.keySet()) {
					dbObject.put(key, setMap.get(key));
				}
				dbObject.put("resourceID", resourceID);
				if(resourceStatus.getName().indexOf("child") <= -1) {
					parentID = resourceID;
				}
				objectEntity = subscriptionDao.getConverter(clazz, dbObject);
			}
			
			BigInteger resourceType = ((Resource) objectEntity).getResourceType();
			
			Query query = new Query();
			query.addCriteria(Criteria.where("parentID").is(parentID));
			
			List<Subscription> findSubscriptionList = (List<Subscription>)subscriptionDao.find(query);
			
			for(Subscription subscriptionItem : findSubscriptionList) {
				
				if (NOTIFICATION_CONTENT_TYPE.WHOLE_RESOURCE.getValue().equals(subscriptionItem.getNotificationContentType())) {
					
					if ((resourceStatus.equals(RESOURCE_STATUS.UPDATED) || resourceStatus.equals(RESOURCE_STATUS.CHILD_UPDATED))) {
						
						if(			resourceType.equals(RESOURCE_TYPE.CONTAINER.getValue())) objectEntity = containerService.findOneContainer(resourceID);
						else if(	resourceType.equals(RESOURCE_TYPE.MGMT_CMD.getValue())) objectEntity = mgmtCmdService.findOneMgmtCmd(resourceID);
						else if(	resourceType.equals(RESOURCE_TYPE.CONTENT_INSTANCE.getValue())) objectEntity = contentInstanceService.findOneContentInstance(resourceID);
						else if(	resourceType.equals(RESOURCE_TYPE.EXEC_INSTANCE.getValue())) objectEntity = execInstanceService.findOneExecInstance(resourceID);
					}
//					mongoLogService.log(logger, LEVEL.INFO, "subscriptionService objectEntity = " + ToStringBuilder.reflectionToString(objectEntity));					
				}
				
				EventNotificationCriteria eventNotificationCriteriaItem = subscriptionItem.getEventNotificationCriteria();
				if(!CommonUtil.isEmpty(eventNotificationCriteriaItem)) {
					
					query = mCommonService.eventNotificationCriteriaToQuery(eventNotificationCriteriaItem, resourceID);
					if (!CommonUtil.isEmpty(query)) {
						
						List<Resource> resourceList = null;
						
						try {
							
							if(			resourceType.equals(RESOURCE_TYPE.CONTAINER.getValue())) resourceList = (List<Resource>) containerDao.find(query);
							else if(	resourceType.equals(RESOURCE_TYPE.MGMT_CMD.getValue())) resourceList = (List<Resource>) mgmtCmdDao.find(query);
							else if(	resourceType.equals(RESOURCE_TYPE.CONTENT_INSTANCE.getValue())) resourceList = (List<Resource>) contentInstanceDao.find(query);
							else if(	resourceType.equals(RESOURCE_TYPE.EXEC_INSTANCE.getValue())) resourceList = (List<Resource>) execInstanceDao.find(query);
							
						} catch (Exception e) {
							logger.error(e.getMessage(),e);
							mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
							throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.findFail.text"));
						}
						
						if (!CommonUtil.isEmpty(resourceList) && resourceList.size() > 0) {
							
							subscriptionRunnableService.startSchedule(subscriptionItem, objectEntity);
							continue;
						}
					}
					
					List<BigInteger> resourceStatusList = eventNotificationCriteriaItem.getResourceStatus();
					if(!CommonUtil.isEmpty(resourceStatusList)) {
						for (BigInteger resourceStatusItem : resourceStatusList) {
						
							if (resourceStatusItem.equals(resourceStatus.getValue())) {
								
								subscriptionRunnableService.startSchedule(subscriptionItem, objectEntity);
								break;
							}
						}
					}
					
					List<BigInteger> operationMonitorList = eventNotificationCriteriaItem.getOperationMonitor();
					if(!CommonUtil.isEmpty(operationMonitorList)) {
						for (BigInteger operationMonitorItem : operationMonitorList) {
							
							if (operationMonitorItem.equals(operationMonitor.getValue())) {
								
								subscriptionRunnableService.startSchedule(subscriptionItem, objectEntity);
								break;
							}
						}
					}
					
				}
			}
		} catch(RSCException e) {
			e.printStackTrace();
			throw new RSCException(e.getCode(), e.getMessage());
			
		} catch(NumberFormatException e) {
			e.printStackTrace();
			throw new RSCException(RSC.SUBSCRIPTION_VERIFICATION_INITIATION_FAILED, e.getMessage());
			
		} catch(Exception e) {
			e.printStackTrace();
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, e.getMessage());
			
		} 
		
		mongoLogService.log(logger, LEVEL.INFO, "subscriptionService sendSubscrition end");
		
	}
	
	/**
	 * subscription send
	 * @param subscriptionItem
	 * @param objectEntity
	 * @throws RSCException
	 */
	public void sendSubscription(Subscription subscriptionItem, Object objectEntity)  throws RSCException {
		try {
			if (!CommonUtil.isEmpty(subscriptionItem.getNotificationURI())) {
			
				mongoLogService.log(logger, LEVEL.INFO, "subscriptionService objectEntity = " + ToStringBuilder.reflectionToString(objectEntity));
				
				String entity = mCommonService.marshalToXmlString(objectEntity);
				try {
					this.sendSubscription(subscriptionItem.getNotificationURI(), entity);
//					subscriptionRunnableService.startSchedule(subscriptionItem, entity);
				} catch(Exception e) {
					e.printStackTrace();
					
					if(PENDING_NOTIFICATION.SEND_ALL_PENDING.getValue().equals(subscriptionItem.getPendingNotification())) {
						SubscriptionPending subscriptionPendingProfile = new SubscriptionPending();
						subscriptionPendingProfile.setNotificationUri(subscriptionItem.getNotificationURI().get(0));
						subscriptionPendingProfile.setContent(entity);
						
						this.createSubscriptionPending(subscriptionPendingProfile);
					}
				}
			}
			
			this.updateSubscriptionExpirationCounter(subscriptionItem);
			
		} catch(Exception e) {
			e.printStackTrace();
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, e.getMessage());
			
		} 
		
		mongoLogService.log(logger, LEVEL.INFO, "subscriptionService sendSubscrition end");		
		
	}
	
	/**
	 * subscription ExpirationCounter update
	 * @param subscriptionItem
	 * @throws RSCException
	 */
	public void updateSubscriptionExpirationCounter(Subscription subscriptionItem) throws RSCException {
		
		String currentTime = CommonUtil.getNowTimestamp();
		BigInteger expirationCounter = subscriptionItem.getExpirationCounter();
		
		if(!CommonUtil.isEmpty(expirationCounter) && CommonUtil.isInteger(expirationCounter.toString())) {
			subscriptionItem.setExpirationCounter(expirationCounter.subtract(BigInteger.ONE));
			if(subscriptionItem.getExpirationCounter().intValue() > 0) {
				Query query = new Query();
				query.addCriteria(Criteria.where("resourceID").is(subscriptionItem.getResourceID()));
				
				Update update = new Update();
				update.set("expirationCounter", 		subscriptionItem.getExpirationCounter());
				update.set("lastModifiedTime", 			currentTime);
				try {
					subscriptionDao.update(query, update);
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getMessage(),e);
					mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
					throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscription.upFail.text"));
				}
				
				mongoLogService.log(logger, LEVEL.INFO, "subscription update success");
			} else {
				this.deleteSubscription(subscriptionItem.getResourceID());
				
				if(!CommonUtil.isEmpty(subscriptionItem.getSubscriberURI())) {
					try {
						String entity = mCommonService.marshalToXmlString(subscriptionItem);
						this.sendSubscription(subscriptionItem.getSubscriberURI(), entity);
					} catch(Exception e) {
						e.printStackTrace();
						mongoLogService.log(logger, LEVEL.ERROR, "subscription delete sendSubscription Exception" + e.getMessage());
					}
				}
				mongoLogService.log(logger, LEVEL.INFO, "subscription delete success");
			}
		}
		
	}
	
	/**
	 * subscription send
	 * @param url
	 * @param stringEntity
	 * @return
	 * @throws Exception
	 */
	public String sendSubscription(String url, String stringEntity) throws Exception {
		String responseBody = "";

		HttpClient httpClient = new HttpClient();
		PostMethod method     = new PostMethod(url);
		method.setRequestHeader("Accept", 		"application/xml;charset=UTF-8");
		method.setRequestHeader("Content-Type", MediaType.APPLICATION_XML_VALUE);
		
		StringRequestEntity requestEntity = new StringRequestEntity(stringEntity, "application/xml", "UTF-8");
		method.setRequestEntity(requestEntity);		
		if(logger.isDebugEnabled()) {
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription call");
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription url  = " + url);
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription data = " + stringEntity);
		}
		int responseStatus = httpClient.executeMethod(method);
		if(logger.isDebugEnabled()) {
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription responseStatus Code = " + responseStatus);
		}
		
		if(responseStatus == HttpStatus.SC_OK || responseStatus == HttpStatus.SC_CREATED) {
			String line = "";
			BufferedReader rd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(method.getResponseBody())));
			while ((line = rd.readLine()) != null) {
				responseBody.concat(line);
			}
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription Success responseStatus Code = " + responseStatus);
			
		} else {
			if(logger.isDebugEnabled()) {
				mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription Exception responseStatus Code = " + responseStatus);
			}
			throw new Exception();
		}

		if(logger.isDebugEnabled()) {
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription responseBody = " + responseBody);
			mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription end");
		}
		
		return responseBody;

	}
	
	/**
	 * subscription send
	 * @param urls
	 * @param stringEntity
	 * @return
	 * @throws Exception
	 */
	public List<String> sendSubscription(List<String> urls, String stringEntity) throws Exception {
		List<String> responseBodyList = new ArrayList<String>();

		HttpClient httpClient = new HttpClient();
		
		for (int i=0; i<urls.size(); i++) {
			String responseBody = "";
			String url = urls.get(i);
			PostMethod method     = new PostMethod(url);
			method.setRequestHeader("Accept", 		"application/xml;charset=UTF-8");
			method.setRequestHeader("Content-Type", MediaType.APPLICATION_XML_VALUE);
			
			try {
			
				StringRequestEntity requestEntity = new StringRequestEntity(stringEntity, "application/xml", "UTF-8");
				method.setRequestEntity(requestEntity);		
				if(logger.isDebugEnabled()) {
					mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription call");
					mongoLogService.log(logger, LEVEL.DEBUG, "subscription url  = " + url);
					mongoLogService.log(logger, LEVEL.DEBUG, "subscription data = " + stringEntity);
				}
				int responseStatus = httpClient.executeMethod(method);
				if(logger.isDebugEnabled()) {
					mongoLogService.log(logger, LEVEL.DEBUG, "subscription responseStatus Code = " + responseStatus);
				}
				
				if(responseStatus == HttpStatus.SC_OK || responseStatus == HttpStatus.SC_CREATED) {
					String line = "";
					BufferedReader rd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(method.getResponseBody())));
					while ((line = rd.readLine()) != null) {
						responseBody.concat(line);
					}
					mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription Success responseStatus Code = " + responseStatus);
					
				} else {
					if(logger.isDebugEnabled()) {
						mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription Exception responseStatus Code = " + responseStatus);
					}
					throw new Exception();
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(),e);
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			}
	
			if(logger.isDebugEnabled()) {
				mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription responseBody = " + responseBody);
				mongoLogService.log(logger, LEVEL.DEBUG, "subscription sendSubscription end");
			}
			
			responseBodyList.add(responseBody);
		}
		
		return responseBodyList;

	}
	
	/**
	 * subscription pending retrieve
	 * @return
	 * @throws RSCException
	 */
	public List<SubscriptionPending> findSubscriptionPending() throws RSCException {
		return findSubscriptionPending(null, null);
	}
	
	/**
	 * subscription pending retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	@SuppressWarnings("unchecked")
	public List<SubscriptionPending> findSubscriptionPending(String key, String value) throws RSCException {
		List<SubscriptionPending> findSubscriptionPendingList = null;

		Query query = new Query();
		if (!CommonUtil.isEmpty(key) && !CommonUtil.isEmpty(value)) {
			query.addCriteria(Criteria.where(key).is(value));
		}

		try {

			findSubscriptionPendingList = (List<SubscriptionPending>) subscriptionPendingDao.find(query);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscriptionPending.findFail.text"));
		}

		return findSubscriptionPendingList;
	}

	/**
	 * subscription pending create
	 * @param subscriptionPendingProfile
	 * @return
	 * @throws RSCException
	 */
	public SubscriptionPending createSubscriptionPending(SubscriptionPending subscriptionPendingProfile) throws RSCException {

		String currentTime = CommonUtil.getNowTimestamp();
		
		try {
			subscriptionPendingProfile.setPadingKey(String.format("%010d", seqDao.move(MovType.UP, SeqType.SUBSCRIPTION_PENDING)));
			subscriptionPendingProfile.setRegDt(currentTime);
			subscriptionPendingDao.insert(subscriptionPendingProfile);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscriptionPending.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "subscriptionPending create success");

		return subscriptionPendingProfile;

	}

	/**
	 * subscription pending delete
	 * @param padingKey
	 * @throws RSCException
	 */
	public void deleteSubscriptionPending(String padingKey) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where("padingKey").is(padingKey));

		try {
			subscriptionPendingDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.subscriptionPending.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "subscriptionPending delete success");

	}

}
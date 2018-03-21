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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.LocationPolicy;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.ContainerDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.OPERATION_MONITOR;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_STATUS;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * container Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class ContainerService {

	private static final Log logger = LogFactory.getLog(ContainerService.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private ContentInstanceService contentInstanceService;
	
	@Autowired
	private SubscriptionService subscriptionService;
	
	@Autowired
	private LocationPolicyService locationPolicyService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private SequenceDao seqDao;
	
	@Autowired
	private ContainerDao containerDao;
	
	/**
	 * container retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Container findOneContainer(String resourceID) throws RSCException {
		return findOneContainer(null, resourceID);
	}
	
	/**
	 * container retrieve
	 * @param parentID
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Container findOneContainer(String parentID, String resourceID) throws RSCException {
		Container findContainerItem = null;
		
		Query query = new Query();
		if(!CommonUtil.isEmpty(parentID)) {
			query.addCriteria(Criteria.where("parentID").is(parentID));
		}
		
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try {
			findContainerItem = (Container)containerDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		return findContainerItem;
	}
	
	/**
	 * container retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Container findOneContainerByResourceName(String parentID, String resourceName) throws RSCException {
		Container findContainerItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findContainerItem = (Container)containerDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		return findContainerItem;
	}
	
	/**
	 * container retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Container findOneContainerByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Container findContainerItem = null;
		Container findNoFilterContainerItem = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findContainerItem = (Container)containerDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findContainerItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterContainerItem = findOneContainerResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findContainerItem)) findContainerItem = findNoFilterContainerItem;
		
		return findContainerItem;
	}
	
	/**
	 * container References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Container findOneContainerResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Container findContainerResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findContainerResourceRefItem = (Container) containerDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		return findContainerResourceRefItem;
	}
	
	/**
	 * container retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Container> findContainer(String key, String value) throws RSCException {
		List<Container> findContainerList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findContainerList = (List<Container>)containerDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		return findContainerList;
	}
	
	/**
	 * container List retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Container> findContainer(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Container>();
		
		List<Container> findContainerList = null;
		List<Container> findContainerNoFilterResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findContainerList = (List<Container>)containerDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findContainerList);
			
			if (filterCriteria.isFilterCriteria()) findContainerNoFilterResourceRefList = findContainerResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findContainerNoFilterResourceRefList.size(); i++) {
				Container noFilterContainer = findContainerNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findContainerList.size(); t++) {
					Container container = findContainerList.get(t);
					if (noFilterContainer.getResourceRef().getResourceID().equals(container.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findContainerList.add(noFilterContainer);
			}
		}
		
		return findContainerList;
	}
	
	/**
	 * container References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Container> findContainerResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Container> findContainerResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findContainerResourceRefList = (List<Container>) containerDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		return findContainerResourceRefList;
	}
	
	/**
	 * container References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Container> findContainerResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Container>();
		
		String includeField = "resourceRef";
		List<Container> findContainerResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findContainerResourceRefList = (List<Container>)containerDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findContainerResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.findFail.text"));
		}
		
		return findContainerResourceRefList;
	}
	
	/**
	 * container create
	 * @param url
	 * @param containerProfile
	 * @return
	 * @throws RSCException
	 */
	public Container createContainer(String url, Container containerProfile) throws RSCException {
		Container cntrItem = new Container();
		
		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP,SeqType.CONTAINER);
		
		String expirationTime = containerProfile.getExpirationTime();
		
		cntrItem.setResourceType(RESOURCE_TYPE.CONTAINER.getValue());
		cntrItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.CONTAINER.getValue(), seqID));
		cntrItem.setResourceName(!CommonUtil.isEmpty(containerProfile.getResourceName()) ? containerProfile.getResourceName() : cntrItem.getResourceID());
		cntrItem.setParentID(containerProfile.getParentID());
		cntrItem.setExpirationTime(expirationTime);
		cntrItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//cntrItem.getAccessControlPolicyIDs().addAll(containerProfile.getAccessControlPolicyIDs());
		cntrItem.getLabels().addAll(containerProfile.getLabels());
		cntrItem.setCreationTime(currentTime);
		cntrItem.setLastModifiedTime(currentTime);
		cntrItem.setStateTag(BigInteger.ZERO);
		cntrItem.getAnnounceTo().addAll(containerProfile.getAnnounceTo());
		cntrItem.getAnnouncedAttribute().addAll(containerProfile.getAnnouncedAttribute());
		cntrItem.setCreator(containerProfile.getCreator());
		cntrItem.setMaxNrOfInstances(containerProfile.getMaxNrOfInstances());
		cntrItem.setMaxByteSize(containerProfile.getMaxByteSize());
		cntrItem.setMaxInstanceAge(containerProfile.getMaxInstanceAge());
		cntrItem.setCurrentNrOfInstances(BigInteger.ZERO);
		cntrItem.setCurrentByteSize(BigInteger.ZERO);
		cntrItem.setContainerType(containerProfile.getContainerType());
		cntrItem.setLocationID(containerProfile.getLocationID());
		cntrItem.setOntologyRef(containerProfile.getOntologyRef());
		cntrItem.setHeartbeatPeriod(containerProfile.getHeartbeatPeriod());
		cntrItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, cntrItem), cntrItem.getResourceName(), RESOURCE_TYPE.CONTAINER.getValue(), cntrItem.getResourceID(), null));
		
		String locationID = containerProfile.getLocationID();
		String resourceID = cntrItem.getResourceID();
		
		if (!CommonUtil.isEmpty(locationID)) {
			
			LocationPolicy findLocationPolicyItem = null;
			if (CommonUtil.isEmpty(findLocationPolicyItem = locationPolicyService.findOneLocationPolicy("resourceID", locationID))) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.locationPolicy.noRegi.text"));
			}
			
			if (CommonUtil.isEmpty(findLocationPolicyItem.getLocationContainerID())) {
				LocationPolicy locationPolicyItem = new LocationPolicy();
				
				locationPolicyItem.setResourceID(locationID);
				locationPolicyItem.setLocationContainerID(resourceID);
				
				locationPolicyService.updateLocationContainerID(locationPolicyItem);
			} else {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.container.exist.locationID.text"));
			}
		}
		
		if (!CommonUtil.isEmpty(cntrItem.getLocationID())) {
			LocationPolicy locationPolicyItem = new LocationPolicy();
			
			locationPolicyItem.setResourceID(cntrItem.getLocationID());
			locationPolicyItem.setLocationContainerID(cntrItem.getResourceID());
			
			locationPolicyService.updateLocationContainerID(locationPolicyItem);
		}
		
		try {
			containerDao.insert(cntrItem);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "container create success");
			
		return this.findOneContainer(cntrItem.getResourceID());
		
	}
	
	/**
	 * container update
	 * @param containerItem
	 * @return
	 * @throws RSCException
	 */
	public Container updateContainer(Container containerProfile) throws RSCException {
		
		//입력받은 accessControlPolicyIDs 검증
		accessControlPolicyService.validateAccessControlPolicyIDs(containerProfile.getAccessControlPolicyIDs());
				
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= containerProfile.getResourceID();
		String expirationTime				= containerProfile.getExpirationTime();
		List<String> accessControlPolicyIDs = containerProfile.getAccessControlPolicyIDs();
		List<String> labels 				= containerProfile.getLabels();
		//BigInteger stateTag 				= containerProfile.getStateTag();
		List<String> announceTo 			= containerProfile.getAnnounceTo();
		List<String> announcedAttribute		= containerProfile.getAnnouncedAttribute();
		String creator						= containerProfile.getCreator();
		BigInteger maxNrOfInstances			= containerProfile.getMaxNrOfInstances();
		BigInteger maxByteSize				= containerProfile.getMaxByteSize();
		BigInteger maxInstanceAge			= containerProfile.getMaxInstanceAge();
		String locationID					= containerProfile.getLocationID();
		String ontologyRef					= containerProfile.getOntologyRef();
		String containerType 				= containerProfile.getContainerType();
		String heartbeatPeriod 				= containerProfile.getHeartbeatPeriod();
		
		Container findContainerItem = this.findOneContainer(containerProfile.getResourceID());
		
		String beforeLocationID = findContainerItem.getLocationID();
		String afterLocationID 	= containerProfile.getLocationID();
		
		if (!CommonUtil.isEmpty(afterLocationID)) {
			
			LocationPolicy findLocationPolicyItem = null;
			if (CommonUtil.isEmpty(findLocationPolicyItem = locationPolicyService.findOneLocationPolicy("resourceID", afterLocationID))) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.locationPolicy.noRegi.text"));
			}
			if (!CommonUtil.isEmpty(beforeLocationID)) {
				
				LocationPolicy beforeLocationPolicyItem = new LocationPolicy();
				
				beforeLocationPolicyItem.setResourceID(beforeLocationID);
				beforeLocationPolicyItem.setLocationContainerID("");
				
				locationPolicyService.updateLocationContainerID(beforeLocationPolicyItem);
			}
			
			if (CommonUtil.isEmpty(findLocationPolicyItem.getLocationContainerID())) {
				LocationPolicy locationPolicyItem = new LocationPolicy();
				
				locationPolicyItem.setResourceID(locationID);
				locationPolicyItem.setLocationContainerID(resourceID);
				
				locationPolicyService.updateLocationContainerID(locationPolicyItem);
			} else {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.container.exist.locationID.text"));
			}
		}
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		//if(!CommonUtil.isNull(stateTag))				update.set("stateTag", stateTag);
		if(!CommonUtil.isNull(announceTo))				update.set("announceTo", announceTo);
		if(!CommonUtil.isNull(announcedAttribute))		update.set("announcedAttribute", announcedAttribute);
		if(!CommonUtil.isNull(creator))					update.set("creator", creator);
		if(!CommonUtil.isNull(maxNrOfInstances))		update.set("maxNrOfInstances", maxNrOfInstances);
		if(!CommonUtil.isNull(maxByteSize))				update.set("maxByteSize", maxByteSize);
		if(!CommonUtil.isNull(maxInstanceAge))			update.set("maxInstanceAge", maxInstanceAge);
		if(!CommonUtil.isNull(locationID))				update.set("locationID", locationID);
		if(!CommonUtil.isNull(ontologyRef))				update.set("ontologyRef", ontologyRef);
		if(!CommonUtil.isNull(containerType))			update.set("containerType", containerType);
		if(!CommonUtil.isNull(heartbeatPeriod))			update.set("heartbeatPeriod", heartbeatPeriod);
														update.set("lastModifiedTime", currentTime);
		
		// when Container Create, Container Update, ContentInstance Create and ContentInstance Delete transactions are finished then Container State Tag should be increased.
		// [STATE TAG INCREASE ISSUE] modification start point
		update.set("stateTag", findContainerItem.getStateTag().add(BigInteger.ONE));
		// [STATE TAG INCREASE ISSUE] modification end point
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try {
			containerDao.update(query, update);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "container update success");
		
		subscriptionService.sendSubscription(containerProfile.getParentID(), containerProfile.getResourceID(), RESOURCE_STATUS.UPDATED, OPERATION_MONITOR.UPDATE, Container.class, update);
		
		return this.findOneContainer(resourceID);
	}
	
	/**
	 * container update
	 * @param containerItem
	 * @throws RSCException
	 */
	public void updateContainerLocationPolicy(Container containerItem) throws RSCException {
		
		if(CommonUtil.isEmpty(this.findOneContainer(containerItem.getResourceID()))) {
			throw new RSCException(CommonCode.RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
		}
		
		String currentTime = CommonUtil.getNowTimestamp();
			
		Update update = new Update();
		update.set("locationID", containerItem.getLocationID());
		update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(containerItem.getResourceID()));
		try { 
			
			containerDao.update(query, update);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "container locationID update success");
		
	}
	
	/**
	 * container delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteContainer(String resourceID) throws RSCException {
		
		Container findContainerItem = this.findOneContainer(resourceID);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		contentInstanceService.deleteRedisObjectsByContainerResourceID(resourceID);
		
		contentInstanceService.deleteContentInstanceByContainerResourceID(resourceID);
		
		try {
			subscriptionService.deleteSubscription("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "subscription remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.subscription.delFail.text"));
		}
		
		if (!CommonUtil.isEmpty(findContainerItem.getLocationID())) {
			LocationPolicy locationPolicyItem = new LocationPolicy();
			
			locationPolicyItem.setResourceID(findContainerItem.getLocationID());
			locationPolicyItem.setLocationContainerID("");
			
			locationPolicyService.updateLocationContainerID(locationPolicyItem);
		}
		
		try {
			containerDao.remove(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.container.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "container delete success");
		
		subscriptionService.sendSubscription(findContainerItem.getParentID(), findContainerItem.getResourceID(), RESOURCE_STATUS.DELETED, OPERATION_MONITOR.DELETE, Container.class, findContainerItem);

	}
	
	/**
	 * container delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteContainerByParentID(String parentID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			List<Container> findContainerList = this.findContainer("parentID", parentID);
			for(Container containerItem : findContainerList) {
				this.deleteContainer(containerItem.getResourceID());
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.container.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "container delete success");
		
	}
	
	/**
	 * container update
	 * @param contentInstanceList
	 * @throws RSCException
	 */
	public void updateContainerCurrentInstanceValue(List<ContentInstance> contentInstanceList) throws RSCException {
		
		List<ContentInstance> newContentInstanceList = new ArrayList<ContentInstance>();
		
		if(!CommonUtil.isEmpty(contentInstanceList) && contentInstanceList.size() > 0) {
			
			Map<String, ContentInstance> listMap = new LinkedHashMap<String, ContentInstance>();
			for(ContentInstance contentInstance : contentInstanceList) {
				if(!listMap.containsKey(contentInstance.getParentID())) {
					listMap.put(contentInstance.getParentID(), contentInstance);
				}
			}
			newContentInstanceList = new ArrayList<ContentInstance>(listMap.values());
			
		}
		
		if(!CommonUtil.isEmpty(newContentInstanceList) && newContentInstanceList.size() > 0) {
			
			for(ContentInstance newContentInstance : newContentInstanceList) {
				updateContainerCurrentInstanceValue(newContentInstance.getParentID());
			}
		}
		
	}
	
	/**
	 * container update
	 * @param resourceID
	 * @throws RSCException
	 */
	public void updateContainerCurrentInstanceValue(String resourceID) throws RSCException {
		
		long   instanceCnt = 0;
		long   currentByte = 0;
		
		Container containerItem = this.findOneContainer(resourceID);
		
		containerItem.setCurrentNrOfInstances(BigInteger.ZERO);
		containerItem.setCurrentByteSize(BigInteger.ZERO);
		containerItem.setOldest("");
		containerItem.setLatest("");
		
		List<ContentInstance> findContentInstanceList = contentInstanceService.findContentInstance(resourceID);
		
		for(ContentInstance contentInstanceItem : findContentInstanceList) {
			instanceCnt++;
			currentByte = currentByte + contentInstanceItem.getContentSize().longValue();
		}
		if(!CommonUtil.isEmpty(findContentInstanceList) && findContentInstanceList.size() > 0) {
			containerItem.setCurrentNrOfInstances(new BigInteger(Long.toString(instanceCnt)));
			containerItem.setCurrentByteSize(new BigInteger(Long.toString(currentByte)));
			containerItem.setOldest(findContentInstanceList.get(0).getResourceID());
			containerItem.setLatest(findContentInstanceList.get(findContentInstanceList.size()-1).getResourceID());
			containerItem.setStateTag(findContentInstanceList.get(findContentInstanceList.size()-1).getStateTag());
		}
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(containerItem.getParentID()));
		query.addCriteria(Criteria.where("resourceID").is(containerItem.getResourceID()));
		
		Update update = new Update();
		try {
			String currentTime = CommonUtil.getNowTimestamp();
			
			update.set("currentNrOfInstances",  containerItem.getCurrentNrOfInstances());
			update.set("currentByteSize",		containerItem.getCurrentByteSize());
			update.set("oldest",				containerItem.getOldest());
			update.set("latest",				containerItem.getLatest());
			update.set("stateTag",				containerItem.getStateTag());
			update.set("lastModifiedTime", 		currentTime);
			
			containerDao.update(query, update);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.container.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "container currentInstanceValue update success");
		
		//subscriptionService.sendSubscription(containerItem.getParentID(), containerItem.getResourceID(), SUBSCRIPTION_RESOURCE_STATUS_TYPE.UPDATED, ContainerVO.class, update);
			
	}
	
}
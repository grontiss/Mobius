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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.ContentInstanceDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.OPERATION_MONITOR;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_STATUS;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;


/**
 * contentInstance Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class ContentInstanceService {

	private static final Log logger = LogFactory.getLog(ContentInstanceService.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private SequenceDao seqDao;

	@Autowired
	private ContentInstanceDao contentInstanceDao;
	
	/**
	 * contentInstance retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance findOneContentInstance(String resourceID) throws RSCException {
		ContentInstance findContentInstanceItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findContentInstanceItem = (ContentInstance) contentInstanceDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}

		return findContentInstanceItem;
	}
	
	/**
	 * contentInstance retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance findOneContentInstanceByResourceName(String parentID, String resourceName) throws RSCException {
		ContentInstance findContentInstanceItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findContentInstanceItem = (ContentInstance) contentInstanceDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}

		return findContentInstanceItem;
	}
	
	/**
	 * contentInstance retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance findOneContentInstance(String resourceID, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		ContentInstance findContentInstanceItem = null;
		ContentInstance findNoFilterContentInstanceItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findContentInstanceItem = (ContentInstance) contentInstanceDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findContentInstanceItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterContentInstanceItem = findOneContentInstanceResourceRefByResourceName(resourceID);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findContentInstanceItem)) findContentInstanceItem = findNoFilterContentInstanceItem;

		return findContentInstanceItem;
	}
	
	/**
	 * contentInstance References retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance findOneContentInstanceResourceRefByResourceName(String resourceID) throws RSCException {
		
		String includeField = "resourceRef";
		ContentInstance findContentInstanceResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		query.fields().include(includeField);
		
		try {
			findContentInstanceResourceRefItem = (ContentInstance) contentInstanceDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		return findContentInstanceResourceRefItem;
	}
	
	/**
	 * contentInstance retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance findOneContentInstanceByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		ContentInstance findContentInstanceItem = null;
		ContentInstance findNoFilterContentInstanceItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findContentInstanceItem = (ContentInstance) contentInstanceDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findContentInstanceItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterContentInstanceItem = findOneContentInstanceResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findContentInstanceItem)) findContentInstanceItem = findNoFilterContentInstanceItem;

		return findContentInstanceItem;
	}
	
	/**
	 * contentInstance References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance findOneContentInstanceResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		ContentInstance findContentInstanceResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findContentInstanceResourceRefItem = (ContentInstance) contentInstanceDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		return findContentInstanceResourceRefItem;
	}
	
	/**
	 * contentInstance count retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public Long findCountContentInstance(String key, String value) throws RSCException {
		Long totalCount = 0l;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			totalCount = contentInstanceDao.count(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}

		return totalCount;
	}
	
	/**
	 * contentInstance retrieve
	 * @param query
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> findContentInstance(Query query) throws RSCException {
		List<ContentInstance> findContentInstanceList = null;
		try {
			findContentInstanceList = (List<ContentInstance>) contentInstanceDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}

		return findContentInstanceList;
	}
	
	/**
	 * contentInstance retrieve
	 * @param query
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> findContentInstance(String key, String value) throws RSCException {
		List<ContentInstance> findContentInstanceList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findContentInstanceList = (List<ContentInstance>) contentInstanceDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}

		return findContentInstanceList;
	}
	
	/**
	 * contentInstance retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> findContentInstance(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<ContentInstance>();
		
		List<ContentInstance> findContentInstanceList = null;
		List<ContentInstance> findContentInstanceNoFilterResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findContentInstanceList = (List<ContentInstance>) contentInstanceDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findContentInstanceList);
			
			if (filterCriteria.isFilterCriteria()) findContentInstanceNoFilterResourceRefList = findContentInstanceResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findContentInstanceNoFilterResourceRefList.size(); i++) {
				ContentInstance noFilterContentInstance = findContentInstanceNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findContentInstanceList.size(); t++) {
					ContentInstance contentInstance = findContentInstanceList.get(t);
					if (noFilterContentInstance.getResourceRef().getResourceID().equals(contentInstance.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findContentInstanceList.add(noFilterContentInstance);
			}
		}

		return findContentInstanceList;
	}
	
	/**
	 * contentInstance References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> findContentInstanceResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<ContentInstance> findContentInstanceResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findContentInstanceResourceRefList = (List<ContentInstance>) contentInstanceDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		return findContentInstanceResourceRefList;
	}
	
	/**
	 * contentInstance References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> findContentInstanceResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<ContentInstance>();
		
		String includeField = "resourceRef";
		List<ContentInstance> findContentInstanceResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findContentInstanceResourceRefList = (List<ContentInstance>) contentInstanceDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findContentInstanceResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}
		
		return findContentInstanceResourceRefList;
	}
	
	/**
	 * contentInstance create
	 * @param url
	 * @param contentInstanceProfile
	 * @return
	 * @throws RSCException
	 */
	public ContentInstance createContentInstance(String url, ContentInstance contentInstanceProfile) throws RSCException {
		
		Container findContainerItem = containerService.findOneContainer(contentInstanceProfile.getParentID());
		
		ContentInstance contentInstanceItem = new ContentInstance();
		
		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.CONTENT_INSTANCE);
		
		String expirationTime = contentInstanceProfile.getExpirationTime();
		
		try {
			contentInstanceItem.setResourceType(RESOURCE_TYPE.CONTENT_INSTANCE.getValue());
			contentInstanceItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.CONTENT_INSTANCE.getValue(), seqID));
			contentInstanceItem.setResourceName(!CommonUtil.isEmpty(contentInstanceProfile.getResourceName()) ? contentInstanceProfile.getResourceName() : contentInstanceItem.getResourceID());
			contentInstanceItem.setParentID(contentInstanceProfile.getParentID());
			contentInstanceItem.getLabels().addAll(contentInstanceProfile.getLabels());
			contentInstanceItem.setCreator(contentInstanceProfile.getCreator());
			contentInstanceItem.setExpirationTime(expirationTime);
			contentInstanceItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
			
			contentInstanceItem.setCreationTime(currentTime);
			contentInstanceItem.setLastModifiedTime(currentTime);
			contentInstanceItem.setStateTag(findContainerItem.getStateTag().add(BigInteger.ONE));
			contentInstanceItem.getAnnounceTo().addAll(contentInstanceProfile.getAnnounceTo());
			contentInstanceItem.getAnnouncedAttribute().addAll(contentInstanceProfile.getAnnouncedAttribute());
			contentInstanceItem.setContentInfo(contentInstanceProfile.getContentInfo());
			contentInstanceItem.setContentSize(new BigInteger(Integer.toString(contentInstanceProfile.getContent().getBytes("UTF-8").length)));
			contentInstanceItem.setOntologyRef(contentInstanceProfile.getOntologyRef());
			contentInstanceItem.setContent(contentInstanceProfile.getContent());
			//contentInstanceItem.setDvvoltage(contentInstanceProfile.getDvvoltage());	//++ USIS
			//contentInstanceItem.setGwvoltage(contentInstanceProfile.getGwvoltage());	//++ USIS
			contentInstanceItem.setPower(contentInstanceProfile.getPower());	//++ USIS
			contentInstanceItem.setBattery(contentInstanceProfile.getBattery());	//++ USIS
			contentInstanceItem.setShunt(contentInstanceProfile.getShunt());	//++ USIS
			contentInstanceItem.setSyncdate(contentInstanceProfile.getSyncdate());	//++ USIS (PQ)
			contentInstanceItem.setLinkType(contentInstanceProfile.getLinkType());
			contentInstanceItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, contentInstanceItem), contentInstanceItem.getResourceName(), RESOURCE_TYPE.CONTENT_INSTANCE.getValue(), contentInstanceItem.getResourceID(), null));
			
			contentInstanceDao.put(contentInstanceItem);
			//contentInstanceDao.insert(contentInstanceItem);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentinstance.createFail.text"));
		}
		
//		subscriptionService.sendSubscription(contentInstanceItem.getParentID(), contentInstanceItem.getResourceID(), RESOURCE_STATUS.CHILD_CREATED, ContentInstance.class, contentInstanceItem);
		
/*
		try {
			containerService.updateContainerCurrentInstanceValue(findContainerVO);

		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new IotException("600", CommonUtil.getMessage("msg.device.container.upFail.text"));
		}

*/
		mongoLogService.log(logger, LEVEL.INFO, "contentInstance create success");

		return contentInstanceItem;
	}

	/**
	 * contentInstance delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteContentInstanceByContainerResourceID(String parentID) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			contentInstanceDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentinstance.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "contentInstance delete success");

	}

	/**
	 * contentInstance delete
	 * @param listContainerIDs
	 * @throws RSCException
	 */
	public void deleteContentInstanceByContainerIDs(ArrayList<String> listContainerIDs) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").in(listContainerIDs));

		try {
			contentInstanceDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentinstance.delFail.text"));
		}

		mongoLogService.log(logger, LEVEL.INFO, "container inner contentInstance delete success");

	}

	/**
	 * contentInstance delete
	 * @param contentInstanceInfo
	 * @throws RSCException
	 */
	public void deleteContentInstance(ContentInstance contentInstanceInfo) throws RSCException {
		String parentID = contentInstanceInfo.getParentID();
		String resourceID = contentInstanceInfo.getResourceID();

		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("resourceID").is(resourceID));
			contentInstanceDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentinstance.delFail.text"));
		}
		
		subscriptionService.sendSubscription(parentID, resourceID, RESOURCE_STATUS.CHILD_DELETED, OPERATION_MONITOR.DELETE, ContentInstance.class, contentInstanceInfo);

		containerService.updateContainerCurrentInstanceValue(parentID);

		mongoLogService.log(logger, LEVEL.INFO, "contentInstance delete success");

	}

	/**
	 * contentInstant List retrieve
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> findContentInstance(String parentID) throws RSCException {
		List<ContentInstance> contentInstanceLIst = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.with(new Sort(Sort.Direction.ASC, "resourceID"));
		try {
			contentInstanceLIst = (List<ContentInstance>) contentInstanceDao.find(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentInstance.findFail.text"));
		}

		return contentInstanceLIst;
	}
	
	/**
	 * redis DB contentInstance List retrieve
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	public List<ContentInstance> getRedisObjectsByParentID(String parentID) throws RSCException {
		List<ContentInstance> findContentInstanceList = null;
		try {

			findContentInstanceList = contentInstanceDao.getRedisObjectsByParentID(parentID);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.container.upFail.text"));
		}
		return findContentInstanceList;
	}

	/**
	 * redis DB contentInstance List delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteRedisObjectsByContainerResourceID(String parentID) throws RSCException {
		try {

			contentInstanceDao.deleteRedisObjectsByParentID(parentID);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.contentinstance.delFail.text"));
		}
	}
	
	/**
	 * check createContentInstance info
	 * @param findContainerItem
	 * @param contentInstanceProfile
	 * @throws RSCException
	 */
	public void checkCreateContentInstance(Container findContainerItem, ContentInstance contentInstanceProfile) throws RSCException {
		try {
			checkCreateContentInstanceDetail(findContainerItem, contentInstanceProfile);
		} catch (RSCException e) {
			containerService.updateContainerCurrentInstanceValue(findContainerItem.getResourceID());
			
			findContainerItem = containerService.findOneContainer(findContainerItem.getResourceID());
			
			checkCreateContentInstanceDetail(findContainerItem, contentInstanceProfile);
		}
	}
	
	/**
	 * check createContentInstance detail info
	 * @param findContainerItem
	 * @param contentInstanceProfile
	 * @throws RSCException
	 */
	public void checkCreateContentInstanceDetail(Container findContainerItem, ContentInstance contentInstanceProfile) throws RSCException {
		BigInteger maxNrOfInstances = findContainerItem.getMaxNrOfInstances();
		BigInteger currentNrOfInstances = findContainerItem.getCurrentNrOfInstances() == null ? BigInteger.ONE : findContainerItem.getCurrentNrOfInstances().add(BigInteger.ONE);
		BigInteger maxByteSize = findContainerItem.getMaxByteSize();
		BigInteger currentByteSize = null;
		
		try {
			BigInteger contentLength = new BigInteger(Integer.toString(contentInstanceProfile.getContent().getBytes("UTF-8").length));
			currentByteSize = findContainerItem.getCurrentByteSize() == null ? BigInteger.ZERO.add(contentLength) : findContainerItem.getCurrentByteSize().add(contentLength);
		} catch (UnsupportedEncodingException e) {
			throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.device.contentinstance.createFail.text"));
		}
		
		if (!CommonUtil.isEmpty(maxNrOfInstances) && currentNrOfInstances.compareTo(maxNrOfInstances) == 1) throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.device.contentinstance.createFail.maxNrOfInstances.text"));
		if (!CommonUtil.isEmpty(maxByteSize) && currentByteSize.compareTo(maxByteSize) == 1) throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.device.contentinstance.createFail.maxByteSize.text"));
	}

}
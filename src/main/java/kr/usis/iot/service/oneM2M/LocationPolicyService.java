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
import java.util.List;

import javax.xml.datatype.Duration;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.LocationPolicy;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.LocationPolicyDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * locationPolicy Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class LocationPolicyService {

	private static final Log logger = LogFactory.getLog(LocationPolicyService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private ContainerService containerService;

	@Autowired
	private LocationPolicyDao locationPolicyDao;

	@Autowired
	private SequenceDao seqDao;

	/**
	 * locationPolicy retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy findOneLocationPolicy(String key, String value) throws RSCException {
		LocationPolicy findLocationPolicyItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findLocationPolicyItem = (LocationPolicy) locationPolicyDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}

		return findLocationPolicyItem;
	}
	
	/**
	 * locationPolicy retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy findOneLocationPolicyByResourceName(String parentID, String resourceName) throws RSCException {
		LocationPolicy findLocationPolicyItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findLocationPolicyItem = (LocationPolicy) locationPolicyDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}

		return findLocationPolicyItem;
	}
	
	/**
	 * locationPolicy retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy findOneLocationPolicyByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		LocationPolicy findLocationPolicyItem = null;
		LocationPolicy findNoFilterLocationPolicyItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findLocationPolicyItem = (LocationPolicy) locationPolicyDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findLocationPolicyItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterLocationPolicyItem = findOneLocationPolicyResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findLocationPolicyItem)) findLocationPolicyItem = findNoFilterLocationPolicyItem;

		return findLocationPolicyItem;
	}
	
	/**
	 * locationPolicy References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy findOneLocationPolicyResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		LocationPolicy findLocationPolicyResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findLocationPolicyResourceRefItem = (LocationPolicy) locationPolicyDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}
		
		return findLocationPolicyResourceRefItem;
	}
	
	/**
	 * locationPolicy retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<LocationPolicy> findLocationPolicy(String key, String value) throws RSCException {
		List<LocationPolicy> findLocationPolicyList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findLocationPolicyList = (List<LocationPolicy>) locationPolicyDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}

		return findLocationPolicyList;
	}
	
	/**
	 * locationPolicy retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<LocationPolicy> findLocationPolicy(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<LocationPolicy>();
		
		List<LocationPolicy> findLocationPolicyList = null;
		List<LocationPolicy> findLocationPolicyNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findLocationPolicyList = (List<LocationPolicy>) locationPolicyDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findLocationPolicyList);
			
			if (filterCriteria.isFilterCriteria()) findLocationPolicyNoFilterResourceRefList = findLocationPolicyResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findLocationPolicyNoFilterResourceRefList.size(); i++) {
				LocationPolicy noFilterLocationPolicy = findLocationPolicyNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findLocationPolicyList.size(); t++) {
					LocationPolicy locationPolicy = findLocationPolicyList.get(t);
					if (noFilterLocationPolicy.getResourceRef().getResourceID().equals(locationPolicy.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findLocationPolicyList.add(noFilterLocationPolicy);
			}
		}

		return findLocationPolicyList;
	}
	
	/**
	 * locationPolicy References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<LocationPolicy> findLocationPolicyResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<LocationPolicy> findLocationPolicyResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findLocationPolicyResourceRefList = (List<LocationPolicy>) locationPolicyDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}
		
		return findLocationPolicyResourceRefList;
	}
	
	/**
	 * locationPolicy References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<LocationPolicy> findLocationPolicyResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<LocationPolicy>();
		
		String includeField = "resourceRef";
		List<LocationPolicy> findLocationPolicyResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findLocationPolicyResourceRefList = (List<LocationPolicy>) locationPolicyDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findLocationPolicyResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}
		
		return findLocationPolicyResourceRefList;
	}
	
	/**
	 * locationPolicy retrieve
	 * @param locationContainerID
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy findOneLocationPolicyByLocationContainerID(String locationContainerID) throws RSCException {
		LocationPolicy findLocationPolicyItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("locationContainerID").is(locationContainerID));

		try {
			findLocationPolicyItem = (LocationPolicy) locationPolicyDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.findFail.text"));
		}

		return findLocationPolicyItem;
	}

	/**
	 * locationPolicy create
	 * @param url
	 * @param locationPolicyProfile
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy createLocationPolicy(String url, LocationPolicy locationPolicyProfile) throws RSCException {
		LocationPolicy locationPolicyItem = new LocationPolicy();
		
		Long seqID = seqDao.move(MovType.UP, SeqType.LOCATION_POLICY);
		String currentTime = CommonUtil.getNowTimestamp();
		
		String expirationTime = locationPolicyProfile.getExpirationTime();
		
		locationPolicyItem.setResourceType(RESOURCE_TYPE.LOCATION_POLICY.getValue());
		locationPolicyItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.LOCATION_POLICY.getValue(), seqID));
		locationPolicyItem.setResourceName(!CommonUtil.isEmpty(locationPolicyProfile.getResourceName()) ? locationPolicyProfile.getResourceName() : locationPolicyItem.getResourceID());
		locationPolicyItem.setParentID(locationPolicyProfile.getParentID());
		locationPolicyItem.setExpirationTime(expirationTime);
		locationPolicyItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//locationPolicyItem.getAccessControlPolicyIDs().addAll(locationPolicyProfile.getAccessControlPolicyIDs());
		locationPolicyItem.setCreationTime(currentTime);
		locationPolicyItem.setLastModifiedTime(currentTime);
		locationPolicyItem.getLabels().addAll(locationPolicyProfile.getLabels());
		locationPolicyItem.getAnnounceTo().addAll(locationPolicyProfile.getAnnounceTo());
		locationPolicyItem.getAnnouncedAttribute().addAll(locationPolicyProfile.getAnnouncedAttribute());
		locationPolicyItem.setLocationSource(locationPolicyProfile.getLocationSource());
		locationPolicyItem.setLocationUpdatePeriod(locationPolicyProfile.getLocationUpdatePeriod());
		locationPolicyItem.setLocationTargetID(locationPolicyProfile.getLocationTargetID());
		locationPolicyItem.setLocationServer(locationPolicyProfile.getLocationServer());
		//locationPolicyItem.setLocationContainerID(locationPolicyProfile.getLocationContainerID());
		locationPolicyItem.setLocationContainerName(locationPolicyProfile.getLocationContainerName());
		//locationPolicyItem.setLocationStatus(locationPolicyProfile.getLocationStatus());
		locationPolicyItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, locationPolicyItem), locationPolicyItem.getResourceName(), RESOURCE_TYPE.LOCATION_POLICY.getValue(), locationPolicyItem.getResourceID(), null));
		
		try {
			locationPolicyDao.insert(locationPolicyItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "locationPolicy create success");
		
		return this.findOneLocationPolicy("resourceID", locationPolicyItem.getResourceID());
		
	}

	/**
	 * locationPolicy delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteLocationPolicy(String key, String value) throws RSCException {
		
		LocationPolicy locationPolicyItem = this.findOneLocationPolicy(key, value);
		if(CommonUtil.isEmpty(locationPolicyItem)) {
			throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.locationPolicy.noRegi.text"));
		}
		
		String locationContainerID = locationPolicyItem.getLocationContainerID();
		if (!CommonUtil.isEmpty(locationContainerID)) containerService.deleteContainer(locationContainerID);
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			locationPolicyDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.locationPolicy.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "locationPolicy delete success");

	}
	
	/**
	 * locationPolicy update
	 * @param locationPolicyProfile
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy updateLocationPolicy(LocationPolicy locationPolicyProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID 					= locationPolicyProfile.getResourceID();
		String expirationTime 				= locationPolicyProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = locationPolicyProfile.getAccessControlPolicyIDs();
		List<String> labels 				= locationPolicyProfile.getLabels();
		List<String> announceTo 			= locationPolicyProfile.getAnnounceTo();
		List<String> announcedAttribute 	= locationPolicyProfile.getAnnouncedAttribute();
		BigInteger locationSource 			= locationPolicyProfile.getLocationSource();
		Duration locationUpdatePeriod 		= locationPolicyProfile.getLocationUpdatePeriod();
		String locationTargetId 			= locationPolicyProfile.getLocationTargetID();
		String locationServer 				= locationPolicyProfile.getLocationServer();
		String locationContainerName 		= locationPolicyProfile.getLocationContainerName();
		
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(announceTo))				update.set("announceTo", announceTo);
		if(!CommonUtil.isNull(announcedAttribute))		update.set("announcedAttribute", announcedAttribute);
		if(!CommonUtil.isNull(locationSource))			update.set("locationSource", locationSource);
		if(!CommonUtil.isNull(locationUpdatePeriod))	update.set("locationUpdatePeriod", locationUpdatePeriod);
		if(!CommonUtil.isNull(locationTargetId))		update.set("locationTargetId", locationTargetId);
		if(!CommonUtil.isNull(locationServer))			update.set("locationServer", locationServer);
		if(!CommonUtil.isNull(locationContainerName))	update.set("locationContainerName", locationContainerName);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try {
			locationPolicyDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.locationPolicy.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "locationPolicy update success");
		
		return this.findOneLocationPolicy("resourceID", resourceID);

	}
	
	/**
	 * locationContainerID update
	 * @param locationPolicyProfile
	 * @return
	 * @throws RSCException
	 */
	public LocationPolicy updateLocationContainerID(LocationPolicy locationPolicyProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID 					= locationPolicyProfile.getResourceID();
		String locationContainerID 			= locationPolicyProfile.getLocationContainerID();
		
		Update update = new Update();
		if(!CommonUtil.isNull(locationContainerID))		update.set("locationContainerID", locationContainerID);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try {
			locationPolicyDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.locationPolicy.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "locationPolicy update success");
		
		return this.findOneLocationPolicy("resourceID", resourceID);

	}

}
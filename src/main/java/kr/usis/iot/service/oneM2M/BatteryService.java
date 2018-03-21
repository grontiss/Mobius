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

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.Battery;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.BatteryDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.MGMT_DEFINITION;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * battery Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class BatteryService {

	private static final Log logger = LogFactory.getLog(BatteryService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private BatteryDao batteryDao;
	
	@Autowired
	private NodeService nodeService;	

	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * battery count retrieve
	 * @param parentID
	 * @return
	 */
	public long getCount(String parentID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		long cnt = 0;
		
		try {
			cnt = batteryDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "battery get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}

	/**
	 * battery retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Battery findOneBatteryByResourceName(String parentID, String resourceName) throws RSCException {
		Battery findBatteryItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findBatteryItem = (Battery) batteryDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}

		return findBatteryItem;
	}
	
	
	/**
	 * battery retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Battery findOneBattery(String resourceID) throws RSCException {
		Battery findBatteryItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findBatteryItem = (Battery)batteryDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}
		
		return findBatteryItem;
	}
	
	/**
	 * battery retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public Battery findOneBattery(String key, String value) throws RSCException {
		Battery findBatteryItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findBatteryItem = (Battery) batteryDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}

		return findBatteryItem;
	}
	
	/**
	 * battery retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Battery findOneBatteryByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Battery findBatteryItem = null;
		Battery findNoFilterBatteryItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findBatteryItem = (Battery) batteryDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findBatteryItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterBatteryItem = findOneBatteryResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findBatteryItem)) findBatteryItem = findNoFilterBatteryItem;

		return findBatteryItem;
	}
	
	/**
	 * battery References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Battery findOneBatteryResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Battery findBatteryResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findBatteryResourceRefItem = (Battery) batteryDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}
		
		return findBatteryResourceRefItem;
	}
	
	/**
	 * battery retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Battery> findBattery(String key, String value) throws RSCException {
		List<Battery> findBatteryList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findBatteryList = (List<Battery>) batteryDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}

		return findBatteryList;
	}
	
	
	/**
	 * battery retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Battery> findBattery(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Battery>();
		
		List<Battery> findBatteryList = null;
		List<Battery> findBatteryNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findBatteryList = (List<Battery>) batteryDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findBatteryList);
			
			if (filterCriteria.isFilterCriteria()) findBatteryNoFilterResourceRefList = findBatteryResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findBatteryNoFilterResourceRefList.size(); i++) {
				Battery noFilterBattery = findBatteryNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findBatteryList.size(); t++) {
					Battery battery = findBatteryList.get(t);
					if (noFilterBattery.getResourceRef().getResourceID().equals(battery.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findBatteryList.add(noFilterBattery);
			}
		}

		return findBatteryList;
	}
	
	/**
	 * battery References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Battery> findBatteryResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Battery> findBatteryResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findBatteryResourceRefList = (List<Battery>) batteryDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}
		
		return findBatteryResourceRefList;
	}
	
	/**
	 * battery References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Battery> findBatteryResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Battery>();
		
		String includeField = "resourceRef";
		List<Battery> findBatteryResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findBatteryResourceRefList = (List<Battery>) batteryDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findBatteryResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.findFail.text"));
		}
		
		return findBatteryResourceRefList;
	}

	/**
	 * battery create
	 * @param url
	 * @param batteryProfile
	 * @return
	 * @throws RSCException
	 */
	public Battery createBattery(String url, Battery batteryProfile) throws RSCException {
		Battery batteryItem = new Battery();

		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.BATTERY);
		
		String expirationTime = batteryProfile.getExpirationTime();
		
		batteryItem.setResourceType(RESOURCE_TYPE.MGMT_OBJ.getValue());
		batteryItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.BATTERY.getValue(), seqID));
		batteryItem.setResourceName(!CommonUtil.isEmpty(batteryProfile.getResourceName()) ? batteryProfile.getResourceName() : batteryItem.getResourceID());
		batteryItem.setParentID(batteryProfile.getParentID());
		batteryItem.setMgmtDefinition(MGMT_DEFINITION.BATTERY.getValue());
		batteryItem.setExpirationTime(expirationTime);
		batteryItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//batteryItem.getAccessControlPolicyIDs().addAll(batteryProfile.getAccessControlPolicyIDs());
		batteryItem.setCreationTime(currentTime);
		batteryItem.setLastModifiedTime(currentTime);
		batteryItem.getLabels().addAll(batteryProfile.getLabels());
		batteryItem.getObjectIDs().addAll(batteryProfile.getObjectIDs());
		batteryItem.getObjectPaths().addAll(batteryProfile.getObjectPaths());
		batteryItem.setDescription(batteryProfile.getDescription());
		batteryItem.setBatteryLevel(batteryProfile.getBatteryLevel());
		batteryItem.setBatteryStatus(batteryProfile.getBatteryStatus());
		batteryItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, batteryItem), batteryItem.getResourceName(), RESOURCE_TYPE.MGMT_OBJ.getValue(), batteryItem.getResourceID(), MGMT_DEFINITION.BATTERY.getValue()));
		
		try {
			batteryDao.insert(batteryItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "battery create success");

		return this.findOneBattery(batteryItem.getResourceID());
	}

	/**
	 * battery delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteBattery(String key, String value) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			batteryDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "battery delete success");

	}
	
	
	/**
	 * battery update
	 * @param batteryProfile
	 * @return
	 * @throws RSCException
	 */
	public Battery updateBattery(Battery batteryProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= batteryProfile.getResourceID();
		String expirationTime				= batteryProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = batteryProfile.getAccessControlPolicyIDs();
		List<String> labels 				= batteryProfile.getLabels();
		List<String> objectIDs 				= batteryProfile.getObjectIDs();
		List<String> objectPaths 			= batteryProfile.getObjectPaths();
		String description 					= batteryProfile.getDescription();
		Long batteryLevel 					= batteryProfile.getBatteryLevel();
		BigInteger batteryStatus			= batteryProfile.getBatteryStatus();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(objectIDs))				update.set("objectIDs", objectIDs);
		if(!CommonUtil.isNull(objectPaths))				update.set("objectPaths", objectPaths);
		if(!CommonUtil.isNull(description))				update.set("description", description);
		if(!CommonUtil.isNull(batteryLevel))			update.set("batteryLevel", batteryLevel);
		if(!CommonUtil.isNull(batteryStatus))			update.set("batteryStatus", batteryStatus);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try { 
			batteryDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.battery.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "battery update success");
		
		return this.findOneBattery(resourceID);
		
	}

}
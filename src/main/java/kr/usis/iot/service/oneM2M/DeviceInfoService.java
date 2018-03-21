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

import java.util.ArrayList;
import java.util.List;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.DeviceInfo;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.DeviceInfoDao;
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
 * deviceInfo Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class DeviceInfoService {

	private static final Log logger = LogFactory.getLog(DeviceInfoService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private DeviceInfoDao deviceInfoDao;
	
	@Autowired
	private NodeService nodeService;	

	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * deviceInfo count retrieve
	 * @param parentID
	 * @return
	 */
	public long getCount(String parentID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		long cnt = 0;
		
		try {
			cnt = deviceInfoDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "deviceInfo get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}

	/**
	 * deviceInfo retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo findOneDeviceInfoByResourceName(String parentID, String resourceName) throws RSCException {
		DeviceInfo findDeviceInfoItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findDeviceInfoItem = (DeviceInfo) deviceInfoDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}

		return findDeviceInfoItem;
	}
	
	/**
	 * deviceInfo retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo findOneDeviceInfo(String key, String value) throws RSCException {
		DeviceInfo findDeviceInfoItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findDeviceInfoItem = (DeviceInfo) deviceInfoDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}

		return findDeviceInfoItem;
	}
	
	/**
	 * deviceInfo retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<DeviceInfo> findDeviceInfo(String key, String value) throws RSCException {
		List<DeviceInfo> findDeviceInfoList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findDeviceInfoList = (List<DeviceInfo>) deviceInfoDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}

		return findDeviceInfoList;
	}
	
	/**
	 * deviceInfo retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<DeviceInfo> findDeviceInfo(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<DeviceInfo>();
		
		List<DeviceInfo> findDeviceInfoList = null;
		List<DeviceInfo> findDeviceInfoNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findDeviceInfoList = (List<DeviceInfo>) deviceInfoDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findDeviceInfoList);
			
			if (filterCriteria.isFilterCriteria()) findDeviceInfoNoFilterResourceRefList = findDeviceInfoResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findDeviceInfoNoFilterResourceRefList.size(); i++) {
				DeviceInfo noFilterDeviceInfo = findDeviceInfoNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findDeviceInfoList.size(); t++) {
					DeviceInfo deviceInfo = findDeviceInfoList.get(t);
					if (noFilterDeviceInfo.getResourceRef().getResourceID().equals(deviceInfo.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findDeviceInfoList.add(noFilterDeviceInfo);
			}
		}

		return findDeviceInfoList;
	}
	
	/**
	 * deviceInfo References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<DeviceInfo> findDeviceInfoResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<DeviceInfo> findDeviceInfoResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findDeviceInfoResourceRefList = (List<DeviceInfo>) deviceInfoDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}
		
		return findDeviceInfoResourceRefList;
	}
	
	/**
	 * deviceInfo References retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<DeviceInfo> findDeviceInfoResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<DeviceInfo>();
		
		String includeField = "resourceRef";
		List<DeviceInfo> findDeviceInfoResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findDeviceInfoResourceRefList = (List<DeviceInfo>) deviceInfoDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findDeviceInfoResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}
		
		return findDeviceInfoResourceRefList;
	}
	
	/**
	 * deviceInfo retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo findOneDeviceInfoByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		DeviceInfo findDeviceInfoItem = null;
		DeviceInfo findNoFilterDeviceInfoItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findDeviceInfoItem = (DeviceInfo) deviceInfoDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findDeviceInfoItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterDeviceInfoItem = findOneDeviceInfoResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findDeviceInfoItem)) findDeviceInfoItem = findNoFilterDeviceInfoItem;

		return findDeviceInfoItem;
	}
	
	/**
	 * deviceInfo References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo findOneDeviceInfoResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		DeviceInfo findDeviceInfoResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findDeviceInfoResourceRefItem = (DeviceInfo) deviceInfoDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}
		
		return findDeviceInfoResourceRefItem;
	}
	
	/**
	 * deviceInfo retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo findOneDeviceInfo(String resourceID) throws RSCException {
		DeviceInfo findDeviceInfoItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findDeviceInfoItem = (DeviceInfo)deviceInfoDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.deviceInfo.findFail.text"));
		}
		
		return findDeviceInfoItem;
	}
	
	/**
	 * deviceInfo create
	 * @param url
	 * @param deviceInfoProfile
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo createDeviceInfo(String url, DeviceInfo deviceInfoProfile) throws RSCException {
		DeviceInfo deviceInfoItem = new DeviceInfo();

		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.DEVICE_INFO);
		
		String expirationTime = deviceInfoProfile.getExpirationTime();
		
		deviceInfoItem.setResourceType(RESOURCE_TYPE.MGMT_OBJ.getValue());
		deviceInfoItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.DEVICE_INFO.getValue(), seqID));
		deviceInfoItem.setResourceName(!CommonUtil.isEmpty(deviceInfoProfile.getResourceName()) ? deviceInfoProfile.getResourceName() : deviceInfoItem.getResourceID());
		deviceInfoItem.setParentID(deviceInfoProfile.getParentID());
		deviceInfoItem.setMgmtDefinition(MGMT_DEFINITION.DEVICE_INFO.getValue());
		deviceInfoItem.setExpirationTime(expirationTime);
		deviceInfoItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//deviceInfoItem.getAccessControlPolicyIDs().addAll(deviceInfoProfile.getAccessControlPolicyIDs());
		deviceInfoItem.setCreationTime(currentTime);
		deviceInfoItem.setLastModifiedTime(currentTime);
		deviceInfoItem.getLabels().addAll(deviceInfoProfile.getLabels());
		deviceInfoItem.getObjectIDs().addAll(deviceInfoProfile.getObjectIDs());
		deviceInfoItem.getObjectPaths().addAll(deviceInfoProfile.getObjectPaths());
		deviceInfoItem.setDescription(deviceInfoProfile.getDescription());
		deviceInfoItem.setDeviceLabel(deviceInfoProfile.getDeviceLabel());
		deviceInfoItem.setManufacturer(deviceInfoProfile.getManufacturer());
		deviceInfoItem.setModel(deviceInfoProfile.getModel());
		deviceInfoItem.setDeviceType(deviceInfoProfile.getDeviceType());
		deviceInfoItem.setFwVersion(deviceInfoProfile.getFwVersion());
		deviceInfoItem.setSwVersion(deviceInfoProfile.getSwVersion());
		deviceInfoItem.setHwVersion(deviceInfoProfile.getHwVersion());
		deviceInfoItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, deviceInfoItem), deviceInfoItem.getResourceName(), RESOURCE_TYPE.MGMT_OBJ.getValue(), deviceInfoItem.getResourceID(), MGMT_DEFINITION.DEVICE_INFO.getValue()));
		
		try {
			deviceInfoDao.insert(deviceInfoItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "deviceInfo create success");

		return this.findOneDeviceInfo(deviceInfoItem.getResourceID());
	}

	/**
	 * deviceInfo delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteDeviceInfo(String key, String value) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			deviceInfoDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "deviceInfo delete success");

	}
	
	/**
	 * deviceInfo update
	 * @param parentID
	 * @param firmwareVersion
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo updateDeviceInfoByFirmware(String parentID, String firmwareVersion) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		String currentTime = CommonUtil.getNow("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		Update updateQuery = new Update();
		updateQuery.set("lastModifiedTime", currentTime);
		if(!CommonUtil.isNull(firmwareVersion))	updateQuery.set("fwVersion", firmwareVersion);
		
		try { 
			deviceInfoDao.update(query, updateQuery);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.deviceInfo.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "Firmware update success");
		
		return this.findOneDeviceInfo("parentID", parentID);
	}
	
	/**
	 * deviceInfo update
	 * @param deviceInfoProfile
	 * @return
	 * @throws RSCException
	 */
	public DeviceInfo updateDeviceInfo(DeviceInfo deviceInfoProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= deviceInfoProfile.getResourceID();
		String expirationTime				= deviceInfoProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = deviceInfoProfile.getAccessControlPolicyIDs();
		List<String> labels 				= deviceInfoProfile.getLabels();
		List<String> objectIDs 				= deviceInfoProfile.getObjectIDs();
		List<String> objectPaths 			= deviceInfoProfile.getObjectPaths();
		String description 					= deviceInfoProfile.getDescription();
		String manufacturer 				= deviceInfoProfile.getManufacturer();
		String model 						= deviceInfoProfile.getModel();
		String deviceType 					= deviceInfoProfile.getDeviceType();
		String fwVersion 					= deviceInfoProfile.getFwVersion();
		String swVersion					= deviceInfoProfile.getSwVersion();
		String hwVersion 					= deviceInfoProfile.getHwVersion();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(objectIDs))				update.set("objectIDs", objectIDs);
		if(!CommonUtil.isNull(objectPaths))				update.set("objectPaths", objectPaths);
		if(!CommonUtil.isNull(description))				update.set("description", description);
		if(!CommonUtil.isNull(manufacturer))			update.set("manufacturer", manufacturer);
		if(!CommonUtil.isNull(model))					update.set("model", model);
		if(!CommonUtil.isNull(deviceType))				update.set("deviceType", deviceType);
		if(!CommonUtil.isNull(fwVersion))				update.set("fwVersion", fwVersion);
		if(!CommonUtil.isNull(swVersion))				update.set("swVersion", swVersion);
		if(!CommonUtil.isNull(hwVersion))				update.set("hwVersion",hwVersion);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try { 
			deviceInfoDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.deviceInfo.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "deviceInfo update success");
		
		return this.findOneDeviceInfo(resourceID);
		
	}

}
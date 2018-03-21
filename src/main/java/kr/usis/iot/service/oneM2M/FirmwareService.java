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
import kr.usis.iot.domain.oneM2M.ActionStatus;
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Firmware;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.FirmwareDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.MGMT_DEFINITION;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;
import kr.usis.iot.util.oneM2M.CommonCode.STATUS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * firmware Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class FirmwareService {

	private static final Log logger = LogFactory.getLog(FirmwareService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private DeviceInfoService deviceInfoService;
	
	@Autowired
	private FirmwareDao firmwareDao;
	
	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * firmware retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Firmware findOneFirmware(String resourceID) throws RSCException {

		Firmware findFirmwareItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {

			findFirmwareItem = (Firmware) firmwareDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}

		return findFirmwareItem;
	}
	
	/**
	 * firmware retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Firmware findOneFirmwareByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;

		Firmware findFirmwareItem = null;
		Firmware findNoFilterFirmwareItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findFirmwareItem = (Firmware) firmwareDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findFirmwareItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterFirmwareItem = findOneFirmwareResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findFirmwareItem)) findFirmwareItem = findNoFilterFirmwareItem;

		return findFirmwareItem;
	}
	
	/**
	 * firmware References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Firmware findOneFirmwareResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Firmware findFirmwareResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findFirmwareResourceRefItem = (Firmware) firmwareDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		return findFirmwareResourceRefItem;
	}
	
	/**
	 * firmware retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Firmware> findFirmware(String key, String value) throws RSCException {

		List<Firmware> findFirmwareList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findFirmwareList = (List<Firmware>) firmwareDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}

		return findFirmwareList;
	}
	
	/**
	 * firmware retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Firmware> findFirmware(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Firmware>();

		List<Firmware> findFirmwareList = null;
		List<Firmware> findFirmwareNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findFirmwareList = (List<Firmware>) firmwareDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findFirmwareList);
			
			if (filterCriteria.isFilterCriteria()) findFirmwareNoFilterResourceRefList = findFirmwareResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findFirmwareNoFilterResourceRefList.size(); i++) {
				Firmware noFilterFirmware = findFirmwareNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findFirmwareList.size(); t++) {
					Firmware firmware = findFirmwareList.get(t);
					if (noFilterFirmware.getResourceRef().getResourceID().equals(firmware.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findFirmwareList.add(noFilterFirmware);
			}
		}

		return findFirmwareList;
	}
	
	/**
	 * firmware References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Firmware> findFirmwareResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Firmware> findFirmwareResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findFirmwareResourceRefList = (List<Firmware>) firmwareDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		return findFirmwareResourceRefList;
	}
	
	/**
	 * firmware References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Firmware> findFirmwareResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Firmware>();

		String includeField = "resourceRef";
		List<Firmware> findFirmwareResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findFirmwareResourceRefList = (List<Firmware>) firmwareDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findFirmwareResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		return findFirmwareResourceRefList;
	}
	
	/**
	 * firmware retrieve
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	public Firmware findOneFirmwareByParentID(String parentID) throws RSCException {
		
		Firmware findFirmwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("updateStatus.status").is(STATUS.SUCCESSFUL.getValue()));
		
		try {
			
			findFirmwareItem = (Firmware) firmwareDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		return findFirmwareItem;
	}
	
	/**
	 * firmware count retrieve
	 * @param parentID
	 * @param status
	 * @return
	 */
	public long getCount(String parentID, BigInteger status){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("updateStatus.status").is(status));
		
		long cnt = 0;
		
		try {
			cnt = firmwareDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "firmware get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * firmware count retrieve
	 * @param parentID
	 * @param version
	 * @return
	 */
	public long getCountByVersion(String parentID, String version){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("version").is(version));
		query.addCriteria(Criteria.where("updateStatus.status").is(STATUS.SUCCESSFUL.getValue()));
		
		long cnt = 0;
		
		try {
			cnt = firmwareDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "firmware get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * firmware count retrieve
	 * @param parentID
	 * @param version
	 * @return
	 * @throws RSCException
	 */
	public long getCountControlling(String parentID, String version) throws RSCException {
		Firmware findFirmwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("version").is(version));
		query.addCriteria(Criteria.where("updateStatus.status").is(STATUS.IN_PROCESS.getValue()));
		
		long cnt = 0;
		
		try {
			findFirmwareItem = (Firmware)firmwareDao.findOne(query);
			
			if(!CommonUtil.isEmpty(findFirmwareItem)) {
				if(CommonUtil.checkElapseTime(findFirmwareItem.getLastModifiedTime(), Long.valueOf(CommonUtil.getConfig("iot.mgmtCmd.status.check.time")), "yyyy-MM-dd'T'HH:mm:ssXXX") > 0){
					this.deleteFirmwareByStatus(findFirmwareItem.getResourceID(), STATUS.IN_PROCESS.getValue());
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		try {
			cnt = firmwareDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "firmware get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * firmware retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Firmware findOneFirmwareByResourceName(String parentID, String resourceName) throws RSCException {
		Firmware findFirmwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findFirmwareItem = (Firmware)firmwareDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.firmware.findFail.text"));
		}
		
		return findFirmwareItem;
	}
	
	/**
	 * firmware create
	 * @param firmwareItem
	 * @throws RSCException
	 */
	public void createFirmware(Firmware firmwareItem) throws RSCException {
		
		try {
			firmwareDao.insert(firmwareItem);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "firmware create success");
		
	}
	
	/**
	 * firmware create
	 * @param url
	 * @param firmwareProfile
	 * @param saveYn
	 * @return
	 * @throws RSCException
	 */
	public Firmware createFirmware(String url, Firmware firmwareProfile, boolean saveYn) throws RSCException {
		Firmware firmwareItem = new Firmware();
		
		String currentTime = CommonUtil.getNowTimestamp();
		long seqID = seqDao.move(MovType.UP, SeqType.FIRMWARE);
		
		String expirationTime = firmwareProfile.getExpirationTime();
		
		firmwareItem.setResourceType(RESOURCE_TYPE.MGMT_OBJ.getValue());
		firmwareItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.FIRMWARE.getValue(), seqID));
		firmwareItem.setResourceName(!CommonUtil.isEmpty(firmwareProfile.getResourceName()) ? firmwareProfile.getResourceName() : firmwareItem.getResourceID());
		firmwareItem.setParentID(firmwareProfile.getParentID());
		firmwareItem.setMgmtDefinition(MGMT_DEFINITION.FIRMWARE.getValue());
		firmwareItem.setExpirationTime(expirationTime);
		firmwareItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//firmwareItem.getAccessControlPolicyIDs().addAll(firmwareProfile.getAccessControlPolicyIDs());
		firmwareItem.setCreationTime(currentTime);
		firmwareItem.setLastModifiedTime(currentTime);
		firmwareItem.getLabels().addAll(firmwareProfile.getLabels());
		firmwareItem.getObjectIDs().addAll(firmwareProfile.getObjectIDs());
		firmwareItem.getObjectPaths().addAll(firmwareProfile.getObjectPaths());
		firmwareItem.setDescription(firmwareProfile.getDescription());
		firmwareItem.setVersion(firmwareProfile.getVersion());
		firmwareItem.setName(firmwareProfile.getName());
		firmwareItem.setURL(firmwareProfile.getURL());
		firmwareItem.setUpdate(firmwareProfile.isUpdate());
		firmwareItem.setUpdateStatus(firmwareProfile.getUpdateStatus());
		firmwareItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, firmwareItem), firmwareItem.getResourceName(), RESOURCE_TYPE.MGMT_OBJ.getValue(), firmwareItem.getResourceID(), MGMT_DEFINITION.FIRMWARE.getValue()));
		
		if (saveYn) {
			try {
				firmwareDao.insert(firmwareItem);
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(),e);
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
				throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.createFail.text"));
			}
			mongoLogService.log(logger, LEVEL.INFO, "firmware create success");
			
			return this.findOneFirmware(firmwareItem.getResourceID());
		} else {
			mongoLogService.log(logger, LEVEL.INFO, "firmware memory create success");
			
			return firmwareItem;
		}
	}
	
	/**
	 * firmware upgrade
	 * @param url
	 * @param nodeInfo
	 * @param cmdType
	 * @param firmwareProfile
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance firmwareUpgrade(String url, Node nodeInfo, String cmdType, Firmware firmwareProfile) throws RSCException {
		String remoteCSEResourceID = nodeInfo.getHostedCSELink();
		
		ArrayList<String[]> listPointOfAceess = null;
		listPointOfAceess = mgmtCmdService.getPointOfAccessListByRemoteCSEResourceID(remoteCSEResourceID);
		
		MgmtCmd findMgmtCmdItem = null;
		if(CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByCmdType(remoteCSEResourceID, cmdType))) {
			throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noFirmware.text"));
		}
		
		firmwareProfile.setParentID(nodeInfo.getResourceID());
		
		Firmware findFirmwareItem = this.createFirmware(url, firmwareProfile, false);
		
		findMgmtCmdItem.setExecReqArgs(mCommonService.marshalToXmlString(findFirmwareItem));
		ExecInstance findExecInstance = mgmtCmdService.createExecInstance(url, findMgmtCmdItem);
		
		boolean isRequestSuccess = mgmtCmdService.callDevice(findMgmtCmdItem, findExecInstance, listPointOfAceess);
		
		if(isRequestSuccess) {
			findFirmwareItem.setUpdateStatus(new ActionStatus(null, STATUS.IN_PROCESS.getValue()));
			this.createFirmware(url, findFirmwareItem, true);
		}
		mongoLogService.log(logger, LEVEL.DEBUG, "[Firmware upgrade control] success");
		
		return findExecInstance;
	}
	
	/**
	 * firmware update
	 * @param resourceID
	 * @param newFirmwareProfile
	 * @return
	 * @throws RSCException
	 */
	public Firmware updateFirmware(Firmware firmwareProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID 					= firmwareProfile.getResourceID();
		String expirationTime				= firmwareProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = firmwareProfile.getAccessControlPolicyIDs();
		List<String> labels 				= firmwareProfile.getLabels();
		List<String> objectIDs 				= firmwareProfile.getObjectIDs();
		List<String> objectPaths 			= firmwareProfile.getObjectPaths();
		String description 					= firmwareProfile.getDescription();
		String version 						= firmwareProfile.getVersion();
		String name 						= firmwareProfile.getName();
		String url 							= firmwareProfile.getURL();
		Boolean update 						= firmwareProfile.isUpdate();
		
		Update updateQuery = new Update();
		if(!CommonUtil.isNull(expirationTime))			updateQuery.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			updateQuery.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	updateQuery.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					updateQuery.set("labels", labels);
		if(!CommonUtil.isNull(objectIDs))				updateQuery.set("objectIDs", objectIDs);
		if(!CommonUtil.isNull(objectPaths))				updateQuery.set("objectPaths", objectPaths);
		if(!CommonUtil.isNull(description))				updateQuery.set("description", description);
		if(!CommonUtil.isNull(version))					updateQuery.set("version", version);
		if(!CommonUtil.isNull(name))					updateQuery.set("name", name);
		if(!CommonUtil.isNull(url))						updateQuery.set("url", url);
		if(!CommonUtil.isNull(update))					updateQuery.set("update", update);
														updateQuery.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try { 
			firmwareDao.update(query, updateQuery);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.firmware.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "Firmware update success");
		
		return this.findOneFirmware(resourceID);
	}
	
	/**
	 * firmware upgrade result update
	 * @param resourceID
	 * @param execResult
	 * @return
	 * @throws RSCException
	 */
	public Firmware firmwareUpgradeResult(String resourceID, String execResult) throws RSCException {
		Firmware findFirmwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		String currentTime = CommonUtil.getNow("yyyy-MM-dd'T'HH:mm:ssXXX"); 
		BigInteger status = new BigInteger(execResult);
		
		Update update = new Update();
		update.set("lastModifiedTime", currentTime);
		update.set("updateStatus.status", status);
		
		try {
			firmwareDao.update(query, update);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.firmware.upFail.text"));
		}
		
		try {
			findFirmwareItem = this.findOneFirmware(resourceID);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.deviceInfo.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "Firmware update success");
		
		return findFirmwareItem;
	}
	
	/**
	 * firmware delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteFirmware(String key, String value) throws RSCException {
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			firmwareDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "firmware delete success");

	}
	
	/**
	 * firmware delete
	 * @param resourceID
	 * @param status
	 * @throws RSCException
	 */
	public void deleteFirmwareByStatus(String resourceID, BigInteger status) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		query.addCriteria(Criteria.where("updateStatus.status").is(status));
		
		try {
			
			firmwareDao.remove(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "firmware delete success");
		
	}
	
	/**
	 * firmware delete
	 * @param resourceID
	 * @param status
	 * @throws RSCException
	 */
	public void deleteFirmwareByUpdateStatus(String resourceID, BigInteger status) throws RSCException {
		
		Query query = new Query(Criteria.where("resourceID").is(resourceID));
		query.addCriteria(Criteria.where("updateStatus.status").is(status));
		
		try {
			firmwareDao.remove(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "firmware delete success");
	}	
}
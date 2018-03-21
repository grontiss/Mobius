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
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.Software;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.SoftwareDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.BOOLEAN;
import kr.usis.iot.util.oneM2M.CommonCode.CMD_TYPE;
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
 * software service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class SoftwareService {

	private static final Log logger = LogFactory.getLog(SoftwareService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
//	@Autowired
//	private AEService aEService;

	@Autowired
	private SoftwareDao softwareDao;

	@Autowired
	private SequenceDao seqDao;
	
	
	/**
	 * software count retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCount(String resourceID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = softwareDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "software get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}	
	
	/**
	 * software count retrieve
	 * @param parentID
	 * @param name
	 * @return
	 */
	public long getCountByName(String parentID, String name){
		return this.getCountByNameVersion(parentID, name, null);
	}
	
	/**
	 * software count retrieve
	 * @param parentID
	 * @param name
	 * @param version
	 * @return
	 */
	public long getCountByNameVersion(String parentID, String name, String version){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("name").is(name));
		if(!CommonUtil.isEmpty(version)) {
			query.addCriteria(Criteria.where("version").is(version));
			
		}
		query.addCriteria(Criteria.where("installStatus").is(STATUS.SUCCESSFUL.getValue()));
		
		
		long cnt = 0;
		
		try {
			cnt = softwareDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "software get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * software count retrieve
	 * @param parentID
	 * @param name
	 * @return
	 * @throws RSCException
	 */
	public long getCountControlling(String parentID, String name) throws RSCException {
		Software findSoftwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("installStatus").is(STATUS.IN_PROCESS.getValue()));
		
		long cnt = 0;
		
		try {
			findSoftwareItem = (Software)softwareDao.findOne(query);
			
			if(!CommonUtil.isEmpty(findSoftwareItem)) {
				
				if(CommonUtil.checkElapseTime(findSoftwareItem.getLastModifiedTime(), Long.valueOf(CommonUtil.getConfig("iot.mgmtCmd.status.check.time")), "yyyy-MM-dd'T'HH:mm:ssXXX") > 0){
					this.deleteSoftwareByInstallStatus(findSoftwareItem.getResourceID(), STATUS.IN_PROCESS.getValue());
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		try {
			cnt = softwareDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "software get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}	

	/**
	 * software cmdType retrieve
	 * @param findSoftwareItem
	 * @return
	 * @throws RSCException
	 */
	public String getSoftwareCmdType(Software findSoftwareItem) throws RSCException {
		
		String cmdType = CMD_TYPE.SOFTWARE_INSTALL.getValue();
		
		if(!CommonUtil.isEmpty(findSoftwareItem)) {
			Boolean install = findSoftwareItem.isInstall();
			Boolean uninstall = findSoftwareItem.isUninstall();
			
			if(!CommonUtil.isEmpty(install) && !CommonUtil.isEmpty(uninstall)) {
				if(install.equals(BOOLEAN.FALSE.getValue()) && uninstall.equals(BOOLEAN.TRUE.getValue())) {
					cmdType = CMD_TYPE.SOFTWARE_UNINSTALL.getValue();
				}
			}
		} else {
			throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}

		return cmdType;
	}
	
	/**
	 * software install status retrieve
	 * @param resourceID
	 * @param status
	 * @return
	 * @throws RSCException
	 */
	public Software findOneSoftwareByInstallStaus(String resourceID, BigInteger status) throws RSCException {
		Software findSoftwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		query.addCriteria(Criteria.where("installStatus.status").is(status));
		
		try {
			findSoftwareItem = (Software) softwareDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		return findSoftwareItem;
	}
	
	/**
	 * software retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public Software findOneSoftware(String key, String value) throws RSCException {
		Software findSoftwareItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findSoftwareItem = (Software) softwareDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}

		return findSoftwareItem;
	}
	
	/**
	 * software retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Software findOneSoftwareByResourceName(String parentID, String resourceName) throws RSCException {
		Software findSoftwareItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findSoftwareItem = (Software) softwareDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}

		return findSoftwareItem;
	}
	
	/**
	 * software retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Software findOneSoftwareByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Software findSoftwareItem = null;
		Software findNoFilterSoftwareItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findSoftwareItem = (Software) softwareDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findSoftwareItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterSoftwareItem = findOneSoftwareResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findSoftwareItem)) findSoftwareItem = findNoFilterSoftwareItem;

		return findSoftwareItem;
	}
	
	/**
	 * software References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Software findOneSoftwareResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Software findSoftwareResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findSoftwareResourceRefItem = (Software) softwareDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		return findSoftwareResourceRefItem;
	}
	
	/**
	 * software retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Software> findSoftware(String key, String value) throws RSCException {
		List<Software> findSoftwareList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findSoftwareList = (List<Software>) softwareDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}

		return findSoftwareList;
	}
	
	/**
	 * software retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Software> findSoftware(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Software>();
		
		List<Software> findSoftwareList = null;
		List<Software> findSoftwareNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findSoftwareList = (List<Software>) softwareDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findSoftwareList);
			
			if (filterCriteria.isFilterCriteria()) findSoftwareNoFilterResourceRefList = findSoftwareResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findSoftwareNoFilterResourceRefList.size(); i++) {
				Software noFilterSoftware = findSoftwareNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findSoftwareList.size(); t++) {
					Software software = findSoftwareList.get(t);
					if (noFilterSoftware.getResourceRef().getResourceID().equals(software.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findSoftwareList.add(noFilterSoftware);
			}
		}

		return findSoftwareList;
	}
	
	/**
	 * software References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Software> findSoftwareResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Software> findSoftwareResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findSoftwareResourceRefList = (List<Software>) softwareDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		return findSoftwareResourceRefList;
	}
	
	/**
	 * software References retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Software> findSoftwareResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Software>();
		
		String includeField = "resourceRef";
		List<Software> findSoftwareResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findSoftwareResourceRefList = (List<Software>) softwareDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findSoftwareResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		return findSoftwareResourceRefList;
	}
	
	/**
	 * software retrieve
	 * @param parentID
	 * @param resourceName
	 * @param status
	 * @return
	 * @throws RSCException
	 */
	public Software findOneSoftwareByResourceName(String parentID, String resourceName, STATUS status) throws RSCException {
		Software findSoftwareItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.addCriteria(Criteria.where("installStatus.status").is(status.getValue()));
		
		try {
			findSoftwareItem = (Software)softwareDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.software.findFail.text"));
		}
		
		return findSoftwareItem;
	}
	
	/**
	 * software create
	 * @param url
	 * @param softwareProfile
	 * @param saveYn
	 * @return
	 * @throws RSCException
	 */
	public Software createSoftware(String url, Software softwareProfile, boolean saveYn) throws RSCException {
		Software softwareItem = new Software();
		
		String currentTime = CommonUtil.getNowTimestamp();
		long seqID = seqDao.move(MovType.UP, SeqType.SOFTWARE);
		
		String expirationTime = softwareProfile.getExpirationTime();

		softwareItem.setResourceType(RESOURCE_TYPE.MGMT_OBJ.getValue());
		softwareItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.SOFTWARE.getValue(), seqID));
		softwareItem.setResourceName(!CommonUtil.isEmpty(softwareProfile.getResourceName()) ? softwareProfile.getResourceName() : softwareItem.getResourceID());
		softwareItem.setParentID(softwareProfile.getParentID());
		softwareItem.setMgmtDefinition(MGMT_DEFINITION.SOFTWARE.getValue());
		softwareItem.setExpirationTime(expirationTime);
		softwareItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//softwareItem.getAccessControlPolicyIDs().addAll(softwareProfile.getAccessControlPolicyIDs());
		softwareItem.setCreationTime(currentTime);
		softwareItem.setLastModifiedTime(currentTime);
		softwareItem.getLabels().addAll(softwareProfile.getLabels());
		softwareItem.getObjectIDs().addAll(softwareProfile.getObjectIDs());
		softwareItem.getObjectPaths().addAll(softwareProfile.getObjectPaths());
		softwareItem.setDescription(softwareProfile.getDescription());
		softwareItem.setVersion(softwareProfile.getVersion());
		softwareItem.setName(softwareProfile.getName());
		softwareItem.setURL(softwareProfile.getURL());
		softwareItem.setInstall(softwareProfile.isInstall());
		softwareItem.setUninstall(softwareProfile.isUninstall());
		softwareItem.setInstallStatus(softwareProfile.getInstallStatus());
		softwareItem.setActivate(softwareProfile.isActivate());
		softwareItem.setDeactivate(softwareProfile.isDeactivate());
		softwareItem.setActiveStatus(softwareProfile.getActiveStatus());
		softwareItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, softwareItem), softwareItem.getResourceName(), RESOURCE_TYPE.MGMT_OBJ.getValue(), softwareItem.getResourceID(), MGMT_DEFINITION.SOFTWARE.getValue()));
		
		if (saveYn) {
			try {
				softwareDao.insert(softwareItem);
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(),e);
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
				throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.createFail.text"));
			}
			mongoLogService.log(logger, LEVEL.INFO, "software create success");
			
			return this.findOneSoftware("resourceID", softwareItem.getResourceID());
			
		} else {
			mongoLogService.log(logger, LEVEL.INFO, "software memory create success");
			
			return softwareItem;
		}
	}

	/**
	 * software delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteSoftware(String key, String value) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			softwareDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "software delete success");

	}
	
	/**
	 * software delete
	 * @param resourceID
	 * @param status
	 * @throws RSCException
	 */
	public void deleteSoftwareByInstallStatus(String resourceID, BigInteger status) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		query.addCriteria(Criteria.where("installStatus.status").is(status));
		
		try {
			softwareDao.remove(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "software delete success");
	}
	
	/**
	 * software update
	 * @param softwareProfile
	 * @return
	 * @throws RSCException
	 */
	public Software updateSoftware(Software softwareProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= softwareProfile.getResourceID();
		String expirationTime				= softwareProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = softwareProfile.getAccessControlPolicyIDs();
		List<String> labels 				= softwareProfile.getLabels();
		List<String> objectIDs 				= softwareProfile.getObjectIDs();
		List<String> objectPaths 			= softwareProfile.getObjectPaths();
		String description 					= softwareProfile.getDescription();
		String version 						= softwareProfile.getVersion();
		String name 						= softwareProfile.getName();
		String url 							= softwareProfile.getURL();
		Boolean install 					= softwareProfile.isInstall();
		Boolean uninstall 					= softwareProfile.isUninstall();
		Boolean activate					= softwareProfile.isActivate();
		Boolean deactivate 					= softwareProfile.isDeactivate();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(objectIDs))				update.set("objectIDs", objectIDs);
		if(!CommonUtil.isNull(objectPaths))				update.set("objectPaths", objectPaths);
		if(!CommonUtil.isNull(description))				update.set("description", description);
		if(!CommonUtil.isNull(version))					update.set("version",version);
		if(!CommonUtil.isNull(name))					update.set("name",name);
		if(!CommonUtil.isNull(url))						update.set("url", url);
		if(!CommonUtil.isNull(install))					update.set("install",install);
		if(!CommonUtil.isNull(uninstall))				update.set("uninstall",uninstall );
		if(!CommonUtil.isNull(activate))				update.set("activate", activate);
		if(!CommonUtil.isNull(deactivate))				update.set("deactivate", deactivate);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try { 
			softwareDao.update(query, update);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.software.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "software update success");
		
		return this.findOneSoftware("resourceID", resourceID);

	}
	
}
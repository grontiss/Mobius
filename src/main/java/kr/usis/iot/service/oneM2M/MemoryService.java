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
import kr.usis.iot.domain.oneM2M.Memory;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.MemoryDao;
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
 * memory Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class MemoryService {

	private static final Log logger = LogFactory.getLog(MemoryService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private MemoryDao memoryDao;
	
	@Autowired
	private NodeService nodeService;

	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * memory count retrieve
	 * @param parentID
	 * @return
	 */
	public long getCount(String parentID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		long cnt = 0;
		
		try {
			cnt = memoryDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "memory get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}

	/**
	 * memory retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Memory findOneMemoryByResourceName(String parentID, String resourceName) throws RSCException {
		Memory findMemoryItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findMemoryItem = (Memory) memoryDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}

		return findMemoryItem;
	}
	
	
	/**
	 * memory retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Memory findOneMemory(String resourceID) throws RSCException {
		Memory findMemoryItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findMemoryItem = (Memory)memoryDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}
		
		return findMemoryItem;
	}
	
	/**
	 * memory retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public Memory findOneMemory(String key, String value) throws RSCException {
		Memory findMemoryItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findMemoryItem = (Memory) memoryDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}

		return findMemoryItem;
	}
	
	/**
	 * memory retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Memory findOneMemoryByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Memory findMemoryItem = null;
		Memory findNoFilterMemoryItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findMemoryItem = (Memory) memoryDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findMemoryItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterMemoryItem = findOneMemoryResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findMemoryItem)) findMemoryItem = findNoFilterMemoryItem;

		return findMemoryItem;
	}
	
	/**
	 * memory References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Memory findOneMemoryResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Memory findMemoryResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findMemoryResourceRefItem = (Memory) memoryDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}
		
		return findMemoryResourceRefItem;
	}
	
	/**
	 * memory retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Memory> findMemory(String key, String value) throws RSCException {
		List<Memory> findMemoryList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findMemoryList = (List<Memory>) memoryDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}

		return findMemoryList;
	}
	
	
	/**
	 * memory retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Memory> findMemory(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Memory>();
		
		List<Memory> findMemoryList = null;
		List<Memory> findMemoryNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findMemoryList = (List<Memory>) memoryDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findMemoryList);
			
			if (filterCriteria.isFilterCriteria()) findMemoryNoFilterResourceRefList = findMemoryResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findMemoryNoFilterResourceRefList.size(); i++) {
				Memory noFilterMemory = findMemoryNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findMemoryList.size(); t++) {
					Memory memory = findMemoryList.get(t);
					if (noFilterMemory.getResourceRef().getResourceID().equals(memory.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findMemoryList.add(noFilterMemory);
			}
		}

		return findMemoryList;
	}
	
	/**
	 * memory References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Memory> findMemoryResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Memory> findMemoryResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findMemoryResourceRefList = (List<Memory>) memoryDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}
		
		return findMemoryResourceRefList;
	}
	
	/**
	 * memory References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Memory> findMemoryResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Memory>();
		
		String includeField = "resourceRef";
		List<Memory> findMemoryResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findMemoryResourceRefList = (List<Memory>) memoryDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findMemoryResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.findFail.text"));
		}
		
		return findMemoryResourceRefList;
	}

	/**
	 * memory create
	 * @param url
	 * @param memoryProfile
	 * @return
	 * @throws RSCException
	 */
	public Memory createMemory(String url, Memory memoryProfile) throws RSCException {
		Memory memoryItem = new Memory();

		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.MEMORY);
		
		String expirationTime = memoryProfile.getExpirationTime();
		
		memoryItem.setResourceType(RESOURCE_TYPE.MGMT_OBJ.getValue());
		memoryItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.MEMORY.getValue(), seqID));
		memoryItem.setResourceName(!CommonUtil.isEmpty(memoryProfile.getResourceName()) ? memoryProfile.getResourceName() : memoryItem.getResourceID());
		memoryItem.setParentID(memoryProfile.getParentID());
		memoryItem.setMgmtDefinition(MGMT_DEFINITION.MEMORY.getValue());
		memoryItem.setExpirationTime(expirationTime);
		memoryItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//memoryItem.getAccessControlPolicyIDs().addAll(memoryProfile.getAccessControlPolicyIDs());
		memoryItem.setCreationTime(currentTime);
		memoryItem.setLastModifiedTime(currentTime);
		memoryItem.getLabels().addAll(memoryProfile.getLabels());
		memoryItem.getObjectIDs().addAll(memoryProfile.getObjectIDs());
		memoryItem.getObjectPaths().addAll(memoryProfile.getObjectPaths());
		memoryItem.setDescription(memoryProfile.getDescription());
		memoryItem.setMemAvailable(memoryProfile.getMemAvailable());
		memoryItem.setMemTotal(memoryProfile.getMemTotal());
		memoryItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, memoryItem), memoryItem.getResourceName(), RESOURCE_TYPE.MGMT_OBJ.getValue(), memoryItem.getResourceID(), MGMT_DEFINITION.MEMORY.getValue()));
		
		try {
			memoryDao.insert(memoryItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "memory create success");

		return this.findOneMemory(memoryItem.getResourceID());
	}

	/**
	 * memory delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteMemory(String key, String value) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			memoryDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "memory delete success");

	}
	
	
	/**
	 * memory update
	 * @param memoryProfile
	 * @return
	 * @throws RSCException
	 */
	public Memory updateMemory(Memory memoryProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= memoryProfile.getResourceID();
		String expirationTime				= memoryProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = memoryProfile.getAccessControlPolicyIDs();
		List<String> labels 				= memoryProfile.getLabels();
		List<String> objectIDs 				= memoryProfile.getObjectIDs();
		List<String> objectPaths 			= memoryProfile.getObjectPaths();
		String description 					= memoryProfile.getDescription();
		BigInteger memAvailable 			= memoryProfile.getMemAvailable();
		BigInteger memTotal					= memoryProfile.getMemTotal();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(objectIDs))				update.set("objectIDs", objectIDs);
		if(!CommonUtil.isNull(objectPaths))				update.set("objectPaths", objectPaths);
		if(!CommonUtil.isNull(description))				update.set("description", description);
		if(!CommonUtil.isNull(memAvailable))			update.set("memAvailable", memAvailable);
		if(!CommonUtil.isNull(memTotal))				update.set("memTotal", memTotal);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try { 
			memoryDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.memory.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "memory update success");
		
		return this.findOneMemory(resourceID);
		
	}

}
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
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Reboot;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.RebootDao;
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
 * reboot Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class RebootService {

	private static final Log logger = LogFactory.getLog(RebootService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private RebootDao rebootDao;
	
	@Autowired
	private NodeService nodeService;	

	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * reboot count retrieve
	 * @param parentID
	 * @return
	 */
	public long getCount(String parentID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		long cnt = 0;
		
		try {
			cnt = rebootDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "reboot get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}

	/**
	 * reboot retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Reboot findOneRebootByResourceName(String parentID, String resourceName) throws RSCException {
		Reboot findRebootItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findRebootItem = (Reboot) rebootDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}

		return findRebootItem;
	}
	
	
	/**
	 * reboot retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Reboot findOneReboot(String resourceID) throws RSCException {
		Reboot findRebootItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findRebootItem = (Reboot)rebootDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}
		
		return findRebootItem;
	}
	
	/**
	 * reboot retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public Reboot findOneReboot(String key, String value) throws RSCException {
		Reboot findRebootItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findRebootItem = (Reboot) rebootDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}

		return findRebootItem;
	}
	
	/**
	 * reboot retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Reboot findOneRebootByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Reboot findRebootItem = null;
		Reboot findNoFilterRebootItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findRebootItem = (Reboot) rebootDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findRebootItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterRebootItem = findOneRebootResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findRebootItem)) findRebootItem = findNoFilterRebootItem;

		return findRebootItem;
	}
	
	/**
	 * reboot References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Reboot findOneRebootResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Reboot findRebootResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findRebootResourceRefItem = (Reboot) rebootDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}
		
		return findRebootResourceRefItem;
	}
	
	/**
	 * reboot retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Reboot> findReboot(String key, String value) throws RSCException {
		List<Reboot> findRebootList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findRebootList = (List<Reboot>) rebootDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}

		return findRebootList;
	}
	
	
	/**
	 * reboot retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Reboot> findReboot(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Reboot>();
		
		List<Reboot> findRebootList = null;
		List<Reboot> findRebootNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findRebootList = (List<Reboot>) rebootDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findRebootList);
			
			if (filterCriteria.isFilterCriteria()) findRebootNoFilterResourceRefList = findRebootResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findRebootNoFilterResourceRefList.size(); i++) {
				Reboot noFilterReboot = findRebootNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findRebootList.size(); t++) {
					Reboot reboot = findRebootList.get(t);
					if (noFilterReboot.getResourceRef().getResourceID().equals(reboot.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findRebootList.add(noFilterReboot);
			}
		}

		return findRebootList;
	}
	
	/**
	 * reboot References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Reboot> findRebootResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Reboot> findRebootResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findRebootResourceRefList = (List<Reboot>) rebootDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}
		
		return findRebootResourceRefList;
	}
	
	/**
	 * reboot References retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Reboot> findRebootResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Reboot>();
		
		String includeField = "resourceRef";
		List<Reboot> findRebootResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findRebootResourceRefList = (List<Reboot>) rebootDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findRebootResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.findFail.text"));
		}
		
		return findRebootResourceRefList;
	}

	/**
	 * reboot create
	 * @param url
	 * @param rebootProfile
	 * @return
	 * @throws RSCException
	 */
	public Reboot createReboot(String url, Reboot rebootProfile) throws RSCException {
		Reboot rebootItem = new Reboot();

		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.REBOOT);
		
		String expirationTime = rebootProfile.getExpirationTime();
		
		rebootItem.setResourceType(RESOURCE_TYPE.MGMT_OBJ.getValue());
		rebootItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.REBOOT.getValue(), seqID));
		rebootItem.setResourceName(!CommonUtil.isEmpty(rebootProfile.getResourceName()) ? rebootProfile.getResourceName() : rebootItem.getResourceID());
		rebootItem.setParentID(rebootProfile.getParentID());
		rebootItem.setMgmtDefinition(MGMT_DEFINITION.REBOOT.getValue());
		rebootItem.setExpirationTime(expirationTime);
		rebootItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//rebootItem.getAccessControlPolicyIDs().addAll(rebootProfile.getAccessControlPolicyIDs());
		rebootItem.setCreationTime(currentTime);
		rebootItem.setLastModifiedTime(currentTime);
		rebootItem.getLabels().addAll(rebootProfile.getLabels());
		rebootItem.getObjectIDs().addAll(rebootProfile.getObjectIDs());
		rebootItem.getObjectPaths().addAll(rebootProfile.getObjectPaths());
		rebootItem.setDescription(rebootProfile.getDescription());
		rebootItem.setReboot(rebootProfile.isReboot());
		rebootItem.setFactoryReset(rebootProfile.isFactoryReset());
		rebootItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, rebootItem), rebootItem.getResourceName(), RESOURCE_TYPE.MGMT_OBJ.getValue(), rebootItem.getResourceID(), MGMT_DEFINITION.REBOOT.getValue()));
		
		try {
			rebootDao.insert(rebootItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.createFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "reboot create success");

		return this.findOneReboot(rebootItem.getResourceID());
	}

	/**
	 * reboot delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteReboot(String key, String value) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			rebootDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "reboot delete success");

	}
	
	
	/**
	 * reboot update
	 * @param rebootProfile
	 * @return
	 * @throws RSCException
	 */
	public Reboot updateReboot(Reboot rebootProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= rebootProfile.getResourceID();
		String expirationTime				= rebootProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = rebootProfile.getAccessControlPolicyIDs();
		List<String> labels 				= rebootProfile.getLabels();
		List<String> objectIDs 				= rebootProfile.getObjectIDs();
		List<String> objectPaths 			= rebootProfile.getObjectPaths();
		String description 					= rebootProfile.getDescription();
		Boolean reboot 						= rebootProfile.isReboot();
		Boolean factoryReset				= rebootProfile.isFactoryReset();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(objectIDs))				update.set("objectIDs", objectIDs);
		if(!CommonUtil.isNull(objectPaths))				update.set("objectPaths", objectPaths);
		if(!CommonUtil.isNull(description))				update.set("description", description);
		if(!CommonUtil.isNull(reboot))					update.set("reboot", reboot);
		if(!CommonUtil.isNull(factoryReset))			update.set("factoryReset", factoryReset);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		try { 
			rebootDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.reboot.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "reboot update success");
		
		return this.findOneReboot(resourceID);
		
	}

}
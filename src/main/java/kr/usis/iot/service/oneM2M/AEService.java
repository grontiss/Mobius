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
import kr.usis.iot.domain.oneM2M.AE;
import kr.usis.iot.domain.oneM2M.AccessControlPolicy;
import kr.usis.iot.domain.oneM2M.AccessControlRule;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.SetOfAcrs;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.AEDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.KEY_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * AE Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class AEService {

	private static final Log logger = LogFactory.getLog(AEService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private GroupService groupService;

	@Autowired
	private AEDao aEDao;
	
	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * AE count retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public long getCount(String resourceID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = aEDao.count(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		return cnt;
	}

	/**
	 * AE retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public AE findOneAE(String resourceID) throws RSCException {
		AE findAEItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {
			findAEItem = (AE) aEDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}

		return findAEItem;
	}
	
	/**
	 * AE retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public AE findOneAE(String key, String value) throws RSCException {
		AE findAEItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findAEItem = (AE) aEDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}

		return findAEItem;
	}
	
	/**
	 * AE retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<AE> findAE(String key, String value) throws RSCException {
		List<AE> findAEList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findAEList = (List<AE>) aEDao.find(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}

		return findAEList;
	}
	
	/**
	 * AE retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<AE> findAE(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<AE>();
		
		List<AE> findAEList = null;
		List<AE> findAENoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findAEList = (List<AE>) aEDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findAEList);
			
			if (filterCriteria.isFilterCriteria()) findAENoFilterResourceRefList = findAEResourceRef(key, value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findAENoFilterResourceRefList.size(); i++) {
				AE noFilterAE = findAENoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findAEList.size(); t++) {
					AE ae = findAEList.get(t);
					if (noFilterAE.getResourceRef().getResourceID().equals(ae.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findAEList.add(noFilterAE);
			}
		}

		return findAEList;
	}
	
	/**
	 * AE References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<AE> findAEResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<AE> findAEResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findAEResourceRefList = (List<AE>) aEDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		return findAEResourceRefList;
	}
	
	/**
	 * AE References retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<AE> findAEResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<AE>();
		
		String includeField = "resourceRef";
		List<AE> findAEResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findAEResourceRefList = (List<AE>) aEDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findAEResourceRefList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		return findAEResourceRefList;
	}
	
	/**
	 * AE retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public AE findOneAEByResourceName(String parentID, String resourceName) throws RSCException {
		AE findAEItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findAEItem = (AE) aEDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		return findAEItem;
	}
	
	/**
	 * AE retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public AE findOneAEByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		AE findAEItem = null;
		AE findNoFilterAEItem = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findAEItem = (AE) aEDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findAEItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterAEItem = findOneAEResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findAEItem)) findAEItem = findNoFilterAEItem;
		
		return findAEItem;
	}
	
	/**
	 * AE References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public AE findOneAEResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		AE findAEResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findAEResourceRefItem = (AE) aEDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.findFail.text"));
		}
		
		return findAEResourceRefItem;
	}
	
	/**
	 * AE create
	 * @param url
	 * @param aeProfile
	 * @param aKey
	 * @return
	 * @throws RSCException
	 */
	public AE createAE(String url, AE aeProfile, boolean checkMccP) throws RSCException {
		
		Long seqID = seqDao.move(MovType.UP, SeqType.AE);
		String aeResourceID = CommonUtil.seqIDToResourceID(SEQ_PREFIX.AE.getValue(), seqID);
		String aeID = CommonUtil.seqIDToResourceID(SEQ_PREFIX.AEID.getValue(), seqID);
		String aKey = mCommonService.createAKey(checkMccP, aeID);
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		String expirationTime = aeProfile.getExpirationTime();
		
		AccessControlPolicy accessControlPolicyItem = null;
		if (!CommonUtil.isEmpty(aKey)) {
			
			accessControlPolicyItem = new AccessControlPolicy();
			
			SetOfAcrs privileges = new SetOfAcrs();
			AccessControlRule accessControlRule = new AccessControlRule();
			accessControlRule.getAccessControlOriginators().add(aKey);
			privileges.getAccessControlRule().add(accessControlRule);
			
			accessControlPolicyItem.setParentID(aeResourceID);
			accessControlPolicyItem.setExpirationTime(expirationTime);
			accessControlPolicyItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
			accessControlPolicyItem.setKeyType(KEY_TYPE.AKEY.getName());
			accessControlPolicyItem.setPrivileges(privileges);
			accessControlPolicyItem.setSelfPrivileges(privileges);
			
			try {
				accessControlPolicyItem = accessControlPolicyService.createAccessControlPolicy(url, accessControlPolicyItem);
				
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
				throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.accessControlPolicy.regFail.text"));
			}
		}
		
		AE aeItem = new AE();
		
		aeItem.setResourceType(RESOURCE_TYPE.AE.getValue());
		aeItem.setResourceID(aeResourceID);
		aeItem.setResourceName(!CommonUtil.isEmpty(aeProfile.getResourceName()) ? aeProfile.getResourceName() : aeItem.getResourceID());
		aeItem.setParentID(aeProfile.getParentID());
		aeItem.setExpirationTime(expirationTime);
		aeItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		if (!CommonUtil.isEmpty(accessControlPolicyItem)) aeItem.getAccessControlPolicyIDs().add(accessControlPolicyItem.getResourceID());
		aeItem.setCreationTime(currentTime);
		aeItem.setLastModifiedTime(currentTime);
		aeItem.getLabels().addAll(aeProfile.getLabels());
		aeItem.getAnnounceTo().addAll(aeProfile.getAnnounceTo());
		aeItem.getAnnouncedAttribute().addAll(aeProfile.getAnnouncedAttribute());
		aeItem.setAppName(aeProfile.getAppName());
		aeItem.setAppID(aeProfile.getAppID());
		aeItem.setAEID(aeID);
		aeItem.getPointOfAccess().addAll(aeProfile.getPointOfAccess());
		aeItem.setOntologyRef(aeProfile.getOntologyRef());
		aeItem.setNodeLink(aeProfile.getNodeLink());
		aeItem.setPassCode(aeProfile.getPassCode());
		aeItem.setAKey(aKey);
		aeItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, aeItem), aeItem.getResourceName(), RESOURCE_TYPE.AE.getValue(), aeItem.getResourceID(), null));
		
		try {
			aEDao.insert(aeItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "AE create success");

		return this.findOneAE("resourceID", aeItem.getResourceID());
	}
	
	/**
	 * AE delete
	 * @param key
	 * @param value
	 * @throws RSCException
	 */
	public void deleteAE(String key, String value) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		try {

			aEDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "AE delete success");

	}
	
	/**
	 * AE delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteAE(String resourceID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			
			aEDao.remove(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.AE.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "AE delete success");
		
	}
	
	/**
	 * AE delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteAEChild(String resourceID) throws RSCException {
		
		try {
			accessControlPolicyService.deleteAccessControlPolicyByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "AE inner accessControlPolicy remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.delFail.text"));
		}
		
		try {
			containerService.deleteContainerByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "AE inner container remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.AE.container.delFail.text"));
		}
		
		try {
			groupService.deleteGroupByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "AE inner Group remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.AE.Group.delFail.text"));
		}
		
		this.deleteAE(resourceID);
	}
	
	/**
	 * AE delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteAEByParentID(String parentID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			List<AE> findAEList = this.findAE("parentID", parentID);
			for(AE aeItem : findAEList) {
				this.deleteAEChild(aeItem.getResourceID());
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.AE.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "AE delete success");
		
	}
	
	/**
	 * aKey update
	 * @param aeItem
	 * @return
	 * @throws RSCException
	 */
	public AE updateAEAkey(AE aeItem) throws RSCException {
		String resourceID = aeItem.getResourceID();
		String currentTime = CommonUtil.getNowTimestamp();

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		Update update = new Update();
		update.set("lastModifiedTime", currentTime);
		update.set("aKey", aeItem.getAKey());
		
		try {
			aEDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.AE.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "AE update success");
		
		return this.findOneAE(resourceID);

	}
	
	/**
	 * AE update
	 * @param aeProfile
	 * @return
	 * @throws RSCException
	 */
	public AE updateAE(AE aeProfile) throws RSCException {
		
		String currentTime					= CommonUtil.getNowTimestamp();
		
		String resourceID					= aeProfile.getResourceID();
		String expirationTime				= aeProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = aeProfile.getAccessControlPolicyIDs();
		List<String> labels 				= aeProfile.getLabels();
		List<String> announceTo 			= aeProfile.getAnnounceTo();
		List<String> announcedAttribute		= aeProfile.getAnnouncedAttribute();
		String appName						= aeProfile.getAppName();
		List<String> pointOfAccess			= aeProfile.getPointOfAccess();
		String ontologyRef					= aeProfile.getOntologyRef();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(announceTo))				update.set("announceTo", announceTo);
		if(!CommonUtil.isNull(announcedAttribute))		update.set("announcedAttribute", announcedAttribute);
		if(!CommonUtil.isNull(appName))					update.set("appName", appName);
		if(!CommonUtil.isNull(pointOfAccess))			update.set("pointOfAccess", pointOfAccess);
		if(!CommonUtil.isNull(ontologyRef))				update.set("ontologyRef", ontologyRef);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			aEDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.AE.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "AE update success");
		
		return this.findOneAE(resourceID);
		
	}

}
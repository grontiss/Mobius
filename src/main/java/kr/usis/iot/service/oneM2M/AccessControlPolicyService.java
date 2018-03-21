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

import java.util.List;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.AccessControlPolicy;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.AccessControlPolicyDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.KEY_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import biz.source_code.base64Coder.Base64Coder;

/**
 * accessControlPolicy Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class AccessControlPolicyService {

	private static final Log logger = LogFactory.getLog(AccessControlPolicyService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private AccessControlPolicyDao accessControlPolicyDao;
	
	@Autowired
	private SequenceDao seqDao;	
	
	/**
	 * accessControlPolicy Retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public AccessControlPolicy findOneAccessControlPolicy(String resourceID) throws RSCException {

		AccessControlPolicy findAccessControlPolicyItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {

			findAccessControlPolicyItem = (AccessControlPolicy) accessControlPolicyDao.findOne(query);

		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.findFail.text"));
		}

		return findAccessControlPolicyItem;
	}
	
	/**
	 * ++ USIS 2016-07-11
	 * accessControlPolicy Retrieve
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public AccessControlPolicy findOneAccessControlPolicyByResourceName(String resourceName) throws RSCException {

		AccessControlPolicy findAccessControlPolicyItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {

			findAccessControlPolicyItem = (AccessControlPolicy) accessControlPolicyDao.findOne(query);

		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.findFail.text"));
		}

		return findAccessControlPolicyItem;
	}
	
	/**
	 * accessControlPolicy List Retrieve
	 * @param resourceIDs
	 * @return
	 * @throws RSCException
	 */
	public List<AccessControlPolicy> findAccessControlPolicyList(List<String> resourceIDs) throws RSCException {

		List<AccessControlPolicy> findAccessControlPolicyList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").in(resourceIDs));

		try {

			findAccessControlPolicyList = (List<AccessControlPolicy>)accessControlPolicyDao.find(query);

		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.findFail.text"));
		}

		return findAccessControlPolicyList;
	}
	
	/**
	 * accessControlPolicy List Retrieve
	 * @param resourceIDs
	 * @return
	 * @throws RSCException
	 */
	public List<AccessControlPolicy> findAccessControlPolicyListByParentID(String parentID) throws RSCException {

		List<AccessControlPolicy> findAccessControlPolicyList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));

		try {

			findAccessControlPolicyList = (List<AccessControlPolicy>)accessControlPolicyDao.find(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.findFail.text"));
		}

		return findAccessControlPolicyList;
	}
	
	/**
	 * accessControlPolicy Retrieve
	 * @param originators
	 * @return
	 * @throws RSCException
	 */
	public AccessControlPolicy findOneAccessControlPolicyByOriginators(String originators) throws RSCException {
		
		AccessControlPolicy findOneAccessControlPolicyItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("selfPrivileges.accessControlRule.0.accessControlOriginators").is(originators));
		
		try {
			
			List<AccessControlPolicy> findAccessControlPolicyList = (List<AccessControlPolicy>) accessControlPolicyDao.find(query);
			for (int i=0; i<findAccessControlPolicyList.size(); i++) {
				AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.findFail.text"));
		}
		
		return findOneAccessControlPolicyItem;
	}
	
	/**
	 * accessControlPolicy count Retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCount(String resourceID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = accessControlPolicyDao.count(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "accessControlPolicy get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * accessControlPolicy Create
	 * @param accessControlPolicyVoProfile
	 * @return
	 * @throws RSCException
	 */
	public AccessControlPolicy createAccessControlPolicy(String url, AccessControlPolicy accessControlPolicyProfile) throws RSCException {
		
		AccessControlPolicy accessControlPolicyItem = new AccessControlPolicy();
		
		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.ACCESS_CONTROL_POLICY);
		
		String expirationTime = accessControlPolicyProfile.getExpirationTime();
		
		accessControlPolicyItem.setResourceType(RESOURCE_TYPE.ACCESS_CONTROL_POLICY.getValue());
		accessControlPolicyItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.ACCESS_CONTROL_POLICY.getValue(), seqID));
		accessControlPolicyItem.setResourceName(!CommonUtil.isEmpty(accessControlPolicyProfile.getResourceName()) ? accessControlPolicyProfile.getResourceName() : accessControlPolicyItem.getResourceID());
		accessControlPolicyItem.setParentID(accessControlPolicyProfile.getParentID());
		accessControlPolicyItem.setExpirationTime(expirationTime);
		accessControlPolicyItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		accessControlPolicyItem.setKeyType(accessControlPolicyProfile.getKeyType());
		accessControlPolicyItem.getLabels().addAll(accessControlPolicyProfile.getLabels());
		accessControlPolicyItem.setCreationTime(currentTime);
		accessControlPolicyItem.setLastModifiedTime(currentTime);
		accessControlPolicyItem.getAnnounceTo().addAll(accessControlPolicyProfile.getAnnounceTo());
		accessControlPolicyItem.getAnnouncedAttribute().addAll(accessControlPolicyProfile.getAnnouncedAttribute());
		accessControlPolicyItem.setPrivileges(accessControlPolicyProfile.getPrivileges());
		accessControlPolicyItem.setSelfPrivileges(accessControlPolicyProfile.getSelfPrivileges());
		accessControlPolicyItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, accessControlPolicyItem), accessControlPolicyItem.getResourceName(), CommonCode.RESOURCE_TYPE.ACCESS_CONTROL_POLICY.getValue(), accessControlPolicyItem.getResourceID(), null));
		
		mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		mongoLogService.log(logger, LEVEL.DEBUG, "[ accessControlPolicyVO data ]");
		mongoLogService.log(logger, LEVEL.DEBUG, accessControlPolicyProfile.toString());
		mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");

		try {
			accessControlPolicyDao.insert(accessControlPolicyItem);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "createAccessControlPolicy Exception : " + e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.accessControlPolicy.regFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "accessControlPolicy create success");

		return this.findOneAccessControlPolicy(accessControlPolicyItem.getResourceID());
	}
	
	/**
	 * accessControlPolicy dKey Update
	 * @param accesscontrolPolicyIDs
	 * @param newDKey
	 * @return
	 * @throws RSCException
	 */
	public AccessControlPolicy updateAccessControlPolicyByDKey(List<String> accesscontrolPolicyIDs, String newDKey) throws RSCException {
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		List<AccessControlPolicy> findAccessControlPolicyList = this.findAccessControlPolicyList(accesscontrolPolicyIDs);
		for (int i=0; i<findAccessControlPolicyList.size(); i++) {
			AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
			
			try {
				
				if (KEY_TYPE.DKEY.getName().equals(accessControlPolicy.getKeyType())) {
				
					List<String> accessControlOriginatorList = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators();
					for (int t=0; t<accessControlOriginatorList.size(); t++) {
						
						Update update = new Update();
						if(!CommonUtil.isNull(newDKey))			update.set("privileges.accessControlRule.0.accessControlOriginators.0", newDKey);
						if(!CommonUtil.isNull(newDKey))			update.set("selfPrivileges.accessControlRule.0.accessControlOriginators.0", newDKey);
																update.set("lastModifiedTime", currentTime);
																
						Query query = new Query();
						query.addCriteria(Criteria.where("resourceID").is(accessControlPolicy.getResourceID()));
						
						accessControlPolicyDao.update(query, update);
						
						mongoLogService.log(logger, LEVEL.DEBUG, "accessControl update success");
						
						return this.findOneAccessControlPolicy(accessControlPolicy.getResourceID());
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				mongoLogService.log(logger, LEVEL.ERROR, "accessControl update Exception");
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
				throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.upFail.text"));
			}
		}
		
		return null;
	}
	
	/**
	 * accessControlPolicy aKey Update
	 * @param accesscontrolPolicyIDs
	 * @param newDKey
	 * @return
	 * @throws RSCException
	 */
	public AccessControlPolicy updateAccessControlPolicyByAKey(List<String> accesscontrolPolicyIDs, String newAKey) throws RSCException {
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		List<AccessControlPolicy> findAccessControlPolicyList = this.findAccessControlPolicyList(accesscontrolPolicyIDs);
		for (int i=0; i<findAccessControlPolicyList.size(); i++) {
			AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
			
			try {
				
				if (KEY_TYPE.AKEY.getName().equals(accessControlPolicy.getKeyType())) {
				
					List<String> accessControlOriginatorList = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators();
					for (int t=0; t<accessControlOriginatorList.size(); t++) {
							
						Update update = new Update();
						if(!CommonUtil.isNull(newAKey))			update.set("privileges.accessControlRule.0.accessControlOriginators.0", newAKey);
						if(!CommonUtil.isNull(newAKey))			update.set("selfPrivileges.accessControlRule.0.accessControlOriginators.0", newAKey);
																update.set("lastModifiedTime", currentTime);
																
						Query query = new Query();
						query.addCriteria(Criteria.where("resourceID").is(accessControlPolicy.getResourceID()));
						
						accessControlPolicyDao.update(query, update);
						
						mongoLogService.log(logger, LEVEL.DEBUG, "accessControl update success");
						
						return this.findOneAccessControlPolicy(accessControlPolicy.getResourceID());
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				mongoLogService.log(logger, LEVEL.ERROR, "accessControl update Exception");
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
				throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.upFail.text"));
			}
		}
		
		return null;
	}
	
	/**
	 * verify dKey accessControlPolicy
	 * @param checkMccP
	 * @param accessControlPolicyIDs
	 * @param dKey
	 * @throws RSCException
	 */
	public void verifyAccessControlPolicyByDKey(boolean checkMccP, List<String> accessControlPolicyIDs, String dKey) throws RSCException {
		if (checkMccP) return;
		
		boolean verify = false;
		
		if(CommonUtil.isEmpty(dKey)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "dKey " + CommonUtil.getMessage("msg.input.empty.text"));
			
		}
		
		try{
			mongoLogService.log(logger, LEVEL.DEBUG, "dKey decode before	: " + dKey);
			dKey = Base64Coder.decodeString(dKey);
			mongoLogService.log(logger, LEVEL.DEBUG, "dKey decode after		: " + dKey);
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "dKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
		}		
		
		List<AccessControlPolicy> findAccessControlPolicyList = this.findAccessControlPolicyList(accessControlPolicyIDs);
		
		if (CommonUtil.isEmpty(findAccessControlPolicyList)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.dKey.verifyFail.text"));
		}
		
		for (int i=0; i<findAccessControlPolicyList.size(); i++) {
			AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
			try {
				String keyType = accessControlPolicy.getKeyType();
				String findDKey = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators().get(0);
				if (KEY_TYPE.DKEY.getName().equals(keyType) && dKey.equals(findDKey)) verify = true;
			} catch (Exception e) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.dKey.verifyFail.text"));				
			}
		}
		
		if (!verify) throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.dKey.verifyFail.text"));
		
	}
	
	/**
	 * verify aKey accessControlPolicy
	 * @param checkMccP
	 * @param accessControlPolicyIDs
	 * @param aKey
	 * @throws RSCException
	 */
	public void verifyAccessControlPolicyByAKey(boolean checkMccP, List<String> accessControlPolicyIDs, String aKey) throws RSCException {
		if (checkMccP) return;
		
		boolean verify = false;
		
		if(CommonUtil.isEmpty(aKey)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "aKey " + CommonUtil.getMessage("msg.input.empty.text"));
			
		}
		
		try{
			mongoLogService.log(logger, LEVEL.DEBUG, "aKey decode before	: " + aKey);
			aKey = Base64Coder.decodeString(aKey);
			mongoLogService.log(logger, LEVEL.DEBUG, "aKey decode after		: " + aKey);
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "aKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
		}		
		
		List<AccessControlPolicy> findAccessControlPolicyList = this.findAccessControlPolicyList(accessControlPolicyIDs);
		
		if (CommonUtil.isEmpty(findAccessControlPolicyList)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.aKey.verifyFail.text"));
		}
		
		for (int i=0; i<findAccessControlPolicyList.size(); i++) {
			AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
			try {
				String keyType = accessControlPolicy.getKeyType();
				String findAKey = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators().get(0);
				if (KEY_TYPE.AKEY.getName().equals(keyType) && aKey.equals(findAKey)) verify = true;
			} catch (Exception e) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.aKey.verifyFail.text"));				
			}
		}
		
		if (!verify) throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.aKey.verifyFail.text"));
		
	}
	
	/**
	 * verify uKey accessControlPolicy
	 * @param checkMccP
	 * @param accessControlPolicyIDs
	 * @param uKey
	 * @throws RSCException
	 */
	public void verifyAccessControlPolicyByUKey(boolean checkMccP, List<String> accessControlPolicyIDs, String uKey) throws RSCException {
		if (checkMccP) return;
		
		boolean verify = false;
		
		if(CommonUtil.isEmpty(uKey)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "uKey " + CommonUtil.getMessage("msg.input.empty.text"));
			
		}
		
		try{
			mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode before	: " + uKey);
			uKey = Base64Coder.decodeString(uKey);
			mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode after		: " + uKey);
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "uKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
		}
		
		List<AccessControlPolicy> findAccessControlPolicyList = this.findAccessControlPolicyList(accessControlPolicyIDs);
		
		if (CommonUtil.isEmpty(findAccessControlPolicyList)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));
		}
		
		for (int i=0; i<findAccessControlPolicyList.size(); i++) {
			AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
			try {
				String keyType = accessControlPolicy.getKeyType();
				String findUKey = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators().get(0);
				if (KEY_TYPE.UKEY.getName().equals(keyType) && uKey.equals(findUKey)) verify = true;
			} catch (Exception e) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));				
			}
		}
		
		if (!verify) throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));
		
	}
	
	/**
	 * accessControlPolicy List delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteAccessControlPolicyByParentID(String parentID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			List<AccessControlPolicy> findAccessControlPolicyList = this.findAccessControlPolicyListByParentID(parentID);
			for(AccessControlPolicy accessControlPolicyItem : findAccessControlPolicyList) {
				this.deleteAccessControlPolicy(accessControlPolicyItem.getResourceID());
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd delete success");
		
	}
	
	/**
	 * accessControlPolicy delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteAccessControlPolicy(String resourceID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			accessControlPolicyDao.remove(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.accessControlPolicy.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "accessControlPolicy delete success");

	}
	
	/**
	 * 입력된 accessControlPolicyIDs 가 정상적인지 검증
	 * @param accessControlPolicyIDs
	 * @throws RSCException
	 */
	public void validateAccessControlPolicyIDs(List<String> accessControlPolicyIDs) throws RSCException {
		
		if (CommonUtil.isEmpty(accessControlPolicyIDs) || accessControlPolicyIDs.size() <= 0) return;
		
		int resourceIDLength = 22;
		
		for (String accessControlPolicyID : accessControlPolicyIDs) {
			if (this.getCount(accessControlPolicyID) <= 0) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.accessControlPolicy.noRegi.text"));
			} else if (accessControlPolicyID.length() != resourceIDLength) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.accessControlPolicy.idPrefix.notMattch.text"));
			} else if (!accessControlPolicyID.startsWith(SEQ_PREFIX.ACCESS_CONTROL_POLICY.getValue())) {
				throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.accessControlPolicy.idPrefix.notMattch.text"));
			}
		}
	}
}
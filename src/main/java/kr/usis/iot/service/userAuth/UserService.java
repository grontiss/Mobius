/**
 * Copyright (c) 2015, Youngsu Lee < yslee@ntels.com > < goraehimjul@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.service.userAuth;

import java.util.ArrayList;
import java.util.List;

import kr.usis.iot.domain.common.CertKeyVO;
import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.AE;
import kr.usis.iot.domain.oneM2M.AccessControlPolicy;
import kr.usis.iot.domain.oneM2M.AccessControlRule;
import kr.usis.iot.domain.oneM2M.ChildResourceRef;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.Group;
import kr.usis.iot.domain.oneM2M.LocationPolicy;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.SetOfAcrs;
import kr.usis.iot.mdao.oneM2M.AEDao;
import kr.usis.iot.mdao.oneM2M.AccessControlPolicyDao;
import kr.usis.iot.mdao.oneM2M.GroupDao;
import kr.usis.iot.mdao.oneM2M.NodeDao;
import kr.usis.iot.mdao.oneM2M.RemoteCSEDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.GroupService;
import kr.usis.iot.service.oneM2M.LocationPolicyService;
import kr.usis.iot.service.oneM2M.NodeService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.util.CertUtil;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.KEY_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.ntels.nisf.util.PropertiesUtil;

import biz.source_code.base64Coder.Base64Coder;

/**
 * accessControlPolicy Service.
 * @author <ul>
 *         <li>Youngsu Lee < yslee@ntels.com > < goraehimjul@gmail.com ></li>
 *         </ul>
 */
@Service
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private AEService aEService;
	
	@Autowired
	private LocationPolicyService locationPolicyService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private NodeService nodeService;
	
	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private AccessControlPolicyDao accessControlPolicyDao;
	
	@Autowired
	private RemoteCSEDao remoteCSEDao;
	
	@Autowired
	private AEDao aEDao;
	
	@Autowired
	private NodeDao nodeDao;
	
	@Autowired
	private GroupDao groupDao;
	
	@Autowired
	private CertUtil certUtil;
	

	
	/**
	 * accessControlRule Create
	 * @param accessControlPolicyVoProfile
	 * @return AccessControlPolicy
	 * @throws RSCException
	 */
	public AccessControlPolicy createUserAccessControlPolicy(String url, String uKey, AccessControlPolicy accessControlPolicyProfile) throws RSCException {

		SetOfAcrs privileges = new SetOfAcrs();
		AccessControlRule accessControlRule = new AccessControlRule();
		accessControlRule.getAccessControlOriginators().add(uKey);
		privileges.getAccessControlRule().add(accessControlRule);
		
		accessControlPolicyProfile.setKeyType(KEY_TYPE.UKEY.getName());
		accessControlPolicyProfile.setPrivileges(privileges);
		accessControlPolicyProfile.setSelfPrivileges(privileges);
		
		try {
			return accessControlPolicyService.createAccessControlPolicy(url, accessControlPolicyProfile);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.accessControlPolicy.regFail.text"));
		}
	}
	
	/**
	 * create uKey
	 * @param checkMccP
	 * @param userName
	 * @return
	 * @throws RSCException
	 */
	public String createUKey(boolean checkMccP, String userName) throws RSCException {
		if (checkMccP) return null;
		
		CertKeyVO certKeyVO = null;
		String uKey = "";
		
		certKeyVO	= certUtil.createUserKey(userName);
		uKey		= certKeyVO.getCert_client_id();
		
		if(!"200".equals(certKeyVO.getResult_code())) {
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, certKeyVO.getResult_msg());
		}
		else if(CommonUtil.isEmpty(uKey)) {
			throw new RSCException(RSC.BAD_REQUEST, "[create uKey Exception] " + certKeyVO.getResult_msg());
		}
		
		return uKey;
	}
	
	/**
	 * accessControlPolicy uKey Update
	 * @param accessControlPolicyVoProfile
	 * @param newUKey
	 * @return AccessControlPolicy
	 * @throws RSCException
	 */
	public AccessControlPolicy updateAccessControlPolicyByUKey(AccessControlPolicy accessControlPolicyProfile, String newUKey) throws RSCException {
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		try {
			
			if (KEY_TYPE.UKEY.getName().equals(accessControlPolicyProfile.getKeyType())) {
			
				List<String> accessControlOriginatorList = accessControlPolicyProfile.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators();
				for (int i=0; i<accessControlOriginatorList.size(); i++) {
						
					Update update = new Update();
					
					if(!CommonUtil.isNull(newUKey))
						update.set("privileges.accessControlRule."+i+".accessControlOriginators."+i, newUKey);
					
					if(!CommonUtil.isNull(newUKey))
						update.set("selfPrivileges.accessControlRule."+i+".accessControlOriginators."+i, newUKey);
					
					update.set("lastModifiedTime", currentTime);
															
					Query query = new Query();
					query.addCriteria(Criteria.where("resourceID").is(accessControlPolicyProfile.getResourceID()));
					
					accessControlPolicyDao.update(query, update);
					
					mongoLogService.log(logger, LEVEL.DEBUG, "accessControl uKey update success");
					
					return accessControlPolicyService.findOneAccessControlPolicy(accessControlPolicyProfile.getResourceID());
				}
			}
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "accessControl uKey update Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.upFail.text"));
		}
		
		return null;
	}
	
	/**
	 * user accessControlPolicy delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteUserAccessControlPolicy(AccessControlPolicy accessControlPolicyProfile) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(accessControlPolicyProfile.getResourceID()));
		String acpID = accessControlPolicyProfile.getResourceID();
		
		try {
			accessControlPolicyDao.remove(query);
			mongoLogService.log(logger, LEVEL.DEBUG, "accesControlPolicy delete success");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.accessControlPolicy.delFail.text"));
		}
		
		// remoteCSE
		try {
			List<RemoteCSE> findRemoteCSEList = remoteCSEService.findRemoteCSE("parentID", accessControlPolicyProfile.getParentID());
			for (int i=0; i<findRemoteCSEList.size(); i++) {
				RemoteCSE remoteCSEProfile = findRemoteCSEList.get(i);
				
				if (remoteCSEProfile.getAccessControlPolicyIDs().contains(acpID)) {
					remoteCSEProfile.getAccessControlPolicyIDs().remove(acpID);
					String currentTime = CommonUtil.getNowTimestamp();
					
					Update update = new Update();
					update.set("accessControlPolicyIDs", remoteCSEProfile.getAccessControlPolicyIDs());
					update.set("lastModifiedTime", currentTime);
																
					query = new Query();
					query.addCriteria(Criteria.where("resourceID").is(remoteCSEProfile.getResourceID()));
						
					remoteCSEDao.update(query, update);
						
					mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE accesControlPolicyIDs update success");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			mongoLogService.log(logger, LEVEL.DEBUG, CommonUtil.getMessage("msg.remoteCSE.acpIDs.upFail.text"));
		}
		
		// node
		try {
			List<Node> findNodeList = nodeService.findNode("parentID", accessControlPolicyProfile.getParentID());
			for (int i=0; i<findNodeList.size(); i++) {
				Node nodeProfile = findNodeList.get(i);
				
				if (nodeProfile.getAccessControlPolicyIDs().contains(acpID)) {
					nodeProfile.getAccessControlPolicyIDs().remove(acpID);
					String currentTime = CommonUtil.getNowTimestamp();
					
					Update update = new Update();
					update.set("accessControlPolicyIDs", nodeProfile.getAccessControlPolicyIDs());
					update.set("lastModifiedTime", currentTime);
																
					query = new Query();
					query.addCriteria(Criteria.where("resourceID").is(nodeProfile.getResourceID()));
						
					nodeDao.update(query, update);
						
					mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE accesControlPolicyIDs update success");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			mongoLogService.log(logger, LEVEL.DEBUG, CommonUtil.getMessage("msg.node.acpIDs.upFail.text"));
		}
		
		// AE
		try {
			List<AE> findAEList = aEService.findAE("parentID", accessControlPolicyProfile.getParentID());
			for (int i=0; i<findAEList.size(); i++) {
				AE aeProfile = findAEList.get(i);
				
				if (aeProfile.getAccessControlPolicyIDs().contains(acpID)) {
					aeProfile.getAccessControlPolicyIDs().remove(acpID);
					String currentTime = CommonUtil.getNowTimestamp();
					
					Update update = new Update();
					update.set("accessControlPolicyIDs", aeProfile.getAccessControlPolicyIDs());
					update.set("lastModifiedTime", currentTime);
																
					query = new Query();
					query.addCriteria(Criteria.where("resourceID").is(aeProfile.getResourceID()));
						
					aEDao.update(query, update);
						
					mongoLogService.log(logger, LEVEL.DEBUG, "AE accesControlPolicyIDs update success");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			mongoLogService.log(logger, LEVEL.DEBUG, CommonUtil.getMessage("msg.AE.acpIDs.upFail.text"));
		}
		
		// Group
		try {
			List<Group> findGroupList = groupService.findGroup("parentID", accessControlPolicyProfile.getParentID());
			for (int i=0; i<findGroupList.size(); i++) {
				Group groupProfile = findGroupList.get(i);
				
				if (groupProfile.getAccessControlPolicyIDs().contains(acpID)) {
					groupProfile.getAccessControlPolicyIDs().remove(acpID);
					String currentTime = CommonUtil.getNowTimestamp();
					
					Update update = new Update();
					update.set("accessControlPolicyIDs", groupProfile.getAccessControlPolicyIDs());
					update.set("lastModifiedTime", currentTime);
																
					query = new Query();
					query.addCriteria(Criteria.where("resourceID").is(groupProfile.getResourceID()));
						
					groupDao.update(query, update);
						
					mongoLogService.log(logger, LEVEL.DEBUG, "group accesControlPolicyIDs update success");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			mongoLogService.log(logger, LEVEL.DEBUG, CommonUtil.getMessage("msg.group.acpIDs.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "user accessControlPolicy delete success with relations(remoteCSE/node/AE/group/locationPolicy");
	}
	
	/**
	 * verify uKey accessControlPolicy
	 * @param checkMccP
	 * @param accessControlPolicyIDs
	 * @param uKey
	 * @return 
	 * @throws RSCException
	 */
	public void verifyAccessControlPolicyByUKey(boolean checkMccP, AccessControlPolicy accessControlPolicyProfile, String uKey) throws RSCException {
		if (checkMccP) return;
		
		boolean verify = false;
		
		if (CommonUtil.isEmpty(uKey)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "uKey " + CommonUtil.getMessage("msg.input.empty.text"));
		}
		
		try {
			mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode before	: " + uKey);
			uKey = Base64Coder.decodeString(uKey);
			mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode after		: " + uKey);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "uKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
		}
		
		try {
			if (KEY_TYPE.UKEY.getName().equals(accessControlPolicyProfile.getKeyType())) {
			
				List<String> accessControlOriginatorList = accessControlPolicyProfile.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators();
				for (int i=0; i<accessControlOriginatorList.size(); i++) {
					String findUKey = accessControlOriginatorList.get(i);
					if (uKey.equals(findUKey)) verify = true;
				}
			}
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "accessControl uKey verification Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.system.error.text"));
		}

		if (!verify) throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));
	}
	
	/**
	 * authenticate User
	 * @param checkMccP
	 * @param accessControlPolicyIDs
	 * @param uKey
	 * @throws RSCException
	 */
	public void authUser(boolean checkMccP, List<String> accessControlPolicyIDs, String uKey) throws RSCException {
		if (checkMccP) return;
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) return;
		
		boolean verify = false;
		boolean isUKey = false;
		
		if(CommonUtil.isEmpty(uKey)) {
			mongoLogService.log(logger, LEVEL.DEBUG, "uKey " + CommonUtil.getMessage("msg.input.empty.text"));
		} else {
			try{
				mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode before	: " + uKey);
				uKey = Base64Coder.decodeString(uKey);
				mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode after		: " + uKey);
			}catch(Exception e){
				e.printStackTrace();
				logger.error(e.getMessage(),e);
				throw new RSCException(CommonCode.RSC.BAD_REQUEST, "uKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
			}		
		}
		
		List<AccessControlPolicy> findAccessControlPolicyList = accessControlPolicyService.findAccessControlPolicyList(accessControlPolicyIDs);
		
		if (CommonUtil.isEmpty(findAccessControlPolicyList)) {
			mongoLogService.log(logger, LEVEL.DEBUG, "empty : " + "findAccessControlPolicyList");
			verify = true;
		} else {
			for (int i=0; i<findAccessControlPolicyList.size(); i++) {
				AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
				try {
					String keyType = accessControlPolicy.getKeyType();
					String findUKey = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators().get(0);
					if (KEY_TYPE.UKEY.getName().equals(keyType)) {
						isUKey = true;
						if (uKey.equals(findUKey)) verify = true;
					}
				} catch (Exception e) {
					throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));				
				}
			}
		}
		
		if (isUKey && !verify) throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));
	}
	
	/**
	 * authenticate User when search resource
	 * @param checkMccP
	 * @param accessControlPolicyIDs
	 * @param uKey
	 * @return boolean
	 * @throws RSCException
	 */
	public boolean authUserSearch(boolean checkMccP, List<String> accessControlPolicyIDs, String uKey) throws RSCException {
		if (checkMccP) return true;
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) return true;
		
		boolean verify = false;
		boolean isUKey = false;
		
		if(CommonUtil.isEmpty(uKey)) {
			mongoLogService.log(logger, LEVEL.DEBUG, "uKey " + CommonUtil.getMessage("msg.input.empty.text"));
		} else {
			try{
				mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode before	: " + uKey);
				uKey = Base64Coder.decodeString(uKey);
				mongoLogService.log(logger, LEVEL.DEBUG, "uKey decode after		: " + uKey);
			}catch(Exception e){
				e.printStackTrace();
				logger.error(e.getMessage(),e);
				throw new RSCException(CommonCode.RSC.BAD_REQUEST, "uKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
			}		
		}
		
		List<AccessControlPolicy> findAccessControlPolicyList = accessControlPolicyService.findAccessControlPolicyList(accessControlPolicyIDs);
		
		if (CommonUtil.isEmpty(findAccessControlPolicyList)) {
			mongoLogService.log(logger, LEVEL.DEBUG, "empty : " + "findAccessControlPolicyList");
			verify = true;
		} else {
			for (int i=0; i<findAccessControlPolicyList.size(); i++) {
				AccessControlPolicy accessControlPolicy = findAccessControlPolicyList.get(i);
				try {
					String keyType = accessControlPolicy.getKeyType();
					String findUKey = accessControlPolicy.getSelfPrivileges().getAccessControlRule().get(0).getAccessControlOriginators().get(0);
					if (KEY_TYPE.UKEY.getName().equals(keyType)) {
						isUKey = true;
						if (uKey.equals(findUKey)) verify = true;
					}
				} catch (Exception e) {
					throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.uKey.verifyFail.text"));				
				}
			}
		}
		
		if (isUKey && !verify)
			return false;
		else 
			return true;
	}
	
	/**
	 * authenticate User when search resource
	 * @param parentID
	 * @param childResourceRefList
	 * @param uKey
	 * @param resourceType
	 * @return boolean
	 * @throws RSCException
	 */
	public List<ChildResourceRef> searchRefByUserAuth(String parentID, List<ChildResourceRef> childResourceRefList, String uKey, RESOURCE_TYPE resourceType) throws RSCException {
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn")))
			return childResourceRefList;
		
		List<ChildResourceRef> childResRefListResult = new ArrayList<ChildResourceRef>();
		
		switch(resourceType) {
		case AE:
			for (int i=0; i < childResourceRefList.size(); i++) {
				ChildResourceRef childResourceRef = childResourceRefList.get(i);
				AE findAEItem = null;
				if(!CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(parentID, childResourceRef.getResourceName()))){
					if (this.authUserSearch(false, findAEItem.getAccessControlPolicyIDs(), uKey)) {
						childResRefListResult.add(childResourceRef);
					}
				}
			}
			break;
		case LOCATION_POLICY:
			for (int i=0; i < childResourceRefList.size(); i++) {
				ChildResourceRef childResourceRef = childResourceRefList.get(i);
				LocationPolicy findLocationPolicyItem = null;
				if(!CommonUtil.isEmpty(findLocationPolicyItem = locationPolicyService.findOneLocationPolicyByResourceName(parentID, childResourceRef.getResourceName()))){
					Container findContainerItem = null;
					
					if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainer(findLocationPolicyItem.getLocationContainerID()))) {
						childResRefListResult.add(childResourceRef);
					} else {
						RemoteCSE findRemoteCSEItem	= remoteCSEService.findOneRemoteCSE(findContainerItem.getParentID());
						AE findAEItem				= aEService.findOneAE(findContainerItem.getParentID());

						if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
							if (this.authUserSearch(false, findRemoteCSEItem.getAccessControlPolicyIDs(), uKey)) {
								childResRefListResult.add(childResourceRef);
							}
						} else if (!CommonUtil.isEmpty(findAEItem)) {
							findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findAEItem.getParentID());
							if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
								if (this.authUserSearch(false, findRemoteCSEItem.getAccessControlPolicyIDs(), uKey)) {
									childResRefListResult.add(childResourceRef);
								}
							} else {
								if (this.authUserSearch(false, findAEItem.getAccessControlPolicyIDs(), uKey)) {
									childResRefListResult.add(childResourceRef);
								}
							}
						} else {
							childResRefListResult.add(childResourceRef);
						}
					}
				}
			}
			break;
		case GROUP:
			for (int i=0; i < childResourceRefList.size(); i++) {
				ChildResourceRef childResourceRef = childResourceRefList.get(i);
				Group findGroupItem = null;
				if(!CommonUtil.isEmpty(findGroupItem = groupService.findOneGroupByResourceName(parentID, childResourceRef.getResourceName()))){
					if (this.authUserSearch(false, findGroupItem.getAccessControlPolicyIDs(), uKey)) {
						childResRefListResult.add(childResourceRef);
					}
				}
			}
			break;
		case NODE:
			for (int i=0; i < childResourceRefList.size(); i++) {
				ChildResourceRef childResourceRef = childResourceRefList.get(i);
				Node findNodeItem = null;
				if(!CommonUtil.isEmpty(findNodeItem = nodeService.findOneNodeByResourceName(parentID, childResourceRef.getResourceName()))){
					if (this.authUserSearch(false, findNodeItem.getAccessControlPolicyIDs(), uKey)) {
						childResRefListResult.add(childResourceRef);
					}
				}
			}
			break;
		case REMOTE_CSE:
			for (int i=0; i < childResourceRefList.size(); i++) {
				ChildResourceRef childResourceRef = childResourceRefList.get(i);
				RemoteCSE findRemoteCSEItem = null;
				if(!CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(parentID, childResourceRef.getResourceName()))){
					if (this.authUserSearch(false, findRemoteCSEItem.getAccessControlPolicyIDs(), uKey)) {
						childResRefListResult.add(childResourceRef);
					}
				}
			}
			break;
		default:
			break;
		}
		
		return childResRefListResult;

	}
	
	/**
	 * authenticate User when search AE
	 * @param parentID
	 * @param childResourceList
	 * @param uKey
	 * @return List<AE>
	 * @throws RSCException
	 */
	public List<AE> searchAEByUserAuth(String parentID, List<AE> childResourceList, String uKey) throws RSCException {
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn")))
			return childResourceList;

		List<AE> childResourceListResult = new ArrayList<AE>();
		
		for (int i=0; i < childResourceList.size(); i++) {
			ResourceRef resourceRef = childResourceList.get(i).getResourceRef();
			AE findAEItem = null;
			if(!CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(parentID, resourceRef.getResourceName()))){
				if (this.authUserSearch(false, findAEItem.getAccessControlPolicyIDs(), uKey)) {
					childResourceListResult.add(childResourceList.get(i));
				}
			}
		}

		return childResourceListResult;
	}
	
	/**
	 * authenticate User when search LocationPolicy
	 * @param parentID
	 * @param childResourceList
	 * @param uKey
	 * @return List<LocationPolicy>
	 * @throws RSCException
	 */
	public List<LocationPolicy> searchLocationPolicyByUserAuth(String parentID, List<LocationPolicy> childResourceList, String uKey) throws RSCException {
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn")))
			return childResourceList;

		List<LocationPolicy> childResourceListResult = new ArrayList<LocationPolicy>();
		
		for (int i=0; i < childResourceList.size(); i++) {
			ResourceRef resourceRef = childResourceList.get(i).getResourceRef();
			LocationPolicy findLocationPolicyItem = null;
			
			if(!CommonUtil.isEmpty(findLocationPolicyItem = locationPolicyService.findOneLocationPolicyByResourceName(parentID, resourceRef.getResourceName()))){
				Container findContainerItem = null;
				
				if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainer(findLocationPolicyItem.getLocationContainerID()))) {
					childResourceListResult.add(childResourceList.get(i));
				} else {
					RemoteCSE findRemoteCSEItem	= remoteCSEService.findOneRemoteCSE(findContainerItem.getParentID());
					AE findAEItem				= aEService.findOneAE(findContainerItem.getParentID());

					if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
						if (this.authUserSearch(false, findRemoteCSEItem.getAccessControlPolicyIDs(), uKey)) {
							childResourceListResult.add(childResourceList.get(i));
						}
					} else if (!CommonUtil.isEmpty(findAEItem)) {
						findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findAEItem.getParentID());
						if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
							if (this.authUserSearch(false, findRemoteCSEItem.getAccessControlPolicyIDs(), uKey)) {
								childResourceListResult.add(childResourceList.get(i));
							}
						} else {
							if (this.authUserSearch(false, findAEItem.getAccessControlPolicyIDs(), uKey)) {
								childResourceListResult.add(childResourceList.get(i));
							}
						}
					} else {
						childResourceListResult.add(childResourceList.get(i));
					}
				}
			}

		}

		return childResourceListResult;
	}
	
	/**
	 * authenticate User when search Group
	 * @param parentID
	 * @param childResourceList
	 * @param uKey
	 * @return List<Group>
	 * @throws RSCException
	 */
	public List<Group> searchGroupByUserAuth(String parentID, List<Group> childResourceList, String uKey) throws RSCException {
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn")))
			return childResourceList;

		List<Group> childResourceListResult = new ArrayList<Group>();
		
		for (int i=0; i < childResourceList.size(); i++) {
			ResourceRef resourceRef = childResourceList.get(i).getResourceRef();
			Group findGroupItem = null;
			if(!CommonUtil.isEmpty(findGroupItem = groupService.findOneGroupByResourceName(parentID, resourceRef.getResourceName()))){
				if (this.authUserSearch(false, findGroupItem.getAccessControlPolicyIDs(), uKey)) {
					childResourceListResult.add(childResourceList.get(i));
				}
			}
		}

		return childResourceListResult;
	}
	
	/**
	 * authenticate User when search Node
	 * @param parentID
	 * @param childResourceList
	 * @param uKey
	 * @return List<Node>
	 * @throws RSCException
	 */
	public List<Node> searchNodeByUserAuth(String parentID, List<Node> childResourceList, String uKey) throws RSCException {
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn")))
			return childResourceList;

		List<Node> childResourceListResult = new ArrayList<Node>();
		
		for (int i=0; i < childResourceList.size(); i++) {
			ResourceRef resourceRef = childResourceList.get(i).getResourceRef();
			Node findNodeItem = null;
			if(!CommonUtil.isEmpty(findNodeItem = nodeService.findOneNodeByResourceName(parentID, resourceRef.getResourceName()))){
				if (this.authUserSearch(false, findNodeItem.getAccessControlPolicyIDs(), uKey)) {
					childResourceListResult.add(childResourceList.get(i));
				}
			}
		}

		return childResourceListResult;
	}
	
	/**
	 * authenticate User when search RemoteCSE
	 * @param parentID
	 * @param childResourceList
	 * @param uKey
	 * @return List<RemotgeCSE>
	 * @throws RSCException
	 */
	public List<RemoteCSE> searchRemoteCSEByUserAuth(String parentID, List<RemoteCSE> childResourceList, String uKey) throws RSCException {
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn")))
			return childResourceList;

		List<RemoteCSE> childResourceListResult = new ArrayList<RemoteCSE>();
		
		for (int i=0; i < childResourceList.size(); i++) {
			ResourceRef resourceRef = childResourceList.get(i).getResourceRef();
			RemoteCSE findRemoteCSEItem = null;
			if(!CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(parentID, resourceRef.getResourceName()))){
				if (this.authUserSearch(false, findRemoteCSEItem.getAccessControlPolicyIDs(), uKey)) {
					childResourceListResult.add(childResourceList.get(i));
				}
			}
		}

		return childResourceListResult;
	}
	
	/**
	 * Node accessControlPolicyIDs Update
	 * @param remoteCSEProfile
	 * @param acpID
	 * @return void
	 * @throws RSCException
	 */
	public void updateNodeAccessControlPolicyIDs(RemoteCSE remoteCSEProfile, String acpID) throws RSCException {
		
		try {
			
			if (!CommonUtil.isEmpty(acpID)) {
				
				Node findNodeItem = null;
				if(CommonUtil.isEmpty(findNodeItem = nodeService.findOneNode(remoteCSEProfile.getNodeLink()))) {
					mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE(Node) update Exception");
					throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.upFail.text"));
				}
			
				if (findNodeItem.getAccessControlPolicyIDs().contains(acpID)) {
					return ;
				}
				
				findNodeItem.getAccessControlPolicyIDs().add(acpID);
				String currentTime = CommonUtil.getNowTimestamp();
				
				Update update = new Update();
				update.set("accessControlPolicyIDs", findNodeItem.getAccessControlPolicyIDs());
				update.set("lastModifiedTime", currentTime);
															
				Query query = new Query();
				query.addCriteria(Criteria.where("resourceID").is(findNodeItem.getResourceID()));
					
				nodeDao.update(query, update);
					
				mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE update success");
					
				return ;
			}
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE update Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.upFail.text"));
		}
		
		return ;
	}
	
	/**
	 * RemoteCSE accessControlPolicyIDs Update
	 * @param remoteCSEProfile
	 * @param acpID
	 * @return RemoteCSE
	 * @throws RSCException
	 */
	public RemoteCSE updateDeviceAccessControlPolicyIDs(RemoteCSE remoteCSEProfile, String acpID) throws RSCException {
		
		try {
			
			if (!CommonUtil.isEmpty(acpID)) {
			
				if (remoteCSEProfile.getAccessControlPolicyIDs().contains(acpID)) {
					return remoteCSEService.findOneRemoteCSE(remoteCSEProfile.getResourceID());
				}
				
				remoteCSEProfile.getAccessControlPolicyIDs().add(acpID);
				String currentTime = CommonUtil.getNowTimestamp();
				
				Update update = new Update();
				update.set("accessControlPolicyIDs", remoteCSEProfile.getAccessControlPolicyIDs());
				update.set("lastModifiedTime", currentTime);
															
				Query query = new Query();
				query.addCriteria(Criteria.where("resourceID").is(remoteCSEProfile.getResourceID()));
					
				remoteCSEDao.update(query, update);
					
				mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE update success");
				
				updateNodeAccessControlPolicyIDs(remoteCSEProfile, acpID);
					
				return remoteCSEService.findOneRemoteCSE(remoteCSEProfile.getResourceID());
			}
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE update Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.upFail.text"));
		}
		
		return null;
	}
	
	/**
	 * AE accessControlPolicyIDs Update
	 * @param aeProfile
	 * @param acpID
	 * @return AE
	 * @throws RSCException
	 */
	public AE updateAppAccessControlPolicyIDs(AE aeProfile, String acpID) throws RSCException {
		
		try {
			
			if (!CommonUtil.isEmpty(acpID)) {
			
				if (aeProfile.getAccessControlPolicyIDs().contains(acpID)) {
					return aEService.findOneAE(aeProfile.getResourceID());
				}
				
				aeProfile.getAccessControlPolicyIDs().add(acpID);
				String currentTime = CommonUtil.getNowTimestamp();
				
				Update update = new Update();
				update.set("accessControlPolicyIDs", aeProfile.getAccessControlPolicyIDs());
//				update.set("mappingYn", "Y");
				update.set("lastModifiedTime", currentTime);
															
				Query query = new Query();
				query.addCriteria(Criteria.where("resourceID").is(aeProfile.getResourceID()));
					
				aEDao.update(query, update);
					
				mongoLogService.log(logger, LEVEL.DEBUG, "AE update success");
					
				return aEService.findOneAE(aeProfile.getResourceID());
			}
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "AE update Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.AE.upFail.text"));
		}
		
		return null;
	}
}
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
import java.util.regex.Pattern;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.AccessControlPolicy;
import kr.usis.iot.domain.oneM2M.AccessControlRule;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.SetOfAcrs;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.RemoteCSEDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.CSE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.KEY_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * remoteCSE service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class RemoteCSEService {

	private static final Log logger = LogFactory.getLog(RemoteCSEService.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private RemoteCSEDao remoteCSEDao;
	
	@Autowired
	private NodeService nodeService;	
	
	@Autowired
	private ContainerService containerService;	
	
	@Autowired
	private MgmtCmdService mgmtCmdService;	
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private AEService aEService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private SequenceDao seqDao;

	/**
	 * remoteCSE retrieve
	 * @param cseid
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE findOneRemoteCSEByCSEID(String cseid) throws RSCException {

		RemoteCSE findRemoteCSEItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("cseid").is(cseid));

		try {

			findRemoteCSEItem = (RemoteCSE) remoteCSEDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}

		return findRemoteCSEItem;
	}
	
	/**
	 * remoteCSE retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE findOneRemoteCSEByResourceName(String parentID, String resourceName) throws RSCException {

		RemoteCSE findRemoteCSEItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {

			findRemoteCSEItem = (RemoteCSE) remoteCSEDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}

		return findRemoteCSEItem;
	}
	
	/**
	 * remoteCSE retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE findOneRemoteCSE(String resourceID) throws RSCException {

		RemoteCSE findRemoteCSEItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {

			findRemoteCSEItem = (RemoteCSE) remoteCSEDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}

		return findRemoteCSEItem;
	}
	
	/**
	 * remoteCSE retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE findOneRemoteCSEByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;

		RemoteCSE findRemoteCSEItem = null;
		RemoteCSE findNoFilterRemoteCSEItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findRemoteCSEItem = (RemoteCSE) remoteCSEDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findRemoteCSEItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterRemoteCSEItem = findOneRemoteCSEResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findRemoteCSEItem)) findRemoteCSEItem = findNoFilterRemoteCSEItem;

		return findRemoteCSEItem;
	}
	
	/**
	 * remoteCSE References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE findOneRemoteCSEResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		RemoteCSE findRemoteCSEResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findRemoteCSEResourceRefItem = (RemoteCSE) remoteCSEDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		return findRemoteCSEResourceRefItem;
	}
	
	/**
	 * remoteCSE retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<RemoteCSE> findRemoteCSE(String key, String value) throws RSCException {

		List<RemoteCSE> findRemoteCSEList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {

			findRemoteCSEList = (List<RemoteCSE>) remoteCSEDao.find(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}

		return findRemoteCSEList;
	}
	
	/**
	 * remoteCSE retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<RemoteCSE> findRemoteCSE(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<RemoteCSE>();

		List<RemoteCSE> findRemoteCSEList = null;
		List<RemoteCSE> findRemoteCSENoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findRemoteCSEList = (List<RemoteCSE>) remoteCSEDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findRemoteCSEList);
			
			if (filterCriteria.isFilterCriteria()) findRemoteCSENoFilterResourceRefList = findRemoteCSEResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findRemoteCSENoFilterResourceRefList.size(); i++) {
				RemoteCSE noFilterRemoteCSE = findRemoteCSENoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findRemoteCSEList.size(); t++) {
					RemoteCSE remoteCSE = findRemoteCSEList.get(t);
					if (noFilterRemoteCSE.getResourceRef().getResourceID().equals(remoteCSE.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findRemoteCSEList.add(noFilterRemoteCSE);
			}
		}

		return findRemoteCSEList;
	}
	
	/**
	 * remoteCSE References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<RemoteCSE> findRemoteCSEResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<RemoteCSE> findRemoteCSEResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findRemoteCSEResourceRefList = (List<RemoteCSE>) remoteCSEDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		return findRemoteCSEResourceRefList;
	}
	
	/**
	 * remoteCSE References retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<RemoteCSE> findRemoteCSEResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<RemoteCSE>();

		String includeField = "resourceRef";
		List<RemoteCSE> findRemoteCSEResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findRemoteCSEResourceRefList = (List<RemoteCSE>) remoteCSEDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findRemoteCSEResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		return findRemoteCSEResourceRefList;
	}
	
	/**
	 * remoteCSE retrieve
	 * @param nodeResourceID
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE findOneRemoteCSEByNodeLink(String nodeResourceID) throws RSCException {
		
		RemoteCSE findRemoteCSEItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("nodeLink").is(nodeResourceID));
		
		try {
			
			findRemoteCSEItem = (RemoteCSE) remoteCSEDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		return findRemoteCSEItem;
	}
	
	/**
	 * remoteCSE List retrieve
	 * @param remoteCSECSEID
	 * @return
	 * @throws RSCException
	 */
	public List<RemoteCSE> findLikeListRemoteCSEByID(String remoteCSECSEID) throws RSCException {
		
		List<RemoteCSE> findListRemoteCSEItem = null;
		String reegexResourceId =  "^" + remoteCSECSEID;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("mappingYn").is("N"));
		query.addCriteria(Criteria.where("cseid").regex(Pattern.compile(reegexResourceId)));
		
		try {
			findListRemoteCSEItem = (List<RemoteCSE>) remoteCSEDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.findFail.text"));
		}
		
		return findListRemoteCSEItem;
	}	
	
	/**
	 * remoteCSE count retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCount(String resourceID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = remoteCSEDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * remoteCSE count retrieve
	 * @param remoteCSEResourceIDs
	 * @return
	 */
	public long getCount(List<String> remoteCSEResourceIDs){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").in(remoteCSEResourceIDs));
		
		long cnt = 0;
		
		try {
			cnt = remoteCSEDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * remoteCSE count retrieve
	 * @param remoteCSECSEID
	 * @param passCode
	 * @return
	 */
	public long getCount(String remoteCSECSEID, String passCode){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("cseid").is(remoteCSECSEID));
		query.addCriteria(Criteria.where("passCode").is(passCode));
		
		long cnt = 0;
		
		try {
			cnt = remoteCSEDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * remoteCSE create
	 * @param url
	 * @param remoteCSEVOProfile
	 * @param dKey
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE createRemoteCSE(String url, RemoteCSE remoteCSEVOProfile, String dKey) throws RSCException {
		
		Long seqID = seqDao.move(MovType.UP, SeqType.REMOTE_CSE);
		String remoteCSEResourceID = CommonUtil.seqIDToResourceID(SEQ_PREFIX.REMOTE_CSE.getValue(), seqID);
		
		String expirationTime = remoteCSEVOProfile.getExpirationTime();
		
		AccessControlPolicy accessControlPolicyItem = new AccessControlPolicy();
		
		SetOfAcrs privileges = new SetOfAcrs();
		AccessControlRule accessControlRule = new AccessControlRule();
		accessControlRule.getAccessControlOriginators().add(dKey);
		privileges.getAccessControlRule().add(accessControlRule);
		
		accessControlPolicyItem.setParentID(remoteCSEResourceID);
		accessControlPolicyItem.setExpirationTime(expirationTime);
		accessControlPolicyItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		accessControlPolicyItem.setKeyType(KEY_TYPE.DKEY.getName());
		accessControlPolicyItem.setPrivileges(privileges);
		accessControlPolicyItem.setSelfPrivileges(privileges);
		
		try {
			
			accessControlPolicyItem = accessControlPolicyService.createAccessControlPolicy(url, accessControlPolicyItem);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.accessControlPolicy.regFail.text"));
		}
		
		RemoteCSE remoteCSEItem = new RemoteCSE();
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		remoteCSEItem.setResourceType(CommonCode.RESOURCE_TYPE.REMOTE_CSE.getValue());
		remoteCSEItem.setResourceID(remoteCSEResourceID);
		remoteCSEItem.setResourceName(remoteCSEVOProfile.getResourceName());
		remoteCSEItem.setParentID(remoteCSEVOProfile.getParentID());
		remoteCSEItem.setCSEID(remoteCSEVOProfile.getCSEID());
		remoteCSEItem.setCreationTime(currentTime);
		remoteCSEItem.setLastModifiedTime(currentTime);
		remoteCSEItem.setExpirationTime(expirationTime);
		remoteCSEItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		remoteCSEItem.getAccessControlPolicyIDs().add(accessControlPolicyItem.getResourceID());
		remoteCSEItem.getLabels().addAll(remoteCSEVOProfile.getLabels());
		remoteCSEItem.getAnnounceTo().addAll(remoteCSEVOProfile.getAnnounceTo());
		remoteCSEItem.getAnnouncedAttribute().addAll(remoteCSEVOProfile.getAnnouncedAttribute());
		remoteCSEItem.setCseType(remoteCSEVOProfile.getCseType());
		remoteCSEItem.getPointOfAccess().addAll(remoteCSEVOProfile.getPointOfAccess());
		remoteCSEItem.setCSEBase(remoteCSEVOProfile.getCSEBase());
		remoteCSEItem.setM2MExtID(remoteCSEVOProfile.getM2MExtID());
		remoteCSEItem.setTriggerRecipientID(remoteCSEVOProfile.getTriggerRecipientID());
		remoteCSEItem.setRequestReachability(remoteCSEVOProfile.isRequestReachability());
		remoteCSEItem.setPassCode(remoteCSEVOProfile.getPassCode());
		remoteCSEItem.setMappingYn(remoteCSEVOProfile.getMappingYn());
		remoteCSEItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, remoteCSEItem), remoteCSEItem.getResourceName(), CommonCode.RESOURCE_TYPE.REMOTE_CSE.getValue(), remoteCSEItem.getResourceID(), null));
		
		Node nodeItem = null;
		try {
			
			nodeItem = nodeService.createNode(url, remoteCSEItem);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.node.createFail.text"));
		}
		
		try {
			
			if(!CommonUtil.isEmpty(nodeItem) && !CommonUtil.isEmpty(nodeItem.getResourceID())) {
				remoteCSEItem.setNodeLink(nodeItem.getResourceID());
			}
			remoteCSEDao.insert(remoteCSEItem);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.regFail.text"));
		}
		
		return this.findOneRemoteCSE(remoteCSEItem.getResourceID());
	}
	
	/**
	 * remoteCSE delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteRemoteCSE(String resourceID) throws RSCException {
		
		Query query = new Query(Criteria.where("resourceID").is(resourceID));
		
		if(this.getCount(resourceID) < 1){
			throw new RSCException(CommonCode.RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text") + " : resourceID ["+resourceID+"]");
		}
		
		try {
			remoteCSEDao.remove(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE delete success");
	}
	
	/**
	 * remoteCSE delete
	 * @param resourceID
	 * @param nodeLink
	 * @throws RSCException
	 */
	public void deleteRemoteCSEChild(String resourceID, String nodeLink) throws RSCException {
		
		Node findNodeItem = nodeService.findOneNodeByHostedCSELink(nodeLink);
		
		try {
			nodeService.deleteNodeChild(findNodeItem.getResourceID());
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.delFail.text"));
		}
		
		try {
			containerService.deleteContainerByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE inner container remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.container.delFail.text"));
		}
		
		try {
			mgmtCmdService.deleteMgmtCmdByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE inner mgmtCmd remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.delFail.text"));
		}
		
		try {
			accessControlPolicyService.deleteAccessControlPolicyByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE inner accessControlPolicy remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.delFail.text"));
		}
		
		try {
			aEService.deleteAEByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE inner AE remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.AE.delFail.text"));
		}
		
		try {
			groupService.deleteGroupByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE inner Group remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.Group.delFail.text"));
		}
		
		this.deleteRemoteCSE(resourceID);
	}
	
	/**
	 * dKey update
	 * @param resourceID
	 * @param dKey
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE updateRemoteCSEDkey(String resourceID, String dKey) throws RSCException {
		mongoLogService.log(logger, LEVEL.DEBUG, "updateRemoteCSEDkey start");
		
		Node nodeItem = new Node();
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		Update update = new Update();
		update.set("lastModifiedTime", currentTime);
		
		try {
			if(!CommonUtil.isEmpty(nodeItem) && !CommonUtil.isEmpty(nodeItem.getResourceID())) {
				update.set("nodeLink", nodeItem.getResourceID());
			}
			
			remoteCSEDao.update(query, update);
	
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "updateRemoteCSEDkey update end");
		
		return this.findOneRemoteCSE(resourceID);
	}
	
	/**
	 * remoteCSE update
	 * @param remoteCSEVOProfile
	 * @return
	 * @throws RSCException
	 */
	public RemoteCSE updateRemoteCSE(RemoteCSE remoteCSEProfile) throws RSCException {
		
		RemoteCSE findRemoteCSEItem 		= null;
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID					= remoteCSEProfile.getResourceID();
		String expirationTime				= remoteCSEProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = remoteCSEProfile.getAccessControlPolicyIDs();
		List<String> labels 				= remoteCSEProfile.getLabels();
		List<String> announceTo 			= remoteCSEProfile.getAnnounceTo();
		List<String> announcedAttribute		= remoteCSEProfile.getAnnouncedAttribute();
		List<String> pointOfAccess 			= remoteCSEProfile.getPointOfAccess();
		String m2MExtID						= remoteCSEProfile.getM2MExtID();
		Long triggerRecipientID				= remoteCSEProfile.getTriggerRecipientID();
		Boolean requestReachability 		= remoteCSEProfile.isRequestReachability();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(announceTo))				update.set("announceTo", announceTo);
		if(!CommonUtil.isNull(announcedAttribute))		update.set("announcedAttribute", announcedAttribute);
		if(!CommonUtil.isNull(pointOfAccess))			update.set("pointOfAccess", pointOfAccess);
		if(!CommonUtil.isNull(m2MExtID))				update.set("m2MExtID", m2MExtID);
		if(!CommonUtil.isNull(triggerRecipientID))		update.set("triggerRecipientID", triggerRecipientID);
		if(!CommonUtil.isNull(requestReachability))		update.set("requestReachability", requestReachability);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			
			remoteCSEDao.update(query, update);
			
			mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE update success");
			
			findRemoteCSEItem = this.findOneRemoteCSE(resourceID);
			
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "remoteCSE update Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.upFail.text"));
		}
		
		try { 
			nodeService.updateNode(findRemoteCSEItem);
			
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "RemoteCSEService.updateRemoteCSE Exception");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "remoteCSE update end");
		
		return findRemoteCSEItem;
	}
	

}
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
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Group;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.GroupDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.EXEC_RESULT;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * group Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class GroupService {

	private static final Log logger = LogFactory.getLog(GroupService.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private GroupDao groupDao;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private SequenceDao seqDao;	
	
	
	/**
	 * group retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Group findOneGroup(String resourceID) throws RSCException {

		Group findGroupItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {

			findGroupItem = (Group) groupDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}

		return findGroupItem;
	}
	
	/**
	 * group retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Group> findGroup(String key, String value) throws RSCException {

		List<Group> findGroupList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {

			findGroupList = (List<Group>) groupDao.find(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}

		return findGroupList;
	}
	
	/**
	 * group retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Group> findGroup(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Group>();

		List<Group> findGroupList = null;
		List<Group> findGroupNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findGroupList = (List<Group>) groupDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findGroupList);
			
			if (filterCriteria.isFilterCriteria()) findGroupNoFilterResourceRefList = findGroupResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findGroupNoFilterResourceRefList.size(); i++) {
				Group noFilterGroup = findGroupNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findGroupList.size(); t++) {
					Group group = findGroupList.get(t);
					if (noFilterGroup.getResourceRef().getResourceID().equals(group.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findGroupList.add(noFilterGroup);
			}
		}

		return findGroupList;
	}
	
	/**
	 * group References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Group> findGroupResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Group> findGroupResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findGroupResourceRefList = (List<Group>) groupDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		return findGroupResourceRefList;
	}
	
	/**
	 * group References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Group> findGroupResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Group>();

		String includeField = "resourceRef";
		List<Group> findGroupResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findGroupResourceRefList = (List<Group>) groupDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findGroupResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		return findGroupResourceRefList;
	}
	
	/**
	 * group retrieve
	 * @param groupName
	 * @return
	 * @throws RSCException
	 */
	public Group findOneGroupByName(String groupName) throws RSCException {
		
		Group findGroupItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("groupName").is(groupName));
		
		try {
			
			findGroupItem = (Group) groupDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		return findGroupItem;
	}
	
	/**
	 * group retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Group findOneGroupByResourceName(String parentID, String resourceName) throws RSCException {
		
		Group findGroupItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			
			findGroupItem = (Group) groupDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		return findGroupItem;
	}
	
	
	/**
	 * group retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public Group findOneGroupByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		Group findGroupItem = null;
		Group findNoFilterGroupItem = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		try {
			findGroupItem = (Group) groupDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findGroupItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterGroupItem = findOneGroupResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findGroupItem)) findGroupItem = findNoFilterGroupItem;
		
		return findGroupItem;
	}	
	
	
	/**
	 * Group References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Group findOneGroupResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		Group findGroupResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findGroupResourceRefItem = (Group) groupDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.findFail.text"));
		}
		
		return findGroupResourceRefItem;
	}
	
	
	/**
	 * group count retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCount(String resourceID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = groupDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "group get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * group control request
	 * @param url
	 * @param arrMemberResourceID
	 * @param mgmtCmdResourceName
	 * @param mgmtCmdProfile
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> groupControl(String url, String[] arrMemberResourceID, String mgmtCmdResourceName, MgmtCmd mgmtCmdProfile) throws RSCException {
		List<ArrayList<String[]>> findListAceess = new ArrayList<ArrayList<String[]>>();
		List<ExecInstance> findListExecInstance = new ArrayList<ExecInstance>();
		List<MgmtCmd> findListMgmtCmd = new ArrayList<MgmtCmd>();
		ArrayList<String[]> listPointOfAceess = null;
		ExecInstance findExecInstance = null;
		MgmtCmd findMgmtCmdItem = null;
		
		for (String remoteCSEResourceID : arrMemberResourceID) {
			findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(remoteCSEResourceID, mgmtCmdResourceName);
			
			if(!CommonUtil.isEmpty(findMgmtCmdItem)) {
				findMgmtCmdItem.setExecReqArgs(mgmtCmdProfile.getExecReqArgs());
				
				listPointOfAceess = mgmtCmdService.getPointOfAccessListByRemoteCSEResourceID(remoteCSEResourceID);
				
				findListMgmtCmd.add(findMgmtCmdItem);
				findListAceess.add(listPointOfAceess);
			}
		}
		
		for (int i=0; i<findListMgmtCmd.size(); i++) {
			
			MgmtCmd mgmtCmd = findListMgmtCmd.get(i);
			
			findExecInstance = mgmtCmdService.createExecInstance(url, mgmtCmd);
			
			boolean isRequestSuccess = mgmtCmdService.callDevice(mgmtCmd, findExecInstance, findListAceess.get(i));
			
			if (isRequestSuccess) {
				
				mongoLogService.log(logger, LEVEL.DEBUG, "[MgmtCmd group control] success");
			} else {
				findExecInstance.setExecResult(EXEC_RESULT.STATUS_REQUEST_DENIED.getValue());
				
				mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd group control] fail");
			}
			
			if(!CommonUtil.isEmpty(findExecInstance)) {
				findListExecInstance.add(findExecInstance);
			}
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "[Group MgmtCmd control] success");
		
		return findListExecInstance;
	}
	
	
	/**
	 * Group create
	 * @param url
	 * @param groupProfile
	 * @param uKey
	 * @return
	 * @throws RSCException
	 */
	public Group createGroup(String url, Group groupProfile) throws RSCException {
		
		Long seqID = seqDao.move(MovType.UP, SeqType.GROUP);
		String groupResourceID = CommonUtil.seqIDToResourceID(SEQ_PREFIX.GROUP.getValue(), seqID);
		
		String currentTime = CommonUtil.getNowTimestamp();
		
		String expirationTime = groupProfile.getExpirationTime();
		
		Group groupItem = new Group();
		
		groupItem.setResourceType(RESOURCE_TYPE.GROUP.getValue());
		groupItem.setResourceID(groupResourceID);
		groupItem.setResourceName(!CommonUtil.isEmpty(groupProfile.getResourceName()) ? groupProfile.getResourceName() : groupItem.getResourceID());
		groupItem.setParentID(groupProfile.getParentID());
		groupItem.setExpirationTime(expirationTime);
		groupItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//groupItem.getAccessControlPolicyIDs().add(accessControlPolicyItem.getResourceID());
		groupItem.setCreationTime(currentTime);
		groupItem.setLastModifiedTime(currentTime);
		groupItem.getLabels().addAll(groupProfile.getLabels());
		groupItem.getAnnounceTo().addAll(groupProfile.getAnnounceTo());
		groupItem.getAnnouncedAttribute().addAll(groupProfile.getAnnouncedAttribute());
		groupItem.setCreator(groupProfile.getCreator());
		groupItem.setMemberType(groupProfile.getMemberType());
		groupItem.setCurrentNrOfMembers(groupProfile.getCurrentNrOfMembers());
		groupItem.setMaxNrOfMembers(groupProfile.getMaxNrOfMembers());
		groupItem.getMemberIDs().addAll(groupProfile.getMemberIDs());
		groupItem.getMembersAccessControlPolicyIDs().addAll(groupProfile.getMembersAccessControlPolicyIDs());
		groupItem.setMemberTypeValidated(groupProfile.isMemberTypeValidated());
		groupItem.setConsistencyStrategy(groupProfile.getConsistencyStrategy());
		groupItem.setGroupName(groupProfile.getGroupName());
		groupItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, groupItem), groupItem.getResourceName(), RESOURCE_TYPE.GROUP.getValue(), groupItem.getResourceID(), null));
		
		try {
			groupDao.insert(groupItem);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.INFO, "Group create success");

		return this.findOneGroup(groupItem.getResourceID());
	}
	
	/**
	 * Group delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteGroup(String resourceID) throws RSCException {
		
		Query query = new Query(Criteria.where("resourceID").is(resourceID));
		
		try {
			groupDao.remove(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.group.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "group delete success");
	}
	
	
	/**
	 * Group delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteGroupChild(String resourceID) throws RSCException {
		
		try {
			accessControlPolicyService.deleteAccessControlPolicyByParentID(resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "Group inner accessControlPolicy remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.accessControlPolicy.delFail.text"));
		}
		
		this.deleteGroup(resourceID);
	}
	
	/**
	 * Group delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteGroupByParentID(String parentID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			List<Group> findGroupList = this.findGroup("parentID", parentID);
			for(Group groupItem : findGroupList) {
				this.deleteGroupChild(groupItem.getResourceID());
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.Group.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "Group delete success");
		
	}
	
	
	/**
	 * Group update
	 * @param groupProfile
	 * @return
	 * @throws RSCException
	 */
	public Group updateGroup(Group groupProfile) throws RSCException {
		
		String currentTime							= CommonUtil.getNowTimestamp();
		
		String resourceID							= groupProfile.getResourceID();
		String expirationTime						= groupProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = groupProfile.getAccessControlPolicyIDs();
		List<String> labels 						= groupProfile.getLabels();
		List<String> announceTo 					= groupProfile.getAnnounceTo();
		List<String> announcedAttribute				= groupProfile.getAnnouncedAttribute();
		//String ceator								= groupProfile.getCreator();
		BigInteger maxNrOfMembers					= groupProfile.getMaxNrOfMembers();
		List<String> memberIDs 						= groupProfile.getMemberIDs();
		//List<String> membersAccessControlPolicyIDs 	= groupProfile.getMembersAccessControlPolicyIDs();
		String groupName							= groupProfile.getGroupName();
		 
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(announceTo))				update.set("announceTo", announceTo);
		if(!CommonUtil.isNull(announcedAttribute))		update.set("announcedAttribute", announcedAttribute);
		//if(!CommonUtil.isNull(ceator))					update.set("ceator", ceator);
		if(!CommonUtil.isNull(maxNrOfMembers))			update.set("maxNrOfMembers", maxNrOfMembers);
		if(!CommonUtil.isNull(memberIDs))				update.set("memberIDs", memberIDs);
		//if(!CommonUtil.isNull(membersAccessControlPolicyIDs))	update.set("membersAccessControlPolicyIDs", membersAccessControlPolicyIDs);
		if(!CommonUtil.isNull(groupName))				update.set("groupName", groupName);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			groupDao.update(query, update);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.group.upFail.text"));
		}
		mongoLogService.log(logger, LEVEL.INFO, "Group update success");
		
		return this.findOneGroup(resourceID);
		
	}	
		
}
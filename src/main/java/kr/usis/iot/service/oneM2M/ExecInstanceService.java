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
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.ExecInstanceDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.EXEC_STATUS;
import kr.usis.iot.util.oneM2M.CommonCode.OPERATION_MONITOR;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_STATUS;
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
 * execInstance Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class ExecInstanceService {

private static final Log logger = LogFactory.getLog(ExecInstanceService.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private SubscriptionService subscriptionService;
	
	@Autowired
	private SequenceDao seqDao;
	
	@Autowired
	private ExecInstanceDao execInstanceDao;
	

	/**
	 * execInstance count retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCount(String resourceID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = execInstanceDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "execInstance get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * execInstance create
	 * @param url
	 * @param mgmtCmdProfile
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance createExecInstance(String url, MgmtCmd mgmtCmdProfile) throws RSCException {
		mongoLogService.log(logger, LEVEL.DEBUG, "execInstance create start");
		
		ExecInstance execInstanceItem = new ExecInstance();
		ExecInstance findExecInstanceItem = null;
		
		Long seqID = seqDao.move(MovType.UP, SeqType.EXEC_INSTANCE);
		String currentTime = CommonUtil.getNowTimestamp();
		
		String expirationTime = mgmtCmdProfile.getExpirationTime();
		
		execInstanceItem.setResourceType(RESOURCE_TYPE.EXEC_INSTANCE.getValue());
		execInstanceItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.EXEC_INSTANCE.getValue(), seqID));
		execInstanceItem.setResourceName(execInstanceItem.getResourceID());
		execInstanceItem.setParentID(mgmtCmdProfile.getResourceID());
		execInstanceItem.setExpirationTime(expirationTime);
		execInstanceItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		
		//execInstanceItem.getAccessControlPolicyIDs().addAll(mgmtCmdProfile.getAccessControlPolicyIDs());
		execInstanceItem.setCreationTime(currentTime);
		execInstanceItem.setLastModifiedTime(currentTime);
		execInstanceItem.getLabels().addAll(mgmtCmdProfile.getLabels());
		execInstanceItem.setExecStatus(EXEC_STATUS.INITIATED.getValue());
		execInstanceItem.setExecTarget(mgmtCmdProfile.getExecTarget());
		execInstanceItem.setExecMode(mgmtCmdProfile.getExecMode());
		execInstanceItem.setExecFrequency(mgmtCmdProfile.getExecFrequency());
		execInstanceItem.setExecDelay(mgmtCmdProfile.getExecDelay());
		execInstanceItem.setExecNumber(mgmtCmdProfile.getExecNumber());
		execInstanceItem.setExecReqArgs(mgmtCmdProfile.getExecReqArgs());
		execInstanceItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, execInstanceItem), execInstanceItem.getResourceName(), RESOURCE_TYPE.EXEC_INSTANCE.getValue(), execInstanceItem.getResourceID(), null));

		try {
			execInstanceDao.insert(execInstanceItem);
			mongoLogService.log(logger, LEVEL.DEBUG, "execInstance create success");
			
			findExecInstanceItem = this.findOneExecInstance(execInstanceItem.getResourceID());
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "execInstance create end");
		
		subscriptionService.sendSubscription(findExecInstanceItem.getParentID(), findExecInstanceItem.getResourceID(), RESOURCE_STATUS.CHILD_CREATED, OPERATION_MONITOR.CREATE, ExecInstance.class, findExecInstanceItem);
		
		return findExecInstanceItem;
	}
	
	/**
	 * execInstance retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance findOneExecInstance(String resourceID) throws RSCException {
		ExecInstance findExecInstanceItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {
			findExecInstanceItem = (ExecInstance) execInstanceDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}

		return findExecInstanceItem;
	}
	
	/**
	 * execInstance retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance findOneExecInstanceByResourceName(String parentID, String resourceName) throws RSCException {
		ExecInstance findExecInstanceItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findExecInstanceItem = (ExecInstance) execInstanceDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}

		return findExecInstanceItem;
	}
	
	/**
	 * execInstance retrieve
	 * @param parentID
	 * @param resourceName
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance findOneExecInstanceByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		ExecInstance findExecInstanceItem = null;
		ExecInstance findNoFilterExecInstanceItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findExecInstanceItem = (ExecInstance) execInstanceDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findExecInstanceItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterExecInstanceItem = findOneExecInstanceResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findExecInstanceItem)) findExecInstanceItem = findNoFilterExecInstanceItem;

		return findExecInstanceItem;
	}
	
	/**
	 * execInstance References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance findOneExecInstanceResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		ExecInstance findExecInstanceResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findExecInstanceResourceRefItem = (ExecInstance) execInstanceDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		return findExecInstanceResourceRefItem;
	}
	
	/**
	 * execInstance retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> findExecInstance(String key, String value) throws RSCException {
		List<ExecInstance> findExecInstanceList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findExecInstanceList = (List<ExecInstance>) execInstanceDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}

		return findExecInstanceList;
	}
	
	/**
	 * execInstance retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> findExecInstance(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<ExecInstance>();
		
		List<ExecInstance> findExecInstanceList = null;
		List<ExecInstance> findExecInstanceNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findExecInstanceList = (List<ExecInstance>) execInstanceDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findExecInstanceList);
			
			if (filterCriteria.isFilterCriteria()) findExecInstanceNoFilterResourceRefList = findExecInstanceResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findExecInstanceNoFilterResourceRefList.size(); i++) {
				ExecInstance noFilterExecInstance = findExecInstanceNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findExecInstanceList.size(); t++) {
					ExecInstance execInstance = findExecInstanceList.get(t);
					if (noFilterExecInstance.getResourceRef().getResourceID().equals(execInstance.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findExecInstanceList.add(noFilterExecInstance);
			}
		}

		return findExecInstanceList;
	}
	
	/**
	 * execInstance References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> findExecInstanceResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<ExecInstance> findExecInstanceResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findExecInstanceResourceRefList = (List<ExecInstance>) execInstanceDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		return findExecInstanceResourceRefList;
	}
	
	/**
	 * execInstance References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> findExecInstanceResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<ExecInstance>();
		
		String includeField = "resourceRef";
		List<ExecInstance> findExecInstanceResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findExecInstanceResourceRefList = (List<ExecInstance>) execInstanceDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findExecInstanceResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		return findExecInstanceResourceRefList;
	}
	
	/**
	 * execInstance List retrieve
	 * @param listExecInstanceID
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> findListExecInstanceIDs(List<String> listExecInstanceID) throws RSCException {
		List<ExecInstance> findExecInstanceList = null;
		
		if(CommonUtil.isEmpty(listExecInstanceID) || listExecInstanceID.size() == 0) {
			throw new RSCException(RSC.BAD_REQUEST, "execInstance List " + CommonUtil.getMessage("msg.input.empty.text"));
		}
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").in(listExecInstanceID));
		
		try {
			findExecInstanceList = (List<ExecInstance>) execInstanceDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "execInstance list search success");
		
		return findExecInstanceList;
		
	}	
	
	/**
	 * execInstance List retrieve
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> findListInitedExecInstanceByParentID(String parentID) throws RSCException {
		List<ExecInstance> findExecInstanceList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("execStatus").is(EXEC_STATUS.INITIATED.getValue()));
		
		try {
			findExecInstanceList = (List<ExecInstance>) execInstanceDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.execInstance.findFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd inner execInstance list search success");
		
		return findExecInstanceList;
		
	}
	
	/**
	 * execInstance List retrieve
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	public List<ExecInstance> pollingExecInstanceRetrieveByParentID(String parentID) throws RSCException {
		
		List<ExecInstance> findListExecInstance = this.findListInitedExecInstanceByParentID(parentID);
		
		for (ExecInstance execInstanceVO : findListExecInstance) {
			execInstanceVO.setExecStatus(EXEC_STATUS.PENDING.getValue());
			this.updateExecInstanceResult(execInstanceVO);
			
		}
		
		return findListExecInstance;
		
	}

	/**
	 * execInstance List delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteExecInstance(String parentID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			execInstanceDao.remove(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.execinstance.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "execInstance delete success");
		
	}
	
	/**
	 * execInstance List delete
	 * @param listMgmtCmdResourceID
	 * @throws RSCException
	 */
	public void deleteExecInstanceByMgmtCmdIds(ArrayList<String> listMgmtCmdResourceID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").in(listMgmtCmdResourceID));
		
		try {
			execInstanceDao.remove(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.contentinstance.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd inner execInstance delete success");
		
	}

	/**
	 * execInstance update
	 * @param execInstanceProfile
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance updateExecInstanceResult(ExecInstance execInstanceProfile) throws RSCException {
		mongoLogService.log(logger, LEVEL.DEBUG, "execInstance updateExecInstanceResult start");
		
		if(this.getCount(execInstanceProfile.getResourceID()) < 1) {
			throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.execInstance.noRegi.text"));
		}
		
		ExecInstance findExecInstanceItem = new ExecInstance();
		
		String resourceID = execInstanceProfile.getResourceID();
		String currentTime = CommonUtil.getNowTimestamp();
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		Update update = new Update();
		update.set("lastModifiedTime", currentTime);
		update.set("execStatus", execInstanceProfile.getExecStatus());
		update.set("execResult", execInstanceProfile.getExecResult());
		
		try { 
			
			execInstanceDao.update(query, update);
			
			mongoLogService.log(logger, LEVEL.DEBUG, "execInstance update success");
			
			findExecInstanceItem = this.findOneExecInstance(resourceID);
	
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.execInstance.upFail.text"));
		}

		mongoLogService.log(logger, LEVEL.DEBUG, "execInstance updateExecInstanceResult end");
		
		return findExecInstanceItem;
	}
	
}
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
import java.util.HashMap;
import java.util.List;

import javax.xml.datatype.Duration;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Firmware;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.MgmtCmdOnlyId;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.Software;
import kr.usis.iot.driver.coap.CoAPClient;
import kr.usis.iot.driver.http.HTTPClient;
import kr.usis.iot.driver.http.HttpResponse;
import kr.usis.iot.driver.mqtt.MqttClientUtil;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.MgmtCmdDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.CMD_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.EXEC_RESULT;
import kr.usis.iot.util.oneM2M.CommonCode.EXEC_STATUS;
import kr.usis.iot.util.oneM2M.CommonCode.MGMT_ACCESS_TYPE;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;

/**
 * mgmtCmd Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class MgmtCmdService {

	private static final Log logger = LogFactory.getLog(MgmtCmdService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private ExecInstanceService execInstanceService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private FirmwareService firmwareService;
	
	@Autowired
	private SoftwareService softwareService;
	
	@Autowired
	private SubscriptionService subscriptionService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private SequenceDao seqDao;

	@Autowired
	private MgmtCmdDao mgmtCmdDao;
	
	
	/**
	 * mgmtCmd create
	 * @param url
	 * @param mgmtCmdProfile
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd createMgmtCmd(String url, MgmtCmd mgmtCmdProfile) throws RSCException {
		MgmtCmd mgmtCmdItem = new MgmtCmd();
		
		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.MGMT_CMD);
		
		String expirationTime = mgmtCmdProfile.getExpirationTime();

		mgmtCmdItem.setResourceType(RESOURCE_TYPE.MGMT_CMD.getValue());
		mgmtCmdItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.MGMT_CMD.getValue(), seqID));
		mgmtCmdItem.setResourceName(!CommonUtil.isEmpty(mgmtCmdProfile.getResourceName()) ? mgmtCmdProfile.getResourceName() : mgmtCmdItem.getResourceID());
		mgmtCmdItem.setParentID(mgmtCmdProfile.getParentID());
		mgmtCmdItem.setExpirationTime(expirationTime);
		mgmtCmdItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		//mgmtCmdItem.getAccessControlPolicyIDs().addAll(mgmtCmdProfile.getAccessControlPolicyIDs());
		mgmtCmdItem.getLabels().addAll(mgmtCmdProfile.getLabels());
		mgmtCmdItem.setCreationTime(currentTime);
		mgmtCmdItem.setLastModifiedTime(currentTime);
		mgmtCmdItem.setDescription(mgmtCmdProfile.getDescription());
		mgmtCmdItem.setCmdType(mgmtCmdProfile.getCmdType());
		mgmtCmdItem.setExecReqArgs(mgmtCmdProfile.getExecReqArgs());
		mgmtCmdItem.setExecEnable(mgmtCmdProfile.isExecEnable());
		mgmtCmdItem.setExecTarget(mgmtCmdProfile.getExecTarget());
		mgmtCmdItem.setExecMode(mgmtCmdProfile.getExecMode());
		mgmtCmdItem.setExecFrequency(mgmtCmdProfile.getExecFrequency());
		mgmtCmdItem.setExecDelay(mgmtCmdProfile.getExecDelay());
		mgmtCmdItem.setExecNumber(mgmtCmdProfile.getExecNumber());
		mgmtCmdItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, mgmtCmdItem), mgmtCmdItem.getResourceName(), RESOURCE_TYPE.MGMT_CMD.getValue(), mgmtCmdItem.getResourceID(), null));
		
		mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		mongoLogService.log(logger, LEVEL.DEBUG, "[ mgmtCmdVO data ]");
		mongoLogService.log(logger, LEVEL.DEBUG, mgmtCmdItem.toString());
		mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		try {
			mgmtCmdDao.insert(mgmtCmdItem);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd create success");
		
		return this.findOneMgmtCmd(mgmtCmdItem.getResourceID());
	}

	/**
	 * mgmtCmd retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd findOneMgmtCmdByResourceName(String parentID, String resourceName) throws RSCException {
		MgmtCmd findMgmtCmdItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findMgmtCmdItem = (MgmtCmd) mgmtCmdDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}

		return findMgmtCmdItem;
	}
	
	/**
	 * mgmtCmd retrieve
	 * @param parentID
	 * @param resourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd findOneMgmtCmdByResourceName(String parentID, String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;
		
		MgmtCmd findMgmtCmdItem = null;
		MgmtCmd findNoFilterMgmtCmdItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findMgmtCmdItem = (MgmtCmd) mgmtCmdDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findMgmtCmdItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterMgmtCmdItem = findOneMgmtCmdResourceRefByResourceName(parentID, resourceName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findMgmtCmdItem)) findMgmtCmdItem = findNoFilterMgmtCmdItem;

		return findMgmtCmdItem;
	}
	
	/**
	 * mgmtCmd References retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd findOneMgmtCmdResourceRefByResourceName(String parentID, String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		MgmtCmd findMgmtCmdResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findMgmtCmdResourceRefItem = (MgmtCmd) mgmtCmdDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdResourceRefItem;
	}
	
	/**
	 * mgmtCmd retrieve
	 * @param parentID
	 * @param cmdType
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd findOneMgmtCmdByCmdType(String parentID, String cmdType) throws RSCException {
		MgmtCmd findMgmtCmdItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("cmdType").is(cmdType));
		
		try {
			findMgmtCmdItem = (MgmtCmd) mgmtCmdDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdItem;
	}
	
	/**
	 * mgmtCmd List retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<MgmtCmd> findMgmtCmd(String key, String value) throws RSCException {
		List<MgmtCmd> findMgmtCmdList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findMgmtCmdList = (List<MgmtCmd>)mgmtCmdDao.find(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdList;
	}
	
	/**
	 * mgmtCmd retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<MgmtCmd> findMgmtCmd(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<MgmtCmd>();
		
		List<MgmtCmd> findMgmtCmdList = null;
		List<MgmtCmd> findMgmtCmdNoFilterResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findMgmtCmdList = (List<MgmtCmd>)mgmtCmdDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findMgmtCmdList);
			
			if (filterCriteria.isFilterCriteria()) findMgmtCmdNoFilterResourceRefList = findMgmtCmdResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findMgmtCmdNoFilterResourceRefList.size(); i++) {
				MgmtCmd noFilterMgmtCmd = findMgmtCmdNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findMgmtCmdList.size(); t++) {
					MgmtCmd mgmtCmd = findMgmtCmdList.get(t);
					if (noFilterMgmtCmd.getResourceRef().getResourceID().equals(mgmtCmd.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findMgmtCmdList.add(noFilterMgmtCmd);
			}
		}
		
		return findMgmtCmdList;
	}
	
	/**
	 * mgmtCmd References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<MgmtCmd> findMgmtCmdResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<MgmtCmd> findMgmtCmdResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findMgmtCmdResourceRefList = (List<MgmtCmd>) mgmtCmdDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdResourceRefList;
	}
	
	/**
	 * mgmtCmd References retrieve
	 * @param key
	 * @param value
	 * @param filterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<MgmtCmd> findMgmtCmdResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<MgmtCmd>();
		
		String includeField = "resourceRef";
		List<MgmtCmd> findMgmtCmdResourceRefList = null;
		
		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findMgmtCmdResourceRefList = (List<MgmtCmd>)mgmtCmdDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findMgmtCmdResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdResourceRefList;
	}
	
	/**
	 * mgmtCmd retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd findOneMgmtCmd(String resourceID) throws RSCException {
		MgmtCmd findMgmtCmdItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		try {
			findMgmtCmdItem = (MgmtCmd) mgmtCmdDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdItem;
	}
	
	/**
	 * mgmtCmd List retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<MgmtCmdOnlyId> findListMgmtCmdOnlyID(String key, String value) throws RSCException {
		List<MgmtCmdOnlyId> findMgmtCmdList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		
		try {
			findMgmtCmdList = (List<MgmtCmdOnlyId>)mgmtCmdDao.findMgmtCmdOnlyID(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.mgmtcmd.findFail.text"));
		}
		
		return findMgmtCmdList;
	}
	
	/**
	 * mgmtCmd delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteMgmtCmd(String resourceID) throws RSCException {

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {
			
			execInstanceService.deleteExecInstance(resourceID);
			
			try {
				subscriptionService.deleteSubscription("parentID", resourceID);
			} catch (Exception e) {
				mongoLogService.log(logger, LEVEL.ERROR, "subscription remove");
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
				
				throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.subscription.delFail.text"));
			}
			
			mgmtCmdDao.remove(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd delete success");

	}
	
	/**
	 * mgmtCmd List delete
	 * @param parentID
	 * @throws RSCException
	 */
	public void deleteMgmtCmdByParentID(String parentID) throws RSCException {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		
		try {
			List<MgmtCmd> findMgmtCmdList = this.findMgmtCmd("parentID", parentID);
			for(MgmtCmd mgmtCmdItem : findMgmtCmdList) {
				this.deleteMgmtCmd(mgmtCmdItem.getResourceID());
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.mgmtcmd.delFail.text"));
		}
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd delete success");
		
	}
	
	/**
	 * mgmtCmd count retrieve
	 * @param parentID
	 * @param resourceName
	 * @return
	 */
	public long getCountByResourceName(String parentID, String resourceName){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		
		long cnt = 0;
		
		try {
			cnt = mgmtCmdDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "mgmtCmd get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * execInstance create
	 * @param url
	 * @param findMgmtCmdItem
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance createExecInstance(String url, MgmtCmd findMgmtCmdItem) throws RSCException{
		
		ExecInstance findExecInstanceItem	= null;

		try {
			
			findExecInstanceItem = execInstanceService.createExecInstance(url, findMgmtCmdItem);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] ExecInstance create " + e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.execInstance.createFail.text"));
		}
		
		return findExecInstanceItem;
		
	}
	
	/**
	 * pointOfAccess retrieve
	 * @param remoteCSEResourceID
	 * @return
	 * @throws RSCException
	 */
	public ArrayList<String[]> getPointOfAccessListByRemoteCSEResourceID(String remoteCSEResourceID) throws RSCException{
		RemoteCSE findRemoteCSEItem	= null;
		ArrayList<String[]> listAceess	= new ArrayList<String[]>();
		
		try {
			
			findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(remoteCSEResourceID);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] retrieve " + e.getMessage());
			
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.controlFail.text"));
		}
		
		if(!CommonUtil.isEmpty(findRemoteCSEItem)) {
			List<String> pointOfAccess = findRemoteCSEItem.getPointOfAccess();
			
			if(!CommonUtil.isEmpty(pointOfAccess)) {
				listAceess = this.getPointOfAccessList(pointOfAccess);
			}
			else {
				mongoLogService.log(logger, LEVEL.ERROR, "pointOfAccess " + CommonUtil.getMessage("msg.input.empty.text"));
				
				throw new RSCException(RSC.TARGET_NOT_REACHABLE, "pointOfAccess " + CommonUtil.getMessage("msg.input.empty.text"));
			}
		}
		else {
			
			throw new RSCException(RSC.NOT_FOUND, "[MgmtCmd control] findRemoteCSEItem is empty");
//			throw new IotException("600", CommonUtil.getMessage("msg.device.mgmtObj.delFail.text"));
		}
		
		//
		mongoLogService.log(logger, LEVEL.DEBUG, "[getPointOfAccessList] success");
		return listAceess;
		
	}
	
	/**
	 * pointOfAccess retrieve (non oneM2M)
	 * @param pointOfAccess
	 * @return
	 * @throws RSCException
	 */
	public ArrayList<String[]> getPointOfAccessList(String pointOfAccess) throws RSCException{
		ArrayList<String[]> listAceess	= new ArrayList<String[]>();
		
		if(!CommonUtil.isEmpty(pointOfAccess)) {

			String[] arrAccess = pointOfAccess.split(",");
			
			mongoLogService.log(logger, LEVEL.DEBUG, "###############################################");
			
			for (String accessInfo : arrAccess) {
				mongoLogService.log(logger, LEVEL.DEBUG, "@@ PointOfAccess List (accessInfo : "+accessInfo+") @@");
				
				if(accessInfo.startsWith(MGMT_ACCESS_TYPE.COAP.getValue())) {
					String[]coapServerInfo = accessInfo.split("^");
					if(!CommonUtil.isEmpty(coapServerInfo) && coapServerInfo.length >= 2) {
						accessInfo = coapServerInfo[0];
					}
				}
				listAceess.add(accessInfo.split("\\|"));
				mongoLogService.log(logger, LEVEL.DEBUG, accessInfo);
			}
			mongoLogService.log(logger, LEVEL.DEBUG, "###############################################");
			
			this.checkPointOfAccessType(listAceess);
	
		}
		else {
			mongoLogService.log(logger, LEVEL.ERROR, "The device pointOfAccess is empty.");
			
			throw new RSCException(RSC.TARGET_NOT_REACHABLE, "The device pointOfAccess is empty.");
		}
		return listAceess;
		
	}
	
	/**
	 * pointOfAccess retrieve (oneM2M)
	 * @param pointOfAccess
	 * @return
	 * @throws RSCException
	 */
	public ArrayList<String[]> getPointOfAccessList(List<String> pointOfAccess) throws RSCException{
		ArrayList<String[]> listAceess	= new ArrayList<String[]>();
		
		if(!CommonUtil.isEmpty(pointOfAccess)) {

			String[] arrAccess = new String[pointOfAccess.size()];
			arrAccess = pointOfAccess.toArray(arrAccess);
			
			//mongoLogService.log(logger, LEVEL.DEBUG, "###############################################");
			
			for (String accessInfo : arrAccess) {
				mongoLogService.log(logger, LEVEL.DEBUG, "@@ PointOfAccess List (accessInfo : "+accessInfo+") @@");
				
				if(accessInfo.startsWith(MGMT_ACCESS_TYPE.COAP.getValue())) {
					String[]coapServerInfo = accessInfo.split("^");
					if(!CommonUtil.isEmpty(coapServerInfo) && coapServerInfo.length >= 2) {
						accessInfo = coapServerInfo[0];
					}
				}
				listAceess.add(accessInfo.split("\\|"));
				mongoLogService.log(logger, LEVEL.DEBUG, accessInfo);
			}
			//mongoLogService.log(logger, LEVEL.DEBUG, "###############################################");
			
			this.checkPointOfAccessType(listAceess);
	
		}
		else {
			
			//throw new RSCException(CommonCode.RSC.BAD_REQUEST, "The device pointOfAccess is empty.");
		}
		return listAceess;
		
	}
	
	/**
	 * pointOfAccess type check
	 * @param listAceess
	 * @throws RSCException
	 */
	public void checkPointOfAccessType(List<String[]> listAceess) throws RSCException{
		
		if(CommonUtil.isEmpty(listAceess)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "The device pointOfAccess is empty.");
		}
		
		for (String[] arrAccessInfo : listAceess) {
			
			if(arrAccessInfo[0].equalsIgnoreCase(MGMT_ACCESS_TYPE.POLLING.getValue())) {
				return;
				
			} else {
				
				if(arrAccessInfo.length >= 2) {
					String accessType = arrAccessInfo[0];
					String accessInfo = arrAccessInfo[1];
					
					if(CommonUtil.isEmpty(accessType) && CommonUtil.isEmpty(accessInfo)) {
						mongoLogService.log(logger, LEVEL.ERROR, "pointOfAccess is empty.");
						
						throw new RSCException(CommonCode.RSC.BAD_REQUEST, "pointOfAccess is empty.");
					} else {
						
						boolean isCorrectAccessType = false;
						for (MGMT_ACCESS_TYPE mgmtAccessType : MGMT_ACCESS_TYPE.values()) {
							if(mgmtAccessType.getValue().equals(accessType)) {
								isCorrectAccessType = true;
								break;
							}
						}
						
						if(!isCorrectAccessType) {
							throw new RSCException(CommonCode.RSC.BAD_REQUEST, CommonUtil.getMessage("msg.device.pointOfAccess.type.nosupport", new String[]{accessType}));
						}
					}
					
				} else {
					
					//throw new IotException("600", "The device pointOfAccess is empty.");
				}					
			}
		}
	}
	
	/**
	 * mgmtCmd check
	 * @param mgmtCmdProfile
	 * @throws RSCException
	 */
	public void checkMgmtCmdValidation(MgmtCmd mgmtCmdProfile) throws RSCException{

//		Duration execFrequency 	= mgmtCmdProfile.getExecFrequency();
//		Duration execDelay 		= mgmtCmdProfile.getExecDelay();
//		BigInteger execNumber 	= mgmtCmdProfile.getExecNumber();
//		
//		if(!CommonUtil.isEmpty(execFrequency) && !execFrequency.isSet(DatatypeConstants.SECONDS)) {
//			throw new RSCException(RSC.BAD_REQUEST, "execFrequency " + CommonUtil.getMessage("msg.device.number.text"));
//		}
//		if(!CommonUtil.isEmpty(execDelay) && !execDelay.isSet(DatatypeConstants.SECONDS)) {
//			throw new RSCException(RSC.BAD_REQUEST, "execDelay " + CommonUtil.getMessage("msg.device.number.text"));
//		}
//		if(!CommonUtil.isEmpty(execNumber) && !CommonUtil.isInteger(execNumber.toString())) {
//			throw new RSCException(RSC.BAD_REQUEST, "execNumber " + CommonUtil.getMessage("msg.device.number.text"));
//		}
//		if(CommonUtil.isMinus(execFrequency.getSeconds())) {
//			throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.mgmtCmd.execNumber.max.text", new String[]{"execFrequency"}));
//		}
//		if(CommonUtil.isMinus(execDelay.getSeconds())) {
//			throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.mgmtCmd.execNumber.max.text", new String[]{"execDelay"}));
//		}
//		if(CommonUtil.isMinus(execNumber.intValue())) {
//			throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.mgmtCmd.execNumber.max.text", new String[]{"execNumber"}));
//		}
//		
//		int intExecNumber		= 0;
//		int intMaxExecNumber	= 0;
//		String maxExecNumber	= CommonUtil.getConfig("iot.mgmtCmd.execNumber.max");
//		
//		if(!CommonUtil.isEmpty(execNumber) && !CommonUtil.isEmpty(maxExecNumber)) {
//			try {
//				intExecNumber		= execNumber.intValue();
//				intMaxExecNumber	= Integer.parseInt(maxExecNumber);
//				
//				if(intExecNumber > intMaxExecNumber) {
//					throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.mgmtCmd.execNumber.max.text", new String[]{maxExecNumber}));
//				}
//				
//			} catch (NumberFormatException ne) {
//				ne.printStackTrace();
//				mongoLogService.log(logger, LEVEL.ERROR, ne.getMessage());
//			}
//		}
//		
//		boolean isExecModeTypeError = true;
//		
//		BigInteger reqExecMode = mgmtCmdProfile.getExecMode();
//		for (EXEC_MODE execMode : EXEC_MODE.values()) {
//			if(execMode.getValue().equals(reqExecMode)) {
//				isExecModeTypeError = false;
//				break;
//			}
//		}
//		
//		
//		if(isExecModeTypeError) {
//			throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.mgmtCmd.execMode.type.nosupport"));
//		}
//		
//		if(EXEC_MODE.IMMEDIATE_AND_REPEATEDLY.getValue().equals(reqExecMode)) {
//			if(CommonUtil.isEmpty(execNumber)) {
//				throw new RSCException(RSC.BAD_REQUEST, "execNumber " + CommonUtil.getMessage("msg.input.empty.text"));
//			}			
//		}
//		
//		else if(EXEC_MODE.RANDOM_ONCE.getValue().equals(reqExecMode)) {
//			if(CommonUtil.isEmpty(execDelay)) {
//				throw new RSCException(RSC.BAD_REQUEST, "execDelay " + CommonUtil.getMessage("msg.input.empty.text"));
//			}
//		}
//		
//		else if(EXEC_MODE.RANDOM_AND_REPEATEDLY.getValue().equals(reqExecMode)) {
//			if(CommonUtil.isEmpty(execFrequency)) {
//				throw new RSCException(RSC.BAD_REQUEST, "execFrequency " + CommonUtil.getMessage("msg.input.empty.text"));
//			}
//			else if(CommonUtil.isEmpty(execDelay)) {
//				throw new RSCException(RSC.BAD_REQUEST, "execDelay " + CommonUtil.getMessage("msg.input.empty.text"));
//			}
//			else if(CommonUtil.isEmpty(execNumber)) {
//				throw new RSCException(RSC.BAD_REQUEST, "execNumber " + CommonUtil.getMessage("msg.input.empty.text"));
//			}
//		}
		
	}
	
	/**
	 * device control request
	 * @param mgmtCmdProfile
	 * @param execInstanceItem
	 * @param listPointOfAceess
	 * @return
	 */
	public boolean callDevice(MgmtCmd mgmtCmdProfile, ExecInstance execInstanceItem, ArrayList<String[]> listPointOfAceess) {
		
		boolean isRequestSuccess = false;
		
		ArrayList<String[]> listAceess = listPointOfAceess;
		
		
		if(CommonUtil.isEmpty(listAceess)) {
			mongoLogService.log(logger, LEVEL.ERROR, "pointOfAccess is empty.");
			isRequestSuccess = false;
			
			return isRequestSuccess;
		}
		
		String resourceName	= mgmtCmdProfile.getResourceName();
		
		String[] arrAccessInfo = listAceess.get(0);
		String accessType = "";
		String accessInfo = "";
		String deviceId = mgmtCmdProfile.getExecTarget();
		
		if(!CommonUtil.isEmpty(arrAccessInfo) && arrAccessInfo.length >= 1) {
			accessType = arrAccessInfo[0];
			
			if(accessType.equalsIgnoreCase(MGMT_ACCESS_TYPE.POLLING.getValue())) {
				isRequestSuccess = true;
				return isRequestSuccess;
			}
			
			if(arrAccessInfo.length >= 2) {
				accessInfo = arrAccessInfo[1];
			}
		}
		
		if(MGMT_ACCESS_TYPE.MQTT.getValue().equals(accessType)) {
			isRequestSuccess = this.callMQTT(accessInfo, resourceName, execInstanceItem);
//			mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
//			mongoLogService.log(logger, LEVEL.DEBUG, " deviceId : " + deviceId);
//			mongoLogService.log(logger, LEVEL.DEBUG, " accessInfo : " + accessInfo);
//			mongoLogService.log(logger, LEVEL.DEBUG, " resourceName : " + resourceName);
//			mongoLogService.log(logger, LEVEL.DEBUG, " execInstanceItem : " + execInstanceItem.toString());
//			mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
			
			isRequestSuccess = this.callHTTP(deviceId, accessInfo, resourceName, execInstanceItem);
		}
		else if(MGMT_ACCESS_TYPE.HTTP.getValue().equals(accessType)) {
			isRequestSuccess = this.callHTTP(deviceId, accessInfo, resourceName, execInstanceItem);
		}
		else if(MGMT_ACCESS_TYPE.COAP.getValue().equals(accessType)){
			isRequestSuccess = this.callCOAP(deviceId, accessInfo, resourceName, execInstanceItem);
		}
		else {
			
			accessInfo = accessType;
			isRequestSuccess = this.callHTTP(deviceId, accessInfo, resourceName, execInstanceItem);
		}
		
		if (isRequestSuccess) {
			
			this.requestResultUpdate(execInstanceItem, null, listPointOfAceess);
			
			mongoLogService.log(logger, LEVEL.DEBUG, "[callDevice] success");
		} else {
			
			this.requestResultUpdate(execInstanceItem, EXEC_RESULT.STATUS_REQUEST_DENIED.getValue(), listPointOfAceess);
			
			mongoLogService.log(logger, LEVEL.ERROR, "[callDevice] fail");
		}
		
		return isRequestSuccess;
	}
	
	/**
	 * mgmtCmd control request
	 * @param url
	 * @param mgmtCmdProfile
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance mgmtCmdControl(String url, MgmtCmd mgmtCmdProfile) throws RSCException{
		
		String remoteCSEResourceID	= mgmtCmdProfile.getParentID();
		String mgmtCmdResourceName	= mgmtCmdProfile.getResourceName();
		
		MgmtCmd findMgmtCmdItem			= null;
		ExecInstance findExecInstanceItem	= null;
		
		findMgmtCmdItem = this.findOneMgmtCmdByResourceName(remoteCSEResourceID, mgmtCmdResourceName);
		findMgmtCmdItem.setExecReqArgs(mgmtCmdProfile.getExecReqArgs());
		
		ArrayList<String[]> listPointOfAceess = this.getPointOfAccessListByRemoteCSEResourceID(remoteCSEResourceID);
		
		findExecInstanceItem = this.createExecInstance(url, findMgmtCmdItem);
		
		boolean isRequestSuccess = this.callDevice(findMgmtCmdItem, findExecInstanceItem, listPointOfAceess);
		
		if (isRequestSuccess) {
			
			mongoLogService.log(logger, LEVEL.DEBUG, "[MgmtCmd control] success");
		} else {
			
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] fail");
		}
		
		return findExecInstanceItem;
		
	}

	/**
	 * mgmtCmd update
	 * @param mgmtCmdProfile
	 * @return
	 * @throws RSCException
	 */
	public MgmtCmd updateMgmtCmd(MgmtCmd mgmtCmdProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID 					= mgmtCmdProfile.getResourceID();
		String expirationTime				= mgmtCmdProfile.getExpirationTime();
		//List<String> accessControlPolicyIDs = mgmtCmdProfile.getAccessControlPolicyIDs();
		List<String> labels 				= mgmtCmdProfile.getLabels();
		String description 					= mgmtCmdProfile.getDescription();
		//ExecReqArgsListType execReqArgs 	= mgmtCmdProfile.getExecReqArgs();
		String execReqArgs 					= mgmtCmdProfile.getExecReqArgs();
		String execTarget					= mgmtCmdProfile.getExecTarget();
		BigInteger execMode 				= mgmtCmdProfile.getExecMode();
		Duration execFrequency 				= mgmtCmdProfile.getExecFrequency();
		Duration execDelay 					= mgmtCmdProfile.getExecDelay();
		BigInteger execNumber 				= mgmtCmdProfile.getExecNumber();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		//if(!CommonUtil.isNull(accessControlPolicyIDs))	update.set("accessControlPolicyIDs", accessControlPolicyIDs);
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(description))				update.set("description", description);
		if(!CommonUtil.isNull(execReqArgs))				update.set("execReqArgs", execReqArgs);
		if(!CommonUtil.isNull(execTarget))				update.set("execTarget", execTarget);
		if(!CommonUtil.isNull(execMode))				update.set("execMode", execMode);
		if(!CommonUtil.isNull(execFrequency))			update.set("execFrequency", execFrequency);
		if(!CommonUtil.isNull(execDelay))				update.set("execDelay", execDelay);
		if(!CommonUtil.isNull(execNumber))				update.set("execNumber", execNumber);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		this.updateMgmtCmd(query, update);
		
		return this.findOneMgmtCmd(resourceID);
	}
	
	/**
	 * mgmtCmd update
	 * @param query
	 * @param update
	 * @throws RSCException
	 */
	public void updateMgmtCmd(Query query, Update update) throws RSCException {
		
		try { 
			mgmtCmdDao.update(query, update);
			
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.mgmtcmd.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "mgmtCmd update success");
		
		MgmtCmd findCmdItem = null;
		try {
			findCmdItem = (MgmtCmd) mgmtCmdDao.find(query).get(0);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.mgmtcmd.upFail.text"));
		}
		subscriptionService.sendSubscription(findCmdItem.getParentID(), findCmdItem.getResourceID(), RESOURCE_STATUS.UPDATED, OPERATION_MONITOR.UPDATE, MgmtCmd.class, update);
		
	}
	
	/**
	 * control result update
	 * @param execInstanceItem
	 * @param requestResult
	 * @param listPointOfAceess
	 */
	private void requestResultUpdate(ExecInstance execInstanceItem, BigInteger requestResult, ArrayList<String[]> listPointOfAceess) {
		
		ArrayList<String[]> listAceess = listPointOfAceess;
		
		String[] arrAccessInfo = listAceess.get(0);
		String accessType = null;
		
		if(!CommonUtil.isEmpty(arrAccessInfo) && arrAccessInfo.length >= 1) {
			accessType = arrAccessInfo[0];
		}
		
		if(accessType.equalsIgnoreCase(MGMT_ACCESS_TYPE.POLLING.getValue())) {
			
			return;
		}
		
		if(CommonUtil.isEmpty(requestResult)) {
			
			execInstanceItem.setExecStatus(EXEC_STATUS.PENDING.getValue());
			
		} else {
			execInstanceItem.setExecStatus(EXEC_STATUS.FINISHED.getValue());

		}
		try {
			execInstanceItem.setExecResult(requestResult);
			execInstanceService.updateExecInstanceResult(execInstanceItem);
			
		} catch (RSCException e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	}
	
	/**
	 * xmlStrong to Object
	 * @param execReqArgs
	 * @param cmdType
	 * @return
	 */
	private Object getControlResourceObj(String execReqArgs, String cmdType) {
		Object resourceObj = null;
		if(!CommonUtil.isEmpty(execReqArgs) && !CommonUtil.isEmpty(cmdType)) {
			
			if(cmdType.equals(CMD_TYPE.FIRMWARE_UPGRADE.getValue())) {
				Firmware firmwareInfo = (Firmware)mCommonService.unMarshalFromXmlString(execReqArgs);
				if(!CommonUtil.isEmpty(firmwareInfo)) {
					resourceObj = firmwareInfo;
				}
				
			} else if(cmdType.equals(CMD_TYPE.DEVICE_APP_INSTALL.getValue())) {
				
				Software softwareInfo = (Software)mCommonService.unMarshalFromXmlString(execReqArgs);
				if(!CommonUtil.isEmpty(softwareInfo)) {
					resourceObj = softwareInfo;
				}
			}
			
		}
		
		return resourceObj;
	}
	
	
	/**
	 * MQTT control
	 * @param remoteCSECSEID
	 * @param resourceName
	 * @param execInstanceItem
	 * @return
	 */
	private boolean callMQTT(String remoteCSECSEID, String resourceName, ExecInstance execInstanceItem) {
		mongoLogService.log(logger, LEVEL.DEBUG, "[MgmtCmd control] MQTT control in");
		boolean isRequestSuccess = false;
		
		try {
			
			String execInstance = mCommonService.marshalToXmlString(execInstanceItem);
			
			mongoLogService.log(logger, LEVEL.INFO, "MQTT topic :: " + remoteCSECSEID + ", resourceName :: " + resourceName);
			
			MqttClientUtil mqttClient = new MqttClientUtil();
			mqttClient.publishKetiPayload(remoteCSECSEID, resourceName, execInstance);
			
			isRequestSuccess = true;
			mongoLogService.log(logger, LEVEL.INFO, "MQTT Success :: " + remoteCSECSEID);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd �젣�뼱] MQTT Exception : " + e.getMessage());
		}
		
		return isRequestSuccess;
	}
	
	/**
	 * HTTP control
	 * @param remoteCSECSEID
	 * @param deviceUri
	 * @param resourceName
	 * @param execInstanceItem
	 * @return
	 */
	private boolean callHTTP(String remoteCSECSEID, String deviceUri, String resourceName, ExecInstance execInstanceItem) {
		
		mongoLogService.log(logger, LEVEL.DEBUG, "[MgmtCmd control] HTTP control in");
		boolean isRequestSuccess = false;
		
		try {
			String strUri = null;
			if (deviceUri.contains("http")) {
				
				//deviceUri = deviceUri.replaceAll("http://", "http://61.75.50.245:9000/Mobius/remoteCSE-" + remoteCSECSEID);
				
				//strUri = deviceUri + "/mgmtCmd-" + resourceName;
				//http://01094409317/mgmtCmd-1234567893
				//strUri = "http://61.75.50.245:9000/Mobius/remoteCSE-" + remoteCSECSEID + "/mgmtCmd-" + resourceName;
				strUri = "http://61.75.50.245:9000/UNIBA/remoteCSE-" + remoteCSECSEID + "/mgmtCmd-" + resourceName;
				
			} else {
				
				//strUri = "http://" + deviceUri + "/mgmtCmd-" + resourceName;
				//strUri = "http://61.75.50.245:9000/Mobius/remoteCSE-" + remoteCSECSEID + "/mgmtCmd-" + resourceName;
				strUri = "http://61.75.50.245:9000/UNIBA/remoteCSE-" + remoteCSECSEID + "/mgmtCmd-" + resourceName;
			}
			mongoLogService.log(logger, LEVEL.INFO, "★★★★★★★★★★★★★★★★★★★★  HTTP strUri :: " + strUri);
			
			String execInstance = mCommonService.marshalToXmlString(execInstanceItem);
			
			HTTPClient httpClient = new HTTPClient();
			HashMap<String,String> headers = new HashMap<String,String>();
			headers.put("X-M2M-RI", execInstanceItem.getResourceID());
			//headers.put("X-M2M-Origin", "Mobius");
			headers.put("X-M2M-Origin", "UNIBA");
			headers.put("Content-Type", "application/vnd.onem2m-res+xml;ty=8");
			HttpResponse httpResponse = httpClient.request(RequestMethod.POST, strUri, headers, execInstance);
			
			if(httpResponse.getStatusCode() == HttpStatus.OK.value() || httpResponse.getStatusCode() == HttpStatus.CREATED.value()){
				isRequestSuccess = true;
				mongoLogService.log(logger, LEVEL.INFO, "HTTP Success :: " + strUri + ", " + HttpStatus.OK.name());
			
			} else {
				mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] HTTP fail httpStatusCode :: " + httpResponse.getStatusCode());
				mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] HTTP fail httpStatusCode :: " + HttpStatus.valueOf(httpResponse.getStatusCode()));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] HTTP Exception : " + e.getMessage());
		}
		
		return isRequestSuccess;
	}
	
	/**
	 * CoAP control
	 * @param remoteCSECSEID
	 * @param deviceUri
	 * @param resourceName
	 * @param execInstanceItem
	 * @return
	 */
	private boolean callCOAP(String remoteCSECSEID, String deviceUri, String resourceName, ExecInstance execInstanceItem) {
		
		mongoLogService.log(logger, LEVEL.DEBUG, "[MgmtCmd control] COAP control in");
		boolean isRequestSuccess = false;
		
		try {
			String[] strUriArray = deviceUri.split("\\^");
			String strUriTmp = strUriArray[0];
			
			String strUri = null;
			if (strUriTmp.contains("coap")) {
				
				strUri = strUriTmp + "/mgmtCmd-" + resourceName;
			} else {
				
				strUri = "coap://" + strUriTmp + "/mgmtCmd-" + resourceName;
			}
			mongoLogService.log(logger, LEVEL.INFO, "CoAP strUri :: " + strUri);
			
			String execInstance = mCommonService.marshalToXmlString(execInstanceItem);
			
			CoAPClient coapClient = new CoAPClient();
			HashMap<Integer,Object> headers = new HashMap<Integer,Object>();
			headers.put(new Integer(257), execInstanceItem.getResourceID());
			//headers.put(new Integer(256), "Mobius");
			headers.put(new Integer(256), "UNIBA");
			headers.put(new Integer(12), new Integer(41));
			headers.put(new Integer(267), new Integer(8));
			ResponseCode responseCode = coapClient.request(Code.POST, strUri, headers, execInstance);
			
			if(!CommonUtil.isEmpty(responseCode) && ResponseCode.isSuccess(responseCode)) {
				isRequestSuccess = true;
				mongoLogService.log(logger, LEVEL.INFO, "COAP Success :: " + strUri + ", " + responseCode.name());
			
			} else {
				mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] COAP fail");
				
				if(!CommonUtil.isEmpty(responseCode)) {
					mongoLogService.log(logger, LEVEL.ERROR, "COAP responseCode.name :: " + responseCode.name());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control] COAP Exception : " + e.getMessage());
		}
		
		return isRequestSuccess;
	}
	
	
	/**
	 * mgmtCmd control result update
	 * @param mgmtCmdInfo
	 * @param execInstanceProfile
	 * @param execReqArgs
	 * @return
	 * @throws RSCException
	 */
	public ExecInstance mgmtCmdResultUpdate(MgmtCmd mgmtCmdInfo, ExecInstance execInstanceProfile, String execReqArgs) throws RSCException {

		ExecInstance findExecInstanceItem	= null;
		
		BigInteger execResult = execInstanceProfile.getExecResult();
		String execInstanceID = execInstanceProfile.getResourceID();
		String cmdType = mgmtCmdInfo.getCmdType();
		
		try {
			execInstanceProfile.setExecStatus(EXEC_STATUS.FINISHED.getValue());
			
			findExecInstanceItem = execInstanceService.updateExecInstanceResult(execInstanceProfile);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, "[MgmtCmd control result update] ExecInstance create " + e.getMessage());
			
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.controlFail.text"));
		}
		
		subscriptionService.sendSubscription(findExecInstanceItem.getParentID(), findExecInstanceItem.getResourceID(), RESOURCE_STATUS.CHILD_UPDATED, OPERATION_MONITOR.UPDATE, ExecInstance.class, findExecInstanceItem);
		
		mongoLogService.log(logger, LEVEL.DEBUG, "[MgmtCmd control result update] success");
		
		return findExecInstanceItem;
		
	}
}
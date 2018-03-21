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
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.oneM2M.NodeDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * node Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class NodeService {

	private static final Log logger = LogFactory.getLog(NodeService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private FirmwareService firmwareService;	
	
	@Autowired
	private SoftwareService softwareService;	
	
	@Autowired
	private DeviceInfoService deviceInfoService;	

	@Autowired
	private MemoryService memoryService;	
	
	@Autowired
	private BatteryService batteryService;	
	
	@Autowired
	private RebootService rebootService;	
	
	@Autowired
	private NodeDao nodeDao;
	
	@Autowired
	private SequenceDao seqDao;
	
	
	/**
	 * node retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public Node findOneNode(String resourceID) throws RSCException {

		Node findNodeItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {

			findNodeItem = (Node) nodeDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}

		return findNodeItem;
	}
	
	/**
	 * node retrieve
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public Node findOneNodeByResourceName(String parentID, String resourceName) throws RSCException {

		Node findNodeItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("parentID").is(parentID));
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {

			findNodeItem = (Node) nodeDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}

		return findNodeItem;
	}
	
	/**
	 * node retrieve
	 * @param resourceID
	 * @return
	 * @throws RSCException
	 */
	public List<Node> findNode(String key, String value) throws RSCException {

		List<Node> findNodeList = null;

		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));

		try {

			findNodeList = (List<Node>) nodeDao.find(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}

		return findNodeList;
	}
	
	/**
	 * node retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Node> findNode(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Node>();

		List<Node> findNodeList = null;
		List<Node> findNodeNoFilterResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));

		try {
			findNodeList = (List<Node>) nodeDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findNodeList);
			
			if (filterCriteria.isFilterCriteria()) findNodeNoFilterResourceRefList = findNodeResourceRef(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria()) {
			for (int i=0; i<findNodeNoFilterResourceRefList.size(); i++) {
				Node noFilterNode = findNodeNoFilterResourceRefList.get(i);
				boolean isResourceID = false;
				
				for (int t=0; t<findNodeList.size(); t++) {
					Node node = findNodeList.get(t);
					if (noFilterNode.getResourceRef().getResourceID().equals(node.getResourceRef().getResourceID())) isResourceID = true;
				}
				
				if (!isResourceID) findNodeList.add(noFilterNode);
			}
		}

		return findNodeList;
	}
	
	/**
	 * node References retrieve
	 * @param key
	 * @param value
	 * @return
	 * @throws RSCException
	 */
	public List<Node> findNodeResourceRef(String key, String value) throws RSCException {
		
		String includeField = "resourceRef";
		List<Node> findNodeResourceRefList = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);
		
		try {
			findNodeResourceRefList = (List<Node>) nodeDao.find(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}
		
		return findNodeResourceRefList;
	}
	
	/**
	 * node References retrieve
	 * @param key
	 * @param value
	 * @param requestFilterCriteria
	 * @return
	 * @throws RSCException
	 */
	public List<Node> findNodeResourceRef(String key, String value, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return new ArrayList<Node>();

		String includeField = "resourceRef";
		List<Node> findNodeResourceRefList = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where(key).is(value));
		query.fields().include(includeField);

		try {
			findNodeResourceRefList = (List<Node>) nodeDao.find(query);
			mCommonService.setLimitForFilterCriteria(filterCriteria, findNodeResourceRefList);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}
		
		return findNodeResourceRefList;
	}
	
	/**
	 * node retrieve
	 * @param remoteCSEResourceID
	 * @return
	 * @throws RSCException
	 */
	public Node findOneNodeByHostedCSELink(String resourceID) throws RSCException {

		Node findNodeItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));

		try {

			findNodeItem = (Node) nodeDao.findOne(query);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.findFail.text"));
		}

		return findNodeItem;
	}
	
	/**
	 * node count retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCount(String resourceID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceID").is(resourceID));
		
		long cnt = 0;
		
		try {
			cnt = nodeDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * node count retrieve
	 * @param resourceID
	 * @return
	 */
	public long getCountByNodeID(String nodeID){
		
		Query query = new Query();
		query.addCriteria(Criteria.where("nodeID").is(nodeID));
		
		long cnt = 0;
		
		try {
			cnt = nodeDao.count(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node get count");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		return cnt;
	}
	
	/**
	 * node create
	 * @param url
	 * @param remoteCSEVOProfile
	 * @return
	 * @throws RSCException
	 */
	public Node createNode(String url, RemoteCSE remoteCSEProfile) throws RSCException {
		
		Node rtnNode = null;
		Node nodeItem = new Node();
		
		String currentTime = CommonUtil.getNowTimestamp();
		Long seqID = seqDao.move(MovType.UP, SeqType.NODE);
		String nodeID = remoteCSEProfile.getCSEID();
		
		String expirationTime = remoteCSEProfile.getExpirationTime();
		
		nodeItem.setResourceType(RESOURCE_TYPE.NODE.getValue());
		nodeItem.setResourceID(CommonUtil.seqIDToResourceID(SEQ_PREFIX.NODE.getValue(), seqID));
		nodeItem.setResourceName(remoteCSEProfile.getResourceName());
		nodeItem.setParentID(remoteCSEProfile.getParentID());
		nodeItem.setExpirationTime(expirationTime);
		nodeItem.setExpirationDate(CommonUtil.timestampToDate(expirationTime));
		nodeItem.getAccessControlPolicyIDs().addAll(remoteCSEProfile.getAccessControlPolicyIDs());
		nodeItem.setCreationTime(currentTime);
		nodeItem.setLastModifiedTime(currentTime);
		nodeItem.getLabels().addAll(remoteCSEProfile.getLabels());
		nodeItem.getAnnounceTo().addAll(remoteCSEProfile.getAnnounceTo());
		nodeItem.getAnnouncedAttribute().addAll(remoteCSEProfile.getAnnouncedAttribute());
		nodeItem.setNodeID(nodeID);
		nodeItem.setHostedCSELink(remoteCSEProfile.getResourceID());
		nodeItem.setResourceRef(new ResourceRef(mCommonService.getContentLocation(url, nodeItem), nodeItem.getResourceName(), CommonCode.RESOURCE_TYPE.NODE.getValue(), nodeItem.getResourceID(), null));
		
		mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		mongoLogService.log(logger, LEVEL.DEBUG, "[ nodeVO data ]");
		mongoLogService.log(logger, LEVEL.DEBUG, nodeItem.toString());
		mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		try {
			
			nodeDao.insert(nodeItem);
			
			rtnNode = this.findOneNode(nodeItem.getResourceID());
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR,CommonUtil.getMessage("msg.device.node.createFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "node create success");

		return rtnNode;
	}
	
	/**
	 * node delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteNode(String resourceID) throws RSCException {
		
		Query query = new Query(Criteria.where("resourceID").is(resourceID));
		
		try {
			nodeDao.remove(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "node delete success");
	}
	
	/**
	 * node delete
	 * @param resourceID
	 * @throws RSCException
	 */
	public void deleteNodeChild(String resourceID) throws RSCException {
		
		try {
			firmwareService.deleteFirmware("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.firmware.delFail.text"));
		}
		
		try {
			softwareService.deleteSoftware("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.software.delFail.text"));
		}
		
		try {
			deviceInfoService.deleteDeviceInfo("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.deviceInfo.delFail.text"));
		}
		
		
		try {
			memoryService.deleteMemory("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.memory.delFail.text"));
		}
		
		try {
			batteryService.deleteBattery("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.battery.delFail.text"));
		}
		
		try {
			rebootService.deleteReboot("parentID", resourceID);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.reboot.delFail.text"));
		}
		
		
		Query query = new Query(Criteria.where("resourceID").is(resourceID));
		
		try {
			nodeDao.remove(query);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node remove");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.delFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "node delete success");
	}
	
	/**
	 * node update
	 * @param remoteCSEProfile
	 * @throws RSCException
	 */
	public void updateNode(RemoteCSE remoteCSEProfile) throws RSCException {
		
		String currentTime 					= CommonUtil.getNowTimestamp();
		
		String resourceID 					= remoteCSEProfile.getNodeLink();
		String expirationTime				= remoteCSEProfile.getExpirationTime();
		List<String> labels 				= remoteCSEProfile.getLabels();
		List<String> announceTo 			= remoteCSEProfile.getAnnounceTo();
		List<String> announcedAttribute		= remoteCSEProfile.getAnnouncedAttribute();
		
		Update update = new Update();
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationTime", expirationTime);
		if(!CommonUtil.isNull(expirationTime))			update.set("expirationDate", CommonUtil.timestampToDate(expirationTime));
		if(!CommonUtil.isNull(labels))					update.set("labels", labels);
		if(!CommonUtil.isNull(announceTo))				update.set("announceTo", announceTo);
		if(!CommonUtil.isNull(announcedAttribute))		update.set("announcedAttribute", announcedAttribute);
														update.set("lastModifiedTime", currentTime);
		
		Query query = new Query(Criteria.where("resourceID").is(resourceID));
		
		try {
			nodeDao.update(query, update);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, "node update");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.node.upFail.text"));
		}
		
		mongoLogService.log(logger, LEVEL.DEBUG, "node update success");
	}
	
	
	
}
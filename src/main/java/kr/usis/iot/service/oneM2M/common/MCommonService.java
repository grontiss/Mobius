/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.service.oneM2M.common;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import kr.usis.iot.domain.common.CertKeyVO;
import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.AE;
import kr.usis.iot.domain.oneM2M.Attribute;
import kr.usis.iot.domain.oneM2M.Battery;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.ChildResourceRef;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.domain.oneM2M.DeviceInfo;
import kr.usis.iot.domain.oneM2M.EventNotificationCriteria;
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Firmware;
import kr.usis.iot.domain.oneM2M.Group;
import kr.usis.iot.domain.oneM2M.LocationPolicy;
import kr.usis.iot.domain.oneM2M.Memory;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.MgmtResource;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.Reboot;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.Resource;
import kr.usis.iot.domain.oneM2M.ResourceRef;
import kr.usis.iot.domain.oneM2M.Software;
import kr.usis.iot.domain.oneM2M.Subscription;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.mdao.oneM2M.AEDao;
import kr.usis.iot.mdao.oneM2M.AccessControlPolicyDao;
import kr.usis.iot.mdao.oneM2M.RemoteCSEDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.BatteryService;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.ContentInstanceService;
import kr.usis.iot.service.oneM2M.DeviceInfoService;
import kr.usis.iot.service.oneM2M.ExecInstanceService;
import kr.usis.iot.service.oneM2M.FirmwareService;
import kr.usis.iot.service.oneM2M.GroupService;
import kr.usis.iot.service.oneM2M.LocationPolicyService;
import kr.usis.iot.service.oneM2M.MemoryService;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
import kr.usis.iot.service.oneM2M.NodeService;
import kr.usis.iot.service.oneM2M.RebootService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.SoftwareService;
import kr.usis.iot.service.oneM2M.SubscriptionService;
import kr.usis.iot.util.CertUtil;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;
import kr.usis.iot.util.oneM2M.MediaTypeOneM2M;
import kr.usis.iot.util.oneM2M.CommonCode.FILTER_USAGE;
import kr.usis.iot.util.oneM2M.CommonCode.MGMT_DEFINITION;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RESULT_CONTENT;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;
import kr.usis.iot.util.oneM2M.CommonCode.SHORT_NAMES;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.ntels.nisf.util.PropertiesUtil;

import biz.source_code.base64Coder.Base64Coder;

/**
 * MCommon management Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class MCommonService {

	private static final Log logger = LogFactory.getLog(MCommonService.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private AEService aEService;
	
	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private ContentInstanceService contentInstanceService;
	
	@Autowired
	private LocationPolicyService locationPolicyService;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private NodeService nodeService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private SubscriptionService subscriptionService;
	
	@Autowired
	private ExecInstanceService execInstanceService;
	
	@Autowired
	private FirmwareService firmwareService;
	
	@Autowired
	private SoftwareService softwareService;
	
	@Autowired
	private DeviceInfoService deviceInfoService;
	
	@Autowired
	private BatteryService batteryService;
	
	@Autowired
	private MemoryService memoryService;
	
	@Autowired
	private RebootService rebootService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private RemoteCSEDao remoteCSEDao;
	
	@Autowired
	private AEDao aeDao;
	
	@Autowired
	private AccessControlPolicyDao accessControlPolicyDao;
	
	@Autowired
	private CertUtil certUtil;
	
	static final JAXBContext context = initContext();   
	
    private static JAXBContext initContext() {     
    	JAXBContext jAXBContext = null;
        try {
        	jAXBContext = JAXBContext.newInstance(CSEBase.class,AE.class,Battery.class,Container.class,ContentInstance.class,DeviceInfo.class,Firmware.class,Group.class,LocationPolicy.class,Memory.class,MgmtCmd.class,Reboot.class,RemoteCSE.class,Software.class,Subscription.class,URIList.class);
		} catch (JAXBException e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
        
        return jAXBContext;
    }
	
	/**
	 * verify dKey
	 * @param checkMccP
	 * @param remoteCSEResourceID
	 * @param dKey
	 * @throws RSCException
	 */
	public void verifyDKey(boolean checkMccP, String remoteCSEResourceID, String dKey) throws RSCException {
		this.verifyDKey(checkMccP, remoteCSEResourceID, dKey, null);
	}
	
	/**
	 * verify aKey
	 * @param checkMccP
	 * @param aeResourceID
	 * @param aKey
	 * @throws RSCException
	 */
	public void verifyAKey(boolean checkMccP, String aeResourceID, String aKey) throws RSCException {
		this.verifyAKey(checkMccP, aeResourceID, aKey, null);
	}
	
	
	
	/**
	 * verify dKey
	 * @param checkMccP
	 * @param remoteCSEResourceID
	 * @param dKey
	 * @param nodeResourceID
	 * @throws RSCException
	 */
	public void verifyDKey(boolean checkMccP, String remoteCSEResourceID, String dKey, String nodeResourceID) throws RSCException {
		if (checkMccP) return;
		
		if(CommonUtil.isEmpty(dKey)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "dKey " + CommonUtil.getMessage("msg.input.empty.text"));
			
		} else if(CommonUtil.isEmpty(remoteCSEResourceID) && CommonUtil.isEmpty(nodeResourceID)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "remoteCSEResourceID or nodeResourceID " + CommonUtil.getMessage("msg.input.empty.text"));
			
		}
		
		try{
			mongoLogService.log(logger, LEVEL.DEBUG, "dKey decode before	: " + dKey);
			dKey = Base64Coder.decodeString(dKey);
			mongoLogService.log(logger, LEVEL.DEBUG, "dKey decode after		: " + dKey);
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage(), e);
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "dKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
		}		
		
		Query query = new Query();
		query.addCriteria(Criteria.where("dKey").is(dKey));
		
		if(!CommonUtil.isEmpty(remoteCSEResourceID))	query.addCriteria(Criteria.where("resourceID").is(remoteCSEResourceID));
		if(!CommonUtil.isEmpty(nodeResourceID))			query.addCriteria(Criteria.where("nodeLink").is(nodeResourceID));
		
		RemoteCSE remoteCSEVO = null;
		
		try {
			remoteCSEVO = (RemoteCSE)remoteCSEDao.findOne(query);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.dKey.verifyFail.text"));
		}
		
		if (CommonUtil.isEmpty(remoteCSEVO)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.dKey.verifyFail.text"));
		}
	}
	
	/**
	 * verify aKey
	 * @param checkMccP
	 * @param aeResourceID
	 * @param aKey
	 * @param nodeResourceID
	 * @throws RSCException
	 */
	public void verifyAKey(boolean checkMccP, String aeResourceID, String aKey, String nodeResourceID) throws RSCException {
		if (checkMccP) return;
		
		if(CommonUtil.isEmpty(aKey)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "aKey " + CommonUtil.getMessage("msg.input.empty.text"));
			
		} else if(CommonUtil.isEmpty(aeResourceID) && CommonUtil.isEmpty(nodeResourceID)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "aeResourceID " + CommonUtil.getMessage("msg.input.empty.text"));
			
		}
		
		try{
			mongoLogService.log(logger, LEVEL.DEBUG, "aKey decode before	: " + aKey);
			aKey = Base64Coder.decodeString(aKey);
			mongoLogService.log(logger, LEVEL.DEBUG, "aKey decode after		: " + aKey);
			
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage(), e);
			throw new RSCException(CommonCode.RSC.BAD_REQUEST, "aKey " + CommonUtil.getMessage("msg.base64.decodeFail.text"));
		}		
		
		Query query = new Query();
		query.addCriteria(Criteria.where("aKey").is(aKey));
		
		if(!CommonUtil.isEmpty(aeResourceID))	query.addCriteria(Criteria.where("resourceID").is(aeResourceID));
		
		AE aeVO = null;
		
		try {
			aeVO = (AE)aeDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.device.aKey.verifyFail.text"));
		}
		
		if (CommonUtil.isEmpty(aeVO)) {
			throw new RSCException(CommonCode.RSC.BAD_REQUEST,CommonUtil.getMessage("msg.device.aKey.verifyFail.text"));
		}
	}
	
	/**
	 * patten check CSE-ID (device ID)
	 * @param checkMccP
	 * @param remoteCSEResourceName
	 * @throws RSCException
	 */
	public void checkPatternRemoteCSECSEID(boolean checkMccP, String remoteCSECSEID) throws RSCException {
		if (checkMccP) return;
		
		boolean isError = false;
		CommonCode.RSC responseCode	= CommonCode.RSC.EMPTY;
		String responseMsg	= "";
		
		String[] arrRemoteCSECSEID = remoteCSECSEID.split("\\.");
		int deviceIdLen = arrRemoteCSECSEID.length;
		if(deviceIdLen < 7 || deviceIdLen > 11){
			isError			= true;
			responseCode	= CommonCode.RSC.BAD_REQUEST;
			responseMsg		= CommonUtil.getMessage("device.error.notformat.deviceId");
		}
		
		else if(arrRemoteCSECSEID[0].length() !=1 || arrRemoteCSECSEID[1].length() !=1 || arrRemoteCSECSEID[2].length() !=3 || arrRemoteCSECSEID[3].length() !=1 ){
			isError			= true;
			responseCode	= CommonCode.RSC.BAD_REQUEST;
			responseMsg		= CommonUtil.getMessage("device.error.notformat.deviceId");
		}
		
		if(isError){
			throw new RSCException(responseCode, responseMsg);
		}
	}
	
	/**
	 * patten check app-ID
	 * @param checkMccP
	 * @param aeResourceName
	 * @throws RSCException
	 */
	public void checkPatternAEAppID(boolean checkMccP, String aeAppID) throws RSCException {
		if (checkMccP) return;
		
		boolean isError = false;
		CommonCode.RSC responseCode	= CommonCode.RSC.EMPTY;
		String responseMsg	= "";
		
		String[] arrAEAppID = aeAppID.split("\\.");
		int appIdLen = arrAEAppID.length;
		if(appIdLen < 7 || appIdLen > 11){
			isError			= true;
			responseCode	= CommonCode.RSC.BAD_REQUEST;
			responseMsg		= CommonUtil.getMessage("device.error.notformat.appId");
		}
		
		else if(arrAEAppID[0].length() !=1 || arrAEAppID[1].length() !=1 || arrAEAppID[2].length() !=3 || arrAEAppID[3].length() !=1 ){
			isError			= true;
			responseCode	= CommonCode.RSC.BAD_REQUEST;
			responseMsg		= CommonUtil.getMessage("device.error.notformat.appId");
		}
		
		if(isError){
			throw new RSCException(responseCode, responseMsg);
		}

	}
	
	/**
	 * create dKey
	 * @param checkMccP
	 * @param remoteCSEResourceID
	 * @return
	 * @throws RSCException
	 */
	public String createDeviceKey(boolean checkMccP, String remoteCSEResourceID) throws RSCException {
		if (checkMccP) return null;
		
		CertKeyVO certKeyVO = null;
		String dKey = "";
		
		certKeyVO	= certUtil.createDeviceKey(remoteCSEResourceID);
		dKey		= certKeyVO.getCert_client_id();
		
		if(!"200".equals(certKeyVO.getResult_code())) {
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, certKeyVO.getResult_msg());
		}
		else if(CommonUtil.isEmpty(dKey)) {
			throw new RSCException(RSC.BAD_REQUEST, "[create dKey Exception] " + certKeyVO.getResult_msg());
		}
		
		return dKey;
	}
	
	/**
	 * create aKey
	 * @param checkMccP
	 * @param appID
	 * @return
	 * @throws RSCException
	 */
	public String createAKey(boolean checkMccP, String aeID) throws RSCException {
		if (checkMccP) return null;
		
		CertKeyVO certKeyVO = null;
		String aKey = "";
		
		certKeyVO	= certUtil.createDeviceKey(aeID);
		aKey		= certKeyVO.getCert_client_id();
		
		if(!"200".equals(certKeyVO.getResult_code())) {
			throw new RSCException(RSC.INTERNAL_SERVER_ERROR, certKeyVO.getResult_msg());
		}
		else if(CommonUtil.isEmpty(aKey)) {
			throw new RSCException(RSC.BAD_REQUEST, "[create aKey Exception] " + certKeyVO.getResult_msg());
		}
		
		return aKey;
	}
	
	/**
	 * set response header
	 * @param request
	 * @param responseHeaders
	 * @param resObj
	 * @param resTxt
	 * @param rsc
	 * @param rsm
	 */
	public void setResponseHeader(HttpServletRequest request, HttpHeaders responseHeaders, Object resObj, String resTxt, RSC rsc, String rsm) {
		try {
			
			int iContentLength 		= 0;
			String contentLocation 	= null;
			String contentLength	= null;
			String accept 			= CommonUtil.nvl(request.getHeader("Accept"), "");
			String xM2MRI 			= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "") + UUID.randomUUID().toString();
			
			BigInteger filterUsage	= new BigInteger(CommonUtil.nvl(request.getParameter("fu"), "-1"));
			BigInteger resultContent= new BigInteger(CommonUtil.nvl(request.getParameter("rcn"), "-1"));
			String dKey				= null;
			String aKey				= null;
			
			switch(request.getMethod()) {
			case "POST":
				String url = CommonUtil.getContextPathInfoURL(request);
				contentLocation = getContentLocation(url, resObj);
				
				if (!CommonUtil.isEmpty(resObj)) {
					RESOURCE_TYPE resourceType = RESOURCE_TYPE.getThis(((Resource)resObj).getResourceType());
					switch(resourceType) {
					case REMOTE_CSE:
						dKey = ((RemoteCSE)resObj).getDKey();
						break;
					case AE:
						aKey = ((AE)resObj).getAKey();
						break;
					default:
						break;
					}
				}
				break;
			default:
				break;
			}
			
			if (!CommonUtil.isEmpty(resTxt)) iContentLength = resTxt.getBytes("UTF-8").length;
			if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(resultContent)) {
				contentLength = Integer.toString(0);
			} else {
				contentLength = Integer.toString(iContentLength > 0 ? iContentLength : 0);
			}
			
			if (FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterUsage)
			 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(resultContent)
			 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(resultContent)
			 || RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(resultContent)
			 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(resultContent)) {
				
				if (accept.contains("json")) responseHeaders.setContentType(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_PRSP_JSON);
				else 						 responseHeaders.setContentType(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_PRSP_XML);
				
			} else {
				
				if (accept.contains("json")) responseHeaders.setContentType(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_RES_JSON);
				else 						 responseHeaders.setContentType(MediaTypeOneM2M.APPLICATION_VND_ONEM2M_RES_XML);
				
			}
			
			if (!CommonUtil.isEmpty(xM2MRI)) 			responseHeaders.set("X-M2M-RI", xM2MRI);
			if (!CommonUtil.isEmpty(rsc)) 				responseHeaders.set("X-M2M-RSC", rsc.getValue());
			if (!CommonUtil.isEmpty(rsm)) 				responseHeaders.set("X-M2M-RSM", rsm);
			if (!CommonUtil.isEmpty(contentLocation)) 	responseHeaders.set("Content-Location", contentLocation);
			if (!CommonUtil.isEmpty(contentLength)) 	responseHeaders.setContentLength(Long.parseLong(contentLength));
			if (!CommonUtil.isEmpty(dKey)) 				responseHeaders.set("dKey", dKey);
			if (!CommonUtil.isEmpty(aKey)) 				responseHeaders.set("aKey", aKey);
			
			logger.debug("***************************************************************************");
			logger.debug("X-M2M-RI         : " + xM2MRI);
			logger.debug("X-M2M-RSC        : " + rsc.getValue());
			logger.debug("X-M2M-RSM        : " + rsm);
			logger.debug("Content-Location : " + contentLocation);
			logger.debug("Content-Length   : " + contentLength);
			logger.debug("Content-Type     : " + responseHeaders.getContentType());
			logger.debug("dKey             : " + dKey);
			logger.debug("aKey             : " + aKey);
			logger.debug("***************************************************************************");
			
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * Content-Location retrieve
	 * @param request
	 * @param obj
	 * @return
	 */
	public String getContentLocation(String url, Object obj) {
		String contentLocation = null;
		
		if (!CommonUtil.isEmpty(obj)) {
			
			RESOURCE_TYPE resourceType = RESOURCE_TYPE.getThis(((Resource)obj).getResourceType());
			String resourceName = ((Resource)obj).getResourceName();
			
			switch(resourceType) {
			case REMOTE_CSE:
				contentLocation = url + "/" + RESOURCE_TYPE.REMOTE_CSE.getName() + "-" + resourceName;
				break;
			case AE:
				contentLocation = url + "/" + RESOURCE_TYPE.AE.getName() + "-" + resourceName;
				break;
			case CONTAINER:
				contentLocation = url + "/" + RESOURCE_TYPE.CONTAINER.getName() + "-" + resourceName;
				break;
			case CONTENT_INSTANCE:
				contentLocation = url + "/" + RESOURCE_TYPE.CONTENT_INSTANCE.getName() + "-" + resourceName;
				break;
			case SUBSCRIPTION:
				contentLocation = url + "/" + RESOURCE_TYPE.SUBSCRIPTION.getName() + "-" + resourceName;
				break;
			case LOCATION_POLICY:
				contentLocation = url + "/" + RESOURCE_TYPE.LOCATION_POLICY.getName() + "-" + resourceName;
				break;
			case GROUP:
				contentLocation = url + "/" + RESOURCE_TYPE.REAL_GROUP.getName() + "-" + resourceName;
				break;
			case MGMT_CMD:
				contentLocation = url + "/" + RESOURCE_TYPE.MGMT_CMD.getName() + "-" + resourceName;
				break;
			case EXEC_INSTANCE:
				contentLocation = url + "/" + RESOURCE_TYPE.EXEC_INSTANCE.getName() + "-" + resourceName;
				break;
			case NODE:
				contentLocation = url + "/" + RESOURCE_TYPE.NODE.getName() + "-" + resourceName;
				break;
			case MGMT_OBJ:
				
				try {
					MgmtResource mgmtResource = (MgmtResource)obj;
					
					if (!CommonUtil.isEmpty(mgmtResource.getMgmtDefinition())) {
						MGMT_DEFINITION mgmtDefinition = MGMT_DEFINITION.getThis(mgmtResource.getMgmtDefinition());
						
						switch(mgmtDefinition) {
						case FIRMWARE:
							contentLocation = url + "/" + MGMT_DEFINITION.FIRMWARE.getName() + "-" + resourceName;
							break;
						case SOFTWARE:
							contentLocation = url + "/" + MGMT_DEFINITION.SOFTWARE.getName() + "-" + resourceName;
							break;
						case MEMORY:
							contentLocation = url + "/" + MGMT_DEFINITION.MEMORY.getName() + "-" + resourceName;
							break;
						case BATTERY:
							contentLocation = url + "/" + MGMT_DEFINITION.BATTERY.getName() + "-" + resourceName;
							break;
						case DEVICE_INFO:
							contentLocation = url + "/" + MGMT_DEFINITION.DEVICE_INFO.getName() + "-" + resourceName;
							break;
						case REBOOT:
							contentLocation = url + "/" + MGMT_DEFINITION.REBOOT.getName() + "-" + resourceName;
							break;
						default:
							break;
						}
					}
				} catch (Exception e) {
				}
				
				break;
			default:
				break;
			}
		}
		
		return contentLocation;
	}
	
	/**
	 * check mcc'
	 * @param from
	 * @return
	 */
	public boolean getCheckMccP(String from) {
		String mccPCheck = PropertiesUtil.get("config", "mp.oneM2M.mccPCheck");
		if (!CommonUtil.isEmpty(mccPCheck) && "N".equals(mccPCheck)) return true;
		
		if (from == null || "".equals(from)) return false;
		
		boolean checkMccP = false;
		String[] mccPList = PropertiesUtil.get("config", "mp.oneM2M.mccPList").split("\\|");
		
		for (int i=0; i<mccPList.length; i++) {
			String mccP = mccPList[i];
			if (from.equals(mccP)) {
				checkMccP = true;
				return checkMccP;
			}
		}
		
		return checkMccP;
	}
	
	/**
	 * RSC to HttpStatus
	 * @param rsc
	 * @return
	 */
	public HttpStatus RSC2HttpStatus(RSC rsc) {
		HttpStatus httpStatus = null;
		
		switch (rsc) {
		case ACCEPTED:
			httpStatus = HttpStatus.ACCEPTED;
			break;
		case OK:
		case DELETED:
		case CHANGED:
			httpStatus = HttpStatus.OK;
			break;
		case CREATED:
			httpStatus = HttpStatus.CREATED;
			break;
		case CONFLICT:
		case GROUP_REQUEST_IDENTIFIER_EXISTS:
			httpStatus = HttpStatus.CONFLICT;
			break;
		case BAD_REQUEST:
		case CONTENTS_UNACCEPTABLE:
		case MAX_NUMBERF_OF_MEMBER_EXCEEDED:
		case MEMBER_TYPE_INCONSISTENT:
		case INVALID_CMDTYPE:
		case INVALID_ARGUMENTS:
		case INSUFFICIENT_ARGUMENTS:
		case ALREADY_COMPLETE:
		case COMMAND_NOT_CANCELLABLE:
			httpStatus = HttpStatus.BAD_REQUEST;
			break;
		case NOT_FOUND:
		case TARGET_NOT_REACHABLE:
		case EXTENAL_OBJECT_NOT_REACHABLE:
		case EXTENAL_OBJECT_NOT_FOUND:
			httpStatus = HttpStatus.NOT_FOUND;
			break;
		case OPERATION_NOT_ALLOWED:
			httpStatus = HttpStatus.METHOD_NOT_ALLOWED;
			break;
		case REQUEST_TIMEOUT:
			httpStatus = HttpStatus.REQUEST_TIMEOUT;
			break;
		case SUBSCRIPTION_CREATOR_HAS_NO_PRIVILEGE:
		case ACCESS_DENIED:
		case NO_PRIVILEGE:
		case ALREADY_EXISTS:
		case TARGET_NOT_SUBSCRIBABLE:
		case SUBSCRIPTION_HOST_HAS_NO_PRIVILEGE:
			httpStatus = HttpStatus.FORBIDDEN;
			break;
		case INTERNAL_SERVER_ERROR:
		case SUBSCRIPTION_VERIFICATION_INITIATION_FAILED:
		case MGMT_SESSION_CANNOT_BE_ESTABLISHED:
		case MGMT_SESSION_ESTABLISHMENT_TIMEOUT:
		case MGMT_CONVERSION_ERROR:
		case MGMT_CANCELATION_FAILURE:
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			break;
		case NOT_IMPLEMENTED:
		case NON_BLOCKING_REQUEST_NOT_SUPPORTED:
			httpStatus = HttpStatus.NOT_IMPLEMENTED;
			break;
		default:
			httpStatus = HttpStatus.OK;
			break;
		}
		
		return httpStatus;
	}
	
    /**
     * requestFilterCriteriaToQuery
     * @param filterCriteria
     * @return
     */
    public Query requestFilterCriteriaToQuery(FilterCriteria filterCriteria) {
    	Query query = new Query();
    	
    	if (		!CommonUtil.isEmpty(filterCriteria.getCrb()) && !CommonUtil.isEmpty(filterCriteria.getCra())) {
    		query.addCriteria(Criteria.where("creationTime").lt(filterCriteria.getCrb()).gt(filterCriteria.getCra()));
    	} else if (	!CommonUtil.isEmpty(filterCriteria.getCrb()) &&  CommonUtil.isEmpty(filterCriteria.getCra())) {
    		query.addCriteria(Criteria.where("creationTime").lt(filterCriteria.getCrb()));
    	} else if (	 CommonUtil.isEmpty(filterCriteria.getCrb()) && !CommonUtil.isEmpty(filterCriteria.getCra())) {
    		query.addCriteria(Criteria.where("creationTime").gt(filterCriteria.getCra()));
    	}
    	
    	if (		!CommonUtil.isEmpty(filterCriteria.getMs()) && !CommonUtil.isEmpty(filterCriteria.getUs())) {
    		query.addCriteria(Criteria.where("lastModifiedTime").gt(filterCriteria.getMs()).lt(filterCriteria.getUs()));
    	} else if (	!CommonUtil.isEmpty(filterCriteria.getMs()) &&  CommonUtil.isEmpty(filterCriteria.getUs())) {
    		query.addCriteria(Criteria.where("lastModifiedTime").gt(filterCriteria.getMs()));
    	} else if (	 CommonUtil.isEmpty(filterCriteria.getMs()) && !CommonUtil.isEmpty(filterCriteria.getUs())) {
    		query.addCriteria(Criteria.where("lastModifiedTime").lt(filterCriteria.getUs()));
    	}
    	
    	if (		!CommonUtil.isEmpty(filterCriteria.getSts()) && !CommonUtil.isEmpty(filterCriteria.getStb())) {
    		query.addCriteria(Criteria.where("stateTag").lt(filterCriteria.getSts()).gt(filterCriteria.getStb()));
    	} else if (	!CommonUtil.isEmpty(filterCriteria.getSts()) &&  CommonUtil.isEmpty(filterCriteria.getStb())) {
    		query.addCriteria(Criteria.where("stateTag").lt(filterCriteria.getSts()));
    	} else if (	 CommonUtil.isEmpty(filterCriteria.getSts()) && !CommonUtil.isEmpty(filterCriteria.getStb())) {
    		query.addCriteria(Criteria.where("stateTag").gt(filterCriteria.getStb()));
    	}
    	
    	if (		!CommonUtil.isEmpty(filterCriteria.getExb()) && !CommonUtil.isEmpty(filterCriteria.getExa())) {
    		query.addCriteria(Criteria.where("expirationTime").lt(filterCriteria.getExb()).gt(filterCriteria.getExa()));
    	} else if (	!CommonUtil.isEmpty(filterCriteria.getExb()) && CommonUtil.isEmpty(filterCriteria.getExa())) {
    		query.addCriteria(Criteria.where("expirationTime").lt(filterCriteria.getExb()));
    	} else if (	 CommonUtil.isEmpty(filterCriteria.getExb()) && !CommonUtil.isEmpty(filterCriteria.getExa())) {
    		query.addCriteria(Criteria.where("expirationTime").gt(filterCriteria.getExa()));
    	}

    	if (!CommonUtil.isEmpty(filterCriteria.getLbl())) {
    		List<String> labels = Arrays.asList(filterCriteria.getLbl());
    		query.addCriteria(Criteria.where("labels").in(labels));
    	}
    	
    	if (!CommonUtil.isEmpty(filterCriteria.getRty())) {
    		List<BigInteger> resourceTypes = Arrays.asList(filterCriteria.getRty());
    		query.addCriteria(Criteria.where("resourceType").in(resourceTypes));
    	}
    	
    	if (		!CommonUtil.isEmpty(filterCriteria.getSza()) && !CommonUtil.isEmpty(filterCriteria.getSzb())) {
    		query.addCriteria(Criteria.where("contentSize").gte(filterCriteria.getSza()).lt(filterCriteria.getSzb()));
    	} else if (	!CommonUtil.isEmpty(filterCriteria.getSza()) && CommonUtil.isEmpty(filterCriteria.getSzb())) {
    		query.addCriteria(Criteria.where("contentSize").gte(filterCriteria.getSza()));
    	} else if (	 CommonUtil.isEmpty(filterCriteria.getSza()) && !CommonUtil.isEmpty(filterCriteria.getSzb())) {
    		query.addCriteria(Criteria.where("contentSize").lt(filterCriteria.getSzb()));
    	}
    	
    	if (!CommonUtil.isEmpty(filterCriteria.getCty())) {
    		List<String> contentInfos = Arrays.asList(filterCriteria.getCty());
    		query.addCriteria(Criteria.where("contentInfo").in(contentInfos));
    	}
    	
    	if (!CommonUtil.isEmpty(filterCriteria.getLim())) {
    		query.limit(filterCriteria.getLim().intValue());
    	}
    	
    	if (!CommonUtil.isEmpty(filterCriteria.getAtr())) {
    		for (int i=0; i<filterCriteria.getAtr().length; i++) {
    			Attribute attribute = filterCriteria.getAtr()[i];
    			if (!attribute.getName().equals("pi") && !attribute.getName().equals("rn")) {
    				String shortName = attribute.getName();
    				String longName = SHORT_NAMES.Short2LongName(shortName);
    				query.addCriteria(Criteria.where(longName).is(attribute.getValue()));
    			}
    		}
    	}
    	
    	return query;
    }
    
    /**
     * oneM2M eventNotificationCriteria parameter to query update
     * @param eventNotificationCriteria
     * @param resourceID
     * @return
     */
    public Query eventNotificationCriteriaToQuery(EventNotificationCriteria eventNotificationCriteria, String resourceID) {
    	Query query = null;
    	
    	// createdBefore and createdAfter
    	if (		!CommonUtil.isEmpty(eventNotificationCriteria.getCreatedBefore()) && !CommonUtil.isEmpty(eventNotificationCriteria.getCreatedAfter())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("creationTime").lt(eventNotificationCriteria.getCreatedBefore()).gt(eventNotificationCriteria.getCreatedAfter()));
    	} else if (	!CommonUtil.isEmpty(eventNotificationCriteria.getCreatedBefore()) &&  CommonUtil.isEmpty(eventNotificationCriteria.getCreatedAfter())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("creationTime").lt(eventNotificationCriteria.getCreatedBefore()));
    	} else if (	 CommonUtil.isEmpty(eventNotificationCriteria.getCreatedBefore()) && !CommonUtil.isEmpty(eventNotificationCriteria.getCreatedAfter())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("creationTime").gt(eventNotificationCriteria.getCreatedAfter()));
    	}
    	
    	if (!CommonUtil.isEmpty(query)) {
    		query.addCriteria(Criteria.where("resourceID").is(resourceID));
    		return query;
    	}
    	
    	// modifiedSince and unmodifiedSince
    	if (		!CommonUtil.isEmpty(eventNotificationCriteria.getModifiedSince()) && !CommonUtil.isEmpty(eventNotificationCriteria.getUnmodifiedSince())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("lastModifiedTime").gt(eventNotificationCriteria.getModifiedSince()).lt(eventNotificationCriteria.getUnmodifiedSince()));
    	} else if (	!CommonUtil.isEmpty(eventNotificationCriteria.getModifiedSince()) &&  CommonUtil.isEmpty(eventNotificationCriteria.getUnmodifiedSince())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("lastModifiedTime").gt(eventNotificationCriteria.getModifiedSince()));
    	} else if (	 CommonUtil.isEmpty(eventNotificationCriteria.getModifiedSince()) && !CommonUtil.isEmpty(eventNotificationCriteria.getUnmodifiedSince())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("lastModifiedTime").lt(eventNotificationCriteria.getUnmodifiedSince()));
    	}
    	
    	if (!CommonUtil.isEmpty(query)) {
    		query.addCriteria(Criteria.where("resourceID").is(resourceID));
    		return query;
    	}
    	
    	// stateTagSmaller and stateTagBigger
    	if (		!CommonUtil.isEmpty(eventNotificationCriteria.getStateTagSmaller()) && !CommonUtil.isEmpty(eventNotificationCriteria.getStateTagBigger())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("stateTag").lt(eventNotificationCriteria.getStateTagSmaller()).gt(eventNotificationCriteria.getStateTagBigger()));
    	} else if (	!CommonUtil.isEmpty(eventNotificationCriteria.getStateTagSmaller()) &&  CommonUtil.isEmpty(eventNotificationCriteria.getStateTagBigger())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("stateTag").lt(eventNotificationCriteria.getStateTagSmaller()));
    	} else if (	 CommonUtil.isEmpty(eventNotificationCriteria.getStateTagSmaller()) && !CommonUtil.isEmpty(eventNotificationCriteria.getStateTagBigger())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("stateTag").gt(eventNotificationCriteria.getStateTagBigger()));
    	}
    	
    	if (!CommonUtil.isEmpty(query)) {
    		query.addCriteria(Criteria.where("resourceID").is(resourceID));
    		return query;
    	}
    	
    	// expireBefore and expireAfter
    	if (		!CommonUtil.isEmpty(eventNotificationCriteria.getExpireBefore()) && !CommonUtil.isEmpty(eventNotificationCriteria.getExpireAfter())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("expirationTime").lt(eventNotificationCriteria.getExpireBefore()).gt(eventNotificationCriteria.getExpireAfter()));
    	} else if (	!CommonUtil.isEmpty(eventNotificationCriteria.getExpireBefore()) && CommonUtil.isEmpty(eventNotificationCriteria.getExpireAfter())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("expirationTime").lt(eventNotificationCriteria.getExpireBefore()));
    	} else if (	 CommonUtil.isEmpty(eventNotificationCriteria.getExpireBefore()) && !CommonUtil.isEmpty(eventNotificationCriteria.getExpireAfter())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("expirationTime").gt(eventNotificationCriteria.getExpireAfter()));
    	}
    	
    	if (!CommonUtil.isEmpty(query)) {
    		query.addCriteria(Criteria.where("resourceID").is(resourceID));
    		return query;
    	}

    	// sizeAbove and sizeBelow
    	if (		!CommonUtil.isEmpty(eventNotificationCriteria.getSizeAbove()) && !CommonUtil.isEmpty(eventNotificationCriteria.getSizeBelow())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("contentSize").gte(eventNotificationCriteria.getSizeAbove()).lt(eventNotificationCriteria.getSizeBelow()));
    	} else if (	!CommonUtil.isEmpty(eventNotificationCriteria.getSizeAbove()) && CommonUtil.isEmpty(eventNotificationCriteria.getSizeBelow())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("contentSize").gte(eventNotificationCriteria.getSizeAbove()));
    	} else if (	 CommonUtil.isEmpty(eventNotificationCriteria.getSizeAbove()) && !CommonUtil.isEmpty(eventNotificationCriteria.getSizeBelow())) {
    		query = new Query();
    		query.addCriteria(Criteria.where("contentSize").lt(eventNotificationCriteria.getSizeBelow()));
    	}
    	
    	if (!CommonUtil.isEmpty(query)) {
    		query.addCriteria(Criteria.where("resourceID").is(resourceID));
    		return query;
    	}
    	
    	// attribute
    	if (!CommonUtil.isEmpty(eventNotificationCriteria.getAttribute()) && eventNotificationCriteria.getAttribute().size() > 0) {
    		if (eventNotificationCriteria.getAttribute().size() == 1) {
    			Attribute attribute = eventNotificationCriteria.getAttribute().get(0);
    			if (attribute.getName().equals("ri")) {
    				return query;
    			}
    		}
    		
    		query = new Query();
    		for (int i=0; i<eventNotificationCriteria.getAttribute().size(); i++) {
    			Attribute attribute = eventNotificationCriteria.getAttribute().get(i);
    			if (!attribute.getName().equals("ri")) {
    				
    				String shortName = attribute.getName();
    				String longName = SHORT_NAMES.Short2LongName(shortName);
    				query.addCriteria(Criteria.where(longName).is(attribute.getValue()));
    			}
    		}
    		query.addCriteria(Criteria.where("resourceID").is(resourceID));
    		return query;
    	}
    	
    	return query;
    }
    
    /**
     * getBody
     * @param request
     * @return
     * @throws RSCException
     */
    public String getBody(HttpServletRequest request) throws RSCException {
    	 
        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
 
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
        	throw new RSCException(RSC.BAD_REQUEST, ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                	logger.error(ex.getMessage(), ex);
                	throw new RSCException(RSC.BAD_REQUEST, ex.getMessage());
                }
            }
        }
 
        body = stringBuilder.toString();
        return body;
    }
    
    /**
     * isContentTypeByXml
     * @param request
     * @return
     */
    public boolean isContentTypeByXml(HttpServletRequest request) {
   	 
        boolean isXml = false;
        String contentType 	= CommonUtil.nvl(request.getHeader("Content-Type"), "");
        
        if (contentType.contains("xml")) isXml = true;
        
        return isXml;
    }
    
    /**
     * isAcceptByJson
     * @param request
     * @return
     */
    public boolean isAcceptByJson(HttpServletRequest request) {
   	 
        boolean isJson = false;
        String accept 	= CommonUtil.nvl(request.getHeader("Accept"), "");
        
        if (accept.contains("json")) isJson = true;
        
        return isJson;
    }
    
    /**
     * getBodyObject
     * @param request
     * @param cls
     * @return
     */
    public Object getBodyObject(HttpServletRequest request, Class cls) {
    	 
    	Object profile = null;
        String body = null;
        try {
        	
			body = getBody(request);
			
	        if (isContentTypeByXml(request)) profile = unMarshalFromXmlString(body);
	        else							 profile = unMarshalFromJsonString(cls, body);
	        
		} catch (RSCException e) {
			e.printStackTrace();
		}
        
        return profile;
    }
    
    /**
     * getObject2Txt
     * @param request
     * @param obj
     * @return
     */
    public String getObject2Txt(HttpServletRequest request, Object obj) {
    	
        String txt = null;
        
        if (isAcceptByJson(request)) txt = marshalToJsonString(obj);
        else						 txt = marshalToXmlString(obj);
        
        try {
	        BigInteger filterUsage = new BigInteger(request.getParameter("fu"));
	        if (!CommonUtil.isEmpty(filterUsage) && FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterUsage)) {
	        	if (isAcceptByJson(request)) txt = txt.replaceAll("hiddenTagURIs" , "uril");
	            else						 txt = txt.replaceAll("<hiddenTagURIs>", "").replaceAll("</hiddenTagURIs>", "");
	        }
        } catch (Exception e) {
        }
        
        return txt;
    }
    
	/**
	 * marshalToXmlString
	 * @param obj
	 * @return
	 */
	public String marshalToXmlString(Object obj){
		if (CommonUtil.isEmpty(obj)) return null;
		
		StringWriter sw = new StringWriter();
		
		try {
			Marshaller msh = context.createMarshaller();
			msh.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			msh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
			msh.setProperty(Marshaller.JAXB_FRAGMENT, false);
			msh.marshal(obj, sw);
		} catch (Exception e) {
			return null;
		}
		
		return sw.toString();
	}
	
	/**
	 * unMarshalFromXmlString
	 * @param in
	 * @return
	 */
	public Object unMarshalFromXmlString(String in){
		if (CommonUtil.isEmpty(in)) return null;
		
		Object obj = null;
		StringReader sr = new StringReader(in);
		
		try {
			Unmarshaller msh =  context.createUnmarshaller();
			obj = msh.unmarshal(sr);
			
		} catch (Exception e) {
			return null;
		}
		
		return obj;
	}
	
	/**
	 * marshalToJsonString
	 * @param obj
	 * @return
	 */
	public String marshalToJsonString(Object obj){
		if (CommonUtil.isEmpty(obj)) return null;
		
		String jsonStr = null;
		
		ObjectMapper mapper = new ObjectMapper();
		JaxbAnnotationModule module = new JaxbAnnotationModule();
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);
		
		try {
			jsonStr = mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return jsonStr;
	}
	
	/**
	 * unMarshalFromJsonString
	 * @param cls
	 * @param in
	 * @return
	 */
	public Object unMarshalFromJsonString(Class cls, String in){
		if (CommonUtil.isEmpty(in)) return null;
		
		Object obj = null;
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			JaxbAnnotationModule module = new JaxbAnnotationModule();
			mapper.registerModule(module);
			mapper.setSerializationInclusion(Include.NON_NULL);
			obj = mapper.readValue(in, cls);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return obj;
	}
	
	/**
	 * getTy
	 * @param request
	 * @return
	 */
	public RESOURCE_TYPE getTy(HttpServletRequest request) {
		
		// nCUbe > Mobius 전달 헤더
//		HttpPost post = new HttpPost(uri);
//		post.setHeader("Accept", "application/xml");
//		post.setHeader("Content-Type", "application/vnd.onem2m-res+xml;ty=13;mgd=1001");
		
		String contentType 	= CommonUtil.nvl(request.getHeader("Content-Type"), "");
		RESOURCE_TYPE ty 	= null;
		String[] contentTypeValues = contentType.split(";");
		for (int i=0; i<contentTypeValues.length; i++) {
			String contentTypeValue = contentTypeValues[i].trim();
			
			if (contentTypeValue.contains("ty=")) {
				String value = contentTypeValue.split("=")[1];
				if (!CommonUtil.isEmpty(value)) {
					ty = RESOURCE_TYPE.getThis(new BigInteger(value));
				}
			}
		}
		
		return ty;
	}
	
	/**
	 * getMgd
	 * @param request
	 * @return
	 */
	public MGMT_DEFINITION getMgd(HttpServletRequest request) {
		String contentType 	= CommonUtil.nvl(request.getHeader("Content-Type"), "");
		MGMT_DEFINITION mgd	= null;
		String[] contentTypeValues = contentType.split(";");
		for (int i=0; i<contentTypeValues.length; i++) {
			String contentTypeValue = contentTypeValues[i].trim();
			
			if (contentTypeValue.contains("mgd=")) {
				String value = contentTypeValue.split("=")[1];
				if (!CommonUtil.isEmpty(value)) {
					mgd = MGMT_DEFINITION.getThis(new BigInteger(value));
				}
			}
		}
		
		return mgd;
	}
	
	/**
	 * Search or Result Content(child-resources)
	 * @param resourceID
	 * @param resourceType
	 * @param mgmtDefinition
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	public void search(String resourceID, RESOURCE_TYPE resourceType, MGMT_DEFINITION mgmtDefinition, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(resource)) return;
		
		BigInteger[] childResourceTypeArray = resourceType.getChildResourceTypeArray();
		BigInteger[] childMgmtDefinitionArray = resourceType.getChildMgmtDefinitionArray();
		
		try {
			if (CommonUtil.isEmpty(mgmtDefinition)) {
				
				switch(resourceType) {
				case AE:
					this.searchAE(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case CSE_BASE:
					this.searchCSEBase(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case CONTAINER:
					this.searchContainer(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case CONTENT_INSTANCE:
					
					break;
				case EXEC_INSTANCE:
					this.searchExecInstance(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case LOCATION_POLICY:
					this.searchLocationPolicy(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case MGMT_CMD:
					this.searchMgmtCmd(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case GROUP:
					this.searchGroup(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case NODE:
					this.searchNode(resourceID, childResourceTypeArray, childMgmtDefinitionArray, resource, requestPrimitive, filterCriteria);
					break;
				case REMOTE_CSE:
					this.searchRemoteCSE(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case SUBSCRIPTION:
					
					break;
				default:
					break;
				}
			} else {
				
				switch (mgmtDefinition) {
				case FIRMWARE:
					this.searchFirmware(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case SOFTWARE:
					this.searchSoftware(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case DEVICE_INFO:
					this.searchDeviceInfo(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case BATTERY:
					this.searchBattery(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case MEMORY:
					this.searchMemory(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				case REBOOT:
					this.searchReboot(resourceID, childResourceTypeArray, resource, requestPrimitive, filterCriteria);
					break;
				default:
					break;
				}
			}
			
		} catch (RSCException e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(e.getCode(), e.getMessage());
		}
	}
	
	/**
	 * Search AE
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchAE(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case CONTAINER:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByContainerChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getChildResource().addAll(childResourceRefList);
					
					List<Container> findContainerNoFilterResourceRefList = searchResourceByContainerNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getContainer().addAll(findContainerNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByContainer(resource, requestPrimitive, filterCriteria, resourceID);
						
				} else {
					
					List<Container> findContainerList = searchResourceByContainer(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getContainer().addAll(findContainerList);
				}
				break;
			case GROUP:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByGroupChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getChildResource().addAll(childResourceRefList);
					
					List<Group> findGroupNofilterResourceRefList = searchResourceByGroupNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getGroup().addAll(findGroupNofilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByGroup(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Group> findGroupList = searchResourceByGroup(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getGroup().addAll(findGroupList);
				}
				break;
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((AE)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search CSEBase
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchCSEBase(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case AE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByAEChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<AE> findAENoFilterResourceRefList = searchResourceByAENoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getAE().addAll(findAENoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByAE(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<AE> findAEList = searchResourceByAE(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getAE().addAll(findAEList);
					
				}
				break;
			case CONTAINER:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByContainerChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<Container> findContainerNoFilterResourceRefList = searchResourceByContainerNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getContainer().addAll(findContainerNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByContainer(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Container> findContainerList = searchResourceByContainer(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getContainer().addAll(findContainerList);
				}
				break;
			case LOCATION_POLICY:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByLocationPolicyChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<LocationPolicy> findLocationNoFilterPolicyResourceRefList = searchResourceByLocationPolicyNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getLocationPolicy().addAll(findLocationNoFilterPolicyResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByLocationPolicy(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<LocationPolicy> findLocationPolicyList = searchResourceByLocationPolicy(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getLocationPolicy().addAll(findLocationPolicyList);
				}
				break;
			case MGMT_CMD:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByMgmtCmdChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<MgmtCmd> findMgmtCmdNoFilterResourceRefList = searchResourceByMgmtCmdNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getMgmtCmd().addAll(findMgmtCmdNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByMgmtCmd(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<MgmtCmd> findMgmtCmdList = searchResourceByMgmtCmd(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getMgmtCmd().addAll(findMgmtCmdList);
				}
				break;
			case GROUP:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByGroupChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<Group> findGroupNofilterResourceRefList = searchResourceByGroupNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getGroup().addAll(findGroupNofilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByGroup(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Group> findGroupList = searchResourceByGroup(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getGroup().addAll(findGroupList);
				}
				break;
			case NODE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByNodeChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<Node> findNodeNoFilterResourceRefList = searchResourceByNodeNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getNode().addAll(findNodeNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByNode(resource, requestPrimitive, filterCriteria, resourceID);
				
				} else {
					
					List<Node> findNodeList = searchResourceByNode(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getNode().addAll(findNodeList);
				}
				break;
			case REMOTE_CSE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> findChildResourceRefList = findResourceByRemoteCSEChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(findChildResourceRefList);
					
					List<RemoteCSE> findRemoteCSENoFilterResourceRefList = searchResourceByRemoteCSENoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getRemoteCSE().addAll(findRemoteCSENoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByRemoteCSE(resource, requestPrimitive, filterCriteria, resourceID);
						
				} else {
					
					List<RemoteCSE> findRemoteCSEList = searchResourceByRemoteCSE(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getRemoteCSE().addAll(findRemoteCSEList);
				}
				break;
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((CSEBase)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search container
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchContainer(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case CONTAINER:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByContainerChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getChildResource().addAll(childResourceRefList);
					
					List<Container> findContainerNoFilterResourceRefList = searchResourceByContainerNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getContainer().addAll(findContainerNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByContainer(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Container> findContainerList = searchResourceByContainer(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getContainer().addAll(findContainerList);
				}
				break;
			case CONTENT_INSTANCE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByContentInstanceChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getChildResource().addAll(childResourceRefList);
					
					List<ContentInstance> findContentInstanceNoFilterResourceRefList = searchResourceByContentInstanceNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getContentInstance().addAll(findContentInstanceNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByContentInstance(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<ContentInstance> findContentInstanceList = searchResourceByContentInstance(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getContentInstance().addAll(findContentInstanceList);
				}
				break;
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Container)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search execInstance
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchExecInstance(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((ExecInstance)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((ExecInstance)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((ExecInstance)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search locationPolicy
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchLocationPolicy(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((LocationPolicy)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((LocationPolicy)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((LocationPolicy)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search mgmtCmd
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchMgmtCmd(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case EXEC_INSTANCE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByExecInstanceChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((MgmtCmd)resource).getChildResource().addAll(childResourceRefList);
					
					List<ExecInstance> findExecInstanceNoFilterResourceRefList = searchResourceByExecInstanceNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((MgmtCmd)resource).getExecInstance().addAll(findExecInstanceNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByExecInstance(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<ExecInstance> findExecInstanceList = searchResourceByExecInstance(requestPrimitive, filterCriteria, resourceID);
					((MgmtCmd)resource).getExecInstance().addAll(findExecInstanceList);
				}
				break;
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((MgmtCmd)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((MgmtCmd)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((MgmtCmd)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search group
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchGroup(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Group)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Group)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Group)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search node
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param childMgmtDefinitionArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchNode(String resourceID, BigInteger[] childResourceTypeArray, BigInteger[] childMgmtDefinitionArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
		
		if (CommonUtil.isEmpty(childMgmtDefinitionArray)) return;
		
		for (int i=0; i<childMgmtDefinitionArray.length; i++) {
			
			MGMT_DEFINITION childMgmtDefinition = CommonCode.MGMT_DEFINITION.getThis(childMgmtDefinitionArray[i]);
			switch(childMgmtDefinition) {
			case FIRMWARE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByFirmwareChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<Firmware> findFirmwareNoFilterResourceRefList = searchResourceByFirmwareNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getFirmware().addAll(findFirmwareNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByFirmware(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Firmware> findFirmwareList = searchResourceByFirmware(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getFirmware().addAll(findFirmwareList);
				}
				break;
			case SOFTWARE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySoftwareChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<Software> findSoftwareNoFilterResourceRefList = searchResourceBySoftwareNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getSoftware().addAll(findSoftwareNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySoftware(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Software> findSoftwareList = searchResourceBySoftware(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getSoftware().addAll(findSoftwareList);
				}
				break;
			case DEVICE_INFO:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByDeviceInfoChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<DeviceInfo> findDeviceInfoNoFilterResourceRefList = searchResourceByDeviceInfoNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getDeviceInfo().addAll(findDeviceInfoNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByDeviceInfo(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<DeviceInfo> findDeviceInfoList = searchResourceByDeviceInfo(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getDeviceInfo().addAll(findDeviceInfoList);
				}
				break;
			case BATTERY:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByBatteryChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<Battery> findBatteryNoFilterResourceRefList = searchResourceByBatteryNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getBattery().addAll(findBatteryNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByBattery(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Battery> findBatteryList = searchResourceByBattery(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getBattery().addAll(findBatteryList);
				}
				break;
			case MEMORY:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByMemoryChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<Memory> findMemoryNoFilterResourceRefList = searchResourceByMemoryNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getMemory().addAll(findMemoryNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByMemory(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Memory> findMemoryList = searchResourceByMemory(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getMemory().addAll(findMemoryList);
				}
				break;
			case REBOOT:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByRebootChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getChildResource().addAll(childResourceRefList);
					
					List<Reboot> findRebootNoFilterResourceRefList = searchResourceByRebootNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getReboot().addAll(findRebootNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByReboot(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Reboot> findRebootList = searchResourceByReboot(requestPrimitive, filterCriteria, resourceID);
					((Node)resource).getReboot().addAll(findRebootList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search remoteCSE
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchRemoteCSE(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case AE:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByAEChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getChildResource().addAll(childResourceRefList);
					
					List<AE> findAENoFilterResourceRefList = searchResourceByAENoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getAE().addAll(findAENoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
						
					discoveryResourceByAE(resource, requestPrimitive, filterCriteria, resourceID);
						
				} else {
					
					List<AE> findAEList = searchResourceByAE(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getAE().addAll(findAEList);
					
				}
				break;
			case CONTAINER:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByContainerChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getChildResource().addAll(childResourceRefList);
					
					List<Container> findContainerNoFilterResourceRefList = searchResourceByContainerNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getContainer().addAll(findContainerNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
						
					discoveryResourceByContainer(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Container> findContainerList = searchResourceByContainer(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getContainer().addAll(findContainerList);
				}
				break;
			case MGMT_CMD:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByMgmtCmdChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getChildResource().addAll(childResourceRefList);
					
					List<MgmtCmd> findMgmtCmdNoFilterResourceRefList = searchResourceByMgmtCmdNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getMgmtCmd().addAll(findMgmtCmdNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByMgmtCmd(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<MgmtCmd> findMgmtCmdList = searchResourceByMgmtCmd(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getMgmtCmd().addAll(findMgmtCmdList);
				}
				break;
			case GROUP:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceByGroupChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getChildResource().addAll(childResourceRefList);
					
					List<Group> findGroupNofilterResourceRefList = searchResourceByGroupNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getGroup().addAll(findGroupNofilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceByGroup(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Group> findGroupList = searchResourceByGroup(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getGroup().addAll(findGroupList);
				}
				break;
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((RemoteCSE)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search firmware
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchFirmware(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Firmware)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Firmware)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Firmware)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search software
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchSoftware(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Software)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Software)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Software)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search deviceInfo
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchDeviceInfo(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((DeviceInfo)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((DeviceInfo)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((DeviceInfo)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search battery
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchBattery(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Battery)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Battery)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Battery)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search memory
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchMemory(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Memory)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Memory)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Memory)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Search reboot
	 * @param resourceID
	 * @param childResourceTypeArray
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @throws RSCException
	 */
	private void searchReboot(String resourceID, BigInteger[] childResourceTypeArray, Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		
		if (CommonUtil.isEmpty(childResourceTypeArray)) return;
		
		for (int i=0; i<childResourceTypeArray.length; i++) {
			
			RESOURCE_TYPE childResourceType = CommonCode.RESOURCE_TYPE.getThis(childResourceTypeArray[i]);
			switch(childResourceType) {
			case SUBSCRIPTION:
				if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
					
					List<ChildResourceRef> childResourceRefList = findResourceBySubscriptionChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Reboot)resource).getChildResource().addAll(childResourceRefList);
					
					List<Subscription> findSubscriptionNoFilterResourceRefList = searchResourceBySubscriptionNoFilterChildResourceRefrences(requestPrimitive, filterCriteria, resourceID);
					((Reboot)resource).getSubscription().addAll(findSubscriptionNoFilterResourceRefList);
					
				} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
					
					discoveryResourceBySubscription(resource, requestPrimitive, filterCriteria, resourceID);
					
				} else {
					
					List<Subscription> findSubscriptionList = searchResourceBySubscription(requestPrimitive, filterCriteria, resourceID);
					((Reboot)resource).getSubscription().addAll(findSubscriptionList);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * ContainerChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByContainerChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Container> findContainerResourceRefList = containerService.findContainerResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findContainerResourceRefList.size(); t++) {
			Container containerResourceRef = findContainerResourceRefList.get(t);
			ResourceRef resourceRef = containerResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * Search ContainerNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Container> searchResourceByContainerNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Container> findContainerNoFilterResourceRefList = containerService.findContainerResourceRef("parentID", parentID);
		for (int t=0; t<findContainerNoFilterResourceRefList.size(); t++) {
			Container containerResourceRef = findContainerNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = containerResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.CONTAINER, null, containerResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findContainerNoFilterResourceRefList;
	}
	
	/**
	 * Search Container
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Container> searchResourceByContainer(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Container> findContainerList = containerService.findContainer("parentID", parentID, filterCriteria);
		for (int t=0; t<findContainerList.size(); t++) {
			Container container = findContainerList.get(t);
			this.search(container.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, container, requestPrimitive, filterCriteria);
		}
		
		return findContainerList;
	}
	
	/**
	 * discovery Container
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByContainer(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Container> findContainerList = containerService.findContainer("parentID", parentID, filterCriteria);
		for (int t=0; t<findContainerList.size(); t++) {
			Container container = findContainerList.get(t);
			
			if (!CommonUtil.isEmpty(container.getResourceID())) ((URIList)resource).addURI((container.getResourceRef().getValue()));
			
			this.search(container.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * GroupChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByGroupChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Group> findGroupResourceRefList = groupService.findGroupResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findGroupResourceRefList.size(); t++) {
			Group groupResourceRef = findGroupResourceRefList.get(t);
			ResourceRef resourceRef = groupResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * Search GroupNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Group> searchResourceByGroupNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Group> findGroupNofilterResourceRefList = groupService.findGroupResourceRef("parentID", parentID);
		for (int t=0; t<findGroupNofilterResourceRefList.size(); t++) {
			Group groupResourceRef = findGroupNofilterResourceRefList.get(t);
			ResourceRef resourceRef = groupResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.GROUP, null, groupResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findGroupNofilterResourceRefList;
	}
	
	/**
	 * search Group
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Group> searchResourceByGroup(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Group> findGroupList = groupService.findGroup("parentID", parentID, filterCriteria);
		for (int t=0; t<findGroupList.size(); t++) {
			Group group = findGroupList.get(t);
			this.search(group.getResourceRef().getResourceID(), RESOURCE_TYPE.GROUP, null, group, requestPrimitive, filterCriteria);
		}
		
		return findGroupList;
	}
	
	/**
	 * discovery Group
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByGroup(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Group> findGroupList = groupService.findGroup("parentID", parentID, filterCriteria);
		for (int t=0; t<findGroupList.size(); t++) {
			Group group = findGroupList.get(t);
			
			if (!CommonUtil.isEmpty(group.getResourceID())) ((URIList)resource).addURI((group.getResourceRef().getValue()));
			
			this.search(group.getResourceRef().getResourceID(), RESOURCE_TYPE.GROUP, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * SubscriptionChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceBySubscriptionChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Subscription> findSubscriptionResourceRefList = subscriptionService.findSubscriptionResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findSubscriptionResourceRefList.size(); t++) {
			Subscription subscriptionResourceRef = findSubscriptionResourceRefList.get(t);
			ResourceRef resourceRef = subscriptionResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search SubscriptionNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Subscription> searchResourceBySubscriptionNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Subscription> findSubscriptionNoFilterResourceRefList = subscriptionService.findSubscriptionResourceRef("parentID", parentID);
		for (int t=0; t<findSubscriptionNoFilterResourceRefList.size(); t++) {
			Subscription subscriptionResourceRef = findSubscriptionNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = subscriptionResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, subscriptionResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findSubscriptionNoFilterResourceRefList;
	}
	
	/**
	 * search Subscription
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Subscription> searchResourceBySubscription(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Subscription> findSubscriptionList = subscriptionService.findSubscription("parentID", parentID, filterCriteria);
		for (int t=0; t<findSubscriptionList.size(); t++) {
			Subscription subscription = findSubscriptionList.get(t);
			this.search(subscription.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, subscription, requestPrimitive, filterCriteria);
		}
		
		return findSubscriptionList;
	}
	
	/**
	 * discovery Subscription
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceBySubscription(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Subscription> findSubscriptionList = subscriptionService.findSubscription("parentID", parentID, filterCriteria);
		for (int t=0; t<findSubscriptionList.size(); t++) {
			Subscription subscription = findSubscriptionList.get(t);
			
			if (!CommonUtil.isEmpty(subscription.getResourceID())) ((URIList)resource).addURI((subscription.getResourceRef().getValue()));
			
			this.search(subscription.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * AEChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByAEChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<AE> findAEResourceRefList = aEService.findAEResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findAEResourceRefList.size(); t++) {
			AE aeResourceRef = findAEResourceRefList.get(t);
			ResourceRef resourceRef = aeResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search AENoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<AE> searchResourceByAENoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<AE> findAENoFilterResourceRefList = aEService.findAEResourceRef("parentID", parentID);
		for (int t=0; t<findAENoFilterResourceRefList.size(); t++) {
			AE aeResourceRef = findAENoFilterResourceRefList.get(t);
			ResourceRef resourceRef = aeResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.AE, null, aeResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findAENoFilterResourceRefList;
	}
	
	/**
	 * search AE
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<AE> searchResourceByAE(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<AE> findAEList = aEService.findAE("parentID", parentID, filterCriteria);
		for (int t=0; t<findAEList.size(); t++) {
			AE ae = findAEList.get(t);
			this.search(ae.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, ae, requestPrimitive, filterCriteria);
		}
		
		return findAEList;
	}
	
	/**
	 * discovery AE
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByAE(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<AE> findAEList = aEService.findAE("parentID", parentID, filterCriteria);
		for (int t=0; t<findAEList.size(); t++) {
			AE ae = findAEList.get(t);
			
			if (!CommonUtil.isEmpty(ae.getResourceID())) ((URIList)resource).addURI(ae.getResourceRef().getValue());
			
			this.search(ae.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * LocationPolicyChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByLocationPolicyChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<LocationPolicy> findLocationPolicyResourceRefList = locationPolicyService.findLocationPolicyResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findLocationPolicyResourceRefList.size(); t++) {
			LocationPolicy locationPolicyResourceRef = findLocationPolicyResourceRefList.get(t);
			ResourceRef resourceRef = locationPolicyResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search LocationPolicyNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<LocationPolicy> searchResourceByLocationPolicyNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<LocationPolicy> findLocationNoFilterPolicyResourceRefList = locationPolicyService.findLocationPolicyResourceRef("parentID", parentID);
		for (int t=0; t<findLocationNoFilterPolicyResourceRefList.size(); t++) {
			LocationPolicy locationPolicyResourceRef = findLocationNoFilterPolicyResourceRefList.get(t);
			ResourceRef resourceRef = locationPolicyResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.LOCATION_POLICY, null, locationPolicyResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findLocationNoFilterPolicyResourceRefList;
	}
	
	/**
	 * search LocationPolicy
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<LocationPolicy> searchResourceByLocationPolicy(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<LocationPolicy> findLocationPolicyList = locationPolicyService.findLocationPolicy("parentID", parentID, filterCriteria);
		for (int t=0; t<findLocationPolicyList.size(); t++) {
			LocationPolicy locationPolicy = findLocationPolicyList.get(t);
			this.search(locationPolicy.getResourceRef().getResourceID(), RESOURCE_TYPE.LOCATION_POLICY, null, locationPolicy, requestPrimitive, filterCriteria);
		}
		
		return findLocationPolicyList;
	}
	
	/**
	 * discovery LocationPolicy
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByLocationPolicy(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<LocationPolicy> findLocationPolicyList = locationPolicyService.findLocationPolicy("parentID", parentID, filterCriteria);
		for (int t=0; t<findLocationPolicyList.size(); t++) {
			LocationPolicy locationPolicy = findLocationPolicyList.get(t);
			
			if (!CommonUtil.isEmpty(locationPolicy.getResourceID())) ((URIList)resource).addURI((locationPolicy.getResourceRef().getValue()));
			
			this.search(locationPolicy.getResourceRef().getResourceID(), RESOURCE_TYPE.LOCATION_POLICY, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * MgmtCmdChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByMgmtCmdChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<MgmtCmd> findMgmtCmdResourceRefList = mgmtCmdService.findMgmtCmdResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findMgmtCmdResourceRefList.size(); t++) {
			MgmtCmd mgmtCmdResourceRef = findMgmtCmdResourceRefList.get(t);
			ResourceRef resourceRef = mgmtCmdResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search MgmtCmdNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<MgmtCmd> searchResourceByMgmtCmdNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<MgmtCmd> findMgmtCmdNoFilterResourceRefList = mgmtCmdService.findMgmtCmdResourceRef("parentID", parentID);
		for (int t=0; t<findMgmtCmdNoFilterResourceRefList.size(); t++) {
			MgmtCmd mgmtCmdResourceRef = findMgmtCmdNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = mgmtCmdResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_CMD, null, mgmtCmdResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findMgmtCmdNoFilterResourceRefList;
	}
	
	/**
	 * search MgmtCmd
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<MgmtCmd> searchResourceByMgmtCmd(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<MgmtCmd> findMgmtCmdList = mgmtCmdService.findMgmtCmd("parentID", parentID, filterCriteria);
		for (int t=0; t<findMgmtCmdList.size(); t++) {
			MgmtCmd mgmtCmd = findMgmtCmdList.get(t);
			this.search(mgmtCmd.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_CMD, null, mgmtCmd, requestPrimitive, filterCriteria);
		}
		
		return findMgmtCmdList;
	}
	
	/**
	 * discovery MgmtCmd
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByMgmtCmd(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<MgmtCmd> findMgmtCmdList = mgmtCmdService.findMgmtCmd("parentID", parentID, filterCriteria);
		for (int t=0; t<findMgmtCmdList.size(); t++) {
			MgmtCmd mgmtCmd = findMgmtCmdList.get(t);
			
			if (!CommonUtil.isEmpty(mgmtCmd.getResourceID())) ((URIList)resource).addURI((mgmtCmd.getResourceRef().getValue()));
			
			this.search(mgmtCmd.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_CMD, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * NodeChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByNodeChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Node> findNodeResourceRefList = nodeService.findNodeResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findNodeResourceRefList.size(); t++) {
			Node nodeResourceRef = findNodeResourceRefList.get(t);
			ResourceRef resourceRef = nodeResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search NodeNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Node> searchResourceByNodeNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Node> findNodeNoFilterResourceRefList = nodeService.findNodeResourceRef("parentID", parentID);
		for (int t=0; t<findNodeNoFilterResourceRefList.size(); t++) {
			Node nodeResourceRef = findNodeNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = nodeResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.NODE, null, nodeResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findNodeNoFilterResourceRefList;
	}
	
	/**
	 * search Node
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Node> searchResourceByNode(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Node> findNodeList = nodeService.findNode("parentID", parentID, filterCriteria);
		for (int t=0; t<findNodeList.size(); t++) {
			Node node = findNodeList.get(t);
			this.search(node.getResourceRef().getResourceID(), RESOURCE_TYPE.NODE, null, node, requestPrimitive, filterCriteria);
		}
		
		return findNodeList;
	}
	
	/**
	 * discovery Node
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByNode(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Node> findNodeList = nodeService.findNode("parentID", parentID, filterCriteria);
		for (int t=0; t<findNodeList.size(); t++) {
			Node node = findNodeList.get(t);
			
			if (!CommonUtil.isEmpty(node.getResourceID())) ((URIList)resource).addURI(node.getResourceRef().getValue());
			
			this.search(node.getResourceRef().getResourceID(), RESOURCE_TYPE.NODE, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * RemoteCSEChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByRemoteCSEChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<RemoteCSE> findRemoteCSEResourceRefList = remoteCSEService.findRemoteCSEResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findRemoteCSEResourceRefList.size(); t++) {
			RemoteCSE remoteCSEResourceRef = findRemoteCSEResourceRefList.get(t);
			ResourceRef resourceRef = remoteCSEResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search RemoteCSENoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<RemoteCSE> searchResourceByRemoteCSENoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<RemoteCSE> findRemoteCSENoFilterResourceRefList = remoteCSEService.findRemoteCSEResourceRef("parentID", parentID);
		for (int t=0; t<findRemoteCSENoFilterResourceRefList.size(); t++) {
			RemoteCSE remoteCSEResourceRef = findRemoteCSENoFilterResourceRefList.get(t);
			ResourceRef resourceRef = remoteCSEResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.REMOTE_CSE, null, remoteCSEResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findRemoteCSENoFilterResourceRefList;
	}
	
	/**
	 * search RemoteCSE
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<RemoteCSE> searchResourceByRemoteCSE(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<RemoteCSE> findRemoteCSEList = remoteCSEService.findRemoteCSE("parentID", parentID, filterCriteria);
		for (int t=0; t<findRemoteCSEList.size(); t++) {
			RemoteCSE remoteCSE = findRemoteCSEList.get(t);
			this.search(remoteCSE.getResourceRef().getResourceID(), RESOURCE_TYPE.REMOTE_CSE, null, remoteCSE, requestPrimitive, filterCriteria);
		}
		
		return findRemoteCSEList;
	}
	
	/**
	 * discovery RemoteCSE
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByRemoteCSE(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<RemoteCSE> findRemoteCSEList = remoteCSEService.findRemoteCSE("parentID", parentID, filterCriteria);
		for (int t=0; t<findRemoteCSEList.size(); t++) {
			RemoteCSE remoteCSE = findRemoteCSEList.get(t);
			
			if (!CommonUtil.isEmpty(remoteCSE.getResourceID())) ((URIList)resource).addURI(remoteCSE.getResourceRef().getValue());
			
			this.search(remoteCSE.getResourceRef().getResourceID(), RESOURCE_TYPE.REMOTE_CSE, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * ContentInstanceChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByContentInstanceChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<ContentInstance> findContentInstanceResourceRefList = contentInstanceService.findContentInstanceResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findContentInstanceResourceRefList.size(); t++) {
			ContentInstance contentInstanceResourceRef = findContentInstanceResourceRefList.get(t);
			ResourceRef resourceRef = contentInstanceResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search ContentInstanceNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ContentInstance> searchResourceByContentInstanceNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ContentInstance> findContentInstanceNoFilterResourceRefList = contentInstanceService.findContentInstanceResourceRef("parentID", parentID);
		for (int t=0; t<findContentInstanceNoFilterResourceRefList.size(); t++) {
			ContentInstance contentInstanceResourceRef = findContentInstanceNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = contentInstanceResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, contentInstanceResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findContentInstanceNoFilterResourceRefList;
	}
	
	/**
	 * search ContentInstance
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ContentInstance> searchResourceByContentInstance(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ContentInstance> findContentInstanceList = contentInstanceService.findContentInstance("parentID", parentID, filterCriteria);
		for (int t=0; t<findContentInstanceList.size(); t++) {
			ContentInstance contentInstance = findContentInstanceList.get(t);
			this.search(contentInstance.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, contentInstance, requestPrimitive, filterCriteria);
		}
		
		return findContentInstanceList;
	}
	
	/**
	 * discovery ContentInstance
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByContentInstance(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ContentInstance> findContentInstanceList = contentInstanceService.findContentInstance("parentID", parentID, filterCriteria);
		for (int t=0; t<findContentInstanceList.size(); t++) {
			ContentInstance contentInstance = findContentInstanceList.get(t);
			
			if (!CommonUtil.isEmpty(contentInstance.getResourceID())) ((URIList)resource).addURI(contentInstance.getResourceRef().getValue());
			
			this.search(contentInstance.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * ExecInstanceChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByExecInstanceChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<ExecInstance> findExecInstanceResourceRefList = execInstanceService.findExecInstanceResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findExecInstanceResourceRefList.size(); t++) {
			ExecInstance execInstanceResourceRef = findExecInstanceResourceRefList.get(t);
			ResourceRef resourceRef = execInstanceResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search ExecInstanceNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ExecInstance> searchResourceByExecInstanceNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ExecInstance> findExecInstanceNoFilterResourceRefList = execInstanceService.findExecInstanceResourceRef("parentID", parentID);
		for (int t=0; t<findExecInstanceNoFilterResourceRefList.size(); t++) {
			ExecInstance execInstanceResourceRef = findExecInstanceNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = execInstanceResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.EXEC_INSTANCE, null, execInstanceResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findExecInstanceNoFilterResourceRefList;
	}
	
	/**
	 * search ExecInstance
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ExecInstance> searchResourceByExecInstance(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ExecInstance> findExecInstanceList = execInstanceService.findExecInstance("parentID", parentID, filterCriteria);
		for (int t=0; t<findExecInstanceList.size(); t++) {
			ExecInstance execInstance = findExecInstanceList.get(t);
			this.search(execInstance.getResourceRef().getResourceID(), RESOURCE_TYPE.EXEC_INSTANCE, null, execInstance, requestPrimitive, filterCriteria);
		}
		
		return findExecInstanceList;
	}
	
	/**
	 * discovery ExecInstance
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByExecInstance(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ExecInstance> findExecInstanceList = execInstanceService.findExecInstance("parentID", parentID, filterCriteria);
		for (int t=0; t<findExecInstanceList.size(); t++) {
			ExecInstance execInstance = findExecInstanceList.get(t);
			
			if (!CommonUtil.isEmpty(execInstance.getResourceID())) ((URIList)resource).addURI((execInstance.getResourceRef().getValue()));
			
			this.search(execInstance.getResourceRef().getResourceID(), RESOURCE_TYPE.EXEC_INSTANCE, null, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * FirmwareChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByFirmwareChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Firmware> findFirmwareResourceRefList = firmwareService.findFirmwareResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findFirmwareResourceRefList.size(); t++) {
			Firmware firmwareResourceRef = findFirmwareResourceRefList.get(t);
			ResourceRef resourceRef = firmwareResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search FirmwareNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Firmware> searchResourceByFirmwareNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Firmware> findFirmwareNoFilterResourceRefList = firmwareService.findFirmwareResourceRef("parentID", parentID);
		for (int t=0; t<findFirmwareNoFilterResourceRefList.size(); t++) {
			Firmware firmwareResourceRef = findFirmwareNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = firmwareResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.FIRMWARE, firmwareResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findFirmwareNoFilterResourceRefList;
	}
	
	/**
	 * search Firmware
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Firmware> searchResourceByFirmware(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Firmware> findFirmwareList = firmwareService.findFirmware("parentID", parentID, filterCriteria);
		for (int t=0; t<findFirmwareList.size(); t++) {
			Firmware firmware = findFirmwareList.get(t);
			this.search(firmware.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.FIRMWARE, firmware, requestPrimitive, filterCriteria);
		}
		
		return findFirmwareList;
	}
	
	/**
	 * discovery Firmware
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByFirmware(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Firmware> findFirmwareList = firmwareService.findFirmware("parentID", parentID, filterCriteria);
		for (int t=0; t<findFirmwareList.size(); t++) {
			Firmware firmware = findFirmwareList.get(t);
			
			if (!CommonUtil.isEmpty(firmware.getResourceID())) ((URIList)resource).addURI((firmware.getResourceRef().getValue()));
			
			this.search(firmware.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.FIRMWARE, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * SoftwareChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceBySoftwareChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Software> findSoftwareResourceRefList = softwareService.findSoftwareResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findSoftwareResourceRefList.size(); t++) {
			Software softwareResourceRef = findSoftwareResourceRefList.get(t);
			ResourceRef resourceRef = softwareResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search SoftwareNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Software> searchResourceBySoftwareNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Software> findSoftwareNoFilterResourceRefList = softwareService.findSoftwareResourceRef("parentID", parentID);
		for (int t=0; t<findSoftwareNoFilterResourceRefList.size(); t++) {
			Software softwareResourceRef = findSoftwareNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = softwareResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.SOFTWARE, softwareResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findSoftwareNoFilterResourceRefList;
	}
	
	/**
	 * search Software
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Software> searchResourceBySoftware(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Software> findSoftwareList = softwareService.findSoftware("parentID", parentID, filterCriteria);
		for (int t=0; t<findSoftwareList.size(); t++) {
			Software software = findSoftwareList.get(t);
			this.search(software.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.SOFTWARE, software, requestPrimitive, filterCriteria);
		}
		
		return findSoftwareList;
	}
	
	/**
	 * discovery Software
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceBySoftware(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Software> findSoftwareList = softwareService.findSoftware("parentID", parentID, filterCriteria);
		for (int t=0; t<findSoftwareList.size(); t++) {
			Software software = findSoftwareList.get(t);
			
			if (!CommonUtil.isEmpty(software.getResourceID())) ((URIList)resource).addURI((software.getResourceRef().getValue()));
			
			this.search(software.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.SOFTWARE, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * DeviceInfoChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByDeviceInfoChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<DeviceInfo> findDeviceInfoResourceRefList = deviceInfoService.findDeviceInfoResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findDeviceInfoResourceRefList.size(); t++) {
			DeviceInfo deviceInfoResourceRef = findDeviceInfoResourceRefList.get(t);
			ResourceRef resourceRef = deviceInfoResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search DeviceInfoNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<DeviceInfo> searchResourceByDeviceInfoNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<DeviceInfo> findDeviceInfoNoFilterResourceRefList = deviceInfoService.findDeviceInfoResourceRef("parentID", parentID);
		for (int t=0; t<findDeviceInfoNoFilterResourceRefList.size(); t++) {
			DeviceInfo deviceInfoResourceRef = findDeviceInfoNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = deviceInfoResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.DEVICE_INFO, deviceInfoResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findDeviceInfoNoFilterResourceRefList;
	}
	
	/**
	 * search DeviceInfo
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<DeviceInfo> searchResourceByDeviceInfo(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<DeviceInfo> findDeviceInfoList = deviceInfoService.findDeviceInfo("parentID", parentID, filterCriteria);
		for (int t=0; t<findDeviceInfoList.size(); t++) {
			DeviceInfo deviceInfo = findDeviceInfoList.get(t);
			this.search(deviceInfo.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.DEVICE_INFO, deviceInfo, requestPrimitive, filterCriteria);
		}
		
		return findDeviceInfoList;
	}
	
	/**
	 * discovery DeviceInfo
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByDeviceInfo(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<DeviceInfo> findDeviceInfoList = deviceInfoService.findDeviceInfo("parentID", parentID, filterCriteria);
		for (int t=0; t<findDeviceInfoList.size(); t++) {
			DeviceInfo deviceInfo = findDeviceInfoList.get(t);
			
			if (!CommonUtil.isEmpty(deviceInfo.getResourceID())) ((URIList)resource).addURI((deviceInfo.getResourceRef().getValue()));
			
			this.search(deviceInfo.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.DEVICE_INFO, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * BatteryChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByBatteryChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Battery> findBatteryResourceRefList = batteryService.findBatteryResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findBatteryResourceRefList.size(); t++) {
			Battery batteryResourceRef = findBatteryResourceRefList.get(t);
			ResourceRef resourceRef = batteryResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search BatteryNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Battery> searchResourceByBatteryNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Battery> findBatteryNoFilterResourceRefList = batteryService.findBatteryResourceRef("parentID", parentID);
		for (int t=0; t<findBatteryNoFilterResourceRefList.size(); t++) {
			Battery batteryResourceRef = findBatteryNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = batteryResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.BATTERY, batteryResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findBatteryNoFilterResourceRefList;
	}
	
	/**
	 * search Battery
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Battery> searchResourceByBattery(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Battery> findBatteryList = batteryService.findBattery("parentID", parentID, filterCriteria);
		for (int t=0; t<findBatteryList.size(); t++) {
			Battery battery = findBatteryList.get(t);
			this.search(battery.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.BATTERY, battery, requestPrimitive, filterCriteria);
		}
		
		return findBatteryList;
	}
	
	/**
	 * discovery Battery
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByBattery(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Battery> findBatteryList = batteryService.findBattery("parentID", parentID, filterCriteria);
		for (int t=0; t<findBatteryList.size(); t++) {
			Battery battery = findBatteryList.get(t);
			
			if (!CommonUtil.isEmpty(battery.getResourceID())) ((URIList)resource).addURI((battery.getResourceRef().getValue()));
			
			this.search(battery.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.BATTERY, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * MemoryChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByMemoryChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Memory> findMemoryResourceRefList = memoryService.findMemoryResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findMemoryResourceRefList.size(); t++) {
			Memory memoryResourceRef = findMemoryResourceRefList.get(t);
			ResourceRef resourceRef = memoryResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search MemoryNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Memory> searchResourceByMemoryNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Memory> findMemoryNoFilterResourceRefList = memoryService.findMemoryResourceRef("parentID", parentID);
		for (int t=0; t<findMemoryNoFilterResourceRefList.size(); t++) {
			Memory memoryResourceRef = findMemoryNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = memoryResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.MEMORY, memoryResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findMemoryNoFilterResourceRefList;
	}
	
	/**
	 * search Memory
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Memory> searchResourceByMemory(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Memory> findMemoryList = memoryService.findMemory("parentID", parentID, filterCriteria);
		for (int t=0; t<findMemoryList.size(); t++) {
			Memory memory = findMemoryList.get(t);
			this.search(memory.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.MEMORY, memory, requestPrimitive, filterCriteria);
		}
		
		return findMemoryList;
	}
	
	/**
	 * discovery Memory
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByMemory(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Memory> findMemoryList = memoryService.findMemory("parentID", parentID, filterCriteria);
		for (int t=0; t<findMemoryList.size(); t++) {
			Memory memory = findMemoryList.get(t);
			
			if (!CommonUtil.isEmpty(memory.getResourceID())) ((URIList)resource).addURI((memory.getResourceRef().getValue()));
			
			this.search(memory.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.MEMORY, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * RebootChildResourceRefrences Retrieve
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<ChildResourceRef> findResourceByRebootChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<ChildResourceRef> childResourceRefList = new ArrayList<ChildResourceRef>();
		List<Reboot> findRebootResourceRefList = rebootService.findRebootResourceRef("parentID", parentID, filterCriteria);
		for (int t=0; t<findRebootResourceRefList.size(); t++) {
			Reboot rebootResourceRef = findRebootResourceRefList.get(t);
			ResourceRef resourceRef = rebootResourceRef.getResourceRef();
			childResourceRefList.add(resourceRef);
		}

		return childResourceRefList;
	}
	
	/**
	 * search RebootNoFilterChildResourceRefrences
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Reboot> searchResourceByRebootNoFilterChildResourceRefrences(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Reboot> findRebootNoFilterResourceRefList = rebootService.findRebootResourceRef("parentID", parentID);
		for (int t=0; t<findRebootNoFilterResourceRefList.size(); t++) {
			Reboot rebootResourceRef = findRebootNoFilterResourceRefList.get(t);
			ResourceRef resourceRef = rebootResourceRef.getResourceRef();
			this.search(resourceRef.getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.REBOOT, rebootResourceRef, requestPrimitive, filterCriteria);
		}
		
		return findRebootNoFilterResourceRefList;
	}
	
	/**
	 * search Reboot
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @return
	 * @throws RSCException
	 */
	private List<Reboot> searchResourceByReboot(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Reboot> findRebootList = rebootService.findReboot("parentID", parentID, filterCriteria);
		for (int t=0; t<findRebootList.size(); t++) {
			Reboot reboot = findRebootList.get(t);
			this.search(reboot.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.REBOOT, reboot, requestPrimitive, filterCriteria);
		}
		
		return findRebootList;
	}
	
	/**
	 * discovery Reboot
	 * @param resource
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param parentID
	 * @throws RSCException
	 */
	private void discoveryResourceByReboot(Object resource, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, String parentID) throws RSCException {
		List<Reboot> findRebootList = rebootService.findReboot("parentID", parentID, filterCriteria);
		for (int t=0; t<findRebootList.size(); t++) {
			Reboot reboot = findRebootList.get(t);
			
			if (!CommonUtil.isEmpty(reboot.getResourceID())) ((URIList)resource).addURI((reboot.getResourceRef().getValue()));
			
			this.search(reboot.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.REBOOT, resource, requestPrimitive, filterCriteria);
		}
	}
	
	/**
	 * filterCriteria limit Set
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param object
	 */
	public void setLimitForFilterCriteria(RequestPrimitive requestPrimitive, FilterCriteria filterCriteria, Object object) {
		if (   !FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())
			&& !RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
			&& !RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
			
			if (!CommonUtil.isEmpty(filterCriteria.getLim()) && !CommonUtil.isEmpty(object)) filterCriteria.setLim(BigInteger.valueOf(filterCriteria.getLim().intValue() - 1));
		}
	}
	
	/**
	 * filterCriteria limit Set
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param object
	 */
	public void setLimitForFilterCriteria(FilterCriteria filterCriteria, List<?> objects) {
		if (!CommonUtil.isEmpty(filterCriteria.getLim())) filterCriteria.setLim(BigInteger.valueOf(filterCriteria.getLim().intValue() - objects.size()));
	}
}
/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.controller.oneM2M;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
import kr.usis.iot.service.oneM2M.NodeService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.FILTER_USAGE;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
import kr.usis.iot.util.oneM2M.CommonCode.RESULT_CONTENT;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ntels.nisf.util.crypto.Sha256Util;

import biz.source_code.base64Coder.Base64Coder;

/**
 * remoteCSE management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Controller
public class RemoteCSEAction {

	private static final Log logger = LogFactory.getLog(RemoteCSEAction.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private CSEBaseService cseBaseService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private NodeService nodeService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private MCommonService mCommonService;
	
	
	/**
	 * remoteCSE Create
	 * @param cseBaseResourceName
	 * @param remoteCSEProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"}, params = "rty=16")
	@ResponseBody
	public ResponseEntity<Object> remoteCSECreate(@PathVariable String cseBaseResourceName
			 									, RequestPrimitive requestPrimitive
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSECreate start >>>");
		logger.debug("====================================================================");
		
		RemoteCSE remoteCSEProfile	= (RemoteCSE)mCommonService.getBodyObject(request, RemoteCSE.class);
		HttpHeaders responseHeaders	= new HttpHeaders();
		Object responseVo			= null;
		String responseTxt			= null;
		HttpStatus httpStatus		= HttpStatus.CREATED;
		RSC responseCode			= RSC.CREATED;
		String responseMsg			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String passCode	= CommonUtil.nvl(request.getHeader("passCode"), "");
		
		boolean checkMccP = mCommonService.getCheckMccP(from);
		boolean isError = false;
		
		if(CommonUtil.isEmpty(from)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-Origin " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(requestID)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-RI " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(!checkMccP && CommonUtil.isEmpty(passCode)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "passCode " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(cseBaseResourceName)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "CSEBase resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(remoteCSEProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<remoteCSE> " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(remoteCSEProfile.getCSEID())) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<remoteCSE> CSE-ID " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(remoteCSEProfile.isRequestReachability())) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<remoteCSE> requestReachability " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSECreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			String dKey 				= null;
			String remoteCSECSEID 		= remoteCSEProfile.getCSEID();
			String remoteCSEResourceName= !CommonUtil.isEmpty(name) ? name : remoteCSEProfile.getCSEID();
			String nodeResourceName		= !CommonUtil.isEmpty(name) ? name : remoteCSEProfile.getCSEID();
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			mCommonService.checkPatternRemoteCSECSEID(checkMccP, remoteCSECSEID);
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))){
				
				mgmtCmdService.getPointOfAccessList(remoteCSEProfile.getPointOfAccess());
				
				Node findNodeItem = null;
				if(!CommonUtil.isEmpty(findNodeItem = nodeService.findOneNodeByResourceName(findCSEBaseItem.getResourceID(), nodeResourceName))){
					throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.node.duplicate.text"));
				}
				
				dKey = mCommonService.createDeviceKey(checkMccP, remoteCSECSEID);
				
				String url = CommonUtil.getURL(request);
				
				remoteCSEProfile.setResourceName(remoteCSEResourceName);
				remoteCSEProfile.setParentID(findCSEBaseItem.getResourceID());
				remoteCSEProfile.setCSEBase(findCSEBaseItem.getResourceID());
				remoteCSEProfile.setCSEID(remoteCSECSEID);
				remoteCSEProfile.setPassCode(Sha256Util.getEncrypt(passCode));
				remoteCSEProfile.setMappingYn("N");
				
				findRemoteCSEItem = remoteCSEService.createRemoteCSE(url, remoteCSEProfile, dKey);
				findRemoteCSEItem.setDKey(dKey);
				
			} else {
				mongoLogService.log(logger, LEVEL.DEBUG, "findRemoteCSEItem : " + findRemoteCSEItem.toString());
				
				String oldPassCode = findRemoteCSEItem.getPassCode();
				if (!CommonUtil.isEmpty(oldPassCode) && !oldPassCode.equals(Sha256Util.getEncrypt(passCode))) {
					throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.passCode.error.text"));
				}

				dKey = mCommonService.createDeviceKey(checkMccP, remoteCSECSEID);
				
				accessControlPolicyService.updateAccessControlPolicyByDKey(findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
				findRemoteCSEItem.setDKey(dKey);
				
				responseCode = RSC.CONFLICT;
				responseMsg = CommonUtil.getMessage("msg.device.duplicate.text");
				
			}
			
			if (!CommonUtil.isEmpty(findRemoteCSEItem.getDKey())) {
				findRemoteCSEItem.setDKey(Base64Coder.encodeString(findRemoteCSEItem.getDKey()));
			}
			
			responseVo = findRemoteCSEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSECreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSECreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	
	/**
	 * remoteCSE Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEDelete(@PathVariable String cseBaseResourceName
			 									, @PathVariable String remoteCSEResourceName
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEDelete start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.DELETED;
		String responseMsg 			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 	= CommonUtil.nvl(request.getHeader("dKey"), "");
		
		boolean checkMccP = mCommonService.getCheckMccP(from);
		boolean isError = false;
		
		if(CommonUtil.isEmpty(from)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-Origin " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(requestID)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-RI " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(cseBaseResourceName)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "CSEBase resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(remoteCSEResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
			
			remoteCSEService.deleteRemoteCSEChild(findRemoteCSEItem.getResourceID(), findRemoteCSEItem.getNodeLink());
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	
	/**
	 * remoteCSE Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param remoteCSEProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.PUT, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEUpdate(@PathVariable String cseBaseResourceName
			 									, @PathVariable String remoteCSEResourceName
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEUpdate start >>>");
		logger.debug("====================================================================");
		
		RemoteCSE remoteCSEProfile	= (RemoteCSE)mCommonService.getBodyObject(request, RemoteCSE.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 	= CommonUtil.nvl(request.getHeader("dKey"), "");
		
		boolean checkMccP = mCommonService.getCheckMccP(from);
		boolean isError = false;
		
		if(CommonUtil.isEmpty(from)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-Origin " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(requestID)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-RI " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(cseBaseResourceName)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "CSEBase resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(remoteCSEResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(remoteCSEProfile)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<remoteCSE> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}		
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
			
			List<String> pointOfAccess = remoteCSEProfile.getPointOfAccess();
			if (!CommonUtil.isEmpty(pointOfAccess)) {
				
				mgmtCmdService.getPointOfAccessList(remoteCSEProfile.getPointOfAccess());
			}
			
			remoteCSEProfile.setResourceID(findRemoteCSEItem.getResourceID());
			
			findRemoteCSEItem = remoteCSEService.updateRemoteCSE(remoteCSEProfile);
			
			if (!CommonUtil.isEmpty(findRemoteCSEItem.getDKey())) {
				findRemoteCSEItem.setDKey(Base64Coder.encodeString(findRemoteCSEItem.getDKey()));
			}
			responseVo = findRemoteCSEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * remoteCSE Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSERetrieve(@PathVariable String cseBaseResourceName
			 									  , @PathVariable String remoteCSEResourceName
			 									  , RequestPrimitive requestPrimitive
			 									  , FilterCriteria filterCriteria
												  , HttpServletRequest request
												  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSERetrieve start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.OK;
		String responseMsg 			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey		= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic 	= CommonUtil.nvl(request.getHeader("topic"), "");
		
		boolean checkMccP = mCommonService.getCheckMccP(from);
		boolean isError = false;
		
		if(CommonUtil.isEmpty(from)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-Origin " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(requestID)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-RI " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(cseBaseResourceName)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "CSEBase resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(remoteCSEResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSERetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName, requestPrimitive, filterCriteria))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			if (!CommonUtil.isEmpty(findRemoteCSEItem.getDKey())) {
				findRemoteCSEItem.setDKey(Base64Coder.encodeString(findRemoteCSEItem.getDKey()));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findRemoteCSEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.REMOTE_CSE, null, findRemoteCSEItem, requestPrimitive, filterCriteria);
				
				responseVo = findRemoteCSEItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				RemoteCSE emptyRemoteCSEItem = new RemoteCSE();
				mCommonService.search(findRemoteCSEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.REMOTE_CSE, null, emptyRemoteCSEItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyRemoteCSEItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findRemoteCSEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.REMOTE_CSE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findRemoteCSEItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSERetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSERetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
}

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.AE;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
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
 * AE management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 * @history By JiHoon KIM jhkim@usis.kr
 * LOG DB 적재  (기존방식)
 * mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
 * LOG FILE 적재 (추가방식)
 * logger.error(">>> commonCSEBaseBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
 */
@Controller
public class AEAction {

	private static final Log logger = LogFactory.getLog(AEAction.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private CSEBaseService cseBaseService;
	
	@Autowired
	private AEService aEService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	
	/**
	 * AE Create(Root child)
	 * @param cseBaseResourceName
	 * @param aeProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"}, params = {"rty=2"})
	@ResponseBody
	public ResponseEntity<Object> AECreate(@PathVariable String cseBaseResourceName
										 , RequestPrimitive requestPrimitive
										 , HttpServletRequest request
										 , HttpServletResponse response){
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "AECreate start >>>");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("AECreate start >>>");
		logger.debug("====================================================================");
		
		AE aeProfile 				= (AE)mCommonService.getBodyObject(request, AE.class);
		HttpHeaders responseHeaders	= new HttpHeaders();
		Object responseVo			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.CREATED;
		RSC responseCode 			= RSC.CREATED;
		String responseMsg 			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String passCode	= CommonUtil.nvl(request.getHeader("passCode"), "");
		
		// mcc 체크
		boolean checkMccP = mCommonService.getCheckMccP(from);
		boolean isError = false;
		
		// Header 체크
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
		else if(CommonUtil.isEmpty(aeProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<AE> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(aeProfile.getAppID())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<AE> App-ID " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		// Error 로그 적재
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AECreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			String aKey  = null;
			String appID = aeProfile.getAppID();
			
			CSEBase findCSEBaseItem = null;
			
			// CSEBase 체크
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			// app-ID 체크
			mCommonService.checkPatternAEAppID(checkMccP, appID);
			
			AE findAEItem = null;
			
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), name))){
				
				mgmtCmdService.getPointOfAccessList(aeProfile.getPointOfAccess());
				
				String url = CommonUtil.getURL(request);
				
				aeProfile.setResourceName(name);
				aeProfile.setParentID(findCSEBaseItem.getResourceID());
				aeProfile.setPassCode(Sha256Util.getEncrypt(passCode));
				
				findAEItem = aEService.createAE(url, aeProfile, checkMccP);
				
			} else {
				
				String oldPassCode = findAEItem.getPassCode();
				if (!CommonUtil.isEmpty(oldPassCode) && !oldPassCode.equals(Sha256Util.getEncrypt(passCode))) {
					throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.passCode.error.text"));
				}
				
				aKey = mCommonService.createAKey(checkMccP, findAEItem.getAEID());
				
				accessControlPolicyService.updateAccessControlPolicyByAKey(findAEItem.getAccessControlPolicyIDs(), aKey);
				findAEItem.setAKey(aKey);
				
				responseCode = RSC.CONFLICT;
				responseMsg = CommonUtil.getMessage("msg.device.AE.duplicate.text");
			}
			
			if (!CommonUtil.isEmpty(findAEItem.getAKey())) {
				findAEItem.setAKey(Base64Coder.encodeString(findAEItem.getAKey()));
			}
			
			responseVo = findAEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
		
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AECreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "AECreate end <<<");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("AECreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Delete (Root child)
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> AEDelete(@PathVariable String cseBaseResourceName
			 							 , @PathVariable String aeResourceName
										 , HttpServletRequest request
										 , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("AEDelete start >>>");
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
		String aKey 	= CommonUtil.nvl(request.getHeader("aKey"), "");
		
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
		else if(CommonUtil.isEmpty(aeResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AEDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);
			
			aEService.deleteAEChild(findAEItem.getResourceID());
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Update(Root child)
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param aeProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}", method = RequestMethod.PUT, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> AEUpdate(@PathVariable String cseBaseResourceName
										 , @PathVariable String aeResourceName
										 , HttpServletRequest request
										 , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("AEUpdate start >>>");
		logger.debug("====================================================================");
		
		AE aeProfile				= (AE)mCommonService.getBodyObject(request, AE.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String aKey 	= CommonUtil.nvl(request.getHeader("aKey"), "");
		
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
		else if(CommonUtil.isEmpty(aeResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(aeProfile)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<AE> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AEUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}		
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);
			
			mgmtCmdService.getPointOfAccessList(aeProfile.getPointOfAccess());
			
			aeProfile.setResourceID(findAEItem.getResourceID());
			
			findAEItem = aEService.updateAE(aeProfile);
			
			if (!CommonUtil.isEmpty(findAEItem.getAKey())) {
				findAEItem.setAKey(Base64Coder.encodeString(findAEItem.getAKey()));
			}
			
			responseVo = findAEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Retrieve (Root child)
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> AERetrieve(@PathVariable String cseBaseResourceName
			 							   , @PathVariable String aeResourceName
			 							   , RequestPrimitive requestPrimitive
			 							   , FilterCriteria filterCriteria
										   , HttpServletRequest request
										   , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("AERetrieve start >>>");
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
		String uKey		= CommonUtil.nvl(request.getHeader("uKey"), "");;
		
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
		else if(CommonUtil.isEmpty(aeResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AERetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName, requestPrimitive, filterCriteria))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findAEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, findAEItem, requestPrimitive, filterCriteria);
				
				responseVo = findAEItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				AE emptyAEItem = new AE();
				mCommonService.search(findAEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, emptyAEItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyAEItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findAEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findAEItem;
			}
			
			if (!CommonUtil.isEmpty(findAEItem.getAKey())) {
				findAEItem.setAKey(Base64Coder.encodeString(findAEItem.getAKey()));
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AERetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AERetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Create (RemoteCSE child)
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.POST, produces = {"application/xml", "application/json"}, params = {"rty=2"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAECreate(@PathVariable String cseBaseResourceName
			 									  , @PathVariable String remoteCSEResourceName
												  , RequestPrimitive requestPrimitive
												  , HttpServletRequest request
												  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAECreate start >>>");
		logger.debug("====================================================================");
		
		AE aeProfile				= (AE)mCommonService.getBodyObject(request, AE.class);
		HttpHeaders responseHeaders	= new HttpHeaders();
		Object responseVo			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.CREATED;
		RSC responseCode			= RSC.CREATED;
		String responseMsg			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
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
		else if(CommonUtil.isEmpty(aeProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<AE> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(aeProfile.getAppID())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<AE> App-ID " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEAECreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			String appID = aeProfile.getAppID();
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
			
			mCommonService.checkPatternAEAppID(checkMccP, appID);
			
			AE findAEItem = null;
			if (!CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), name))) {
				
				responseVo = findAEItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.AE.duplicate.text"));
			}
			
			String url = CommonUtil.getURL(request);
			
			aeProfile.setResourceName(name);
			aeProfile.setParentID(findRemoteCSEItem.getResourceID());
			
			findAEItem = aEService.createAE(url, aeProfile, true);
			
			responseVo = findAEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAECreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAECreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Delete (RemoteCSE child)
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEDelete(@PathVariable String cseBaseResourceName
			 									, @PathVariable String remoteCSEResourceName
												, @PathVariable String aeResourceName
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEDelete start >>>");
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
		else if(CommonUtil.isEmpty(aeResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEAEDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
			
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			aEService.deleteAEChild(findAEItem.getResourceID());
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAEDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Update (RemoteCSE child)
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param aeProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}", method = RequestMethod.PUT, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEUpdate(@PathVariable String cseBaseResourceName
			 									  , @PathVariable String remoteCSEResourceName
												  , @PathVariable String aeResourceName
												  , HttpServletRequest request
												  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEUpdate start >>>");
		logger.debug("====================================================================");
		
		AE aeProfile 				= (AE)mCommonService.getBodyObject(request, AE.class);
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
		else if(CommonUtil.isEmpty(aeResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		else if(CommonUtil.isEmpty(aeProfile)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<AE> " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEAEUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
			
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			mgmtCmdService.getPointOfAccessList(aeProfile.getPointOfAccess());
			
			aeProfile.setResourceID(findAEItem.getResourceID());
			
			findAEItem = aEService.updateAE(aeProfile);
			
			responseVo = findAEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAEUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE Retrieve (RemoteCSE child)
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAERetrieve(@PathVariable String cseBaseResourceName
			 										, @PathVariable String remoteCSEResourceName
													, @PathVariable String aeResourceName
													, RequestPrimitive requestPrimitive
													, FilterCriteria filterCriteria
													, HttpServletRequest request
													, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAERetrieve start >>>");
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
		else if(CommonUtil.isEmpty(aeResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> remoteCSEAERetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName, requestPrimitive, filterCriteria))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findAEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, findAEItem, requestPrimitive, filterCriteria);
				
				responseVo = findAEItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				AE emptyAEItem = new AE();
				mCommonService.search(findAEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, emptyAEItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyAEItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findAEItem.getResourceRef().getResourceID(), RESOURCE_TYPE.AE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findAEItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAERetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAERetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
}

/**
 * Copyright (c) 2015, Youngsu Lee < yslee@ntels.com > < goraehimjul@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.controller.userAuth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.AE;
import kr.usis.iot.domain.oneM2M.AccessControlPolicy;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.LocationPolicy;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.LocationPolicyService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.service.userAuth.UserService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.RESULT_CONTENT;
import kr.usis.iot.util.oneM2M.CommonCode.RSC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ntels.nisf.util.PropertiesUtil;
import com.ntels.nisf.util.crypto.Sha256Util;

import biz.source_code.base64Coder.Base64Coder;

/**
 * User Management Action.
 * @author <ul>
 *         <li>Youngsu Lee < yslee@ntels.com > < goraehimjul@gmail.com ></li>
 *         </ul>
 */
@Controller
public class UserAction {

	private static final Logger logger = LoggerFactory.getLogger(UserAction.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private CSEBaseService cseBaseService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private AEService aEService;
	
	@Autowired
	private LocationPolicyService locationPolicyService;
	
	@Autowired
	private ContainerService containerService;
	
	
	/**
	 * User Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/userCreate", method = RequestMethod.POST, produces = {"application/xml", "application/json"}, params = "rty=1")
	@ResponseBody
	public ResponseEntity<Object> userCreate(@PathVariable String cseBaseResourceName
			 									, RequestPrimitive requestPrimitive
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("userCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders	= new HttpHeaders();
		AccessControlPolicy responseVo		= null;
		String responseTxt			= null;
		HttpStatus httpStatus		= HttpStatus.CREATED;
		RSC responseCode			= RSC.CREATED;
		String responseMsg			= "";
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) {
			responseCode	= RSC.OPERATION_NOT_ALLOWED;
			responseMsg		= CommonUtil.getMessage("msg.user.auth.config.disabled");
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> userCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}	
				
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "kr"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String passCode	= CommonUtil.nvl(request.getHeader("passCode"), "");
		
		String uKey = "";
		
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
		else if(CommonUtil.isEmpty(name)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "X-M2M-NM " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(passCode)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "passCode " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> userCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			 
			AccessControlPolicy findAccessControlPolicyItem = null;
			if(CommonUtil.isEmpty(findAccessControlPolicyItem = accessControlPolicyService.findOneAccessControlPolicyByResourceName(name))){
				
				uKey = userService.createUKey(checkMccP, name);
				
				String url = CommonUtil.getURL(request);
				url = url.replaceAll("\\/userCreate", "");
			
				findAccessControlPolicyItem = new AccessControlPolicy();
				
				findAccessControlPolicyItem.setParentID(findCSEBaseItem.getResourceID());
				findAccessControlPolicyItem.setResourceName(name);
				findAccessControlPolicyItem.setPassCode(Sha256Util.getEncrypt(passCode));
				findAccessControlPolicyItem = userService.createUserAccessControlPolicy(url, uKey, findAccessControlPolicyItem);
				
			} else {
				mongoLogService.log(logger, LEVEL.DEBUG, "findRemoteCSEItem : " + findAccessControlPolicyItem.toString());
				
				String oldPassCode = findAccessControlPolicyItem.getPassCode();
				if (!CommonUtil.isEmpty(oldPassCode) && !oldPassCode.equals(Sha256Util.getEncrypt(passCode))) {
					throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.passCode.error.text"));
				}

				uKey = userService.createUKey(checkMccP, name);
				
				findAccessControlPolicyItem = userService.updateAccessControlPolicyByUKey(findAccessControlPolicyItem, uKey);
				
				responseCode = RSC.CONFLICT;
				responseMsg = CommonUtil.getMessage("msg.user.duplicate.text");
				
			}
			
			responseVo = findAccessControlPolicyItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			logger.debug(">>> userCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("userCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * User Retrieve
	 * @param cseBaseResourceName
	 * @param accessControlPolicyResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/accessControlPolicy-{accessControlPolicyResourceName}/userRetrieve", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> userRetrieve(@PathVariable String cseBaseResourceName
			 							      , @PathVariable String accessControlPolicyResourceName
			 							      , HttpServletRequest request
			 							      , HttpServletResponse response){
		

		logger.debug("====================================================================");
		logger.debug("userRetrieve start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		AccessControlPolicy responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.OK;
		String responseMsg 			= "";
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) {
			responseCode	= RSC.OPERATION_NOT_ALLOWED;
			responseMsg		= CommonUtil.getMessage("msg.user.auth.config.disabled");
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> userRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "kr"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey		= CommonUtil.nvl(request.getHeader("uKey"), "");
		
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
		else if(CommonUtil.isEmpty(accessControlPolicyResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "accessControlPolicy resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(uKey)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "uKey " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> userRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AccessControlPolicy findAccessControlPolicyItem = null;
			if(CommonUtil.isEmpty(findAccessControlPolicyItem = accessControlPolicyService.findOneAccessControlPolicyByResourceName(accessControlPolicyResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.user.notExist.text"));
			}
			
			userService.verifyAccessControlPolicyByUKey(checkMccP, findAccessControlPolicyItem, uKey);
			
			responseVo = findAccessControlPolicyItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			logger.debug(">>> userRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("userRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * User Delete
	 * @param cseBaseResourceName
	 * @param accessControlPolicyResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/accessControlPolicy-{accessControlPolicyResourceName}/userDelete", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> userDelete(@PathVariable String cseBaseResourceName
											  , @PathVariable String accessControlPolicyResourceName
											  , HttpServletRequest request
											  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("userDelete start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.DELETED;
		String responseMsg 			= "";
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) {
			responseCode	= RSC.OPERATION_NOT_ALLOWED;
			responseMsg		= CommonUtil.getMessage("msg.user.auth.config.disabled");
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> userDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "kr"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey 	= CommonUtil.nvl(request.getHeader("uKey"), "");
		
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
		else if(CommonUtil.isEmpty(accessControlPolicyResourceName)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "accessControlPolicy resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(uKey)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "uKey " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> userDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			 
			AccessControlPolicy findAccessControlPolicyItem = null;
			if(CommonUtil.isEmpty(findAccessControlPolicyItem = accessControlPolicyService.findOneAccessControlPolicyByResourceName(accessControlPolicyResourceName))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.user.notExist.text"));
			}
			
			userService.verifyAccessControlPolicyByUKey(checkMccP, findAccessControlPolicyItem, uKey);
			userService. deleteUserAccessControlPolicy(findAccessControlPolicyItem);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			logger.debug(">>> userDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("userDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	/**
	 * remoteCSE Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/deviceUserMappingUpdate", method = RequestMethod.PUT, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> deviceUserMappingUpdate(@PathVariable String cseBaseResourceName
			 									, @PathVariable String remoteCSEResourceName
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("deviceUserMappingUpdate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		RemoteCSE responseVo 		= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) {
			responseCode	= RSC.OPERATION_NOT_ALLOWED;
			responseMsg		= CommonUtil.getMessage("msg.user.auth.config.disabled");
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> deviceUserMappingUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "kr"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
//		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String uKey 	= CommonUtil.nvl(request.getHeader("uKey"), "");
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
		else if(CommonUtil.isEmpty(uKey)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "uKey " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(passCode)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "passCode " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> deviceUserMappingUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			String oldPassCode = findRemoteCSEItem.getPassCode();
			if (!CommonUtil.isEmpty(oldPassCode) && !oldPassCode.equals(Sha256Util.getEncrypt(passCode))) {
				throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.passCode.error.text"));
			}
			
			AccessControlPolicy findAccessControlPolicyItem = null;
			if(CommonUtil.isEmpty(findAccessControlPolicyItem = accessControlPolicyService.findOneAccessControlPolicyByOriginators(Base64Coder.decodeString(uKey)))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.user.notExist.text"));
			}
			
			userService.verifyAccessControlPolicyByUKey(checkMccP, findAccessControlPolicyItem, uKey);
			
			findRemoteCSEItem = userService.updateDeviceAccessControlPolicyIDs(findRemoteCSEItem, findAccessControlPolicyItem.getResourceID());
			
			responseVo = findRemoteCSEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			logger.debug(">>> deviceUserMappingUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("deviceUserMappingUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * AE Update (Root child)
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName}/appUserMappingUpdate", method = RequestMethod.PUT, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> appUserMappingUpdate(@PathVariable String cseBaseResourceName
			 									, @PathVariable String aeResourceName
												, HttpServletRequest request
												, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("appUserMappingUpdate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		AE responseVo 		= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";
		
		if (!"Y".equals(PropertiesUtil.get("config", "iot.user.auth.use.yn"))) {
			responseCode	= RSC.OPERATION_NOT_ALLOWED;
			responseMsg		= CommonUtil.getMessage("msg.user.auth.config.disabled");
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> appUserMappingUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "kr"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
//		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String uKey 	= CommonUtil.nvl(request.getHeader("uKey"), "");
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
		else if(CommonUtil.isEmpty(uKey)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "uKey " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if(CommonUtil.isEmpty(passCode)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "passCode " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		
		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> appUserMappingUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}		
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			 
			AE findAEItem = null;
			if(CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.AE.noRegi.text"));
			}
			
			String oldPassCode = findAEItem.getPassCode();
			if (!CommonUtil.isEmpty(oldPassCode) && !oldPassCode.equals(Sha256Util.getEncrypt(passCode))) {
				throw new RSCException(RSC.BAD_REQUEST, CommonUtil.getMessage("msg.passCode.error.text"));
			}
			
			AccessControlPolicy findAccessControlPolicyItem = null;
			if(CommonUtil.isEmpty(findAccessControlPolicyItem = accessControlPolicyService.findOneAccessControlPolicyByOriginators(Base64Coder.decodeString(uKey)))){
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.user.notExist.text"));
			}
			
			userService.verifyAccessControlPolicyByUKey(checkMccP, findAccessControlPolicyItem, uKey);
			
			findAEItem = userService.updateAppAccessControlPolicyIDs(findAEItem, findAccessControlPolicyItem.getResourceID());
			
			responseVo = findAEItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			logger.debug(">>> appUserMappingUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("appUserMappingUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
}

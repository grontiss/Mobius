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
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.ExecInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.ApiLogService;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.ExecInstanceService;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.EXEC_STATUS;
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

/**
 * mgmtCmd management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Controller
public class MgmtCmdAction {

	private static final Log logger = LogFactory.getLog(MgmtCmdAction.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private CSEBaseService cseBaseService;

	@Autowired
	private RemoteCSEService remoteCSEService;

	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private ExecInstanceService execInstanceService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private ApiLogService apiLogService;
	
	/**
	 * mgmtCmd Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = "rty=12")
	@ResponseBody
	public ResponseEntity<Object> mgmtCmdCreate(@PathVariable String cseBaseResourceName
			 								  , @PathVariable String remoteCSEResourceName
											  , RequestPrimitive requestPrimitive
											  , HttpServletRequest request
											  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdCreate start >>>");
		logger.debug("====================================================================");
		
		MgmtCmd mgmtCmdProfile		= (MgmtCmd)mCommonService.getBodyObject(request, MgmtCmd.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.CREATED;
		RSC responseCode 			= RSC.CREATED;
		String responseMsg 			= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey		= CommonUtil.nvl(request.getHeader("dKey"), "");
		
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
		else if(CommonUtil.isEmpty(mgmtCmdProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<mgmtCmd> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(mgmtCmdProfile.getCmdType())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<mgmtCmd> cmdType " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(mgmtCmdProfile.getExecTarget())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<mgmtCmd> execTarget " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> mgmtCmdCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if (CommonUtil.isEmpty((findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
			
			MgmtCmd findMgmtCmdItem = null;
			if (!CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), name))) {
				responseVo = findMgmtCmdItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.mgmtCmd.duplicate.text"));
			}
			 
			mgmtCmdService.checkMgmtCmdValidation(mgmtCmdProfile);
			
			String url = CommonUtil.getURL(request);
			
			mgmtCmdProfile.setResourceName(name);
			mgmtCmdProfile.setParentID(findRemoteCSEItem.getResourceID());
			
			findMgmtCmdItem = mgmtCmdService.createMgmtCmd(url, mgmtCmdProfile);
			
			responseVo = findMgmtCmdItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> mgmtCmdCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * mgmtCmd Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/mgmtCmd-{mgmtCmdResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> mgmtCmdDelete(@PathVariable String cseBaseResourceName
			 								  , @PathVariable String remoteCSEResourceName
											  , @PathVariable String mgmtCmdResourceName
											  , HttpServletRequest request
											  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdDelete start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.DELETED;
		String responseMsg  		= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey		= CommonUtil.nvl(request.getHeader("dKey"), "");
		
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
		else if(CommonUtil.isEmpty(mgmtCmdResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> mgmtCmdDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if (CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}

			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);

			MgmtCmd findMgmtCmdItem = null;
			if (CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}

			mgmtCmdService.deleteMgmtCmd(findMgmtCmdItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> mgmtCmdDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	
	/**
	 * mgmtCmd Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param mgmtCmdProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/mgmtCmd-{mgmtCmdResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> mgmtCmdUpdate(@PathVariable String cseBaseResourceName
			 								  , @PathVariable String remoteCSEResourceName
											  , @PathVariable String mgmtCmdResourceName
											  , HttpServletRequest request
											  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdUpdate start >>>");
		logger.debug("====================================================================");
		
		MgmtCmd mgmtCmdProfile		= (MgmtCmd)mCommonService.getBodyObject(request, MgmtCmd.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey		= CommonUtil.nvl(request.getHeader("dKey"), "");
		
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
		else if(CommonUtil.isEmpty(mgmtCmdResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		else if(CommonUtil.isEmpty(mgmtCmdProfile)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<mgmtCmd> " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> mgmtCmdUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			MgmtCmd findMgmtCmdItem = null;
			if(CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}
			 
			mgmtCmdService.checkMgmtCmdValidation(mgmtCmdProfile);
			
			mgmtCmdProfile.setResourceID(findMgmtCmdItem.getResourceID());
			
			MgmtCmd mgmtCmdItem = mgmtCmdService.updateMgmtCmd(mgmtCmdProfile);
			
			responseVo = mgmtCmdItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> mgmtCmdUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * mgmtCmd Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/mgmtCmd-{mgmtCmdResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> mgmtCmdRetrieve(@PathVariable String cseBaseResourceName
			 									, @PathVariable String remoteCSEResourceName
												, @PathVariable String mgmtCmdResourceName
												, RequestPrimitive requestPrimitive
												, FilterCriteria filterCriteria
												, HttpServletRequest request
												, HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdRetrieve start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.OK;
		String responseMsg  		= "";
		
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
		else if(CommonUtil.isEmpty(mgmtCmdResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> mgmtCmdRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty((findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			MgmtCmd findMgmtCmdItem = null;
			if(CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.notFound.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findMgmtCmdItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_CMD, null, findMgmtCmdItem, requestPrimitive, filterCriteria);
				
				responseVo = findMgmtCmdItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				MgmtCmd emptyMgmtCmdItem = new MgmtCmd();
				mCommonService.search(findMgmtCmdItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_CMD, null, emptyMgmtCmdItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyMgmtCmdItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findMgmtCmdItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_CMD, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findMgmtCmdItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> mgmtCmdRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * mgmtCmd Control Request
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param mgmtCmdProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/mgmtCmd-{mgmtCmdResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = "rty=8")
	@ResponseBody
	public ResponseEntity<Object> mgmtCmdControl(@PathVariable String cseBaseResourceName
			 								   , @PathVariable String remoteCSEResourceName
											   , @PathVariable String mgmtCmdResourceName
											   , HttpServletRequest request
											   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdControl start >>>");
		logger.debug("====================================================================");
		
		MgmtCmd mgmtCmdProfile		= (MgmtCmd)mCommonService.getBodyObject(request, MgmtCmd.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CREATED;
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
		else if(CommonUtil.isEmpty(mgmtCmdResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		else if(CommonUtil.isEmpty(mgmtCmdProfile)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<mgmtCmd> " + CommonUtil.getMessage("msg.input.empty.text");
			
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> mgmtCmdControl ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if(CommonUtil.isEmpty((findRemoteCSEItem = remoteCSEService.findOneRemoteCSEByResourceName(findCSEBaseItem.getResourceID(), remoteCSEResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			if(mgmtCmdService.getCountByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName) < 1) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}
			
			String url = CommonUtil.getURL(request);
			
			mgmtCmdProfile.setResourceName(mgmtCmdResourceName);
			mgmtCmdProfile.setParentID(findRemoteCSEItem.getResourceID());
			ExecInstance findExecInstanceItem = mgmtCmdService.mgmtCmdControl(url, mgmtCmdProfile);
			
			responseVo = findExecInstanceItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> mgmtCmdControl ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdControl end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	

	/**
	 * mgmtCmd Control Result Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param execInstanceResourceName
	 * @param execInstanceProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/mgmtCmd-{mgmtCmdResourceName}/execInstance-{execInstanceResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> mgmtCmdResultUpdate(@PathVariable String cseBaseResourceName
			 										, @PathVariable String remoteCSEResourceName
													, @PathVariable String mgmtCmdResourceName
													, @PathVariable String execInstanceResourceName
													, HttpServletRequest request
													, HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdResultUpdate start >>>");
		logger.debug("====================================================================");
		
		ExecInstance execInstanceProfile= (ExecInstance)mCommonService.getBodyObject(request, ExecInstance.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.OK;
		RSC responseCode 				= RSC.CHANGED;
		String responseMsg 				= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey		= CommonUtil.nvl(request.getHeader("dKey"), "");
		String sKey		= CommonUtil.nvl(request.getHeader("sKey"), "");
		
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
		else if(CommonUtil.isEmpty(mgmtCmdResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(execInstanceResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "execInstance resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(execInstanceProfile)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<execInstance> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> mgmtCmdResultUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			MgmtCmd findMgmtCmdItem = null;
			if(CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}
			
			ExecInstance findExecInstanceItem = null;
			if(CommonUtil.isEmpty(findExecInstanceItem = execInstanceService.findOneExecInstanceByResourceName(findMgmtCmdItem.getResourceID(), execInstanceResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.execInstance.noRegi.text"));
			}
			
			if(findExecInstanceItem.getExecStatus().equals(EXEC_STATUS.FINISHED.getValue())) {
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.execInstance.finish.text"));
			}			
			
			execInstanceProfile.setResourceID(findExecInstanceItem.getResourceID());
			
			findExecInstanceItem = mgmtCmdService.mgmtCmdResultUpdate(findMgmtCmdItem, execInstanceProfile, findExecInstanceItem.getExecReqArgs());
			
			responseVo = findExecInstanceItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> mgmtCmdResultUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("mgmtCmdResultUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}


}
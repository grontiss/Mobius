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
 * execInstance management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Controller
public class ExecInstanceAction {

	private static final Log logger = LogFactory.getLog(ExecInstanceAction.class);
	
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
	 * execInstance Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param execInstanceResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/mgmtCmd-{mgmtCmdResourceName}/execInstance-{execInstanceResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> execInstanceRetrieve(@PathVariable String cseBaseResourceName
			 										 , @PathVariable String remoteCSEResourceName
													 , @PathVariable String mgmtCmdResourceName
													 , @PathVariable String execInstanceResourceName
													 , RequestPrimitive requestPrimitive
													 , FilterCriteria filterCriteria
													 , HttpServletRequest request
													 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("execInstanceRetrieve start >>>");
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
		else if(CommonUtil.isEmpty(execInstanceResourceName)){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "execInstance resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> execInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			MgmtCmd findMgmtCmdItem = null;
			if(CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}
			
			ExecInstance findExecInstanceItem = null;
			if (CommonUtil.isEmpty(findExecInstanceItem = execInstanceService.findOneExecInstanceByResourceName(findMgmtCmdItem.getResourceID(), execInstanceResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.execInstance.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findExecInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.EXEC_INSTANCE, null, findExecInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = findExecInstanceItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				ExecInstance emptyExecInstanceItem = new ExecInstance();
				mCommonService.search(findExecInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.EXEC_INSTANCE, null, emptyExecInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyExecInstanceItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findExecInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.EXEC_INSTANCE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findExecInstanceItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> execInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("execInstanceRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}


	/**
	 * pollingExecInstance Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/mgmtCmd-{mgmtCmdResourceName}/execInstance", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> pollingExecInstanceRetrieve(@PathVariable String cseBaseResourceName
			 												, @PathVariable String remoteCSEResourceName
															, @PathVariable String mgmtCmdResourceName
															, HttpServletRequest request
															, HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("pollingExecInstanceRetrieve start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		MgmtCmd responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.OK;
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
			logger.debug(">>> pollingExecInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			MgmtCmd findMgmtCmdItem = null;
			if(CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);			
			
			List<ExecInstance> findExecInstanceList = null;
			if (CommonUtil.isEmpty(findExecInstanceList = execInstanceService.pollingExecInstanceRetrieveByParentID(findMgmtCmdItem.getResourceID()))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.execInstance.noRegi.text"));
			}
			
			responseVo = new MgmtCmd();
			responseVo.getExecInstance().addAll(findExecInstanceList);
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> pollingExecInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("pollingExecInstanceRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

}
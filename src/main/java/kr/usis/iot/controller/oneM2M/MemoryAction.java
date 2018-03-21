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
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Memory;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.MemoryService;
import kr.usis.iot.service.oneM2M.NodeService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.FILTER_USAGE;
import kr.usis.iot.util.oneM2M.CommonCode.MGMT_DEFINITION;
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
 * memory management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Controller
public class MemoryAction {

	private static final Log logger = LogFactory.getLog(MemoryAction.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private CSEBaseService cseBaseService;

	@Autowired
	private RemoteCSEService remoteCSEService;

	@Autowired
	private NodeService nodeService;

	@Autowired
	private MemoryService memoryService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;

	/**
	 * memory Create
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param memoryProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName:.*}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=13", "mgd=1003"})
	@ResponseBody
	public ResponseEntity<Object> memoryCreate(@PathVariable String cseBaseResourceName
			 								 , @PathVariable String nodeResourceName
			 								 , RequestPrimitive requestPrimitive
			 								 , HttpServletRequest request
			 								 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("memoryCreate start >>>");
		logger.debug("====================================================================");
		
		Memory memoryProfile		= (Memory)mCommonService.getBodyObject(request, Memory.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.CREATED;
		RSC responseCode 			= RSC.CREATED;
		String responseMsg 			= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
	String from 		= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 		= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID	= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 		= CommonUtil.nvl(request.getHeader("dKey"), "");

		boolean checkMccP	= mCommonService.getCheckMccP(from);
		boolean isError 	= false;
		
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
		else if (CommonUtil.isEmpty(nodeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "node resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(memoryProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<memory> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(memoryProfile.getMemAvailable())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<memory> memAvailable " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(memoryProfile.getMemTotal())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<memory> memTotal " + CommonUtil.getMessage("msg.input.empty.text");
		}


		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> memoryCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			Node findNodeItem = null;
			if (CommonUtil.isEmpty((findNodeItem = nodeService.findOneNodeByResourceName(findCSEBaseItem.getResourceID(), nodeResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.node.noRegi.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if (CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findNodeItem.getHostedCSELink()))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}

			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);

			Memory memoryItem = null;
			if (!CommonUtil.isEmpty(memoryItem = memoryService.findOneMemoryByResourceName(findNodeItem.getResourceID(), name))) {
				responseVo = memoryItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.memory.duplicate.text"));
			}
			
			String url = CommonUtil.getURL(request);
			
			memoryProfile.setResourceName(name);
			memoryProfile.setParentID(findNodeItem.getResourceID());
			
			memoryItem = memoryService.createMemory(url, memoryProfile);
			
			responseVo = memoryItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> memoryCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("memoryCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * memory Update
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param memoryResourceName
	 * @param memoryProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName}/memory-{memoryResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> memoryUpdate(@PathVariable String cseBaseResourceName
			 								 , @PathVariable String nodeResourceName
			 								 , @PathVariable String memoryResourceName
			 								 , HttpServletRequest request
			 								 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("memoryUpdate start >>>");
		logger.debug("====================================================================");
		
		Memory memoryProfile		= (Memory)mCommonService.getBodyObject(request, Memory.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 		= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID	= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 		= CommonUtil.nvl(request.getHeader("dKey"), "");

		boolean checkMccP	= mCommonService.getCheckMccP(from);
		boolean isError		= false;
		
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
		else if (CommonUtil.isEmpty(nodeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "node resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(memoryResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "memory resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(memoryProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<memory> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> memoryUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			Node findNodeItem = null;
			if (CommonUtil.isEmpty(findNodeItem = nodeService.findOneNodeByResourceName(findCSEBaseItem.getResourceID(), nodeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.node.noRegi.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if (CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findNodeItem.getHostedCSELink()))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}

			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);

			Memory findMemoryItem = null;
			if (CommonUtil.isEmpty((findMemoryItem = memoryService.findOneMemoryByResourceName(findNodeItem.getResourceID(), memoryResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.memory.noRegi.text"));
			}

			memoryProfile.setResourceID(findMemoryItem.getResourceID());
			
			findMemoryItem = memoryService.updateMemory(memoryProfile);
			
			responseVo = findMemoryItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> memoryUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("memoryUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * memory Delete
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param memoryResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName}/memory-{memoryResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> memoryDelete(@PathVariable String cseBaseResourceName
			 								 , @PathVariable String nodeResourceName
			 								 , @PathVariable String memoryResourceName
			 								 , HttpServletRequest request
			 								 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("memoryDelete start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.DELETED;
		String responseMsg 			= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 		= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID	= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 		= CommonUtil.nvl(request.getHeader("dKey"), "");

		boolean checkMccP 	= mCommonService.getCheckMccP(from);
		boolean isError 	= false;
		
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
		else if (CommonUtil.isEmpty(nodeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "node resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(memoryResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "memory resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> memoryDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			Node findNodeItem = null;
			if (CommonUtil.isEmpty(findNodeItem = nodeService.findOneNodeByResourceName(findCSEBaseItem.getResourceID(), nodeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.node.noRegi.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if (CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findNodeItem.getHostedCSELink()))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}

			accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);

			Memory findMemoryItem = null;
			if (CommonUtil.isEmpty((findMemoryItem = memoryService.findOneMemoryByResourceName(findNodeItem.getResourceID(), memoryResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.memory.noRegi.text"));
			}

			memoryService.deleteMemory("resourceID", findMemoryItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> memoryDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("memoryDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * memory Retrieve
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param memoryResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName}/memory-{memoryResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> memoryRetrieve(@PathVariable String cseBaseResourceName
			 								   , @PathVariable String nodeResourceName
			 								   , @PathVariable String memoryResourceName
			 								   , RequestPrimitive requestPrimitive
			 								   , FilterCriteria filterCriteria
			 								   , HttpServletRequest request
			 								   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("memoryRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(nodeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "node resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(memoryResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "memory resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> memoryRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			Node findNodeItem = null;
			if (CommonUtil.isEmpty(findNodeItem = nodeService.findOneNodeByResourceName(findCSEBaseItem.getResourceID(), nodeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.node.noRegi.text"));
			}
			
			RemoteCSE findRemoteCSEItem = null;
			if (CommonUtil.isEmpty(findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findNodeItem.getHostedCSELink()))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.noRegi.text"));
			}
			
			Memory findMemoryItem = null;
			if (CommonUtil.isEmpty((findMemoryItem = memoryService.findOneMemoryByResourceName(findNodeItem.getResourceID(), memoryResourceName, requestPrimitive, filterCriteria)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.memory.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findMemoryItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.MEMORY, findMemoryItem, requestPrimitive, filterCriteria);
				
				responseVo = findMemoryItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Memory emptyMemoryItem = new Memory();
				mCommonService.search(findMemoryItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.MEMORY, emptyMemoryItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyMemoryItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findMemoryItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.MEMORY, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findMemoryItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> memoryRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("memoryRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
}
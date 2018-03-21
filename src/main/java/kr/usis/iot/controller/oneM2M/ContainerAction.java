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
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.ContainerService;
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
 * container management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Controller
public class ContainerAction {

	private static final Log logger = LogFactory.getLog(ContainerAction.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private CSEBaseService cseBaseService;

	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private AEService aEService;

	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;

	/**
	 * container Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=3"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerCreate(@PathVariable String cseBaseResourceName
														 , @PathVariable String remoteCSEResourceName
														 , RequestPrimitive requestPrimitive
														 , HttpServletRequest request
														 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerCreate start >>>");
		logger.debug("====================================================================");
		
		Container containerProfile	= (Container)mCommonService.getBodyObject(request, Container.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.CREATED;
		RSC responseCode 			= RSC.CREATED;
		String responseMsg 			= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name		= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(containerProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<container> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEContainerCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Container findContainerItem = null;
			if (!CommonUtil.isEmpty((findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), name)))) {
				responseVo = findContainerItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.container.duplicate.text"));
			}
			
			String url = CommonUtil.getURL(request);
			
			containerProfile.setResourceName(name);
			containerProfile.setParentID(findRemoteCSEItem.getResourceID());
			containerProfile.setCreator(findRemoteCSEItem.getResourceID());
			
			Container containerItem = containerService.createContainer(url, containerProfile);
			
			responseVo = containerItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * container Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/container-{containerResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerDelete(@PathVariable String cseBaseResourceName
														 , @PathVariable String remoteCSEResourceName
														 , @PathVariable String containerResourceName
														 , HttpServletRequest request
														 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerDelete start >>>");
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEContainerDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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

			Container containerItem = null;
			if (CommonUtil.isEmpty(containerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			containerService.deleteContainer(containerItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * container Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param containerProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/container-{containerResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerUpdate(@PathVariable String cseBaseResourceName
														 , @PathVariable String remoteCSEResourceName
														 , @PathVariable String containerResourceName
														 , HttpServletRequest request
														 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerUpdate start >>>");
		logger.debug("====================================================================");
		
		Container containerProfile	= (Container)mCommonService.getBodyObject(request, Container.class);
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(containerProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<container> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEContainerUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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

			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			containerProfile.setResourceID(findContainerItem.getResourceID());
			
			findContainerItem = containerService.updateContainer(containerProfile);
			
			responseVo = findContainerItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * container Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/container-{containerResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerRetrieve(@PathVariable String cseBaseResourceName
														   , @PathVariable String remoteCSEResourceName
														   , @PathVariable String containerResourceName
														   , RequestPrimitive requestPrimitive
														   , FilterCriteria filterCriteria
														   , HttpServletRequest request
														   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEContainerRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, findContainerItem, requestPrimitive, filterCriteria);
				
				responseVo = findContainerItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Container emptyContainerItem = new Container();
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, emptyContainerItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyContainerItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findContainerItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * AE container Create
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=3"})
	@ResponseBody
	public ResponseEntity<Object> AEContainerCreate(@PathVariable String cseBaseResourceName
												  , @PathVariable String aeResourceName
												  , RequestPrimitive requestPrimitive
												  , HttpServletRequest request
												  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerCreate start >>>");
		logger.debug("====================================================================");
		
		Container containerProfile	= (Container)mCommonService.getBodyObject(request, Container.class);
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
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(containerProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<container> " + CommonUtil.getMessage("msg.input.empty.text");
		}

		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AEContainerCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}

			accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);

			Container findContainerItem = null;
			if (!CommonUtil.isEmpty((findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), name)))) {
				responseVo = findContainerItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.container.duplicate.text"));
			}

			String url = CommonUtil.getURL(request);
			
			containerProfile.setResourceName(name);
			containerProfile.setParentID(findAEItem.getResourceID());
			containerProfile.setCreator(findAEItem.getResourceID());
			
			Container containerItem = containerService.createContainer(url, containerProfile);
			
			responseVo = containerItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEContainerCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("AEContainerCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * AE container Delete
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContainerDelete(@PathVariable String cseBaseResourceName
												  , @PathVariable String aeResourceName
												  , @PathVariable String containerResourceName
												  , HttpServletRequest request
												  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerDelete start >>>");
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
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AEContainerDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}

			accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);

			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			containerService.deleteContainer(findContainerItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEContainerDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContainerDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	/**
	 * AE container Update
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param containerProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContainerUpdate(@PathVariable String cseBaseResourceName
												  , @PathVariable String aeResourceName
												  , @PathVariable String containerResourceName
												  , HttpServletRequest request
												  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerUpdate start >>>");
		logger.debug("====================================================================");
		
		Container containerProfile	= (Container)mCommonService.getBodyObject(request, Container.class);
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
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(containerProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<container> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AEContainerUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);

			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			containerProfile.setParentID(findAEItem.getResourceID());
			containerProfile.setResourceID(findContainerItem.getResourceID());
			
			findContainerItem = containerService.updateContainer(containerProfile);
			
			responseVo = findContainerItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEContainerUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContainerUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * AE container Retrieve
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContainerRetrieve(@PathVariable String cseBaseResourceName
													, @PathVariable String aeResourceName
													, @PathVariable String containerResourceName
													, RequestPrimitive requestPrimitive
													, FilterCriteria filterCriteria
													, HttpServletRequest request
													, HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> AEContainerRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}

			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, findContainerItem, requestPrimitive, filterCriteria);
				
				responseVo = findContainerItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Container emptyContainerItem = new Container();
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, emptyContainerItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyContainerItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findContainerItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> AEContainerRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContainerRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * RemoteCSE AE container Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=3"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContainerCreate(@PathVariable String cseBaseResourceName
														   , @PathVariable String remoteCSEResourceName
														   , @PathVariable String aeResourceName
														   , RequestPrimitive requestPrimitive
														   , HttpServletRequest request
														   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerCreate start >>>");
		logger.debug("====================================================================");
		
		Container containerProfile	= (Container)mCommonService.getBodyObject(request, Container.class);
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(containerProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<container> " + CommonUtil.getMessage("msg.input.empty.text");
		}

		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> remoteCSEAEContainerCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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

			AE findAEItem = null;
			if (CommonUtil.isEmpty((findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}

			Container findContainerItem = null;
			if (!CommonUtil.isEmpty((findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), name)))) {
				responseVo = findContainerItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.container.duplicate.text"));
			}

			String url = CommonUtil.getURL(request);
			
			containerProfile.setResourceName(name);
			containerProfile.setParentID(findAEItem.getResourceID());
			containerProfile.setCreator(findRemoteCSEItem.getResourceID());
			
			Container containerItem = containerService.createContainer(url, containerProfile);
			
			responseVo = containerItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContainerCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * RemoteCSE AE container Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContainerDelete(@PathVariable String cseBaseResourceName
														   , @PathVariable String remoteCSEResourceName
														   , @PathVariable String aeResourceName
														   , @PathVariable String containerResourceName
														   , HttpServletRequest request
														   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerDelete start >>>");
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContainerDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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

			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}

			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			containerService.deleteContainer(findContainerItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContainerDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * RemoteCSE AE container Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param containerProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContainerUpdate(@PathVariable String cseBaseResourceName
														   , @PathVariable String remoteCSEResourceName
														   , @PathVariable String aeResourceName
														   , @PathVariable String containerResourceName
														   , HttpServletRequest request
														   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerUpdate start >>>");
		logger.debug("====================================================================");
		
		Container containerProfile	= (Container)mCommonService.getBodyObject(request, Container.class);
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(containerProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<container> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContainerUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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

			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}

			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			containerProfile.setParentID(findAEItem.getResourceID());
			containerProfile.setResourceID(findContainerItem.getResourceID());
			
			findContainerItem = containerService.updateContainer(containerProfile);
			
			responseVo = findContainerItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContainerUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	

	/**
	 * RemoteCSE AE container Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContainerRetrieve(@PathVariable String cseBaseResourceName
															 , @PathVariable String remoteCSEResourceName
															 , @PathVariable String aeResourceName
															 , @PathVariable String containerResourceName
															 , RequestPrimitive requestPrimitive
															 , FilterCriteria filterCriteria
															 , HttpServletRequest request
															 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(remoteCSEResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "remoteCSE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "AE resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContainerRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aEService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, findContainerItem, requestPrimitive, filterCriteria);
				
				responseVo = findContainerItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Container emptyContainerItem = new Container();
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, emptyContainerItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyContainerItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findContainerItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTAINER, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findContainerItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContainerRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
}
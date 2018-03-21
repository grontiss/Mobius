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
import kr.usis.iot.domain.oneM2M.MgmtCmd;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.Subscription;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.MgmtCmdService;
import kr.usis.iot.service.oneM2M.RemoteCSEService;
import kr.usis.iot.service.oneM2M.SubscriptionService;
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
 * subscription management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Controller
public class SubscriptionAction {

	private static final Log logger = LogFactory.getLog(SubscriptionAction.class);

	@Autowired
	private MongoLogService mongoLogService;

	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private CSEBaseService cseBaseService;

	@Autowired
	private RemoteCSEService remoteCSEService;
	
	@Autowired
	private AEService aeService;	

	@Autowired
	private ContainerService containerService;
	
	@Autowired
	private MgmtCmdService mgmtCmdService;
	
	@Autowired
	private SubscriptionService subscriptionService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;	

	/**
	 * container subscription Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param subscriptionProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=23"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerSubscriptionCreate(@PathVariable String cseBaseResourceName
			 														 , @PathVariable String remoteCSEResourceName
																	 , @PathVariable String containerResourceName
																	 , RequestPrimitive requestPrimitive
																	 , HttpServletRequest request
																	 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionCreate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.CREATED;
		RSC responseCode 				= RSC.CREATED;
		String responseMsg 				= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");

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
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationURI())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationURI " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationContentType())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationContentType " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContainerSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (!CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), name))) {
				responseVo = findSubscriptionItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.subscription.duplicate.text"));
			}
			
			subscriptionService.checkSubscriptionValidation(subscriptionProfile);

			String url = CommonUtil.getURL(request);
			
			subscriptionProfile.setResourceName(name);
			subscriptionProfile.setParentID(findContainerItem.getResourceID());
			
			Subscription subscriptionItem = subscriptionService.createSubscription(url, subscriptionProfile);
			
			responseVo = subscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * container subscription Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerSubscriptionDelete(@PathVariable String cseBaseResourceName
			 														 , @PathVariable String remoteCSEResourceName
																	 , @PathVariable String containerResourceName
																	 , @PathVariable String subscriptionResourceName
																	 , HttpServletRequest request
																	 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionDelete start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");

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
			responseMsg 	= "containerResourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContainerSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}

			subscriptionService.deleteSubscription(findSubscriptionItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * container subscription Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param subscriptionProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerSubscriptionUpdate(@PathVariable String cseBaseResourceName
			 														 , @PathVariable String remoteCSEResourceName
																	 , @PathVariable String containerResourceName
																	 , @PathVariable String subscriptionResourceName
																	 , HttpServletRequest request
																	 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionUpdate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.OK;
		RSC responseCode 				= RSC.CHANGED;
		String responseMsg 				= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");

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
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContainerSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}

			subscriptionService.checkSubscriptionValidation(subscriptionProfile);

			subscriptionProfile.setResourceID(findSubscriptionItem.getResourceID());
			
			findSubscriptionItem = subscriptionService.updateSubscription(subscriptionProfile);
			
			responseVo = findSubscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * container subscription Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContainerSubscriptionRetrieve(@PathVariable String cseBaseResourceName
			 														   , @PathVariable String remoteCSEResourceName
																	   , @PathVariable String containerResourceName
																	   , @PathVariable String subscriptionResourceName
																	   , RequestPrimitive requestPrimitive
																	   , FilterCriteria filterCriteria
																	   , HttpServletRequest request
																	   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionRetrieve start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");
		
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
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceID " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContainerSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}

			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, findSubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = findSubscriptionItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Subscription emptySubscriptionItem = new Subscription();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, emptySubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = emptySubscriptionItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findSubscriptionItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEContainerSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContainerSubscriptionRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * mgmtCmd subscription Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param subscriptionProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/mgmtCmd-{mgmtCmdResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=23"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEMgmtCmdSubscriptionCreate(@PathVariable String cseBaseResourceName
			 													   , @PathVariable String remoteCSEResourceName
																   , @PathVariable String mgmtCmdResourceName
																   , RequestPrimitive requestPrimitive
																   , HttpServletRequest request
																   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionCreate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.CREATED;
		RSC responseCode 				= RSC.CREATED;
		String responseMsg 				= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");

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
		else if (CommonUtil.isEmpty(mgmtCmdResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationURI())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationURI " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationContentType())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationContentType " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (!CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findMgmtCmdItem.getResourceID(), name))) {
				responseVo = findSubscriptionItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.subscription.duplicate.text"));
			}

			subscriptionService.checkSubscriptionValidation(subscriptionProfile);

			String url = CommonUtil.getURL(request);
			
			subscriptionProfile.setResourceName(name);
			subscriptionProfile.setParentID(findMgmtCmdItem.getResourceID());
			
			Subscription subscriptionItem = subscriptionService.createSubscription(url, subscriptionProfile);
			
			responseVo = subscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * mgmtCmd subscription Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param subscriptionResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/mgmtCmd-{mgmtCmdResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEMgmtCmdSubscriptionDelete(@PathVariable String cseBaseResourceName
			 													   , @PathVariable String remoteCSEResourceName
																   , @PathVariable String mgmtCmdResourceName
																   , @PathVariable String subscriptionResourceName
																   , HttpServletRequest request
																   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionDelete start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");

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
		else if (CommonUtil.isEmpty(mgmtCmdResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}

			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findMgmtCmdItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}

			subscriptionService.deleteSubscription(findSubscriptionItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * mgmtCmd subscription Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param subscriptionResourceName
	 * @param subscriptionProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/mgmtCmd-{mgmtCmdResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEMgmtCmdSubscriptionUpdate(@PathVariable String cseBaseResourceName
			 													   , @PathVariable String remoteCSEResourceName
																   , @PathVariable String mgmtCmdResourceName
																   , @PathVariable String subscriptionResourceName
																   , HttpServletRequest request
																   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionUpdate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.OK;
		RSC responseCode 				= RSC.CHANGED;
		String responseMsg 				= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");

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
		else if (CommonUtil.isEmpty(mgmtCmdResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}

			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findMgmtCmdItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			subscriptionService.checkSubscriptionValidation(subscriptionProfile);

			subscriptionProfile.setResourceID(findSubscriptionItem.getResourceID());
			subscriptionProfile.setCreator(findRemoteCSEItem.getResourceID());
			
			findSubscriptionItem = subscriptionService.updateSubscription(subscriptionProfile);
			
			responseVo = findSubscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * mgmtCmd subscription Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param mgmtCmdResourceName
	 * @param subscriptionResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/mgmtCmd-{mgmtCmdResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEMgmtCmdSubscriptionRetrieve(@PathVariable String cseBaseResourceName
			 														 , @PathVariable String remoteCSEResourceName
																	 , @PathVariable String mgmtCmdResourceName
																	 , @PathVariable String subscriptionResourceName
																	 , RequestPrimitive requestPrimitive
																	 , FilterCriteria filterCriteria
																	 , HttpServletRequest request
																	 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionRetrieve start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");
		
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
		else if (CommonUtil.isEmpty(mgmtCmdResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "mgmtCmd resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findMgmtCmdItem = mgmtCmdService.findOneMgmtCmdByResourceName(findRemoteCSEItem.getResourceID(), mgmtCmdResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.mgmtCmd.noRegi.text"));
			}

			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findMgmtCmdItem.getResourceID(), subscriptionResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, findSubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = findSubscriptionItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Subscription emptySubscriptionItem = new Subscription();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, emptySubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = emptySubscriptionItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findSubscriptionItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEMgmtCmdSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEMgmtCmdSubscriptionRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * AE-container subscription Create
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=23"})
	@ResponseBody
	public ResponseEntity<Object> AEContainerSubscriptionCreate(@PathVariable String cseBaseResourceName
			 												  , @PathVariable String aeResourceName
			 												  , @PathVariable String containerResourceName
			 												  , RequestPrimitive requestPrimitive
			 												  , HttpServletRequest request
			 												  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionCreate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.CREATED;
		RSC responseCode 				= RSC.CREATED;
		String responseMsg 				= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
	
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
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationURI())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationURI " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationContentType())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationContentType " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContainerSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");	
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
	
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (!CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), name))) {
				responseVo = findSubscriptionItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.subscription.duplicate.text"));
			}
			
			subscriptionService.checkSubscriptionValidation(subscriptionProfile);
			
			String url = CommonUtil.getURL(request);
			
			subscriptionProfile.setResourceName(name);
			subscriptionProfile.setParentID(findContainerItem.getResourceID());
			
			Subscription subscriptionItem = subscriptionService.createSubscription(url, subscriptionProfile);
			
			responseVo = subscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
	
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEContainerSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");	
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * AE-container subscription Delete
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContainerSubscriptionDelete(@PathVariable String cseBaseResourceName
			 												  , @PathVariable String aeResourceName
			 												  , @PathVariable String containerResourceName
			 												  , @PathVariable String subscriptionResourceName
			 												  , HttpServletRequest request
			 												  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionDelete start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
	
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
			responseMsg 	= "container ResourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContainerSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
	
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			subscriptionService.deleteSubscription(findSubscriptionItem.getResourceID());
	
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEContainerSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");	
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * AE-container subscription Update
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param subscriptionProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContainerSubscriptionUpdate(@PathVariable String cseBaseResourceName
			 												  , @PathVariable String aeResourceName
															  , @PathVariable String containerResourceName
															  , @PathVariable String subscriptionResourceName
															  , HttpServletRequest request
															  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionUpdate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.OK;
		RSC responseCode 				= RSC.CHANGED;
		String responseMsg 				= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
	
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
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContainerSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
	
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			subscriptionService.checkSubscriptionValidation(subscriptionProfile);
			
			subscriptionProfile.setResourceID(findSubscriptionItem.getResourceID());
			
			findSubscriptionItem = subscriptionService.updateSubscription(subscriptionProfile);
			
			responseVo = findSubscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
	
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> AEContainerSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");	
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * AE-container subscription Retrieve
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContainerSubscriptionRetrieve(@PathVariable String cseBaseResourceName
			 													, @PathVariable String aeResourceName
																, @PathVariable String containerResourceName
																, @PathVariable String subscriptionResourceName
																, RequestPrimitive requestPrimitive
																, FilterCriteria filterCriteria
																, HttpServletRequest request
																, HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionRetrieve start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		
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
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContainerSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			AE findAEItem = null;
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findCSEBaseItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, findSubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = findSubscriptionItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Subscription emptySubscriptionItem = new Subscription();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, emptySubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = emptySubscriptionItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findSubscriptionItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> AEContainerSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");	
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContainerSubscriptionRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * remoteCSE-AE-container subscription Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=23"})
	@ResponseBody
	public ResponseEntity<Object> remortCSEAEContainerSubscriptionCreate(@PathVariable String cseBaseResourceName
			 												  		   , @PathVariable String remoteCSEResourceName
			 												  		   , @PathVariable String aeResourceName
			 												  		   , @PathVariable String containerResourceName
			 												  		   , RequestPrimitive requestPrimitive
			 												  		   , HttpServletRequest request
			 												  		   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remortCSEAEContainerSubscriptionCreate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.CREATED;
		RSC responseCode 				= RSC.CREATED;
		String responseMsg 				= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");
	
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
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationURI())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationURI " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(subscriptionProfile.getNotificationContentType())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<subscription> notificationContentType " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remortCSEAEContainerSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty((findAEItem = aeService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (!CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), name))) {
				responseVo = findSubscriptionItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.subscription.duplicate.text"));
			}
			
			subscriptionService.checkSubscriptionValidation(subscriptionProfile);
			
			String url = CommonUtil.getURL(request);
			
			subscriptionProfile.setResourceName(name);
			subscriptionProfile.setParentID(findContainerItem.getResourceID());
			
			Subscription subscriptionItem = subscriptionService.createSubscription(url, subscriptionProfile);
			
			responseVo = subscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
	
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remortCSEAEContainerSubscriptionCreate ERROR message ["+responseCode+":"+responseMsg+"]");		
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remortCSEAEContainerSubscriptionCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * remoteCSE-AE-container subscription Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remortCSEAEContainerSubscriptionDelete(@PathVariable String cseBaseResourceName
			 												  		   , @PathVariable String remoteCSEResourceName
			 												  		   , @PathVariable String aeResourceName
			 												  		   , @PathVariable String containerResourceName
			 												  		   , @PathVariable String subscriptionResourceName
			 												  		   , HttpServletRequest request
			 												  		   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remortCSEAEContainerSubscriptionDelete start >>>");
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
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");
	
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
			responseMsg 	= "container ResourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remortCSEAEContainerSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			subscriptionService.deleteSubscription(findSubscriptionItem.getResourceID());
	
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remortCSEAEContainerSubscriptionDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remortCSEAEContainerSubscriptionDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * remoteCSE-AE-container subscription Update
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param subscriptionProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remortCSEAEContainerSubscriptionUpdate(@PathVariable String cseBaseResourceName
			 														   , @PathVariable String remoteCSEResourceName
			 														   , @PathVariable String aeResourceName
			 														   , @PathVariable String containerResourceName
			 														   , @PathVariable String subscriptionResourceName
			 														   , HttpServletRequest request
			 														   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerSubscriptionUpdate start >>>");
		logger.debug("====================================================================");
		
		Subscription subscriptionProfile= (Subscription)mCommonService.getBodyObject(request, Subscription.class);
		HttpHeaders responseHeaders 	= new HttpHeaders();
		Object responseVo 				= null;
		String responseTxt				= null;
		HttpStatus httpStatus 			= HttpStatus.OK;
		RSC responseCode 				= RSC.CHANGED;
		String responseMsg 				= "";
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey   	= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic  	= CommonUtil.nvl(request.getHeader("topic"), "");
	
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
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(subscriptionProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<subscription> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContainerSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			subscriptionService.checkSubscriptionValidation(subscriptionProfile);
			
			subscriptionProfile.setResourceID(findSubscriptionItem.getResourceID());
			
			findSubscriptionItem = subscriptionService.updateSubscription(subscriptionProfile);
			
			responseVo = findSubscriptionItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
	
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContainerSubscriptionUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContainerSubscriptionUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * remoteCSE-AE-container subscription Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param subscriptionResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}/subscription-{subscriptionResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remortCSEAEContainerSubscriptionRetrieve(@PathVariable String cseBaseResourceName
			 															 , @PathVariable String remoteCSEResourceName
			 															 , @PathVariable String aeResourceName
			 															 , @PathVariable String containerResourceName
			 															 , @PathVariable String subscriptionResourceName
			 															 , RequestPrimitive requestPrimitive
			 															 , FilterCriteria filterCriteria
			 															 , HttpServletRequest request
			 															 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remortCSEAEContainerSubscriptionRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(subscriptionResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "subscription resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remortCSEAEContainerSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findAEItem = aeService.findOneAEByResourceName(findRemoteCSEItem.getResourceID(), aeResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.AE.noRegi.text"));
			}
			
			Container findContainerItem = null;
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			Subscription findSubscriptionItem = null;
			if (CommonUtil.isEmpty(findSubscriptionItem = subscriptionService.findOneSubscriptionByResourceName(findContainerItem.getResourceID(), subscriptionResourceName, requestPrimitive, filterCriteria))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.subscription.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, findSubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = findSubscriptionItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Subscription emptySubscriptionItem = new Subscription();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, emptySubscriptionItem, requestPrimitive, filterCriteria);
				
				responseVo = emptySubscriptionItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findSubscriptionItem.getResourceRef().getResourceID(), RESOURCE_TYPE.SUBSCRIPTION, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findSubscriptionItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remortCSEAEContainerSubscriptionRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remortCSEAEContainerSubscriptionRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	

}
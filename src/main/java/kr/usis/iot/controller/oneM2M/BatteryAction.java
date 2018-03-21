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
import kr.usis.iot.domain.oneM2M.Battery;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.Node;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.BatteryService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
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
 * battery management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>hkchoi</li>
 *         </ul>
 */
@Controller
public class BatteryAction {

	private static final Log logger = LogFactory.getLog(BatteryAction.class);

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
	private BatteryService batteryService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;

	/**
	 * battery Create
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param batteryProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName:.*}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=13", "mgd=1006"})
	@ResponseBody
	public ResponseEntity<Object> batteryCreate(@PathVariable String cseBaseResourceName
			 								 , @PathVariable String nodeResourceName
			 								 , RequestPrimitive requestPrimitive
			 								 , HttpServletRequest request
			 								 , HttpServletResponse response) {
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryCreate start >>>");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryCreate start >>>");
		logger.debug("====================================================================");
		
		Battery batteryProfile		= (Battery)mCommonService.getBodyObject(request, Battery.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.CREATED;
		RSC responseCode 			= RSC.CREATED;
		String responseMsg 			= "";
		
		String from 		= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 		= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID	= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 		= CommonUtil.nvl(request.getHeader("dKey"), "");

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
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
		else if (CommonUtil.isEmpty(batteryProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<battery> " + CommonUtil.getMessage("msg.input.empty.text");
		}


		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> batteryCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Battery batteryItem = null;
			if (!CommonUtil.isEmpty(batteryItem = batteryService.findOneBatteryByResourceName(findNodeItem.getResourceID(), name))) {
				responseVo = batteryItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.battery.duplicate.text"));
			}
			
			String url = CommonUtil.getURL(request);
			
			batteryProfile.setResourceName(name);
			batteryProfile.setParentID(findNodeItem.getResourceID());
			
			batteryItem = batteryService.createBattery(url, batteryProfile);
			
			responseVo = batteryItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> batteryCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryCreate end <<<");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * battery Update
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param batteryResourceName
	 * @param batteryProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName}/battery-{batteryResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> batteryUpdate(@PathVariable String cseBaseResourceName
			 								  , @PathVariable String nodeResourceName
			 								  , @PathVariable String batteryResourceName
			 								  , HttpServletRequest request
			 								  , HttpServletResponse response) {
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryUpdate start >>>");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryUpdate start >>>");
		logger.debug("====================================================================");
		
		Battery batteryProfile		= (Battery)mCommonService.getBodyObject(request, Battery.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.CHANGED;
		String responseMsg 			= "";
		
		String from 		= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID	= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 		= CommonUtil.nvl(request.getHeader("dKey"), "");

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
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
		else if (CommonUtil.isEmpty(batteryResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "battery resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(batteryProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<battery> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> batteryUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Battery findBatteryItem = null;
			if (CommonUtil.isEmpty((findBatteryItem = batteryService.findOneBatteryByResourceName(findNodeItem.getResourceID(), batteryResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.battery.noRegi.text"));
			}
			
			batteryProfile.setResourceID(findBatteryItem.getResourceID());
			
			findBatteryItem = batteryService.updateBattery(batteryProfile);
			
			responseVo = findBatteryItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> batteryUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryUpdate end <<<");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * battery Delete
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param batteryResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName}/battery-{batteryResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> batteryDelete(@PathVariable String cseBaseResourceName
			 								 , @PathVariable String nodeResourceName
			 								 , @PathVariable String batteryResourceName
			 								 , HttpServletRequest request
			 								 , HttpServletResponse response) {
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryDelete start >>>");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryDelete start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.DELETED;
		String responseMsg 			= "";
		
		String from 		= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID	= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 		= CommonUtil.nvl(request.getHeader("dKey"), "");

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
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
		else if (CommonUtil.isEmpty(batteryResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "battery resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> batteryDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Battery findBatteryItem = null;
			if (CommonUtil.isEmpty((findBatteryItem = batteryService.findOneBatteryByResourceName(findNodeItem.getResourceID(), batteryResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.battery.noRegi.text"));
			}
			
			batteryService.deleteBattery("resourceID", findBatteryItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> batteryDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryDelete end <<<");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * battery Retrieve
	 * @param cseBaseResourceName
	 * @param nodeResourceName
	 * @param batteryResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName}/battery-{batteryResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> batteryRetrieve(@PathVariable String cseBaseResourceName
			 								   , @PathVariable String nodeResourceName
			 								   , @PathVariable String batteryResourceName
			 								   , RequestPrimitive requestPrimitive
			 								   , FilterCriteria filterCriteria
			 								   , HttpServletRequest request
			 								   , HttpServletResponse response) {
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryRetrieve start >>>");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryRetrieve start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders = new HttpHeaders();
		Object responseVo 			= null;
		String responseTxt			= null;
		HttpStatus httpStatus 		= HttpStatus.OK;
		RSC responseCode 			= RSC.OK;
		String responseMsg 			= "";
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String uKey		= CommonUtil.nvl(request.getHeader("uKey"), "");
		String topic 	= CommonUtil.nvl(request.getHeader("topic"), "");
		
		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
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
		else if (CommonUtil.isEmpty(batteryResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "battery resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.error(">>> batteryRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Battery findBatteryItem = null;
			if (CommonUtil.isEmpty((findBatteryItem = batteryService.findOneBatteryByResourceName(findNodeItem.getResourceID(), batteryResourceName, requestPrimitive, filterCriteria)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.battery.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findBatteryItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.BATTERY, findBatteryItem, requestPrimitive, filterCriteria);
				
				responseVo = findBatteryItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				Battery emptyBatteryItem = new Battery();
				mCommonService.search(findBatteryItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.BATTERY, emptyBatteryItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyBatteryItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findBatteryItem.getResourceRef().getResourceID(), RESOURCE_TYPE.MGMT_OBJ, MGMT_DEFINITION.BATTERY, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findBatteryItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> batteryRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
		
		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		//mongoLogService.log(logger, LEVEL.DEBUG, "batteryRetrieve end <<<");
		//mongoLogService.log(logger, LEVEL.DEBUG, "====================================================================");
		
		logger.debug("====================================================================");
		logger.debug("batteryRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
}
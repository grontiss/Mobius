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
import kr.usis.iot.domain.oneM2M.LocationPolicy;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.LocationPolicyService;
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
 * locationPolicy management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Controller
public class LocationPolicyAction {

	private static final Log logger = LogFactory.getLog(LocationPolicyAction.class);

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
	private LocationPolicyService locationPolicyService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;

	/**
	 * LocationPolicy Create
	 * @param cseBaseResourceName
	 * @param locationPolicyProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = "rty=10")
	@ResponseBody
	public ResponseEntity<Object> locationPolicyCreate(@PathVariable String cseBaseResourceName
			 										 , RequestPrimitive requestPrimitive
													 , HttpServletRequest request
													 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyCreate start >>>");
		logger.debug("====================================================================");
		
		LocationPolicy locationPolicyProfile= (LocationPolicy)mCommonService.getBodyObject(request, LocationPolicy.class);
		HttpHeaders responseHeaders 		= new HttpHeaders();
		Object responseVo 					= null;
		String responseTxt					= null;
		HttpStatus httpStatus 				= HttpStatus.CREATED;
		RSC responseCode 					= RSC.CREATED;
		String responseMsg 					= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String name 	= CommonUtil.nvl(request.getHeader("X-M2M-NM"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 	= CommonUtil.nvl(request.getHeader("dKey"), "");
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
		else if(CommonUtil.isEmpty(locationPolicyProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<locationPolicy> " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		else if (CommonUtil.isEmpty(locationPolicyProfile.getLocationSource())) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<locationPolicy> locationSource " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> locationPolicyCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}
			
			LocationPolicy findLocationPolicyItem = null;
			if (!CommonUtil.isEmpty((findLocationPolicyItem = locationPolicyService.findOneLocationPolicyByResourceName(findCSEBaseItem.getResourceID(), name)))) {
				responseVo = findLocationPolicyItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.locationPolicy.duplicate.text"));
			}
			
			String url = CommonUtil.getURL(request);
			
			locationPolicyProfile.setResourceName(name);
			locationPolicyProfile.setParentID(findCSEBaseItem.getResourceID());
			
			findLocationPolicyItem = locationPolicyService.createLocationPolicy(url, locationPolicyProfile);
			
			responseVo = findLocationPolicyItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> locationPolicyCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

	/**
	 * LocationPolicy Delete
	 * @param cseBaseResourceName
	 * @param locationPolicyResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/locationPolicy-{locationPolicyResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> locationPolicyDelete(@PathVariable String cseBaseResourceName
			 										 , @PathVariable String locationPolicyResourceName
													 , HttpServletRequest request
													 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyDelete start >>>");
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
		else if (CommonUtil.isEmpty(locationPolicyResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "locationPolicy resourceName" + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> locationPolicyDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}

			LocationPolicy findLocationPolicyItem = null;
			if (CommonUtil.isEmpty((findLocationPolicyItem = locationPolicyService.findOneLocationPolicyByResourceName(findCSEBaseItem.getResourceID(), locationPolicyResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.locationPolicy.noRegi.text"));
			}
			
			String locationContainerID  = findLocationPolicyItem.getLocationContainerID();
			Container findContainerItem = null;
			if (!CommonUtil.isEmpty(findContainerItem = containerService.findOneContainer(locationContainerID))) {
				
				RemoteCSE findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findContainerItem.getParentID());
				AE findAEItem 				= aEService.findOneAE(findContainerItem.getParentID());
				if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
					accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
					
				} else if (!CommonUtil.isEmpty(findAEItem)) {
					findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findAEItem.getParentID());
					if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
						accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
						
					} else {
						accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);
					}
					
				}
			}
			
			locationPolicyService.deleteLocationPolicy("resourceID", findLocationPolicyItem.getResourceID());

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> locationPolicyDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}

	/**
	 * LocationPolicy Update
	 * @param cseBaseResourceName
	 * @param locationPolicyResourceName
	 * @param locationPolicyProfile
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/locationPolicy-{locationPolicyResourceName}", method = RequestMethod.PUT, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> locationPolicyUpdate(@PathVariable String cseBaseResourceName
			 										 , @PathVariable String locationPolicyResourceName
													 , HttpServletRequest request
													 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyUpdate start >>>");
		logger.debug("====================================================================");
		
		LocationPolicy locationPolicyProfile= (LocationPolicy)mCommonService.getBodyObject(request, LocationPolicy.class);
		HttpHeaders responseHeaders 		= new HttpHeaders();
		Object responseVo 					= null;
		String responseTxt					= null;
		HttpStatus httpStatus 				= HttpStatus.OK;
		RSC responseCode 					= RSC.CHANGED;
		String responseMsg 					= "";

		CommonUtil.setLocale(CommonUtil.nvl(request.getHeader("locale"), "ko"));
		
		String from 	= CommonUtil.nvl(request.getHeader("X-M2M-Origin"), "");
		String requestID= CommonUtil.nvl(request.getHeader("X-M2M-RI"), "");
		String dKey 	= CommonUtil.nvl(request.getHeader("dKey"), "");
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
		else if (CommonUtil.isEmpty(locationPolicyResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "locationPolicy resourceName" + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(locationPolicyProfile)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<locationPolicy> " + CommonUtil.getMessage("msg.input.empty.text");	
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> locationPolicyUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}

			LocationPolicy findLocationPolicyItem = null;
			if (CommonUtil.isEmpty((findLocationPolicyItem = locationPolicyService.findOneLocationPolicyByResourceName(findCSEBaseItem.getResourceID(), locationPolicyResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.locationPolicy.noRegi.text"));
			}
			 
			String locationContainerID  = findLocationPolicyItem.getLocationContainerID();
			Container findContainerItem = null;
			if (!CommonUtil.isEmpty(findContainerItem = containerService.findOneContainer(locationContainerID))) {
				
				RemoteCSE findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findContainerItem.getParentID());
				AE findAEItem 				= aEService.findOneAE(findContainerItem.getParentID());
				if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
					accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
					
				} else if (!CommonUtil.isEmpty(findAEItem)) {
					findRemoteCSEItem = remoteCSEService.findOneRemoteCSE(findAEItem.getParentID());
					if (!CommonUtil.isEmpty(findRemoteCSEItem)) {
						accessControlPolicyService.verifyAccessControlPolicyByDKey(checkMccP, findRemoteCSEItem.getAccessControlPolicyIDs(), dKey);
						
					} else {
						accessControlPolicyService.verifyAccessControlPolicyByAKey(checkMccP, findAEItem.getAccessControlPolicyIDs(), aKey);
					}
					
				}
			}
			
			locationPolicyProfile.setResourceID(findLocationPolicyItem.getResourceID());
			
			findLocationPolicyItem = locationPolicyService.updateLocationPolicy(locationPolicyProfile);
			
			responseVo = findLocationPolicyItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> locationPolicyUpdate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyUpdate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * LocationPolicy Retrieve
	 * @param cseBaseResourceName
	 * @param locationPolicyResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/locationPolicy-{locationPolicyResourceName}", method = RequestMethod.GET, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> locationPolicyRetrieve(@PathVariable String cseBaseResourceName
			 										   , @PathVariable String locationPolicyResourceName
			 										   , RequestPrimitive requestPrimitive
			 										   , FilterCriteria filterCriteria
													   , HttpServletRequest request
													   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(locationPolicyResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "locationPolicy resourceName" + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> locationPolicyRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}

		try {
			
			CSEBase findCSEBaseItem = null;
			if (CommonUtil.isEmpty(findCSEBaseItem = cseBaseService.findOneCSEBaseByResourceName(cseBaseResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.empty.text"));
			}

			LocationPolicy findLocationPolicyItem = null;
			if (CommonUtil.isEmpty((findLocationPolicyItem = locationPolicyService.findOneLocationPolicyByResourceName(findCSEBaseItem.getResourceID(), locationPolicyResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.locationPolicy.noRegi.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findLocationPolicyItem.getResourceRef().getResourceID(), RESOURCE_TYPE.LOCATION_POLICY, null, findLocationPolicyItem, requestPrimitive, filterCriteria);
				
				responseVo = findLocationPolicyItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				LocationPolicy emptyLocationPolicyItem = new LocationPolicy();
				mCommonService.search(findLocationPolicyItem.getResourceRef().getResourceID(), RESOURCE_TYPE.LOCATION_POLICY, null, emptyLocationPolicyItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyLocationPolicyItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findLocationPolicyItem.getResourceRef().getResourceID(), RESOURCE_TYPE.LOCATION_POLICY, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findLocationPolicyItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg = e.getMessage();
			
			logger.debug(">>> locationPolicyRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("locationPolicyRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}

}
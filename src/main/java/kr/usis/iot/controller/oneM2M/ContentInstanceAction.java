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
import kr.usis.iot.domain.oneM2M.Battery;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.Container;
import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RemoteCSE;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.domain.oneM2M.URIList;
import kr.usis.iot.service.apilog.ApiLogService;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.AEService;
import kr.usis.iot.service.oneM2M.AccessControlPolicyService;
import kr.usis.iot.service.oneM2M.CSEBaseService;
import kr.usis.iot.service.oneM2M.ContainerService;
import kr.usis.iot.service.oneM2M.ContentInstanceService;
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
 * contentInstance management Action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         <li>khlee</li>
 *         </ul>
 */
@Controller
public class ContentInstanceAction {

	private static final Log logger = LogFactory.getLog(ContentInstanceAction.class);
	
	public static final String CONTENTINSTANCE_OLDEST = "oldest";
	public static final String CONTENTINSTANCE_LATEST = "latest";
	
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
	private ContentInstanceService contentInstanceService;
	
	@Autowired
	private AccessControlPolicyService accessControlPolicyService;
	
	@Autowired
	private ApiLogService apiLogService;

	
	/**
	 * contentInstance Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param contentInstanceProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/container-{containerResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=4"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContentInstanceCreate(@PathVariable String cseBaseResourceName
			 												   , @PathVariable String remoteCSEResourceName
															   , @PathVariable String containerResourceName
															   , RequestPrimitive requestPrimitive
															   , HttpServletRequest request
															   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContentInstanceCreate start >>>");
		logger.debug("====================================================================");
		
		ContentInstance contentInstanceProfile	= (ContentInstance)mCommonService.getBodyObject(request, ContentInstance.class);
		HttpHeaders responseHeaders 			= new HttpHeaders();
		Object responseVo 						= null;
		String responseTxt						= null;
		HttpStatus httpStatus 					= HttpStatus.CREATED;
		RSC responseCode 						= RSC.CREATED;
		String responseMsg 						= "";

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
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(contentInstanceProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<contentInstance> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(contentInstanceProfile.getContent())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<contentInstance> content " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContentInstanceCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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

			Container findContainerItem = null;
			if (CommonUtil.isEmpty((findContainerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			ContentInstance contentInstanceItem = null;
			if (!CommonUtil.isEmpty(contentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), name))) {
				responseVo = contentInstanceItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.contentinstance.duplicate.text"));
			}
			
			contentInstanceService.checkCreateContentInstance(findContainerItem, contentInstanceProfile);
			
			String url = CommonUtil.getURL(request);
			
			contentInstanceProfile.setResourceName(name);
			contentInstanceProfile.setParentID(findContainerItem.getResourceID());
			contentInstanceProfile.setCreator(findRemoteCSEItem.getResourceID());
			if (!CommonUtil.isEmpty(findContainerItem.getMaxInstanceAge())) contentInstanceProfile.setExpirationTime(CommonUtil.getMoveTimestampBySeconds(findContainerItem.getMaxInstanceAge().intValue()));
			
			contentInstanceItem = contentInstanceService.createContentInstance(url, contentInstanceProfile);
			
			responseVo = contentInstanceItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEContentInstanceCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContentInstanceCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	
	/**
	 * contentInstance Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param contentInstanceResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName}/container-{containerResourceName}/contentInstance-{contentInstanceResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContentInstanceDelete(@PathVariable String cseBaseResourceName
			 												 , @PathVariable String remoteCSEResourceName
															 , @PathVariable String containerResourceName
															 , @PathVariable String contentInstanceResourceName
															 , HttpServletRequest request
															 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContentInstanceDelete start >>>");
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
		else if (CommonUtil.isEmpty(contentInstanceResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "contentInstance resourceName" + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContentInstanceDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			ContentInstance contentInstanceItem = null;
			if (CommonUtil.isEmpty(contentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(containerItem.getResourceID(), contentInstanceResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.contentinstance.noRegi.text"));
			}
			
			contentInstanceService.deleteContentInstance(contentInstanceItem);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEContentInstanceDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContentInstanceDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	/**
	 * contentInstance Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param containerResourceName
	 * @param contentInstanceResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = {"/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/container-{containerResourceName}/contentInstance-{contentInstanceResourceName}"
							,"/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/container-{containerResourceName}/{contentInstanceResourceName:latest}"
							,"/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/container-{containerResourceName}/{contentInstanceResourceName:oldest}"
							}
				  , method = RequestMethod.GET
				  , produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEContentInstanceRetrieve(@PathVariable String cseBaseResourceName
			 													 , @PathVariable String remoteCSEResourceName
																 , @PathVariable String containerResourceName
																 , @PathVariable String contentInstanceResourceName
																 , RequestPrimitive requestPrimitive
																 , FilterCriteria filterCriteria
																 , HttpServletRequest request
																 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContentInstanceRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(contentInstanceResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "contentInstance resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEContentInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			Container containerItem = null;
			if (CommonUtil.isEmpty(containerItem = containerService.findOneContainerByResourceName(findRemoteCSEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			ContentInstance findContentInstanceItem = null;
			if( CONTENTINSTANCE_LATEST.equals(contentInstanceResourceName) ) {
				findContentInstanceItem = contentInstanceService.findOneContentInstance(containerItem.getLatest(), requestPrimitive, filterCriteria);
			} else if ( CONTENTINSTANCE_OLDEST.equals(contentInstanceResourceName) ) {
				findContentInstanceItem = contentInstanceService.findOneContentInstance(containerItem.getOldest(), requestPrimitive, filterCriteria);
			} else {
				findContentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(containerItem.getResourceID(), contentInstanceResourceName, requestPrimitive, filterCriteria);
			}
			
			if (CommonUtil.isEmpty(findContentInstanceItem)) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.contentinstance.notFound.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, findContentInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = findContentInstanceItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				ContentInstance emptyContentInstanceItem = new ContentInstance();
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, emptyContentInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyContentInstanceItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findContentInstanceItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEContentInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEContentInstanceRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * AE contentInstance Create
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param contentInstanceProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=4"})
	@ResponseBody
	public ResponseEntity<Object> AEContentInstanceCreate(@PathVariable String cseBaseResourceName
			 											, @PathVariable String aeResourceName
														, @PathVariable String containerResourceName
														, RequestPrimitive requestPrimitive
														, HttpServletRequest request
														, HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContentInstanceCreate start >>>");
		logger.debug("====================================================================");
		
		ContentInstance contentInstanceProfile	= (ContentInstance)mCommonService.getBodyObject(request, ContentInstance.class);
		HttpHeaders responseHeaders 			= new HttpHeaders();
		Object responseVo 						= null;
		String responseTxt						= null;
		HttpStatus httpStatus 					= HttpStatus.CREATED;
		RSC responseCode 						= RSC.CREATED;
		String responseMsg 						= "";

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
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(contentInstanceProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<contentInstance> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(contentInstanceProfile.getContent())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<contentInstance> content " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContentInstanceCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty((findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			ContentInstance contentInstanceItem = null;
			if (!CommonUtil.isEmpty(contentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), name))) {
				responseVo = contentInstanceItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.contentinstance.duplicate.text"));
			}
			
			contentInstanceService.checkCreateContentInstance(findContainerItem, contentInstanceProfile);
			
			String url = CommonUtil.getURL(request);
			
			contentInstanceProfile.setResourceName(name);
			contentInstanceProfile.setParentID(findContainerItem.getResourceID());
			contentInstanceProfile.setCreator(findAEItem.getResourceID());
			if (!CommonUtil.isEmpty(findContainerItem.getMaxInstanceAge())) contentInstanceProfile.setExpirationTime(CommonUtil.getMoveTimestampBySeconds(findContainerItem.getMaxInstanceAge().intValue()));
			
			contentInstanceItem = contentInstanceService.createContentInstance(url, contentInstanceProfile);
			
			responseVo = contentInstanceItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> AEContentInstanceCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("AEContentInstanceCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * AE contentInstance Delete
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param contentInstanceResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/contentInstance-{contentInstanceResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContentInstanceDelete(@PathVariable String cseBaseResourceName
			 										  , @PathVariable String aeResourceName
													  , @PathVariable String containerResourceName
													  , @PathVariable String contentInstanceResourceName
													  , HttpServletRequest request
													  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContentInstanceDelete start >>>");
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
		else if (CommonUtil.isEmpty(contentInstanceResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "contentInstance resourceName" + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContentInstanceDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			ContentInstance findContentInstanceItem = null;
			if (CommonUtil.isEmpty(findContentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), contentInstanceResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.contentinstance.noRegi.text"));
			}
			
			contentInstanceService.deleteContentInstance(findContentInstanceItem);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> AEContentInstanceDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContentInstanceDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	/**
	 * AE contentInstance Retrieve
	 * @param cseBaseResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param contentInstanceResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = {"/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/contentInstance-{contentInstanceResourceName}"
							,"/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/{contentInstanceResourceName:latest}"
							,"/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}/{contentInstanceResourceName:oldest}"			
							}
				  , method = RequestMethod.GET
				  , produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> AEContentInstanceRetrieve(@PathVariable String cseBaseResourceName
			 											  , @PathVariable String aeResourceName
														  , @PathVariable String containerResourceName
														  , @PathVariable String contentInstanceResourceName
														  , RequestPrimitive requestPrimitive
														  , FilterCriteria filterCriteria
														  , HttpServletRequest request
														  , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("AEContentInstanceRetrieve start >>>");
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
		else if (CommonUtil.isEmpty(contentInstanceResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "contentInstance resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> AEContentInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			ContentInstance findContentInstanceItem = null;
			if( CONTENTINSTANCE_LATEST.equals(contentInstanceResourceName) ) {
				findContentInstanceItem = contentInstanceService.findOneContentInstance(findContainerItem.getLatest(), requestPrimitive, filterCriteria);
			} else if ( CONTENTINSTANCE_OLDEST.equals(contentInstanceResourceName) ) {
				findContentInstanceItem = contentInstanceService.findOneContentInstance(findContainerItem.getOldest(), requestPrimitive, filterCriteria);
			} else {
				findContentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), contentInstanceResourceName, requestPrimitive, filterCriteria);
			}
			
			if (CommonUtil.isEmpty(findContentInstanceItem)) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.contentinstance.notFound.text"));
			}
			
			if (	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, findContentInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = findContentInstanceItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				ContentInstance emptyContentInstanceItem = new ContentInstance();
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, emptyContentInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyContentInstanceItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findContentInstanceItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> AEContentInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("AEContentInstanceRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * remoteCSE AE contentInstance Create
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param contentInstanceProfile
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = { "application/xml", "application/json" }, params = {"rty=4"})
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContentInstanceCreate(@PathVariable String cseBaseResourceName
			 													 , @PathVariable String remoteCSEResourceName
																 , @PathVariable String aeResourceName
																 , @PathVariable String containerResourceName
																 , RequestPrimitive requestPrimitive
																 , HttpServletRequest request
																 , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContentInstanceCreate start >>>");
		logger.debug("====================================================================");
		
		ContentInstance contentInstanceProfile	= (ContentInstance)mCommonService.getBodyObject(request, ContentInstance.class);
		HttpHeaders responseHeaders 			= new HttpHeaders();
		Object responseVo 						= null;
		String responseTxt						= null;
		HttpStatus httpStatus 					= HttpStatus.CREATED;
		RSC responseCode 						= RSC.CREATED;
		String responseMsg 						= "";

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
			responseMsg 	= "remoteCSE CSE-ID " + CommonUtil.getMessage("msg.input.empty.text");
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
		else if (CommonUtil.isEmpty(contentInstanceProfile)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "<contentInstance> " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if(CommonUtil.isEmpty(contentInstanceProfile.getContent())){
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "<contentInstance> content " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if(isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContentInstanceCreate ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty((findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName)))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			ContentInstance contentInstanceItem = null;
			if (!CommonUtil.isEmpty(contentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), name))) {
				responseVo = contentInstanceItem;
				responseTxt = mCommonService.getObject2Txt(request, responseVo);
				throw new RSCException(RSC.CONFLICT, CommonUtil.getMessage("msg.device.contentinstance.duplicate.text"));
			}
			
			contentInstanceService.checkCreateContentInstance(findContainerItem, contentInstanceProfile);
			
			String url = CommonUtil.getURL(request);
			
			contentInstanceProfile.setResourceName(name);
			contentInstanceProfile.setParentID(findContainerItem.getResourceID());
			contentInstanceProfile.setCreator(findRemoteCSEItem.getResourceID());
			if (!CommonUtil.isEmpty(findContainerItem.getMaxInstanceAge())) contentInstanceProfile.setExpirationTime(CommonUtil.getMoveTimestampBySeconds(findContainerItem.getMaxInstanceAge().intValue()));
			
			contentInstanceItem = contentInstanceService.createContentInstance(url, contentInstanceProfile);
			
			responseVo = contentInstanceItem;
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContentInstanceCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		if (RESULT_CONTENT.HIERARCHICAL_ADDRESS.getValue().equals(requestPrimitive.getRcn())) responseTxt = null;
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContentInstanceCreate end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
	
	/**
	 * remoteCSE AE contentInstance Delete
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param contentInstanceResourceName
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}/contentInstance-{contentInstanceResourceName}", method = RequestMethod.DELETE, produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContentInstanceDelete(@PathVariable String cseBaseResourceName
			 												   , @PathVariable String remoteCSEResourceName
															   , @PathVariable String aeResourceName
															   , @PathVariable String containerResourceName
															   , @PathVariable String contentInstanceResourceName
															   , HttpServletRequest request
															   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContentInstanceDelete start >>>");
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
			responseMsg 	= "remoteCSE CSE-ID " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(aeResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "aeResourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(containerResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "container resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		else if (CommonUtil.isEmpty(contentInstanceResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "contentInstance resourceName" + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContentInstanceDelete ERROR message ["+responseCode+":"+responseMsg+"]");
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
			
			ContentInstance findContentInstanceItem = null;
			if (CommonUtil.isEmpty(findContentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), contentInstanceResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.contentinstance.noRegi.text"));
			}
			
			contentInstanceService.deleteContentInstance(findContentInstanceItem);

		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContentInstanceDelete ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContentInstanceDelete end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseHeaders, httpStatus);
	}
	
	/**
	 * remoteCSE AE contentInstance Retrieve
	 * @param cseBaseResourceName
	 * @param remoteCSEResourceName
	 * @param aeResourceName
	 * @param containerResourceName
	 * @param contentInstanceResourceName
	 * @param requestPrimitive
	 * @param filterCriteria
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = {"/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/AE-{aeResourceName:.*}/container-{containerResourceName}/contentInstance-{contentInstanceResourceName}"
							,"/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/AE-{aeResourceName:.*}/container-{containerResourceName}/{contentInstanceResourceName:"+CONTENTINSTANCE_LATEST+"}"
							,"/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/AE-{aeResourceName:.*}/container-{containerResourceName}/{contentInstanceResourceName:"+CONTENTINSTANCE_OLDEST+"}"
							}
				  , method = RequestMethod.GET
				  , produces = { "application/xml", "application/json" })
	@ResponseBody
	public ResponseEntity<Object> remoteCSEAEContentInstanceRetrieve(@PathVariable String cseBaseResourceName
			 													   , @PathVariable String remoteCSEResourceName
																   , @PathVariable String aeResourceName
																   , @PathVariable String containerResourceName
																   , @PathVariable String contentInstanceResourceName
																   , RequestPrimitive requestPrimitive
																   , FilterCriteria filterCriteria
																   , HttpServletRequest request
																   , HttpServletResponse response) {
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContentInstanceRetrieve start >>>");
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
			responseMsg 	= "remoteCSE CSE-ID " + CommonUtil.getMessage("msg.input.empty.text");
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
		else if (CommonUtil.isEmpty(contentInstanceResourceName)) {
			isError = true;
			responseCode 	= RSC.BAD_REQUEST;
			responseMsg 	= "contentInstance resourceName " + CommonUtil.getMessage("msg.input.empty.text");
		}
		
		if (isError) {
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			logger.debug(">>> remoteCSEAEContentInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
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
			if (CommonUtil.isEmpty(findContainerItem = containerService.findOneContainerByResourceName(findAEItem.getResourceID(), containerResourceName))) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.container.noRegi.text"));
			}
			
			ContentInstance findContentInstanceItem = null;
			if( CONTENTINSTANCE_LATEST.equals(contentInstanceResourceName) ) {
				findContentInstanceItem = contentInstanceService.findOneContentInstance(findContainerItem.getLatest(), requestPrimitive, filterCriteria);
			} else if ( CONTENTINSTANCE_OLDEST.equals(contentInstanceResourceName) ) {
				findContentInstanceItem = contentInstanceService.findOneContentInstance(findContainerItem.getOldest(), requestPrimitive, filterCriteria);
			} else {
				findContentInstanceItem = contentInstanceService.findOneContentInstanceByResourceName(findContainerItem.getResourceID(), contentInstanceResourceName, requestPrimitive, filterCriteria);
			}
			
			if (CommonUtil.isEmpty(findContentInstanceItem)) {
				throw new RSCException(RSC.NOT_FOUND, CommonUtil.getMessage("msg.device.contentinstance.notFound.text"));
			}
			
			if ( 	RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
				 || RESULT_CONTENT.ATTRIBUTES_AND_CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, findContentInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = findContentInstanceItem;
				
			} else if (	RESULT_CONTENT.CHILD_RESOURCES.getValue().equals(requestPrimitive.getRcn())
					 || RESULT_CONTENT.CHILD_RESOURCE_REFERENCES.getValue().equals(requestPrimitive.getRcn())) {
				
				ContentInstance emptyContentInstanceItem = new ContentInstance();
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, emptyContentInstanceItem, requestPrimitive, filterCriteria);
				
				responseVo = emptyContentInstanceItem;
			} else if (	FILTER_USAGE.DISCOVERY_CRITERIA.getValue().equals(filterCriteria.getFu())) {
				
				URIList uriList = new URIList();
				mCommonService.search(findContentInstanceItem.getResourceRef().getResourceID(), RESOURCE_TYPE.CONTENT_INSTANCE, null, uriList, requestPrimitive, filterCriteria);
				
				responseVo = uriList;
			} else {
				responseVo = findContentInstanceItem;
			}
			
			responseTxt = mCommonService.getObject2Txt(request, responseVo);
			
		} catch (RSCException e) {
			responseCode = e.getCode();
			responseMsg  = e.getMessage();
			
			logger.debug(">>> remoteCSEAEContentInstanceRetrieve ERROR message ["+responseCode+":"+responseMsg+"]");
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}

		httpStatus = mCommonService.RSC2HttpStatus(responseCode);
		mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
		
		logger.debug("====================================================================");
		logger.debug("remoteCSEAEContentInstanceRetrieve end <<<");
		logger.debug("====================================================================");
		
		return new ResponseEntity<Object>(responseTxt, responseHeaders, httpStatus);
	}
}
package kr.usis.iot.controller.oneM2M.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.usis.iot.controller.oneM2M.AEAction;
import kr.usis.iot.controller.oneM2M.BatteryAction;
import kr.usis.iot.controller.oneM2M.ContainerAction;
import kr.usis.iot.controller.oneM2M.ContentInstanceAction;
import kr.usis.iot.controller.oneM2M.DeviceInfoAction;
import kr.usis.iot.controller.oneM2M.FirmwareAction;
import kr.usis.iot.controller.oneM2M.GroupAction;
import kr.usis.iot.controller.oneM2M.LocationPolicyAction;
import kr.usis.iot.controller.oneM2M.MemoryAction;
import kr.usis.iot.controller.oneM2M.MgmtCmdAction;
import kr.usis.iot.controller.oneM2M.RebootAction;
import kr.usis.iot.controller.oneM2M.RemoteCSEAction;
import kr.usis.iot.controller.oneM2M.SoftwareAction;
import kr.usis.iot.controller.oneM2M.SubscriptionAction;
import kr.usis.iot.controller.userAuth.UserAction;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.MGMT_DEFINITION;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;
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
 * common action.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 * @history By JiHoon KIM jhkim@usis.kr
 * LOG DB 적재  (기존방식)
 * mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
 * LOG FILE 적재 (추가방식)
 * logger.error(">>> commonCSEBaseBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
 */

@Controller
public class CommonAction {

	private static final Log logger = LogFactory.getLog(CommonAction.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;
	
	@Autowired
	private AEAction aeAction;
	
	@Autowired
	private RemoteCSEAction remoteCSEAction;
	
	@Autowired
	private GroupAction groupAction;
	
	@Autowired
	private LocationPolicyAction locationPolicyAction;
	
	@Autowired
	private ContainerAction containerAction;
	
	@Autowired
	private ContentInstanceAction contentInstanceAction;
	
	@Autowired
	private SubscriptionAction subscriptionAction;
	
	@Autowired
	private FirmwareAction firmwareAction;
	
	@Autowired
	private SoftwareAction softwareAction;
	
	@Autowired
	private DeviceInfoAction deviceInfoAction;
	
	@Autowired
	private BatteryAction batteryAction;
	
	@Autowired
	private MemoryAction memoryAction;
	
	@Autowired
	private RebootAction rebootAction;
	
	@Autowired
	private MgmtCmdAction mgmtCmdAction;
	
	@Autowired
	private UserAction userAction;
	
	/**
	 * CSEBase Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 * CSE 등록을 하기 위한 Method
	 */
	@RequestMapping(value = "/{cseBaseResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseBottomCreate(@PathVariable String cseBaseResourceName
									 					  , RequestPrimitive requestPrimitive
														  , HttpServletRequest request
														  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case AE://ty=2
				responseEntity = aeAction.AECreate(cseBaseResourceName, requestPrimitive, request, response);
				break;
			case GROUP://ty=9
				responseEntity = groupAction.groupCreate(cseBaseResourceName, requestPrimitive, request, response);
				break;
			case LOCATION_POLICY://ty=10
				responseEntity = locationPolicyAction.locationPolicyCreate(cseBaseResourceName, requestPrimitive, request, response);
				break;
			case REMOTE_CSE://ty=16
				responseEntity = remoteCSEAction.remoteCSECreate(cseBaseResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-AE Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseAEBottomCreate(@PathVariable String cseBaseResourceName
															, @PathVariable String aeResourceName
										 					, RequestPrimitive requestPrimitive
															, HttpServletRequest request
															, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseAEBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseAEBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseAEBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case CONTAINER://ty=3
				responseEntity = containerAction.AEContainerCreate(cseBaseResourceName, aeResourceName, requestPrimitive, request, response);
				break;
			case GROUP://ty=9
				responseEntity = groupAction.AEGroupCreate(cseBaseResourceName, aeResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseAEBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-AE-container Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseAEContainerBottomCreate(@PathVariable String cseBaseResourceName
																	 , @PathVariable String aeResourceName
																	 , @PathVariable String containerResourceName
												 					 , RequestPrimitive requestPrimitive
																	 , HttpServletRequest request
																	 , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseAEContainerBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseAEContainerBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseAEContainerBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case CONTENT_INSTANCE://ty=4
				responseEntity = contentInstanceAction.AEContentInstanceCreate(cseBaseResourceName, aeResourceName, containerResourceName, requestPrimitive, request, response);
				break;
			case SUBSCRIPTION://ty=23
				responseEntity = subscriptionAction.AEContainerSubscriptionCreate(cseBaseResourceName, aeResourceName, containerResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseAEContainerBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-node Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/node-{nodeResourceName:.*}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseNodeBottomCreate(@PathVariable String cseBaseResourceName
															  , @PathVariable String nodeResourceName
										 					  , RequestPrimitive requestPrimitive
															  , HttpServletRequest request
															  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseNodeBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		MGMT_DEFINITION mgd = mCommonService.getMgd(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseNodeBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseNodeBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case MGMT_OBJ:
				switch (mgd) {
					case FIRMWARE://mgd=1001
						responseEntity = firmwareAction.firmwareCreate(cseBaseResourceName, nodeResourceName, requestPrimitive, request, response);
						break;
					case SOFTWARE://mgd=1002
						responseEntity = softwareAction.softwareCreate(cseBaseResourceName, nodeResourceName, requestPrimitive, request, response);
						break;
					case DEVICE_INFO://mgd=1007
						responseEntity = deviceInfoAction.deviceInfoCreate(cseBaseResourceName, nodeResourceName, requestPrimitive, request, response);
						break;
					case BATTERY://mgd=1006
						responseEntity = batteryAction.batteryCreate(cseBaseResourceName, nodeResourceName, requestPrimitive, request, response);
						break;
					case MEMORY://mgd=1003
						responseEntity = memoryAction.memoryCreate(cseBaseResourceName, nodeResourceName, requestPrimitive, request, response);
						break;
					case REBOOT://mgd=1009
						responseEntity = rebootAction.rebootCreate(cseBaseResourceName, nodeResourceName, requestPrimitive, request, response);
						break;
					default:
						responseCode	= RSC.BAD_REQUEST;
						responseMsg		= "mgd " + CommonUtil.getMessage("msg.input.invalid.text");
						httpStatus = mCommonService.RSC2HttpStatus(responseCode);
						mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
						mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
						logger.error(">>> commonCSEBaseNodeBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
						return new ResponseEntity<Object>(responseHeaders, httpStatus);
				}
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseNodeBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-remoteCSE Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseRemoteCSEBottomCreate(@PathVariable String cseBaseResourceName
																   , @PathVariable String remoteCSEResourceName
												 				   , RequestPrimitive requestPrimitive
																   , HttpServletRequest request
																   , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseRemoteCSEBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case AE://ty=2
				responseEntity = aeAction.remoteCSEAECreate(cseBaseResourceName, remoteCSEResourceName, requestPrimitive, request, response);
				break;
			case CONTAINER://ty=3
				responseEntity = containerAction.remoteCSEContainerCreate(cseBaseResourceName, remoteCSEResourceName, requestPrimitive, request, response);
				break;
			case GROUP://ty=9
				responseEntity = groupAction.remoteCSEGroupCreate(cseBaseResourceName, remoteCSEResourceName, requestPrimitive, request, response);
				break;
			case MGMT_CMD://ty=12
				responseEntity = mgmtCmdAction.mgmtCmdCreate(cseBaseResourceName, remoteCSEResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseRemoteCSEBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-remoteCSE-ae Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseRemoteCSEAEBottomCreate(@PathVariable String cseBaseResourceName
																	 , @PathVariable String remoteCSEResourceName
																	 , @PathVariable String aeResourceName
																	 , RequestPrimitive requestPrimitive
																	 , HttpServletRequest request
																	 , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEAEBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseRemoteCSEAEBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEAEBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case CONTAINER://ty=3
				responseEntity = containerAction.remoteCSEAEContainerCreate(cseBaseResourceName, remoteCSEResourceName, aeResourceName, requestPrimitive, request, response);
				break;
			case GROUP://ty=9
				responseEntity = groupAction.remoteCSEAEGroupCreate(cseBaseResourceName, remoteCSEResourceName, aeResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseRemoteCSEAEBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-remoteCSE-ae-container Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/AE-{aeResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseRemoteCSEAEContainerBottomCreate(@PathVariable String cseBaseResourceName
																			  , @PathVariable String remoteCSEResourceName
																			  , @PathVariable String aeResourceName
																			  , @PathVariable String containerResourceName
																			  , RequestPrimitive requestPrimitive
																			  , HttpServletRequest request
																			  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEAEContainerBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseRemoteCSEAEContainerBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEAEContainerBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case CONTENT_INSTANCE://ty=4
				responseEntity = contentInstanceAction.remoteCSEAEContentInstanceCreate(cseBaseResourceName, remoteCSEResourceName, aeResourceName, containerResourceName, requestPrimitive, request, response);
				break;
			case SUBSCRIPTION://ty=23
				responseEntity = subscriptionAction.remortCSEAEContainerSubscriptionCreate(cseBaseResourceName, remoteCSEResourceName, aeResourceName, containerResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseRemoteCSEAEContainerBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-remoteCSE-container Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:.*}/container-{containerResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseRemoteCSEContainerBottomCreate(@PathVariable String cseBaseResourceName
																			, @PathVariable String remoteCSEResourceName
																			, @PathVariable String containerResourceName
																			, RequestPrimitive requestPrimitive
																			, HttpServletRequest request
																			, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEContainerBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseRemoteCSEContainerBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEContainerBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case CONTENT_INSTANCE://ty=4
				responseEntity = contentInstanceAction.remoteCSEContentInstanceCreate(cseBaseResourceName, remoteCSEResourceName, containerResourceName, requestPrimitive, request, response);
				break;
			case SUBSCRIPTION://ty=23
				responseEntity = subscriptionAction.remoteCSEContainerSubscriptionCreate(cseBaseResourceName, remoteCSEResourceName, containerResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseRemoteCSEContainerBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-remoteCSE-mgmtCmd Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/remoteCSE-{remoteCSEResourceName:[^9.9.999.9].*}/mgmtCmd-{mgmtCmdResourceName}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseRemoteCSEMgmtCmdBottomCreate(@PathVariable String cseBaseResourceName
																		  , @PathVariable String remoteCSEResourceName
																		  , @PathVariable String mgmtCmdResourceName
																		  , RequestPrimitive requestPrimitive
																		  , HttpServletRequest request
																		  , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEMgmtCmdBottomCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseRemoteCSEMgmtCmdBottomCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseRemoteCSEMgmtCmdBottomCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case SUBSCRIPTION://ty=23
				responseEntity = subscriptionAction.remoteCSEMgmtCmdSubscriptionCreate(cseBaseResourceName, remoteCSEResourceName, mgmtCmdResourceName, requestPrimitive, request, response);
				break;
			case EXEC_INSTANCE://ty=8
				responseEntity = mgmtCmdAction.mgmtCmdControl(cseBaseResourceName, remoteCSEResourceName, mgmtCmdResourceName, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseRemoteCSEMgmtCmdBottomCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
	/**
	 * CSEBase-group-fanOutPoint Bottom Create
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName}/group-{groupResourceName}/fanOutPoint", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseGroupFanOutPointControlCreate(@PathVariable String cseBaseResourceName
																		   , @PathVariable String groupResourceName
																		   , RequestPrimitive requestPrimitive
																		   , HttpServletRequest request
																		   , HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseGroupFanOutPointControlCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo						= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseGroupFanOutPointControlCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseGroupFanOutPointControlCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		responseEntity = groupAction.groupControl(cseBaseResourceName, groupResourceName, request, response);
		
		return responseEntity;
	}
	
	
	/**
	 * CSEBase-User ����
	 * @param cseBaseResourceName
	 * @param requestPrimitive
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/{cseBaseResourceName:.*}/userCreate", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
	@ResponseBody
	public ResponseEntity<Object> commonCSEBaseUserCreate(@PathVariable String cseBaseResourceName
										 					, RequestPrimitive requestPrimitive
															, HttpServletRequest request
															, HttpServletResponse response){
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseUserCreate start >>>");
		logger.debug("====================================================================");
		
		HttpHeaders responseHeaders				= new HttpHeaders();
		Object responseVo					= null;
		String responseTxt						= null;
		ResponseEntity<Object> responseEntity	= null;
		HttpStatus httpStatus					= HttpStatus.CREATED;
		RSC responseCode						= RSC.CREATED;
		String responseMsg						= "";
		
		RESOURCE_TYPE ty = mCommonService.getTy(request);
		
		boolean isError = false;
		
		//check mandatory
		if(CommonUtil.isEmpty(ty)) {
			isError			= true;
			responseCode	= RSC.BAD_REQUEST;
			responseMsg		= "ty " + CommonUtil.getMessage("msg.input.empty.text");	
		}

		if(isError){
			httpStatus = mCommonService.RSC2HttpStatus(responseCode);
			mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
			mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
			logger.error(">>> commonCSEBaseUserCreate ERROR message ["+responseCode+":"+responseMsg+"]");
			return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		// validation check end
		
		logger.debug("====================================================================");
		logger.debug("commonCSEBaseUserCreate end ("+ty+")!!!");
		logger.debug("====================================================================");
		
		switch (ty) {
			case ACCESS_CONTROL_POLICY://ty=1
				responseEntity = userAction.userCreate(cseBaseResourceName, requestPrimitive, request, response);
				break;
			default:
				responseCode	= RSC.BAD_REQUEST;
				responseMsg		= "commonCSEBaseUserCreate ty " + CommonUtil.getMessage("msg.input.invalid.text");
				httpStatus = mCommonService.RSC2HttpStatus(responseCode);
				mCommonService.setResponseHeader(request, responseHeaders, responseVo, responseTxt, responseCode, responseMsg);
				mongoLogService.log(logger, LEVEL.ERROR, responseCode + " : " + responseMsg);
				logger.error(">>> commonCSEBaseUserCreate ERROR message ["+ty+":"+responseCode+":"+responseMsg+"]");
				return new ResponseEntity<Object>(responseHeaders, httpStatus);
		}
		
		return responseEntity;
	}
	
}

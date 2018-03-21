/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ntels.nisf.util.PropertiesUtil;

import kr.usis.iot.domain.common.CertKeyVO;

/**
 * cert util
 *
 */
@Component
public class CertUtil {
	
	private static final Log logger = LogFactory.getLog(CertUtil.class);
	
	@Autowired
	private RestTemplate restTemplate;
	
	/**
	 * createDeviceKey
	 * @param remoteCSEResourceID
	 * @return
	 */
	public CertKeyVO createDeviceKey(String remoteCSEResourceID) {

		CertKeyVO certKeyVO = new CertKeyVO();
		certKeyVO.setCert_id(remoteCSEResourceID);
		certKeyVO.setOU(PropertiesUtil.get("openapi", "iot.certEngine.OU.device"));
		
		certKeyVO = createKeyByCallAuthServer(certKeyVO);
		certKeyVO.setCert_type(CommonUtil.cert_types.get("device"));
		
		return certKeyVO; 
	}
	
	/**
	 * createKeyByCallAuthServer
	 * @param certKeyVO
	 * @return
	 */
	private CertKeyVO createKeyByCallAuthServer(CertKeyVO certKeyVO){
		
		String param = "";
		String url = PropertiesUtil.get("openapi", "iot.certEngine.createKey");
		
		if(certKeyVO.getOU().equals(PropertiesUtil.get("openapi", "iot.certEngine.OU.device"))){
			param = "OU=" + certKeyVO.getOU() + "&CN=" + certKeyVO.getCert_id();
		}else if(certKeyVO.getOU().equals(PropertiesUtil.get("openapi", "iot.certEngine.OU.user"))){
			param = "OU=" + certKeyVO.getOU() + "&CN=" + certKeyVO.getCert_id() + "&email=" + certKeyVO.getEmail();
		}else if(certKeyVO.getOU().equals(PropertiesUtil.get("openapi", "iot.certEngine.OU.platform"))){
			param = "OU=" + certKeyVO.getOU() + "&CN=" + certKeyVO.getCert_id();
		}

		String callResult = connectAuthServer(param, url);
		
		Gson gson = new GsonBuilder().serializeNulls().create();
		CertKeyVO resultkeyVO = gson.fromJson(callResult, CertKeyVO.class); 
		
		if(resultkeyVO == null || resultkeyVO.getErrno() == null){
			
			resultkeyVO = new CertKeyVO();
			resultkeyVO.setResult_code("600");
			resultkeyVO.setResult_msg(CommonUtil.getMessage("msg.apicall.error.text"));
			
		}else if(resultkeyVO.getErrno() == 0){
			
			resultkeyVO.setResult_code("200");
			resultkeyVO.setCert_id(certKeyVO.getCert_id());
			resultkeyVO.setCert_client_id(resultkeyVO.getCid());
			resultkeyVO.setCert_public_key(resultkeyVO.getPub());
			resultkeyVO.setCert_private_key(resultkeyVO.getPri());
			
		}else{
			resultkeyVO.setResult_code("600");
			resultkeyVO.setResult_msg(resultkeyVO.getErrmsg());
		}
		
		return resultkeyVO;
	}
	
	/**
	 *connectAuthServer
	 * @param param
	 * @param url
	 * @return
	 */
	public String connectAuthServer(String param, String url){
		
		url = PropertiesUtil.get("openapi", "CertEngine.url") + url;
		
		HttpHeaders headers = new HttpHeaders();
		
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.add("Accept", "*/*");
		
		logger.info("### param : " + param);
		logger.info("### url : " + url);
		HttpEntity<String> entity = new HttpEntity<String>(param, headers);
		
		
		ResponseEntity<String> response = restTemplate.exchange(
				 url
				,HttpMethod.POST
				,entity
				,String.class);
		logger.info("### body : " + response.getBody());

		//Gson gson = new GsonBuilder().serializeNulls().create();
		//CertKeyVO certKeyVO = gson.fromJson(response.getBody(), CertKeyVO.class);

		return response.getBody();
	}
	
	/**
	 * ++ USIS 2016-07-11
	 * createUserKey
	 * @param remoteCSEResourceID
	 * @return
	 */
	public CertKeyVO createUserKey(String userName) {

		CertKeyVO certKeyVO = new CertKeyVO();
		certKeyVO.setCert_id(userName);
		certKeyVO.setOU(PropertiesUtil.get("openapi", "iot.certEngine.OU.user"));
		
		certKeyVO = createKeyByCallAuthServer(certKeyVO);
		certKeyVO.setCert_type(CommonUtil.cert_types.get("user"));
		
		return certKeyVO; 
	}
}

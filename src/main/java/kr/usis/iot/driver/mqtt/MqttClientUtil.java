/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.driver.mqtt;

import java.util.HashMap;

import kr.usis.iot.driver.http.HTTPClient;
import kr.usis.iot.driver.http.HttpResponse;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.util.CommonUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import com.ntels.nisf.util.PropertiesUtil;

/**
 * MQTT client util.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
public class MqttClientUtil implements MqttCallback {
	
	@Autowired
	private MongoLogService mongoLogService;
	
	private static final Log logger = LogFactory.getLog(MqttClientUtil.class);
	
	private String mqttClientId = MqttClient.generateClientId();
	private static String mqttServerUrl = "";
	private static String mqttTopicName = "";
	private static MqttClient mqc;
	
	/**
	 * MqttClientUtil
	 * @throws Exception
	 */
	public MqttClientUtil() throws Exception{
	}
	
	/**
	 * 
	 * @param mqttServerUrl
	 * @param mqttTopicName
	 * @throws Exception
	 */
	public MqttClientUtil(String mqttServerUrl, String mqttTopicName) throws Exception{
		this.mqc = MqttUtil.getMettClient(mqttServerUrl);
		subscribe(mqttTopicName);
		
		this.mqttServerUrl = mqttServerUrl;
		this.mqttTopicName = mqttTopicName;
	}
	
	/**
	 * subscribe
	 * @param mqttTopic
	 */
	public void subscribe(String mqttTopic) {
		try {
			this.mqttTopicName = mqttTopic;
			mqc.subscribe(this.mqttTopicName);
		} catch (MqttException e) {
			System.out.println("[KETI MQTT Client] Subscribe Failed - " + mqttTopic);
			e.printStackTrace();
		}
		mqc.setCallback(this);
	}
	
	/**
	 * publishKetiPayload
	 * @param topic
	 * @param mgmtCmdResourceName
	 * @param controlValue
	 * @throws Exception
	 */
	public void publishKetiPayload(String topic, String mgmtCmdResourceName, String controlValue) throws Exception{
		MqttMessage msg = new MqttMessage();
		String payload = mgmtCmdResourceName + "," + controlValue;
		msg.setPayload(payload.getBytes());
		try {
			mqc.publish(topic, msg);
			System.out.println("[KETI MQTT Client] MQTT Topic \"" + topic + "\" Publish Payload = " + payload);
		} catch (MqttPersistenceException e) {
			System.out.println("[KETI MQTT Client] Publish Failed - " + topic);
			e.printStackTrace();
		} catch (MqttException e) {
			System.out.println("[KETI MQTT Client] Publish Failed - " + topic);
			e.printStackTrace();
		}
	}
	
	/**
	 * publishFullPayload
	 * @param topic
	 * @param payload
	 */
	public void publishFullPayload(String topic, String payload) {
		MqttMessage msg = new MqttMessage();
		msg.setPayload(payload.getBytes());
		try {
			mqc.publish(topic, msg);
			System.out.println("[KETI MQTT Client] MQTT Topic \"" + topic + "\" Publish Payload = " + payload);
		} catch (MqttPersistenceException e) {
			System.out.println("[KETI MQTT Client] Publish Failed - " + topic);
			e.printStackTrace();
		} catch (MqttException e) {
			System.out.println("[KETI MQTT Client] Publish Failed - " + topic);
			e.printStackTrace();
		}
	}
	
	/**
	 * connectionLost
	 */
	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("[KETI MQTT Client] Disconnected from MQTT Server");
		try {
			while(!mqc.isConnected()){
				mqc.connect();
				System.out.println("[KETI MQTT Client] Connection retry");
			}
			mqc.unsubscribe(this.mqttTopicName);
			mqc.subscribe(this.mqttTopicName);
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
		
		System.out.println("[KETI MQTT Client] Connected to Server - " + this.mqttServerUrl);
	}
	
	/**
	 * messageArrived
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) {
		System.out.println("[KETI MQTT Client] MQTT Topic \"" + topic + "\" Subscription Payload = " + byteArrayToString(message.getPayload()));
		
		String fullPayload = byteArrayToString(message.getPayload());
		String deviceId = getDeviceId(fullPayload);
		
		HttpResponse httpResponse = requestMqtt2http(fullPayload);
		
		if (!CommonUtil.isEmpty(httpResponse)) {
			publishFullPayload(deviceId, httpResponse.getResBody());
		}
	}
	
	/**
	 * deliveryComplete
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		System.out.println("[KETI MQTT Client] Message delivered successfully token = " + token);
	}
	
	/**
	 * getDeviceId
	 * @param fullPayload
	 * @return
	 */
	private String getDeviceId(String fullPayload) {
		int deviceIdStIndex = fullPayload.indexOf("<csi>") + "<csi>".length();
		int deviceIdEtIndex = fullPayload.indexOf("</csi>");
		String deviceId = fullPayload.substring(deviceIdStIndex, deviceIdEtIndex);
		
		return deviceId;
	}
	
	/**
	 * requestMqtt2http
	 * @param fullPayload
	 * @return
	 */
	private HttpResponse requestMqtt2http(String fullPayload) {
		HttpResponse httpResponse = null;
		String method = null;
		RequestMethod methodType = null;
		String strUri = null;
		HashMap<String,String> headers = new HashMap<String,String>();
		String body = null;
		String strLocalhost = PropertiesUtil.get("config", "iot.mqtt2http.uri");
		int bodyStIndex = -1;
		
		fullPayload.replace("\\r+", "");
		String[] payloadArray = fullPayload.split("[\\r\\n]+");
		if (payloadArray != null) {
			for (int i=0; i<payloadArray.length; i++) {
				String payload = payloadArray[i];
				if (payload.substring(0, 5).equals("<?xml")) break;
				
				if (i == 0) {
					String[] methodUriArray = payload.split(" ");
					method = methodUriArray[0];
					strUri = methodUriArray[1];
				} else if (i == 1) {
					String[] hostArray = payload.split(":");
					strUri = strLocalhost + strUri;
				} else {
					String[] headerArray = payload.split(":");
					headers.put(headerArray[0], headerArray[1].trim());
				}
			}
		}
		
		if (fullPayload != null) {
			if ((bodyStIndex = fullPayload.indexOf("<?")) > -1) {
				body = fullPayload.substring(bodyStIndex, fullPayload.length());
			}
		}
		
		if (		"GET".equals(method)) {
			methodType = RequestMethod.GET;
		} else if (	"POST".equals(method)) {
			methodType = RequestMethod.POST;
		} else if (	"PUT".equals(method)) {
			methodType = RequestMethod.PUT;
		} else if (	"DELETE".equals(method)) {
			methodType = RequestMethod.DELETE;
		} else {
			return null;
		}
		
		try {
			HTTPClient httpClient = new HTTPClient();
			httpResponse = httpClient.request(methodType, strUri, headers, body);
			
			// HTTP success
			if(httpResponse.getStatusCode() == HttpStatus.OK.value() || httpResponse.getStatusCode() == HttpStatus.CREATED.value()){
				mongoLogService.log(logger, LEVEL.INFO, "HTTP Success :: " + strUri + ", " + HttpStatus.OK.name());
			// HTTP fail
			} else {
				mongoLogService.log(logger, LEVEL.ERROR, "[MQTT protocol] HTTP fail httpStatusCode :: " + httpResponse.getStatusCode());
				mongoLogService.log(logger, LEVEL.ERROR, "[MQTT protocol] HTTP fail httpStatusCode :: " + HttpStatus.valueOf(httpResponse.getStatusCode()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			mongoLogService.log(logger, LEVEL.ERROR, "[MQTT protocol] HTTP Exception : " + e.getMessage());
		}
		
		return httpResponse;
	}
	
	/**
	 * byteArrayToString
	 * @param byteArray
	 * @return
	 */
	public String byteArrayToString(byte[] byteArray)
	{
	    String toString = "";

	    for(int i = 0; i < byteArray.length; i++)
	    {
	        toString += (char)byteArray[i];
	    }

	    return toString;    
	}
	
	/**
	 * getMqttClientId
	 * @return
	 */
	public String getMqttClientId() {
		return mqttClientId;
	}
	
	/**
	 * setMqttClientId
	 * @param mqttClientId
	 */
	public void setMqttClientId(String mqttClientId) {
		this.mqttClientId = mqttClientId;
	}
}
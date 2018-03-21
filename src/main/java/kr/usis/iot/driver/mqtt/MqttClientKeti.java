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

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

/**
 * MQTT client util.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
public class MqttClientKeti implements MqttCallback {
	private String mqttClientId = MqttClient.generateClientId();
	private String mqttServerUrl = "";
	private String mqttTopicName = "";
	private MqttClient mqc;
	
	private BlockingQueue<ArrayList<Object>> interactionManagerQueue;

	/**
	 * MqttClientKeti
	 * @param serverUrl
	 * @param interactionQueue
	 */
	public MqttClientKeti(String serverUrl,
							BlockingQueue<ArrayList<Object>> interactionQueue) {
		
		this.mqttServerUrl = serverUrl;
		this.interactionManagerQueue = interactionQueue;
		
		System.out.println("[KETI MQTT Client] Client Initialize");
		
		try {
			mqc = new MqttClient(mqttServerUrl, mqttClientId);
			
			while(!mqc.isConnected()){
				mqc.connect();
				System.out.println("[KETI MQTT Client] Connection try");
			}
			
			System.out.println("[KETI MQTT Client] Connected to Server - " + mqttServerUrl);
		} catch (MqttException e) {
			e.printStackTrace();
		}
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
	 * @param mgmtObjName
	 * @param controlValue
	 */
	public void publishKetiPayload(String topic, String mgmtObjName, String controlValue) {
		MqttMessage msg = new MqttMessage();
		String payload = mgmtObjName + "," + controlValue;
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
		
		System.out.println("[KETI MQTT Client] Connected to Server - " + mqttServerUrl);
	}
	
	/**
	 * messageArrived
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		System.out.println("[KETI MQTT Client] MQTT Topic \"" + topic + "\" Subscription Payload = " + byteArrayToString(message.getPayload()));
		
		ArrayList<Object> interactionSendArrayList = new ArrayList<Object>();
		interactionSendArrayList.add("requestMgmtCmdControl");
		interactionSendArrayList.add(byteArrayToString(message.getPayload()));
		
		interactionManagerQueue.put(interactionSendArrayList);
	}
	
	/**
	 * deliveryComplete
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		System.out.println("[KETI MQTT Client] Message delivered successfully");
	}
	
	public String byteArrayToString(byte[] byteArray)
	{
	    String toString = "";

	    for(int i = 0; i < byteArray.length; i++)
	    {
	        toString += (char)byteArray[i];
	    }

	    return toString;    
	}
}
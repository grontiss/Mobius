/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.driver.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.springframework.web.bind.annotation.RequestMethod;

import kr.usis.iot.domain.common.IotException;

/**
 * HTTP client.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
public class HTTPClient {
	
	/**
	 * request
	 * @param methodType
	 * @param strUri
	 * @param headers
	 * @param body
	 * @return
	 * @throws IotException
	 */
	public HttpResponse request(RequestMethod methodType, String strUri, Map<String, String> headers, String body) throws IotException {
		URI uri = null; // URI parameter of the request
		HttpResponse httpResponse = null;
		
		try {
			uri = new URI(strUri);
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (RequestMethod.GET.equals(methodType)) {
			httpResponse = getMethod(uri, headers, body);
		} else if (RequestMethod.POST.equals(methodType)) {
			httpResponse = postMethod(uri, headers, body);
		} else if (RequestMethod.PUT.equals(methodType)) {
			httpResponse = putMethod(uri, headers, body);
		} else if (RequestMethod.DELETE.equals(methodType)) {
			httpResponse = deleteMethod(uri, headers, body);
		}
		
		return httpResponse;
	}
	
	/**
	 * getMethod
	 * @param uri
	 * @param headers
	 * @param payloadString
	 * @return
	 */
	private HttpResponse getMethod(URI uri, Map<String, String> headers, String payloadString) {
		int statusCode = -1;
		int newTimeoutInMilliseconds = 3000;
		HttpResponse httpResponse = new HttpResponse();
		
		HttpClient client = new HttpClient();
		client.setTimeout(newTimeoutInMilliseconds);
		GetMethod method = new GetMethod(uri.toString());
		
		if (headers != null) {
			for( Map.Entry<String, String> header : headers.entrySet() ){
				System.out.println("header.getKey : " + header.getKey() + ", header.getValue" + header.getValue());
				method.setRequestHeader(header.getKey(), header.getValue());
	        }
		}
		
		//method.setRequestBody(payloadString);
		
		try {
			statusCode = client.executeMethod(method);
			String resBody = method.getResponseBodyAsString();
			
			httpResponse.setStatusCode(statusCode);
			httpResponse.setResBody(resBody);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return httpResponse;
	}
	
	/**
	 * postMethod
	 * @param uri
	 * @param headers
	 * @param payloadString
	 * @return
	 */
	private HttpResponse postMethod(URI uri, Map<String, String> headers, String payloadString) {
		int statusCode = -1;
		int newTimeoutInMilliseconds = 3000;
		HttpResponse httpResponse = new HttpResponse();
		
		HttpClient client = new HttpClient();
		client.setTimeout(newTimeoutInMilliseconds);
		PostMethod method = new PostMethod(uri.toString());
		
		if (headers != null) {
			for( Map.Entry<String, String> header : headers.entrySet() ){
				System.out.println("header.getKey : " + header.getKey() + ", header.getValue : " + header.getValue());
				method.setRequestHeader(header.getKey(), header.getValue());
	        }
		}
		method.setRequestBody(payloadString);
		
		try {
			statusCode = client.executeMethod(method);
			String resBody = method.getResponseBodyAsString();
			
			httpResponse.setStatusCode(statusCode);
			httpResponse.setResBody(resBody);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return httpResponse;
	}
	
	/**
	 * putMethod
	 * @param uri
	 * @param headers
	 * @param payloadString
	 * @return
	 */
	private HttpResponse putMethod(URI uri, Map<String, String> headers, String payloadString) {
		int statusCode = -1;
		int newTimeoutInMilliseconds = 3000;
		HttpResponse httpResponse = new HttpResponse();
		
		HttpClient client = new HttpClient();
		client.setTimeout(newTimeoutInMilliseconds);
		PutMethod method = new PutMethod(uri.toString());
		
		if (headers != null) {
			for( Map.Entry<String, String> header : headers.entrySet() ){
				System.out.println("header.getKey : " + header.getKey() + ", header.getValue" + header.getValue());
				method.setRequestHeader(header.getKey(), header.getValue());
	        }
		}
		
		method.setRequestBody(payloadString);
		
		try {
			statusCode = client.executeMethod(method);
			String resBody = method.getResponseBodyAsString();

			httpResponse.setStatusCode(statusCode);
			httpResponse.setResBody(resBody);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return httpResponse;
	}
	
	/**
	 * deleteMethod
	 * @param uri
	 * @param headers
	 * @param payloadString
	 * @return
	 */
	private HttpResponse deleteMethod(URI uri, Map<String, String> headers, String payloadString) {
		int statusCode = -1;
		int newTimeoutInMilliseconds = 3000;
		HttpResponse httpResponse = new HttpResponse();
		
		HttpClient client = new HttpClient();
		client.setTimeout(newTimeoutInMilliseconds);
		DeleteMethod method = new DeleteMethod(uri.toString());
		
		if (headers != null) {
			for( Map.Entry<String, String> header : headers.entrySet() ){
				System.out.println("header.getKey : " + header.getKey() + ", header.getValue" + header.getValue());
				method.setRequestHeader(header.getKey(), header.getValue());
	        }
		}
		
		//method.setRequestBody(payloadString);
		
		try {
			statusCode = client.executeMethod(method);
			String resBody = method.getResponseBodyAsString();
			
			httpResponse.setStatusCode(statusCode);
			httpResponse.setResBody(resBody);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return httpResponse;
	}
}

/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/

package kr.usis.iot.driver.coap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import kr.usis.iot.domain.common.IotException;

/**
 * CoAP client.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
public class CoAPClient {
	
	/**
	 * request
	 * @param methodType
	 * @param strUri
	 * @param headers
	 * @param body
	 * @return
	 * @throws IotException
	 */
	public CoAP.ResponseCode request(CoAP.Code methodType, String strUri, Map<Integer, Object> headers, String body) throws IotException {
		URI uri = null; // URI parameter of the request
		
		try {
			uri = new URI(strUri);
		} catch (URISyntaxException e) {
			throw new IotException("600", "[" + strUri + "] URI error");
		}
	
		Response response = push(methodType, uri, headers, body);
		
		return response.getCode();
	}
	
	/**
	 * push
	 * @param methodType
	 * @param uri
	 * @param headers
	 * @param body
	 * @return
	 * @throws IotException
	 */
	private Response push(CoAP.Code methodType, URI uri, Map<Integer, Object> headers, String body) throws IotException {
		Response response = null;
		
		System.out.println("uri : " + uri);
		
		Request request = null;
		if (CoAP.Code.GET.equals(methodType)) {
			request = Request.newGet();
		} else if (	CoAP.Code.POST.equals(methodType)) {
			request = Request.newPost();
		} else if (	CoAP.Code.PUT.equals(methodType)) {
			request = Request.newPut();
		} else if (	CoAP.Code.DELETE.equals(methodType)) {
			request = Request.newDelete();
		} else {
			request = Request.newGet();
		}
		
		request.setURI(uri);
		if (headers != null) {
			for( Map.Entry<Integer, Object> header : headers.entrySet() ){
				System.out.println("header.getKey : " + header.getKey() + ", header.getValue" + header.getValue());
	            
				switch (header.getKey()) {
				case 12:
				case 267:
					request.getOptions().addOption(new Option(header.getKey(), (Integer)header.getValue()));
					break;
				default:
					request.getOptions().addOption(new Option(header.getKey(), (String)header.getValue()));
					break;
				}
	        }
		}
		
		if (body != null) {
			request.setPayload(body);
		}
		
		request.send();
		
		// receive response
		try {
			response = request.waitForResponse(3000);
			
			if (response != null) {
				// response received, output a pretty-print
				System.out.println("response.getPayloadString : " + response.getPayloadString());
			} else {
				System.out.println("No response received.");
			}
			
		} catch (InterruptedException e) {
			throw new IotException("600", "Receiving of response interrupted: " + e.getMessage());
		}
		
		return response;
	}
}

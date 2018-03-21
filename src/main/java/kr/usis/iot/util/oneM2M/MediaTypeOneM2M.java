/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.util.oneM2M;

import org.springframework.http.MediaType;

public class MediaTypeOneM2M extends MediaType {
	
	public MediaTypeOneM2M(String type) {
		super(type);
		
	}

	public static final MediaType APPLICATION_VND_ONEM2M_RES_XML;
	
	public static final String APPLICATION_VND_ONEM2M_RES_XML_VALUE = "application/vnd.onem2m-res+xml;charset=UTF-8";
	
	public final static MediaType APPLICATION_VND_ONEM2M_RES_JSON;
	
	public final static String APPLICATION_VND_ONEM2M_RES_JSON_VALUE = "application/vnd.onem2m-res+json;charset=UTF-8";

	public final static MediaType APPLICATION_VND_ONEM2M_NTFY_XML;

	public final static String APPLICATION_VND_ONEM2M_NTFY_XML_VALUE = "application/vnd.onem2m-ntfy+xml;charset=UTF-8";

	public final static MediaType APPLICATION_VND_ONEM2M_NTFY_JSON;

	public final static String APPLICATION_VND_ONEM2M_NTFY_JSON_VALUE = "application/vnd.onem2m-ntfy+json;charset=UTF-8";

	public final static MediaType APPLICATION_VND_ONEM2M_PRSP_XML;

	public final static String APPLICATION_VND_ONEM2M_PRSP_XML_VALUE = "application/vnd.onem2m-prsp+xml;charset=UTF-8";

	public final static MediaType APPLICATION_VND_ONEM2M_PRSP_JSON;

	public final static String APPLICATION_VND_ONEM2M_PRSP_JSON_VALUE = "application/vnd.onem2m-prsp+json;charset=UTF-8";

	static {
		APPLICATION_VND_ONEM2M_RES_XML = MediaType.valueOf(APPLICATION_VND_ONEM2M_RES_XML_VALUE);
		APPLICATION_VND_ONEM2M_RES_JSON = MediaType.valueOf(APPLICATION_VND_ONEM2M_RES_JSON_VALUE);
		APPLICATION_VND_ONEM2M_NTFY_XML = MediaType.valueOf(APPLICATION_VND_ONEM2M_NTFY_XML_VALUE);
		APPLICATION_VND_ONEM2M_NTFY_JSON = MediaType.valueOf(APPLICATION_VND_ONEM2M_NTFY_JSON_VALUE);
		APPLICATION_VND_ONEM2M_PRSP_XML = MediaType.valueOf(APPLICATION_VND_ONEM2M_PRSP_XML_VALUE);
		APPLICATION_VND_ONEM2M_PRSP_JSON = MediaType.valueOf(APPLICATION_VND_ONEM2M_PRSP_JSON_VALUE);
	}
}

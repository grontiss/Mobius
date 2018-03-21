/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.service.apilog;

import java.util.List;

import kr.usis.iot.domain.apilog.ApiLogVO;
import kr.usis.iot.mdao.apilog.ApiLogDao;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SEQ_PREFIX;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.CALLER_FG;
import kr.usis.iot.util.oneM2M.CommonCode.CALL_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * apiLog management Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class ApiLogService {
	
	private static final Log logger = LogFactory.getLog(ApiLogService.class);
	
	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private ApiLogDao apiLogDao;
	
	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * set apilog
	 * @param callerFg
	 * @param callerId
	 * @param callType
	 * @param successYn
	 * @return
	 */
	public ApiLogVO setApiLog(CALLER_FG callerFg, String callerId, CALL_TYPE callType, String successYn) {
		String regDate = CommonUtil.getNow("yyyyMMddHHmmssSSS");
		ApiLogVO logProfile = new ApiLogVO();
		
		logProfile.setLogKey(this.generateResourceId());
		logProfile.setCallerFg(callerFg.getValue());
		logProfile.setCallerId(callerId);
		logProfile.setCallType(callType.getValue());
		if(!CommonUtil.isEmpty(successYn)) {
			logProfile.setSuccessYn(successYn);	
		}
		logProfile.setRegDate(regDate);
		
		return logProfile;

	}
	
	/**
	 * call log
	 * @param callerFg
	 * @param callerId
	 * @param callType
	 * @param successYn
	 */
	public void log(CALLER_FG callerFg, String callerId, CALL_TYPE callType, String successYn) {
		String regDate = CommonUtil.getNow("yyyyMMddHHmmssSSS");
		ApiLogVO logProfile = new ApiLogVO();
		
		logProfile.setLogKey(this.generateResourceId());
		logProfile.setCallerFg(callerFg.getValue());
		logProfile.setCallerId(callerId);
		logProfile.setCallType(callType.getValue());
		if(!CommonUtil.isEmpty(successYn)) {
			logProfile.setSuccessYn(successYn);	
		}
		logProfile.setRegDate(regDate);
		
		this.log(logProfile);
		
	}
	
	/**
	 * call log
	 * @param apiLogList
	 */
	public void log(List<ApiLogVO> apiLogList) {
		
		if(!CommonUtil.isEmpty(apiLogList) && apiLogList.size() > 0) {
			try { 
				apiLogDao.multi_insert(apiLogList);
			} catch (Exception e) {
				mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			}	
		}
		
	}

	/**
	 * call log
	 * @param logProfile
	 */
	public void log(ApiLogVO logProfile) {
		try { 
			apiLogDao.insert(logProfile);
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}
	}
	
	/**
	 * generate resourceId
	 * @return
	 */
	private String generateResourceId() {
		StringBuffer bufSeq = new StringBuffer(SEQ_PREFIX.MMP_API_CALL_LOG_TBL.getValue());
		try { 
			Long longSeq = seqDao.move(MovType.UP, SeqType.MMP_API_CALL_LOG_TBL);
			bufSeq.append(String.format("%017d", longSeq));
		} catch (Exception e) {
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
		}	
		
		return bufSeq.toString();
	}

}

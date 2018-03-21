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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ntels.nisf.util.PropertiesUtil;

import kr.usis.iot.domain.mongo.LogVO;
import kr.usis.iot.mdao.common.SequenceDao;
import kr.usis.iot.mdao.common.SequenceDao.MovType;
import kr.usis.iot.mdao.common.SequenceDao.SeqType;
import kr.usis.iot.mdao.log.LogDao;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.util.CommonUtil;


/**
 * log management Service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class MongoLogService {
	private static final Log logger = LogFactory.getLog(MongoLogService.class);
	private static final Logger logger1 = LoggerFactory.getLogger(MongoLogService.class);
	private String targetServer = PropertiesUtil.get("config","mlog.target_server.platform");
	
	public enum LEVEL{INFO,	DEBUG, WARN, ERROR}
	
	@Autowired
	private LogDao logDao;
	
	@Autowired
	private SequenceDao seqDao;
	
	/**
	 * call log
	 * @param logger
	 * @param level
	 * @param message
	 */
	public void log(Log logger, LEVEL level, String message) {
		
		if(level.equals(LEVEL.ERROR)) {
			long logKeySeq = seqDao.move(MovType.UP,SeqType.MMP_LOG_MSG_TBL);
			String logKey = Long.toString(logKeySeq);
			
			LogVO log = new LogVO();
			log.setLog_key(logKey);
			log.setTarget_server(targetServer);
			log.setLevel(level.name());
			log.setMessage(message);
			log.setReg_dt(CommonUtil.getNow("yyyyMMddHHmmssSSS"));
			
			try {
				logDao.insert(log);
			} catch (Exception e) {
				MongoLogService.logger.debug(e.getMessage());
			}
			logger.error(message);
		} else if (level.equals(LEVEL.WARN)) {
			logger.warn(message);
		} else if (level.equals(LEVEL.DEBUG)) {
			logger.debug(message);
		} else if (level.equals(LEVEL.INFO)) {
			logger.info(message);
		} 
	}
	
	/**
	 * ++ USIS : 2016-07-11
	 * call log
	 * @param logger
	 * @param level
	 * @param message
	 */
	public void log(Logger logger, LEVEL level, String message) {
		
		if(level.equals(LEVEL.ERROR)) {
			long logKeySeq = seqDao.move(MovType.UP,SeqType.MMP_LOG_MSG_TBL);
			String logKey = Long.toString(logKeySeq);
			
			LogVO log = new LogVO();
			log.setLog_key(logKey);
			log.setTarget_server(targetServer);
			log.setLevel(level.name());
			log.setMessage(message);
			log.setReg_dt(CommonUtil.getNow("yyyyMMddHHmmssSSS"));
			
			try {
				logDao.insert(log);
			} catch (Exception e) {
				MongoLogService.logger1.debug(e.getMessage());
			}
			logger.error(message);
		} else if (level.equals(LEVEL.WARN)) {
			logger.warn(message);
		} else if (level.equals(LEVEL.DEBUG)) {
			logger.debug(message);
		} else if (level.equals(LEVEL.INFO)) {
			logger.info(message);
		} 
	}
}
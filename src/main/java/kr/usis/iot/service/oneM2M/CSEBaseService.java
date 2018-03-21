/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.service.oneM2M;

import kr.usis.iot.domain.common.RSCException;
import kr.usis.iot.domain.oneM2M.CSEBase;
import kr.usis.iot.domain.oneM2M.FilterCriteria;
import kr.usis.iot.domain.oneM2M.RequestPrimitive;
import kr.usis.iot.mdao.oneM2M.CSEBaseDao;
import kr.usis.iot.service.apilog.MongoLogService;
import kr.usis.iot.service.apilog.MongoLogService.LEVEL;
import kr.usis.iot.service.oneM2M.common.MCommonService;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * CSEBase service.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Service
public class CSEBaseService {

	private static final Log logger = LogFactory.getLog(CSEBaseService.class);

	@Autowired
	private MongoLogService mongoLogService;
	
	@Autowired
	private MCommonService mCommonService;

	@Autowired
	private CSEBaseDao cseBaseDao;

	/**
	 * CSEBase retrieve
	 * @param cseid
	 * @return
	 * @throws RSCException
	 */
	public CSEBase findOneCSEBaseByCSEID(String cseid) throws RSCException {
		CSEBase findCSEBaseItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("cseid").is(cseid));

		try {
			findCSEBaseItem = (CSEBase) cseBaseDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.CSEBase.findFail.text"));
		}

		return findCSEBaseItem;
	}
	
	/**
	 * CSEBase retrieve
	 * @param cseid
	 * @return
	 * @throws RSCException
	 */
	public CSEBase findOneCSEBaseByResourceName(String resourceName) throws RSCException {
		CSEBase findCSEBaseItem = null;

		Query query = new Query();
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findCSEBaseItem = (CSEBase) cseBaseDao.findOne(query);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.CSEBase.findFail.text"));
		}

		return findCSEBaseItem;
	}
	
	/**
	 * CSEBase retrieve
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public CSEBase findOneCSEBaseByResourceName(String resourceName, RequestPrimitive requestPrimitive, FilterCriteria filterCriteria) throws RSCException {
		if (!CommonUtil.isEmpty(filterCriteria.getLim()) && filterCriteria.getLim().intValue() <= 0) return null;

		CSEBase findCSEBaseItem = null;
		CSEBase findNoFilterCSEBaseItem = null;

		Query query = mCommonService.requestFilterCriteriaToQuery(filterCriteria);
		query.addCriteria(Criteria.where("resourceName").is(resourceName));

		try {
			findCSEBaseItem = (CSEBase) cseBaseDao.findOne(query);
			mCommonService.setLimitForFilterCriteria(requestPrimitive, filterCriteria, findCSEBaseItem);
			
			if (filterCriteria.isFilterCriteria()) findNoFilterCSEBaseItem = findOneCSEBaseResourceRefByResourceName(resourceName);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.CSEBase.findFail.text"));
		}
		
		if (filterCriteria.isFilterCriteria() && CommonUtil.isEmpty(findCSEBaseItem)) findCSEBaseItem = findNoFilterCSEBaseItem;

		return findCSEBaseItem;
	}
	
	/**
	 * CSEBase References retrieve
	 * @param resourceName
	 * @return
	 * @throws RSCException
	 */
	public CSEBase findOneCSEBaseResourceRefByResourceName(String resourceName) throws RSCException {
		
		String includeField = "resourceRef";
		CSEBase findCSEBaseResourceRefItem = null;
		
		Query query = new Query();
		query.addCriteria(Criteria.where("resourceName").is(resourceName));
		query.fields().include(includeField);
		
		try {
			findCSEBaseResourceRefItem = (CSEBase) cseBaseDao.findOne(query);
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			mongoLogService.log(logger, LEVEL.ERROR, e.getMessage());
			throw new RSCException(CommonCode.RSC.INTERNAL_SERVER_ERROR, CommonUtil.getMessage("msg.CSEBase.findFail.text"));
		}
		
		return findCSEBaseResourceRefItem;
	}
	
}
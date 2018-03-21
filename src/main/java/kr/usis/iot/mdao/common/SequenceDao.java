/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.mdao.common;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import kr.usis.iot.domain.mongo.Sequence;

/**
 * sequence management Dao.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Repository 
public class SequenceDao {
	
	public enum SeqType{ ACCESS_CONTROL_POLICY, REMOTE_CSE, AE, GROUP, CONTAINER, CONTENT_INSTANCE, LOCATION_POLICY, SUBSCRIPTION, MGMT_OBJ, MGMT_CMD, EXEC_INSTANCE, NODE, MMP_API_CALL_LOG_TBL, MMP_LOG_MSG_TBL, MMP_VERT_CONTENT_TBL, VERT_DEVICE, VERT_CONTAINER, VERT_MGMTOBJ, VERT_TID, SOFTWARE, DEVICE_INFO, FIRMWARE, MEMORY, BATTERY, REBOOT, SUBSCRIPTION_PENDING};
	public enum MovType{ UP , DOWN };
	
	public enum SEQ_PREFIX {
		
		CSE_BASE("CB"),
		NODE("ND"),
		FIRMWARE("FW"),
		SOFTWARE("SW"), 
		MEMORY("MM"), 
		BATTERY("BT"), 
		REBOOT("RB"), 
		DEVICE_INFO("DI"), 
		ACCESS_CONTROL_POLICY("AP"),
		REMOTE_CSE("RC"),
		CONTAINER("CT"), 
		CONTENT_INSTANCE("CI"), 
		MGMT_CMD("MC"), 
		EXEC_INSTANCE("EI"), 
		AE("AE"), 
		AEID("S"), 
		SUBSCRIPTION("SS"), 
		LOCATION_POLICY("LP"), 
		GROUP(("GP")),
		MMP_API_CALL_LOG_TBL(("CL"))
		; 
		
		private final String id;
		
		SEQ_PREFIX(String id) {
			this.id = id;
		}
		
		public String getValue() {
			return id;
		}
		
	}
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	private final String COLLECTION_NAME = "MMP_SEQ_TBL";

	/**
	 * insert
	 * @param item
	 */
	public void insert(Sequence item) {
		mongoTemplate.insert(item, COLLECTION_NAME);
	}

	/**
	 * findOne
	 * @param query
	 * @return
	 */
	public Sequence findOne(Query query) {
		return mongoTemplate.findOne(query, Sequence.class, COLLECTION_NAME);
	}

	/**
	 * move
	 * @param m_type
	 * @param s_type
	 * @return
	 */
	public synchronized long move(MovType m_type, SeqType s_type) {
		long move = (m_type == MovType.UP) ? 1:-1;
		String type = s_type.toString();
		
		Query query = new Query();
		query.addCriteria(Criteria.where("type").is(type));
				
		Update update = new Update();
		update.inc("sequence", move);
		
		FindAndModifyOptions option = new FindAndModifyOptions();
		option.upsert(true);
		
		Sequence findObj = mongoTemplate.findAndModify(query, update, option , Sequence.class, COLLECTION_NAME);
		
		return findObj.getSequence();
	}
}

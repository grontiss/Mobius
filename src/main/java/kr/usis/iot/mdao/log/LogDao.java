/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.mdao.log;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import kr.usis.iot.domain.mongo.LogVO;

/**
 * log management Dao.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Repository
public class LogDao {
	@Autowired
	private MongoTemplate mongoTemplate;
	
	private final String collectionName = "MMP_LOG_MSG_TBL";

	/**
	 * upsert
	 * @param item
	 * @throws Exception
	 */
	public void upsert(LogVO item) throws Exception {
		mongoTemplate.save(item,collectionName);
	}

	/**
	 * insert
	 * @param item
	 * @throws Exception
	 */
	public void insert(LogVO item) throws Exception {
		mongoTemplate.insert(item, collectionName);
	}

	/**
	 * find
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<LogVO> find(Query query) throws Exception {
		return mongoTemplate.find(query, LogVO.class, collectionName);
	}

	/**
	 * findOne
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public LogVO findOne(Query query) throws Exception {
		return mongoTemplate.findOne(query, LogVO.class, collectionName);
	}

	/**
	 * findAll
	 * @return
	 * @throws Exception
	 */
	public List<LogVO> findAll() throws Exception {
		return mongoTemplate.findAll(LogVO.class, collectionName);
	}

	/**
	 * count
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public long count(Query query) throws Exception {
		return mongoTemplate.count(query, collectionName);
	}

	/**
	 * update
	 * @param query
	 * @param update
	 * @throws Exception
	 */
	public void update(Query query, Update update) throws Exception {
		mongoTemplate.updateMulti(query, update, collectionName);
	}

	/**
	 * remove
	 * @param query
	 * @throws Exception
	 */
	public void remove(Query query) throws Exception {
		mongoTemplate.remove(query,collectionName);
	}	
}

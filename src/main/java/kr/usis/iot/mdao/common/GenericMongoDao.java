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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.DBObject;

import kr.usis.iot.domain.oneM2M.ContainerOnlyId;
import kr.usis.iot.domain.oneM2M.MgmtCmdOnlyId;

/**
 * GenericMongo management Dao.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Repository 
public class GenericMongoDao {
	
	protected String collectionName = null;
	protected Class cls = null;
	
	@Autowired
	protected MongoTemplate mongoTemplate;
	
	/**
	 * upsert
	 * @param obj
	 * @throws Exception
	 */
	public void upsert(Object obj) throws Exception {
		mongoTemplate.save(obj, this.collectionName);
	}

	/**
	 * insert
	 * @param obj
	 * @throws Exception
	 */
	public void insert(Object obj) throws Exception {
		mongoTemplate.insert(obj, this.collectionName);
	}

	/**
	 * find
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> find(Query query) throws Exception {
		return  mongoTemplate.find(query, this.cls, this.collectionName);
	}
	
	/**
	 * findOne
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public Object findOne(Query query) throws Exception {
		return  mongoTemplate.findOne(query, this.cls, this.collectionName);
	}

	/**
	 * findAll
	 * @return
	 * @throws Exception
	 */
	public List<?> findAll() throws Exception {
		return  mongoTemplate.findAll(this.cls, this.collectionName);
	}

	/**
	 * count
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public long count(Query query) throws Exception { 
		return mongoTemplate.count(query, this.collectionName);
	}

	/**
	 * update
	 * @param query
	 * @param update
	 * @throws Exception
	 */
	public void update(Query query, Update update) throws Exception {
		mongoTemplate.upsert(query, update, collectionName);
	}

	/**
	 * remove
	 * @param query
	 * @throws Exception
	 */
	public void remove(Query query) throws Exception {
		mongoTemplate.remove(query, this.collectionName);
	}

	/**
	 * remove
	 * @param obj
	 * @throws Exception
	 */
	public void remove(Object obj) throws Exception {
		mongoTemplate.remove(obj, this.collectionName);
	}
	
	/**
	 * findAndModify
	 * @param query
	 * @param update
	 * @throws Exception
	 */
	public void findAndModify(Query query, Update update) throws Exception {
		mongoTemplate.findAndModify(query, update, this.cls, this.collectionName);
	}
	
	/**
	 * findContainerOnlyID
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> findContainerOnlyID(Query query) throws Exception {
		return  mongoTemplate.find(query, ContainerOnlyId.class, this.collectionName);
	}

	/**
	 * findMgmtCmdOnlyID
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> findMgmtCmdOnlyID(Query query) throws Exception {
		return  mongoTemplate.find(query, MgmtCmdOnlyId.class, this.collectionName);
	}

	/**
	 * getConverter
	 * @param clazz
	 * @param dbObject
	 * @return
	 * @throws Exception
	 */
	public Object getConverter(Class<?> clazz, DBObject dbObject) throws Exception {
		return mongoTemplate.getConverter().read(clazz, dbObject);
	}
	
}

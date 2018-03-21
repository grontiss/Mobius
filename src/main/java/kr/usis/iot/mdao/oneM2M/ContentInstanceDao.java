/**
 * Copyright (c) 2015, Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com >.
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
   3. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package kr.usis.iot.mdao.oneM2M;

import java.util.List;

import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.mdao.common.GenericRedisDao;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * contentInstance Dao.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Repository
public interface ContentInstanceDao extends GenericRedisDao<ContentInstance> {
	
	/**
	 * getObjectsKeys
	 * @param offset
	 * @param count
	 * @return
	 * @throws Exception
	 */
	public List<String> getObjectsKeys(long offset, long count) throws Exception;

	/**
	 * getObjectsByKeys
	 * @param keys
	 * @return
	 * @throws Exception
	 */
	public List<ContentInstance> getObjectsByKeys(List<String> keys) throws Exception;
	
	/**
	 * redisDeleteObjectsByKeys
	 * @param keys
	 * @throws Exception
	 */
	public void redisDeleteObjectsByKeys(List<String> keys) throws Exception;

	/**
	 * upsert
	 * @param obj
	 * @throws Exception
	 */
	public void upsert(Object obj) throws Exception;

	/**
	 * insert
	 * @param obj
	 * @throws Exception
	 */
	public void insert(Object obj) throws Exception;

	/**
	 * find
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> find(Query query) throws Exception;

	/**
	 * findOne
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public Object findOne(Query query) throws Exception;

	/**
	 * findAll
	 * @return
	 * @throws Exception
	 */
	public List<?> findAll() throws Exception;

	/**
	 * count
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public long count(Query query) throws Exception;

	/**
	 * update
	 * @param query
	 * @param update
	 * @throws Exception
	 */
	public void update(Query query, Update update) throws Exception;

	/**
	 * remove
	 * @param query
	 * @throws Exception
	 */
	public void remove(Query query) throws Exception;

	/**
	 * remove
	 * @param obj
	 * @throws Exception
	 */
	public void remove(Object obj) throws Exception;

	/**
	 * multi_insert
	 * @param contentInstanceList
	 * @throws Exception
	 */
	public void multi_insert(List<ContentInstance> contentInstanceList) throws Exception;
	
	/**
	 * getRedisObjectsByParentID
	 * @param parentID
	 * @return
	 * @throws Exception
	 */
	public List<ContentInstance> getRedisObjectsByParentID(String parentID) throws Exception;
	
	/**
	 * deleteRedisObjectsByParentID
	 * @param parentID
	 * @throws Exception
	 */
	public void deleteRedisObjectsByParentID(String parentID) throws Exception;
}
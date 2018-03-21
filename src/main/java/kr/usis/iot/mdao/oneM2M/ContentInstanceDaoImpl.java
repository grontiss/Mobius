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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import kr.usis.iot.domain.oneM2M.ContentInstance;
import kr.usis.iot.mdao.common.GenericRedisDaoImpl;
import kr.usis.iot.util.CommonUtil;
import kr.usis.iot.util.oneM2M.CommonCode.RESOURCE_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.connection.SortParameters.Order;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.query.SortQueryBuilder;
import org.springframework.stereotype.Repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * contentInstance DaoImpl.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
@Repository
public class ContentInstanceDaoImpl extends GenericRedisDaoImpl<ContentInstance> implements ContentInstanceDao {

	@Autowired
	protected MongoTemplate mongoTemplate;

	private String collectionName = RESOURCE_TYPE.CONTENT_INSTANCE.getName();
	private Class cls = ContentInstance.class;

	/**
	 * getObjectsKeys
	 */
	public List<String> getObjectsKeys(long offset, long count) throws Exception {

		SortQueryBuilder<String> builder = SortQueryBuilder.sort("timestamp");
		builder.alphabetical(true);
		builder.order(Order.ASC);
		builder.limit(offset, count);
		return template.sort(builder.build());
	}

	/**
	 * getRedisObjectsByParentID
	 */
	public List<ContentInstance> getRedisObjectsByParentID(String parentID) throws Exception {
		List<ContentInstance> contentInstanceList = null;
		
		Set<String> keys = template.keys("*_" + parentID + "_*");
		if (!CommonUtil.isEmpty(keys) && keys.size() > 0) {
			List<String> keyList = new ArrayList<String>();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				keyList.add(key);
			}
			contentInstanceList = this.getObjectsByKeys(keyList);
		}

		return contentInstanceList;
	}
	
	/**
	 * deleteRedisObjectsByParentID
	 */
	public void deleteRedisObjectsByParentID(String parentID) throws Exception {
		Set<String> keys = template.keys("*_" + parentID + "_*");
		if (!CommonUtil.isEmpty(keys) && keys.size() > 0) {
			List<String> keyList = new ArrayList<String>();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				keyList.add(key);
			}
			this.redisDeleteObjectsByKeys(keyList);
		}
	}

	/**
	 * getObjectsByKeys
	 */
	public List<ContentInstance> getObjectsByKeys(List<String> keys) throws Exception {
		Gson gson = new Gson();
		List<ContentInstance> objs = new ArrayList<ContentInstance>();

		List<String> results = template.opsForValue().multiGet(keys);

		for (String item : results) {
			if(!CommonUtil.isEmpty(item)) {
				objs.add(gson.fromJson(item, ContentInstance.class));
			}
		}
		return objs;
	}

	/**
	 * redisDeleteObjectsByKeys
	 */
	public void redisDeleteObjectsByKeys(List<String> keys) throws Exception {
		template.delete(keys);
		for (String key : keys) {
			template.opsForList().remove("timestamp", 1, key);
			if (!CommonUtil.isEmpty(key)) {
				template.opsForList().remove("timestamp", 1, key);
			}
		}
	}

	/**
	 * put
	 */
	public String put(ContentInstance contentInstance) {
		return this.put(contentInstance, this.collectionName);
	}

	/**
	 * put
	 */
	public String put(ContentInstance contentInstance, String collectionName) {
		final String time = getTime();
		final String containerId = contentInstance.getParentID();
		final String collection = collectionName;
		
		Gson gson = new Gson();
		Type ObjectType = new TypeToken<ContentInstance>() {
		}.getType();

		final String json = gson.toJson(contentInstance);
		SessionCallback<String> sessionCallback = new SessionCallback<String>() {
			public String execute(RedisOperations operations) throws DataAccessException {
				
				String size = getSequence();
				
				operations.multi();

				try {
					
					String key = time + "." + size + "_" + containerId + "_" + collection;
					
					ListOperations<String, String> listOper = operations.opsForList();
					listOper.rightPush("timestamp", key);
					
					BoundValueOperations<String, String> valueOper = operations.boundValueOps(key);
					valueOper.set(json);
					
					operations.exec();

					return key;

				} catch (Exception e) {
					operations.discard();
					e.printStackTrace();
					return "fail";
				}
			}
		};
		return template.execute(sessionCallback);
	}

	/**
	 * multi_insert
	 */
	public void multi_insert(List<ContentInstance> contentInstanceList) throws Exception {
		mongoTemplate.insert(contentInstanceList, collectionName);
	}

	/**
	 * upsert
	 */
	public void upsert(Object obj) throws Exception {
		mongoTemplate.save(obj, this.collectionName);
	}

	/**
	 * insert
	 */
	public void insert(Object obj) throws Exception {
		mongoTemplate.insert(obj, this.collectionName);
	}

	/**
	 * find
	 */
	public List<?> find(Query query) throws Exception {
		return mongoTemplate.find(query, this.cls, this.collectionName);
	}

	/**
	 * findOne
	 */
	public Object findOne(Query query) throws Exception {
		return mongoTemplate.findOne(query, this.cls, this.collectionName);
	}

	/**
	 * findAll
	 */
	public List<?> findAll() throws Exception {
		return mongoTemplate.findAll(this.cls, this.collectionName);
	}

	/**
	 * count
	 */
	public long count(Query query) throws Exception {
		return mongoTemplate.count(query, this.collectionName);
	}

	/**
	 * update
	 */
	public void update(Query query, Update update) throws Exception {
		mongoTemplate.updateMulti(query, update, this.collectionName);
	}

	/**
	 * remove
	 */
	public void remove(Query query) throws Exception {
		mongoTemplate.remove(query, this.collectionName);
	}

	/**
	 * remove
	 */
	public void remove(Object obj) throws Exception {
		mongoTemplate.remove(obj, this.collectionName);
	}

}
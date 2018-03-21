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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.SortParameters.Order;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.query.SortQueryBuilder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * generic redis management Dao implement.
 * @author <ul>
 *         <li>Sang June Lee < blue4444eye@hanmail.net > < blue4444eye@gmail.com ></li>
 *         </ul>
 */
public abstract class GenericRedisDaoImpl<E> implements
		GenericRedisDao<E> {

	@Autowired
	protected RedisTemplate<String, String> template;
	
	private Class<E> entityClass;
	
	public GenericRedisDaoImpl() {
		
		System.out.println("genericSuperclass : " + getClass().getGenericSuperclass());
		ParameterizedType genericSuperclass = (ParameterizedType) getClass()
				.getGenericSuperclass();
		Type type = genericSuperclass.getActualTypeArguments()[0];
		if (type instanceof ParameterizedType) {
			this.entityClass = (Class) ((ParameterizedType) type).getRawType();
		} else {
			this.entityClass = (Class) type;
		}
	}
	
	/**
	 * put
	 */
	public String put(E obj) {
		return this.put(obj,entityClass.getSimpleName());
	}
	
	/**
	 * put
	 */
	public String put(E obj, String collectionName) {
		final String time = getTime();
		final String collection = collectionName;
		
		Gson gson = new Gson();
		Type ObjectType = new TypeToken<E>() {
		}.getType();
		final String json = gson.toJson(obj);
		
		SessionCallback<String> sessionCallback = new SessionCallback<String>() {
			public String execute(RedisOperations operations)
					throws DataAccessException {
				
				String size = getSequence();
				
				operations.multi();

				try {
					
					String key = time +"."+ size + "_" + collection;
					
					ListOperations<String, String> listOper = operations.opsForList();
					listOper.rightPush("timestamp", key);
					
					BoundValueOperations<String, String> valueOper = operations
							.boundValueOps(key);
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
	 * get
	 */
	public E get(String key) {

		Gson gson = new Gson();
		return gson.fromJson((String) template.opsForValue().get(key),
				entityClass);
	}

	/**
	 * delete
	 */
	public void delete(String key) {
		template.delete(key);
		template.opsForList().remove("timestamp",1,key);
		if(key!=null || !"".equals(key)){
			template.opsForList().remove("timestamp",1,key);
		}
	}
	
	/**
	 * getSequence
	 * @return
	 */
	public String getSequence() {
		String cnt = "0";
		
		try {
			RedisSequenceScript script = new RedisSequenceScript();
			cnt = template.execute(script);
		} catch (Exception e) {
			System.out.println("timestamp list get count error [" + e.getMessage() + "]");
		}
		
		return cnt;
	}

	/**
	 * getObjects
	 */
	public List<E> getObjects() {
		return getObjects(0, 10);
	}

	/**
	 * getObjects
	 */
	public List<E> getObjects(long offset, long count) {
		Gson gson = new Gson();
		List<E> objs = new ArrayList<E>();
		
		SortQueryBuilder<String> builder = SortQueryBuilder.sort("timestamp");
		builder.alphabetical(true);
		builder.order(Order.DESC);
		builder.limit(offset, count);
		List<String> keys = template.sort(builder.build());
		
		List<String> results = template.opsForValue().multiGet(keys);
		
		for (String item : results) {
			objs.add(gson.fromJson(item, entityClass));
		}
		return objs;
	}
	
	/**
	 * getTime
	 * @return
	 */
	public String getTime() {
		return String.valueOf(getTimestamp());
	}

	/**
	 * getTimestamp
	 * @return
	 */
	private long getTimestamp() {
		return System.currentTimeMillis();
	}

}

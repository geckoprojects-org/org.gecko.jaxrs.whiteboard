/**
 * Copyright (c) 2012 - 2020 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.runtime.common;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.osgi.service.jaxrs.client.PromiseRxInvoker;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

/**
 * 
 * @author ilenia
 * @since Jun 12, 2020
 */
public class PromiseRxInvokerImpl implements PromiseRxInvoker {
	
	
	private SyncInvoker syncInvoker;
	private PromiseFactory factory;
	
	public PromiseRxInvokerImpl(SyncInvoker syncInvoker, ExecutorService executorService) {
		this.syncInvoker = syncInvoker;		

        if (executorService != null) {
        	factory = new PromiseFactory(executorService);
        }
        else {
        	factory = new PromiseFactory(
                PromiseFactory.inlineExecutor());
        }				
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#delete()
	 */
	@Override
	public Promise<Response> delete() {		
		return method(HttpMethod.DELETE);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#delete(java.lang.Class)
	 */
	@Override
	public <R> Promise<R> delete(Class<R> clazz) {
		return method(HttpMethod.DELETE, clazz);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#delete(javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> delete(GenericType<R> type) {
		return method(HttpMethod.DELETE, type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#get()
	 */
	@Override
	public Promise<Response> get() {
		return method(HttpMethod.GET);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#get(java.lang.Class)
	 */
	@Override
	public <R> Promise<R> get(Class<R> clazz) {
		return method(HttpMethod.GET, clazz);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#get(javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> get(GenericType<R> type) {
		return method(HttpMethod.GET, type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#head()
	 */
	@Override
	public Promise<Response> head() {
		return method(HttpMethod.HEAD);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#method(java.lang.String, java.lang.Class)
	 */
	@Override
	public <R> Promise<R> method(String method, Class<R> clazz) {
		return factory.submit(() -> syncInvoker.method(method, clazz));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#method(java.lang.String, javax.ws.rs.client.Entity, java.lang.Class)
	 */
	@Override
	public <R> Promise<R> method(String method, Entity<?> entity, Class<R> clazz) {
		return factory.submit(() -> syncInvoker.method(method, entity, clazz));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#method(java.lang.String, javax.ws.rs.client.Entity, javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> method(String method, Entity<?> entity, GenericType<R> type) {
		return factory.submit(() -> syncInvoker.method(method, entity, type));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#method(java.lang.String, javax.ws.rs.client.Entity)
	 */
	@Override
	public Promise<Response> method(String method, Entity<?> entity) {
		return factory.submit(() -> syncInvoker.method(method, entity));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#method(java.lang.String, javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> method(String method, GenericType<R> type) {
		return factory.submit(() -> syncInvoker.method(method, type));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#method(java.lang.String)
	 */
	@Override
	public Promise<Response> method(String method) {
		return factory.submit(() -> syncInvoker.method(method));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#options()
	 */
	@Override
	public Promise<Response> options() {
		return method(HttpMethod.OPTIONS);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#options(java.lang.Class)
	 */
	@Override
	public <R> Promise<R> options(Class<R> clazz) {
		return method(HttpMethod.OPTIONS, clazz);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#options(javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> options(GenericType<R> type) {
		return method(HttpMethod.OPTIONS, type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#post(javax.ws.rs.client.Entity, java.lang.Class)
	 */
	@Override
	public <R> Promise<R> post(Entity<?> arg0, Class<R> clazz) {
		return method(HttpMethod.POST, clazz);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#post(javax.ws.rs.client.Entity, javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> post(Entity<?> arg0, GenericType<R> type) {
		return method(HttpMethod.POST, type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#post(javax.ws.rs.client.Entity)
	 */
	@Override
	public Promise<Response> post(Entity<?> entity) {
		return method(HttpMethod.POST, entity);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#put(javax.ws.rs.client.Entity, java.lang.Class)
	 */
	@Override
	public <R> Promise<R> put(Entity<?> arg0, Class<R> clazz) {
		return method(HttpMethod.PUT, clazz);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#put(javax.ws.rs.client.Entity, javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> put(Entity<?> entity, GenericType<R> type) {
		return method(HttpMethod.PUT, entity, type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#put(javax.ws.rs.client.Entity)
	 */
	@Override
	public Promise<Response> put(Entity<?> entity) {
		return method(HttpMethod.PUT, entity);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#trace()
	 */
	@Override
	public Promise<Response> trace() {
		return factory.submit(() -> syncInvoker.trace());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#trace(java.lang.Class)
	 */
	@Override
	public <R> Promise<R> trace(Class<R> clazz) {
		return factory.submit(() -> syncInvoker.trace(clazz));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.PromiseRxInvoker#trace(javax.ws.rs.core.GenericType)
	 */
	@Override
	public <R> Promise<R> trace(GenericType<R> type) {
		return factory.submit(() -> syncInvoker.trace(type));
	}

}

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

import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.client.SyncInvoker;

import org.osgi.service.jaxrs.client.PromiseRxInvoker;

/**
 * 
 * @author ilenia
 * @since Jun 12, 2020
 */
public class RxInvokerProviderImpl implements RxInvokerProvider<PromiseRxInvoker> {
	
	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.client.RxInvokerProvider#isProviderFor(java.lang.Class)
	 */
	@Override
	public synchronized boolean isProviderFor(Class<?> clazz) {
		if(PromiseRxInvoker.class.equals(clazz)) {
			return true;
		}
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.client.RxInvokerProvider#getRxInvoker(javax.ws.rs.client.SyncInvoker, java.util.concurrent.ExecutorService)
	 */
	@Override
	public synchronized PromiseRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
		return new PromiseRxInvokerImpl(syncInvoker, executorService);		
	}

}

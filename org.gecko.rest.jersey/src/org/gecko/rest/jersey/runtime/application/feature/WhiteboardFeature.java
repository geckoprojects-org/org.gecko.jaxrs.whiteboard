/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.runtime.application.feature;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.osgi.framework.ServiceObjects;

/**
 * A {@link Feature} implementation registering all extensions as singleton and according to there provided contracts. 
 * @author Juergen Albert
 * @since 16.01.2018
 */
public class WhiteboardFeature implements Feature{

	Map<String, JaxRsExtensionProvider> extensions;
	
	@SuppressWarnings("rawtypes")
	Map<Object, ServiceObjects> serviceObjTrackingMap = new HashMap<>();
	
	
	public WhiteboardFeature(Map<String, JaxRsExtensionProvider> extensions) {
		this.extensions = new HashMap<>(extensions);
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.core.Feature#configure(javax.ws.rs.core.FeatureContext)
	 */
	@Override
	public boolean configure(FeatureContext context) {
		extensions.forEach((k, extension) -> {
			Object serviceObject = ((ServiceObjects<?>) extension.getProviderObject()).getService();
			serviceObjTrackingMap.put(serviceObject, (ServiceObjects<?>) extension.getProviderObject());
			
			if(extension.getContracts() != null) {
				context.register(serviceObject, extension.getContracts());
			} else {
				context.register(serviceObject);
			}
		});
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void dispose() {
		serviceObjTrackingMap.forEach((k,v) -> {
			v.ungetService(k);
		});
		serviceObjTrackingMap.clear();
		extensions.clear();
	}
	
//	@SuppressWarnings("unchecked")
//	public void dispose(Object serviceObject) {
//		if(serviceObjTrackingMap.containsKey(serviceObject)) {
//			serviceObjTrackingMap.get(serviceObject).ungetService(serviceObject);
//			serviceObjTrackingMap.remove(serviceObject);
//		}
//	}

	@SuppressWarnings("unchecked")
	public void dispose(JaxRsExtensionProvider extProvider) {
		ServiceObjects<?> objexts = (ServiceObjects<?>) extProvider.getProviderObject();
		serviceObjTrackingMap.entrySet().stream()
			.filter(e -> e.getValue().equals(objexts))
			.findFirst()
			.ifPresent(e -> {
				e.getValue().ungetService(e.getKey());
				serviceObjTrackingMap.remove(e.getKey());
				System.err.println("!!!!!!!!!!!!!!!!!!!!!! REMOVED !!!!!!!!!!!!!!!!!!!!");
			} );
		
	}
	
	
}

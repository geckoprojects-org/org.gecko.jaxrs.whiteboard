/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
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
			if(extension.getContracts() != null) {
				context.register(serviceObject, extension.getContracts());
			}
			context.register(serviceObject);
		});
		return true;
	}
}

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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.gecko.rest.jersey.helper.DispatcherHelper;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider.JaxRsExtension;
import org.glassfish.jersey.InjectionManagerProvider;

/**
 * A {@link Feature} implementation registering all extensions as singleton and according to there provided contracts. 
 * @author Juergen Albert
 * @since 16.01.2018
 */
public class WhiteboardFeature implements Feature{

	public static Comparator<Map.Entry<String, JaxRsExtensionProvider>> PROVIDER_COMPARATOR = (e1, e2)-> 
		DispatcherHelper.PROVIDER_COMPARATOR.compare(e1.getValue(), e2.getValue());
		//			if (p1.getServiceRank() == p2.getServiceRank()) {
		//				return p1.getServiceId().compareTo(p2.getServiceId());
		//			}
		//			return p2.getServiceRank().compareTo(p1.getServiceRank());

	Map<String, JaxRsExtensionProvider> extensions;

	Map<JaxRsExtensionProvider, JaxRsExtension> extensionInstanceTrackingMap = new HashMap<>();


	public WhiteboardFeature(Map<String, JaxRsExtensionProvider> extensions) {
		this.extensions = extensions.entrySet().stream().sorted(PROVIDER_COMPARATOR).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue)->oldValue, LinkedHashMap::new));
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.core.Feature#configure(javax.ws.rs.core.FeatureContext)
	 */
	@Override
	public boolean configure(FeatureContext context) {
		AtomicInteger priority = new AtomicInteger(Priorities.USER + 1000);
		extensions.forEach((k, extension) -> {

			JaxRsExtension je = extension.getExtension(InjectionManagerProvider.getInjectionManager(context));

			extensionInstanceTrackingMap.put(extension, je);
			Map<Class<?>,Integer> contractPriorities = je.getContractPriorities();
			if (contractPriorities.isEmpty()) {
				context.register(je.getExtensionObject(), priority.getAndIncrement());
			} else {
				context.register(je.getExtensionObject(), je.getContractPriorities());
			}
		});
		return true;
	}

	public void dispose() {
		extensionInstanceTrackingMap.forEach((k,v) -> {
			try {
				v.dispose();
			} catch (IllegalArgumentException e) {
				// we can ignore this. Will be thrown by felix if it 
			}
		});
		extensionInstanceTrackingMap.clear();
		extensions.clear();
	}

	public void dispose(JaxRsExtensionProvider extProvider) {
		JaxRsExtension je = extensionInstanceTrackingMap.remove(extProvider);

		if(je != null) {
			je.dispose();
		}
	}
}

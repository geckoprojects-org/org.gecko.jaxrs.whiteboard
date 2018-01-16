package org.eclipselabs.jaxrs.jersey.runtime.application.feature;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;

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
			Object serviceObject = extension.getServiceObjects().getService();
			System.out.println("Configuring feature for " + extension.getName());
			if(extension.getContracts() != null) {
				context.register(serviceObject, extension.getContracts());
			}
			context.register(serviceObject);
		});
		return true;
	}
}

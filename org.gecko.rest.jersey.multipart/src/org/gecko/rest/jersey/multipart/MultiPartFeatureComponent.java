package org.gecko.rest.jersey.multipart;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsWhiteboardTarget;

@JaxrsExtension
@JaxrsName("MultiPartFeatureExtension")
@Component(name = "MultiPartFeatureComponent", property = {"multipart=true"})
@JaxrsApplicationSelect("(!(disableMultipart=true))")
@JaxrsWhiteboardTarget("(!(disableMultipart=true))")
public class MultiPartFeatureComponent implements Feature{

	@Override
	public boolean configure(FeatureContext context) {
		context.register(MultiPartFeature.class);
		System.out.println("Registering MultiPartFeature!");
		return true;
	}
}

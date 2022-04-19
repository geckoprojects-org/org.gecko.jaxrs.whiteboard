package org.gecko.rest.jersey.multipart;

import java.util.logging.Logger;

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
	
	private Logger logger = Logger.getLogger(MultiPartFeatureComponent.class.getName()); 

	@Override
	public boolean configure(FeatureContext context) {
		context.register(MultiPartFeature.class);
		logger.fine("Registering MultiPartFeature!");
		return true;
	}
}

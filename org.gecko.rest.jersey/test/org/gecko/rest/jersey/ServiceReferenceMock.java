/**
 * 
 */
package org.gecko.rest.jersey;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * @author jalbert
 *
 */
public class ServiceReferenceMock<T> implements ServiceReference<T> {

	private Map<Object, Object> props;

	public ServiceReferenceMock(T service, Map<Object, Object> props) {
		this.props = props;
	}
	
	@Override
	public Object getProperty(String key) {
		return props.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		return props.keySet().toArray(new String[0]);
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}

	@Override
	public int compareTo(Object reference) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.framework.ServiceReference#getProperties()
	 */
	@Override
	public Dictionary<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

}

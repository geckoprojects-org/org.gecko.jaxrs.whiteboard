# Gecko.io JaxRs Whiteboard Implementation

This is a OSGi JaxRs Whiteboard Implementation based on Eclipse Jersey.

https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html

https://eclipse-ee4j.github.io/jersey/

This implementation is currently not compliance tested, but we work on it to get it compliant.

We use Jersey 2.32 together with HK2 2.6.1. To use it you will need the following bundles:

* **org.gecko.rest.jersey** - The whiteboard implementation
* **org.gecko.rest.jersey.sse** - A fragement for the use of server sent events
* **org.gecko.rest.jersey.bom** - A bill of material index

You will find the Release and Snapshot artifacts at Maven Central, respectively at https://oss.sonatype.org.

```
<dependency>
  <groupId>org.geckoprojects.jaxrs</groupId>
  <artifactId>org.gecko.rest.jersey.bom</artifactId>
  <version>${version}</version>
  <type>pom</type>
</dependency>
<dependency>
  <groupId>org.geckoprojects.jaxrs</groupId>
  <artifactId>org.gecko.rest.jersey</artifactId>
  <version>${version}</version>
</dependency>
<dependency>
  <groupId>org.geckoprojects.jaxrs</groupId>
  <artifactId>org.gecko.rest.jersey.sse</artifactId>
  <version>${version}</version>
</dependency>
```

## Setup

The default whiteboard uses the Felix Jetty bundle. It starts the server with the default application under 

http://localhost:8181/rest

You can change these setting by using the OSGi Configurator or the Configuration Admin like this:

```json
{
  ":configurator:resource-version": 1,
  
	"JaxRsWhiteboardComponent": 
  	{
	    "jersey.port": 8080,
		"jersey.jaxrs.whiteboard.name" : "demo",
		"jersey.context.path" : "demo" 
	}
}
```

This would run the server at

http://localhost:8080/demo

Please note, that the Felix Jetty implementation runs the OSGi HTTP Servifce by default at port 8080. It may come to an conflict, with the port in this JaxRs server configuration. Therefore you may use the system property `org.osgi.service.http.port=0` to deactivate the HTTP Service.

## Example 

When using the JaxRs Whiteboard, you register you REST resources and extensions as a service. There are some useful Meta-Annotations, that create component properties for you.
```java
@RequireJaxrsWhiteboard
@JaxrsResource
@JaxrsName("demo")
@Component(service = DemoResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/")
public class DemoResource {

	@GET
	@Path("/hello")
	public String hello() {
		return "hello World";
	}

}
```

The the **RequireJaxrsWhiteboard** annotation, you define the dependency to the implementation. In our case Jersey. You need the Whiteboard dependencies in you workspace. Everything you need is defined in the BOM. 

When using bndtools you can simply define a Maven-BOM repository in your workspace:

```
-plugin.jerseyrest:\
	aQute.bnd.repository.maven.pom.provider.BndPomRepository;\
        name="Gecko REST";\
        releaseUrls=https://repo.maven.apache.org/maven2/;\
        snapshotUrls=https://oss.sonatype.org/content/repositories/snapshots/;\
        revision="org.geckoprojects.jaxrs:org.gecko.rest.jersey.index:${version}"
```

To resolve the JaxRs and Whiteboard annotation dependencies from the resource above you just need the following API bundles on the projects build path:

* **javax.ws.rs-api**
* **osgi.cmpn**
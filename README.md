[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rivieracode/RivieraRealm.Lib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rivieracode/RivieraRealm.Lib) 

# RiveraRealm.Lib ( [JAVADOC](https://rivieracode.github.io/RivieraRealm.Lib) )

RiveraRealm.Lib targets Glassfish application server to extend easily Java Authentication and Authorization Service (JAAS).

This API delegates these tasks to a local EJB bean deployed in the Glassfish instance (WAR, EAR) allowing:

* full usage of the container features (JPA, CDI, ...)
* handling custom login, remember me, multi-tenant, ...
* hot deployment (no need to restart the server instance)
* while keeping the benefits of container managed security 

# Glassfish Setup

* Add this project JAR to the domain "lib" folder
* Complete the "login.conf" file in the "config" directory with :

```
RiveraCodeRealm {
    com.rivieracode.realm.LoginModule required;
 };
```

* Create a realm in Configuration > server-config > Security > realms :
	- give any name to the realm (ex : MyRealm) - set the custom class name as "com.rivieracode.realm.Realm"
	- add the additional property LOCAL_BEAN_JNDI with the JNDI name space of the bean implementing the RealmDelegate interface  (ex: java:global/MyDelegatedApplicationName/MyLocalBean )

# Delegated Application Setup

The application managing the authentication and authorization must add the JAR as a dependency and provide an EJB implementing the RealmDelegate interface.

The LOCAL_BEAN_JNDI property in the realm configuration must be set to the JNDI name space of this EJB.

# Client Application Setup

Web.xml just needs to reference the realm and use appropriate groups. The Delegated Application can be merged with the Client Application.

See the RivieraRealm.Ref project for a reference implementation.


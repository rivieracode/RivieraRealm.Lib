/**
 * RiveraRealm.Lib targets Glassfish application server to extend easily 
 * Java Authentication and Authorization Service (JAAS).
 * 
 * <p>
 * This API delegates these tasks to a local EJB bean deployed in the
 * Glassfish instance (WAR, EAR) allowing:
 * <ul>
 * <li>full usage of the container features (JPA, CDI, ...)</li>
 * <li>handling custom login, remember me, multi-tenant, ...</li>
 * <li>hot deployment (no need to restart the server instance)</li>
 * <li>while keeping the benefits of container managed security</li>
 * </ul>
 * 
 * <h2>Glassfish Setup</h2>
 * <ul>
 * <li>Add this project JAR to the domain "lib" folder</li>
 * <li>Complete the "login.conf" file in the "config" directory with :
 * <pre>{@code RiveraCodeRealm {
 *    com.rivieracode.realm.LoginModule required;
 * };}</pre></li>
 * <li>Create a realm in Configuration &gt; server-config &gt; Security &gt; realms :<br>
 * - give any name to the realm (ex : MyRealm)
 * - set the custom class name as "com.rivieracode.realm.Realm"<br>
 * - add the additional property LOCAL_BEAN_JNDI with the JNDI name space of the bean implementing the RealmDelegate interface
 *  <br> (ex: java:global/MyDelegatedApplicationName/MyLocalBean  )</li>
 * </ul>
 * 
 * <h2>Delegated Application Setup</h2>
 * <p>
 * The application managing the authentication and authorization must add the JAR as a dependency and provide
 * an EJB implementing the RealmDelegate interface.
 * <p>
 * The LOCAL_BEAN_JNDI property in the realm configuration must be set to the JNDI name space of this EJB.
 * 
 * <h2>Client Application Setup</h2>
 * <p>
 * Web.xml just needs to reference the realm and use appropriate groups.
 * The Delegated Application can be merged with the Client Application.</p>
 * 
 * <p>See the RivieraRealm.Ref project for a reference implementation.</p>
 * 
 * @author Jean-Michel Tanguy
 * 
 */
package com.rivieracode.realm;

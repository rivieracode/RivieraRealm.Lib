/*
 * Copyright 2015 Jean-Michel Tanguy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rivieracode.realm;

import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import java.security.Principal;
import java.util.Enumeration;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

/**
 * Configures the application server to delegate authentication and authorization to the local EJB.
 * <p>
 * Realm and LoginModule classes are linked in the container configuration
 * (login.conf for Glassfish) :
 * <pre>{@code RivieraRealmContext {
 *   com.rivieracode.realm.LoginModule required;<
 * };}</pre>
 * <p>
 * Custom realm is configured in Glassfish in Configurations : Server-Config :
 * Security : Realms : "MyRealm"
 * <p>
 * Class name must implement as class com.rivieracode.realm.Realm and the
 * property LOCAL_BEAN_JNDI with the JNDI path of the local bean delegated for
 * the authentication.
 *
 * @author Jean-Michel Tanguy
 */
public class Realm extends AppservRealm {

    /**
     * JAAS_CONTEXT : property used in the login.conf to link the LoginModule
     * and the realm (default : RivieraRealmContext)
     */
    public static final String JAAS_CONTEXT_PROPERTY = "JAAS_CONTEXT";
    /**
     * REALM_NAME : name and description of the type of authentication supported
     * (default : RiveraRealm)
     */
    public static final String REALM_NAME_PROPERTY = "REALM_NAME";
    /**
     * LOCAL_BEAN_JNDI : name space of the delegated local bean implementing
     * RealmDelegate (default : java:global/RiveraRealmDB/DelegatedBean)
     */
    public static final String LOCAL_BEAN_JNDI_PROPERTY = "LOCAL_BEAN_JNDI";

    /**
     * RivieraRealm
     */
    private static final String JAAS_CONTEXT_DEFAULT = "RivieraRealmContext";
    /**
     * RivieraRealm
     */
    private static final String REALM_NAME_DEFAULT = "RivieraRealm";
    /**
     * java:global/RivieraRealmRef/DelegatedBean (update required)
     */
    private static final String LOCAL_BEAN_JNDI_DEFAULT = "java:global/RivieraRealmRef/DelegatedBean";

    /**
     * local EJB matching the JNDI
     */
    private RealmDelegate localBean;

    /**
     * Additional properties provided by the realm configuration in Glassfish.
     * They overrides default property values.
     */
    private Properties properties;

    /**
     * Reserved. Called by the container.
     *
     * @param properties Additional properties from the realm configuration in
     * Glassfish overriding default property values.
     * @throws BadRealmException if error in the realm
     * @throws NoSuchRealmException if error in the realm name
     */
    @Override
    public void init(Properties properties) throws BadRealmException, NoSuchRealmException {
        super.init(properties);
        this.properties = properties;
    }

    /**
     * JAAS Context used to link the LoginModule and the Realm in the container
     * configuration. Set by the JAAS_CONTEXT_PROPERTY and the declaration in
     * the login.conf file in Glassfish configuration directory:
     * <p>
     * {@code RivieraRealmContext {
     *   com.rivieracode.realm.LoginModule required;<
     * };}
     *
     * @return JAAS Context name
     */
    @Override
    public synchronized String getJAASContext() {
        return properties.getProperty(JAAS_CONTEXT_PROPERTY, JAAS_CONTEXT_DEFAULT);
    }

    /**
     * Name and description of the realm. Set by the REALM_NAME_PROPERTY
     *
     * @return name of the realm
     */
    @Override
    public String getAuthType() {
        return properties.getProperty(REALM_NAME_PROPERTY, REALM_NAME_DEFAULT);
    }

    /**
     * Delegates authentication to local bean defined by the additional property LOCAL_BEAN_JNDI.
     *
     * @param username Provided username to the container
     * @param password Provided password to the container
     * @return identification to add to principal
     * @throws LoginException if any error 
     */
    public Principal athenticate(String username, String password) throws LoginException {
        try {
            return getLocalBean().authenticate(properties, username, password);
        } catch (javax.ejb.EJBException ex) {
            // allowing local bean hot deploy by capturing JNDI exception
            // forcing new JNDI lookup
            localBean = null;
            // new request
            return getLocalBean().authenticate(properties, username, password);
        }
    }

    /**
     * Gets the authorization by accessing local bean defined by the additional property LOCAL_BEAN_JNDI.
     *
     * @param username User name of the authenticated user.
     * @return List of groups authorized for the authenticated user.
     * @throws InvalidOperationException if any error occurs
     * @throws NoSuchUserException if the user is not existing.
     */
    @Override
    public Enumeration getGroupNames(String username) throws InvalidOperationException, NoSuchUserException {
        try {
            return getLocalBean().getGroupNames(properties, username);
        } catch (javax.ejb.EJBException ex) {
            // Allowing local bean hot deploy by capturing JNDI exception and forcing new JNDI lookup
            localBean = null;
            // New request
            try {
                return getLocalBean().getGroupNames(properties, username);
            } catch (NoSuchUserException | InvalidOperationException exception) {
                throw exception;
            } catch (Exception other) {
                throw new InvalidOperationException("");
            }
        } catch (NoSuchUserException | InvalidOperationException exception) {
            throw exception;
        } catch (Exception other) {
            throw new InvalidOperationException("");
        }
    }

    /**
     * Retrieves local bean as set by property LOCAL_BEAN_JNDI
     *
     * @return Local bean to be used for authentication and authorization
     * @throws LoginException if JNDI path is not correct
     */
    protected RealmDelegate getLocalBean() throws LoginException {
        //  RealmDelegate localBean = null;
        if (localBean == null) {
            String jndi = properties.getProperty(LOCAL_BEAN_JNDI_PROPERTY, LOCAL_BEAN_JNDI_DEFAULT);
            try {
                Context c = new InitialContext();
                localBean = (RealmDelegate) c.lookup(jndi);
            } catch (NamingException ex) {
                Logger.getLogger(Realm.class.getName()).log(Level.SEVERE, null, ex);
                throw new LoginException();
            }
        }
        return localBean;
    }

}

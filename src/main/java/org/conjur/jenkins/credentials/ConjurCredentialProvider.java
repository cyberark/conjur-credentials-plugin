package org.conjur.jenkins.credentials;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.function.Supplier;


import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import org.acegisecurity.Authentication;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentialsImpl;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import jenkins.model.Jenkins;

@Extension
public class ConjurCredentialProvider extends CredentialsProvider {

    private static final Logger LOGGER = Logger.getLogger(ConjurCredentialProvider.class.getName());

    private static final ConcurrentHashMap<String, ConjurCredentialStore> allStores = new ConcurrentHashMap<String, ConjurCredentialStore>();
    private static final ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>> allCredentialSuppliers = new ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>> ();

    // private final Supplier<Collection<StandardCredentials>> credentialsSupplier =
    //         memoizeWithExpiration(CredentialsSupplier.standard(), () ->
    //                 PluginConfiguration.normalize(PluginConfiguration.getInstance().getCache()));

    @Override
    @Nonnull
    public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type,
                                                          ItemGroup itemGroup,
                                                          Authentication authentication) {

        if (type.isAssignableFrom(ConjurSecretCredentials.class) || type.isAssignableFrom(ConjurSecretUsernameSSHKeyCredentials.class)) {
            if (ACL.SYSTEM.equals(authentication)) {
                Collection<StandardCredentials> allCredentials = Collections.emptyList();
                LOGGER.log(Level.INFO, "getCredentials ConjurCredentialProvider: " + this.getId() + " : " + this);
                LOGGER.log(Level.INFO, "Getting Credentials from ConjurCredentialProvider @ " + itemGroup.getFullName() + " : " + itemGroup.getUrl());
    
                String key = String.valueOf(this.hashCode());
                if (allCredentialSuppliers.containsKey(key)) {
                    LOGGER.log(Level.INFO, "To Fetch credentials");
                    Supplier<Collection<StandardCredentials>> credentialSupplier = allCredentialSuppliers.get(key);
                    allCredentials = credentialSupplier.get();
                }
    
                // try {
                    // allCredentials = credentialsSupplier.get();
                // } catch (SdkBaseException e) {
                //     LOG.log(Level.WARNING, "Could not list credentials in Secrets Manager: message=[{0}]", e.getMessage());
                // }
    
                return allCredentials.stream()
                        .filter(c -> type.isAssignableFrom(c.getClass()))
                        // cast to keep generics happy even though we are assignable
                        .map(type::cast)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    @Override
    public CredentialsStore getStore(ModelObject object) {

        if (object == Jenkins.get()) {
            return null;
        }
        
        ConjurCredentialStore store = null;
        Supplier<Collection<StandardCredentials>> supplier = null;
        
        if (object != null) {
            String key = String.valueOf(object.hashCode());
            if (allStores.containsKey(key)) {
                LOGGER.log(Level.INFO, "GetStore EXISTING ConjurCredentialProvider : " + object.getClass().getName() + ": " + object.toString() + " => " + object.hashCode());
                store = allStores.get(key);
            } else {
                LOGGER.log(Level.INFO, "GetStore NEW ConjurCredentialProvider : " + object.getClass().getName() + ": " + object.toString() + " => " + object.hashCode());
                store = new ConjurCredentialStore(this, object);
                supplier = CredentialsSupplier.standard(object);
                allStores.put(key, store);
                allCredentialSuppliers.put(String.valueOf(this.hashCode()), supplier);
            }    
        }

        return store;
    }

    @Override
    public String getIconClassName() {
        return "icon-aws-secrets-manager-credentials-store";
    }

    // private static <T> Supplier<T> memoizeWithExpiration(Supplier<T> base, Supplier<Duration> duration) {
    //     return CustomSuppliers.memoizeWithExpiration(base, duration);
    // }
}

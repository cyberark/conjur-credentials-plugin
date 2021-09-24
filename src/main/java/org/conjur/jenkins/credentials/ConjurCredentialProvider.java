package org.conjur.jenkins.credentials;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.acegisecurity.Authentication;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentials;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

@Extension
public class ConjurCredentialProvider extends CredentialsProvider {

    private static final Logger LOGGER = Logger.getLogger(ConjurCredentialProvider.class.getName());

    private static final ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>> allCredentialSuppliers = new ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>> ();

    private Supplier<Collection<StandardCredentials>> currentCredentialSupplier;

    @Override
    @Nonnull
    public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type,
                                                          @Nonnull Item item,
                                                          @Nonnull Authentication authentication,
                                                          @Nonnull List<DomainRequirement> domainRequirements) {
        return getCredentialsFromSupplier(type, item, authentication);

    }

    @Override
    @Nonnull
    public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type,
                                                          ItemGroup itemGroup,
                                                          Authentication authentication) {
        return getCredentialsFromSupplier(type, itemGroup, authentication);
    }

    private <C extends Credentials> List<C> getCredentialsFromSupplier(@Nonnull Class<C> type,
                                                                        ModelObject context,
                                                                        Authentication authentication) {
        if (type.isAssignableFrom(ConjurSecretCredentials.class) || type.isAssignableFrom(ConjurSecretUsernameSSHKeyCredentials.class)) {
            if (ACL.SYSTEM.equals(authentication)) {
                Collection<StandardCredentials> allCredentials = Collections.emptyList();
                LOGGER.log(Level.FINE, "**** getCredentials ConjurCredentialProvider: " + this.getId() + " : " + this );
                LOGGER.log(Level.FINE, "Getting Credentials from ConjurCredentialProvider @ " + context.getClass().getName());
                LOGGER.log(Level.FINE, "To Fetch credentials");
                allCredentials = currentCredentialSupplier.get();
                getStore(context);
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
    public ConjurCredentialStore getStore(ModelObject object) {

        GlobalConjurConfiguration globalConfig = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
        LOGGER.log(Level.FINE, "**** GlobalConjurConfiguration ==> " + globalConfig);
        if (globalConfig == null || !globalConfig.getEnableJWKS() || !globalConfig.getEnableContextAwareCredentialStore()) {
            LOGGER.log(Level.FINE, "No Conjur Credential Store (Content Aware)");
            return null;
        }

        if (object == Jenkins.get()) {
            return null;
        }
        
        ConjurCredentialStore store = null;
        Supplier<Collection<StandardCredentials>> supplier = null;
        
        if (object != null) {
            String key = String.valueOf(object.hashCode());            
            if (ConjurCredentialStore.getAllStores().containsKey(key)) {
                LOGGER.log(Level.FINEST, "GetStore EXISTING ConjurCredentialProvider : " + object.getClass().getName() + ": " + object.toString() + " => " + object.hashCode());
                store = ConjurCredentialStore.getAllStores().get(key);
            } else {
                LOGGER.log(Level.FINEST, "GetStore NEW ConjurCredentialProvider : " + object.getClass().getName() + ": " + object.toString() + " => " + object.hashCode());
                store = new ConjurCredentialStore(this, object);
                supplier = memoizeWithExpiration(CredentialsSupplier.standard(object), Duration.ofSeconds(120));
                ConjurCredentialStore.getAllStores().put(key, store);
                allCredentialSuppliers.put(key, supplier);
            }    
            currentCredentialSupplier = allCredentialSuppliers.get(key);
        }

        return store;
    }

    @Override
    public String getIconClassName() {
        return "icon-conjur-credentials-store";
    }

    private static <T> Supplier<T> memoizeWithExpiration(Supplier<T> base, Duration duration) {
        return CustomSuppliers.memoizeWithExpiration(base, duration);
    }

}

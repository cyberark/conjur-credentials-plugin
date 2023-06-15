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
import javax.annotation.Nullable;

import org.acegisecurity.Authentication;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

/**
 * Provides the ConjurCredentails extends CredentialProvider *
 */
@Extension
public class ConjurCredentialProvider extends CredentialsProvider {

	private static final Logger LOGGER = Logger.getLogger(ConjurCredentialProvider.class.getName());

	private static final ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>> allCredentialSuppliers = new ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>>();

	private Supplier<Collection<StandardCredentials>> currentCredentialSupplier;

	/**
	 * returns the Credentials as List based on the type,itemGroup and
	 * authentication
	 */

	public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type, @Nullable ItemGroup itemGroup,
			@Nullable Authentication authentication, @Nonnull List<DomainRequirement> domainRequirements) {
		LOGGER.log(Level.FINE, "getCredentials (1)  type >>> :{0} ,{1}", new Object[] { type, itemGroup });
		return getCredentials(type, itemGroup, authentication);
	}

	/**
	 * returns the credentials from the supplier for the item,type and
	 * authentication
	 */
	@Override
	@Nonnull
	public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type, @Nonnull Item item,
			@Nonnull Authentication authentication, @Nonnull List<DomainRequirement> domainRequirements) {
		LOGGER.log(Level.FINE, "getCredentials (2) type >> {0},{1} ", new Object[] { type, item });
		return getCredentialsFromSupplier(type, item, authentication);

	}

	/**
	 * returns the Credentials as List based on the type,itemGroup and
	 * authentication
	 */
	@Override
	@Nonnull
	public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type, ItemGroup itemGroup,
			Authentication authentication) {
		LOGGER.log(Level.FINE, "getCredentials (3) type >> {0},{1}: ", new Object[] { type, itemGroup });
		return getCredentialsFromSupplier(type, itemGroup, authentication);
	}

	private <C extends Credentials> List<C> getCredentialsFromSupplier(@Nonnull Class<C> type, ModelObject context,
			Authentication authentication) {

		LOGGER.log(Level.FINE, "Type: " + type.getName() + " authentication: " + authentication + " context: "
				+ context.getDisplayName());

		if (!type.isInstance(CertificateCredentials.class)
				&& ((type.isInstance(ConjurSecretCredentials.class) || type == ConjurSecretUsernameCredentials.class)
						|| type.isAssignableFrom(ConjurSecretCredentials.class)
						|| type.isAssignableFrom(ConjurSecretUsernameSSHKeyCredentials.class))) {

			LOGGER.log(Level.FINE, "*****");
			if (ACL.SYSTEM.equals(authentication)) {
				Collection<StandardCredentials> allCredentials = Collections.emptyList();

				LOGGER.log(Level.FINE,
						"Getting Credentials from ConjurCredentialProvider @ " + context.getClass().getName());

				getStore(context);

				if (currentCredentialSupplier != null) {
					LOGGER.log(Level.FINE, "Iniside current credentialsupplier>>>>" + currentCredentialSupplier);
					allCredentials = currentCredentialSupplier.get();
					LOGGER.log(Level.FINE, "" + "All credentials List" + allCredentials.toString());

					return allCredentials.stream().filter(c -> type.isAssignableFrom(c.getClass()))
							// cast to keep generics happy even though we are assignable

							.map(type::cast).collect(Collectors.toList());
				}

			} else {
				LOGGER.log(Level.FINE,
						"Getting Credentials from ConjurCredentialProvider @ else part" + context.getClass().getName());
				Collection<StandardCredentials> allCredentials = Collections.emptyList();

				getStore((ModelObject) ((Run) context).getParent());

				if (currentCredentialSupplier != null) {
					LOGGER.log(Level.FINE, "Iniside current credentialsupplier");
					allCredentials = currentCredentialSupplier.get();
					LOGGER.log(Level.FINE, "Iniside current credentialsupplier" + allCredentials.toString());

					return allCredentials.stream().filter(c -> type.isAssignableFrom(c.getClass()))
							// cast to keep generics happy even though we are assignable
							.map(type::cast).collect(Collectors.toList());
				}
			}

		}

		return Collections.emptyList();
	}

	/**
	 * @return the ConjurCredentailStore based on the ModelObject
	 */
	@Override
	public ConjurCredentialStore getStore(ModelObject object) {

		GlobalConjurConfiguration globalConfig = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
		ConjurCredentialStore store = null;
		ConjurCredentialStore parentStore = null;
		Supplier<Collection<StandardCredentials>> supplier = null;

		if (globalConfig == null || !globalConfig.getEnableJWKS()
				|| !globalConfig.getEnableContextAwareCredentialStore()) {
			LOGGER.log(Level.FINE, "No Conjur Credential Store (Content Aware)");
			return null;
		}

		if (object == Jenkins.get()) {
			LOGGER.log(Level.FINE, "jenkins get object" + object.getDisplayName() + Jenkins.get().getDescription());
			return null;

		}

		if (object != null) {

			String key = String.valueOf(object.hashCode());
			LOGGER.log(Level.FINE, "Object Key not null" + object.getDisplayName() + "Key" + key);

			try {
				if (ConjurCredentialStore.getAllStores().containsKey(key)) {
					LOGGER.log(Level.FINEST, "GetStore EXISTING ConjurCredentialProvider : "
							+ object.getClass().getName() + ": " + object.toString() + " => " + object.hashCode());
					store = ConjurCredentialStore.getAllStores().get(key);

					LOGGER.log(Level.FINEST, "All Store detaials" + store);

				} else {

					store = new ConjurCredentialStore(this, object);
					supplier = memoizeWithExpiration(CredentialsSupplier.standard(object), Duration.ofSeconds(120));
					ConjurCredentialStore.getAllStores().put(key, store);
					allCredentialSuppliers.put(key, supplier);
				}
				LOGGER.log(Level.FINE, "currentCredentialSupplier" + key);

				currentCredentialSupplier = allCredentialSuppliers.get(key);

			} catch (Exception ex) {
				LOGGER.log(Level.FINE, ex.getMessage());
			}
		}

		return store;
	}

	/**
	 * 
	 * @return Map containing all credential suppliers
	 */

	public static ConcurrentHashMap<String, Supplier<Collection<StandardCredentials>>> getAllCredentialSuppliers() {
		return allCredentialSuppliers;
	}

	/**
	 * @return iconClassName
	 */
	@Override
	public String getIconClassName() {
		return "icon-conjur-credentials-store";
	}

	/**
	 * check for the expiration for Supplier based on duration to refresh
	 * 
	 * @param <T>
	 * @param base
	 * @param duration
	 * @return
	 */
	public static <T> Supplier<T> memoizeWithExpiration(Supplier<T> base, Duration duration) {
		return CustomSuppliers.memoizeWithExpiration(base, duration);
	}

}

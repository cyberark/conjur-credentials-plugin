package org.conjur.jenkins.conjursecrets;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;

import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;

@NameWith(value = ConjurSecretCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretCredentials extends StandardCredentials {

	static Logger getLogger() {
		return Logger.getLogger(ConjurSecretCredentials.class.getName());
	}

	class NameProvider extends CredentialsNameProvider<ConjurSecretCredentials> {

		@Override
		public String getName(ConjurSecretCredentials c) {
			return c.getDisplayName() + c.getNameTag() + " (" + c.getDescription() + ")";
		}

	}

	String getDisplayName();

	String getNameTag();

	Secret getSecret();

	default Secret secretWithConjurConfigAndContext(ConjurConfiguration conjurConfiguration, ModelObject context) {
		setConjurConfiguration(conjurConfiguration);
		setContext(context);
		return getSecret();
	}

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	void setStoreContext(ModelObject storeContext);

	void setContext(ModelObject context);

	static ConjurSecretCredentials credentialFromContextIfNeeded(ConjurSecretCredentials credential, String credentialID, ModelObject context) {
		if (credential == null && context != null) {
			getLogger().log(Level.FINE, "NOT FOUND at Jenkins Instance Level!");
			Item folder = null;
			if (context instanceof Run) {
				folder = Jenkins.get().getItemByFullName(((Run<?, ?>)context).getParent().getParent().getFullName());
			} else {
				folder = Jenkins.get().getItemByFullName(((AbstractItem)((AbstractItem)context).getParent()).getParent().getFullName());
			}
			return CredentialsMatchers
					.firstOrNull(
							CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
									Collections.<DomainRequirement>emptyList()),
							CredentialsMatchers.withId(credentialID));
		}
		return credential;
	}

	static ConjurSecretCredentials credentialWithID(String credentialID, ModelObject context) {

		getLogger().log(Level.FINE, "* CredentialID: {0}", credentialID);

		ConjurSecretCredentials credential = null;

		credential = CredentialsMatchers
				.firstOrNull(
						CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.get(),
								ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
						CredentialsMatchers.withId(credentialID));

		credential = credentialFromContextIfNeeded(credential, credentialID, context);


		if (credential == null) {
			String contextLevel = String.format("Unable to find credential at %s", 
												(context != null? context.getDisplayName() : "Global Instance Level"));
			throw new InvalidConjurSecretException(contextLevel);
		}

		return credential;
	}

	static void setConjurConfigurationForCredentialWithID(String credentialID, ConjurConfiguration conjurConfiguration, ModelObject context) {

		ConjurSecretCredentials credential = credentialWithID(credentialID, context);

		if (credential != null)
			credential.setConjurConfiguration(conjurConfiguration);

	}
	
	static Secret getSecretFromCredentialIDWithConfigAndContext(String credentialID, 
																ConjurConfiguration conjurConfiguration,
																ModelObject context,
																ModelObject storeContext) {

		ModelObject effectiveContext = context != null? context : storeContext;

		getLogger().log(Level.FINE, "Getting Secret with CredentialID: {0}  context: " + context + " storeContext: " + storeContext, credentialID);
		ConjurSecretCredentials credential = credentialWithID(credentialID, effectiveContext);
		
		return credential.secretWithConjurConfigAndContext(conjurConfiguration, effectiveContext);
	}

}

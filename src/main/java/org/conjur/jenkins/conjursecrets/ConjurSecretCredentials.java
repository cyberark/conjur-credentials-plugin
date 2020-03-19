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

import hudson.model.Item;
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

	default Secret secretWithConjurConfigAndContext(ConjurConfiguration conjurConfiguration, Run<?, ?> context) {
		setConjurConfiguration(conjurConfiguration);
		setContext(context);
		return getSecret();
	}

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	void setContext(Run<?, ?> context);

	static ConjurSecretCredentials credentialWithID(String credentialID, Run<?, ?> context) {

		getLogger().log(Level.INFO, "* CredentialID: {0}", credentialID);

		ConjurSecretCredentials credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(credentialID));

		if(credential == null && context != null) {
            getLogger().log(Level.INFO, "NOT FOUND at Jenkins Instance Level!");
            Item folder = Jenkins.getInstance().getItemByFullName(context.getParent().getParent().getFullName());
			credential = CredentialsMatchers.firstOrNull(
					CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
							Collections.<DomainRequirement>emptyList()),
					CredentialsMatchers.withId(credentialID));
		}
		return credential;
	}

	static void setConjurConfigurationForCredentialWithID(String credentialID, ConjurConfiguration conjurConfiguration, Run<?, ?> context) {

		ConjurSecretCredentials credential = credentialWithID(credentialID, context);

		if (credential != null)
			credential.setConjurConfiguration(conjurConfiguration);

	}
	
	static Secret getSecretFromCredentialIDWithConfigAndContext(String credentialID, 
																ConjurConfiguration conjurConfiguration,
																Run<?, ?> context) {

		ConjurSecretCredentials credential = credentialWithID(credentialID, context);
		
		if (credential == null) return null;

		return credential.secretWithConjurConfigAndContext(conjurConfiguration, context);
	}

}

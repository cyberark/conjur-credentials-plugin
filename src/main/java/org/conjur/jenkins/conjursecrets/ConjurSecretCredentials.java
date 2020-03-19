package org.conjur.jenkins.conjursecrets;

import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.util.ListBoxModel;


@NameWith(value = ConjurSecretCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretCredentials extends StandardCredentials {

	static Logger getLogger() {
		return Logger.getLogger(ConjurSecretCredentials.class.getName());
	}

	class NameProvider extends CredentialsNameProvider<ConjurSecretCredentials> {

		@Override
		public String getName(ConjurSecretCredentials conjurSecretCredential) {
			String description = conjurSecretCredential.getDescription();
			return conjurSecretCredential.getDisplayName() + "/*Conjur*" + " (" + description + ")";
		}

	}

	String getDisplayName();

	Secret getSecret();

	default Secret secretWithConjurConfigAndContext(ConjurConfiguration conjurConfiguration, Run<?, ?> context) {
		setConjurConfiguration(conjurConfiguration);
		setContext(context);
		return getSecret();
	}

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	void setContext(Run<?, ?> context);

	static Secret getSecretFromIDWithConfigAndContext(String credentialID, 
													  ConjurConfiguration conjurConfiguration,
													  Run<?, ?> context) {

		getLogger().log(Level.INFO, "* CredentialID: {0}", credentialID);
		
		ConjurSecretCredentials credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(credentialID));
		
		if(credential == null) {
			getLogger().log(Level.INFO, "NOT FOUND at Jenkins Instance Level!");
			if (context == null) {
				throw new InvalidConjurSecretException("Unable to find credential at Global Instance Level and no current context to determine folder provided");
			}
			Item folder = Jenkins.getInstance().getItemByFullName(context.getParent().getParent().getFullName());
			credential = CredentialsMatchers.firstOrNull(
					CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
							Collections.<DomainRequirement>emptyList()),
					CredentialsMatchers.withId(credentialID));
		}
		
		if (credential == null) return null;

		return credential.secretWithConjurConfigAndContext(conjurConfiguration, context);
	}

}

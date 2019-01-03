package org.conjur.jenkins.conjursecrets;

import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import hudson.Extension;
import hudson.model.Run;
import hudson.util.Secret;

@NameWith(value=ConjurSecretCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretCredentials extends StandardCredentials {
	
	String getDisplayName();
	Secret getSecret();
	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);
	void setContext(Run<?, ?> context);
		
	class NameProvider extends CredentialsNameProvider<ConjurSecretCredentials> {

		@Override
		public String getName(ConjurSecretCredentials conjurSecretCredential) {
			String description = conjurSecretCredential.getDescription();
			return conjurSecretCredential.getDisplayName()
					+ "/*Conjur*"
					+ " (" + description + ")";
		}
		
	}
	
    @Extension
    public static class DescriptorImpl extends BindingDescriptor<ConjurSecretCredentials> {
		
        @Override
        protected Class<ConjurSecretCredentials> type() {
            return ConjurSecretCredentials.class;
        }

        @Override
        public String getDisplayName() {
            return "Conjur Secret Credential";
        }

    }	

}

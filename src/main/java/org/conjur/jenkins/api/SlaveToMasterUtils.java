package org.conjur.jenkins.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.security.SlaveToMasterCallable;

public class SlaveToMasterUtils {
    

	static class NewCertificateCredentials extends SlaveToMasterCallable<CertificateCredentials, IOException> {
		/**
		 * Standardize serialization.
		 */
		private static final long serialVersionUID = 1L;

		ConjurConfiguration configuration;
		// Run<?, ?> context;

		public NewCertificateCredentials(ConjurConfiguration configuration) {
			super();
			this.configuration = configuration;
			// this.context = context;
		}

		/**
		 * {@inheritDoc}
		 */
		public CertificateCredentials call() throws IOException {
			CertificateCredentials certificate = CredentialsMatchers.firstOrNull(
					CredentialsProvider.lookupCredentials(CertificateCredentials.class, Jenkins.get(), ACL.SYSTEM,
							Collections.<DomainRequirement>emptyList()),
					CredentialsMatchers.withId(this.configuration.getCertificateCredentialID()));

			return certificate;
		}
	}

	static class NewAvailableCredentials extends SlaveToMasterCallable<List<UsernamePasswordCredentials>, IOException> {
		/**
		 * Standardize serialization.
		 */
		private static final long serialVersionUID = 1L;

		// Run<?, ?> context;

		// public NewAvailableCredentials(Run<?, ?> context) {
		// super();
		// this.context = context;
		// }

		/**
		 * {@inheritDoc}
		 */
		public List<UsernamePasswordCredentials> call() throws IOException {

			List<UsernamePasswordCredentials> availableCredentials = CredentialsProvider.lookupCredentials(
					UsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM,
					Collections.<DomainRequirement>emptyList());

			// if (context != null) {
			// availableCredentials.addAll(CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
			// context.getParent(), ACL.SYSTEM,
			// Collections.<DomainRequirement>emptyList()));
			// }

			return availableCredentials;
		}
	}


}
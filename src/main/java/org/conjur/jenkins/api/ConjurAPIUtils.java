package org.conjur.jenkins.api;

import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentials;

import hudson.remoting.Channel;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.security.SlaveToMasterCallable;
import okhttp3.OkHttpClient;

public class ConjurAPIUtils {
	
	static Logger getLogger() {
		return Logger.getLogger(ConjurAPIUtils.class.getName());
	}

	static CertificateCredentials certificateFromConfiguration(ConjurConfiguration configuration) {
		Channel channel = Channel.current();

		CertificateCredentials certificate = null;

		if (channel == null) {
			if (configuration.getCertificateCredentialID() == null ) { return null;}
			certificate = CredentialsMatchers.firstOrNull(
					CredentialsProvider.lookupCredentials(CertificateCredentials.class, Jenkins.get(), ACL.SYSTEM,
							Collections.<DomainRequirement>emptyList()),
					CredentialsMatchers.withId(configuration.getCertificateCredentialID()));
		} else {
			certificate = (CertificateCredentials) objectFromMaster(channel,
					new ConjurAPIUtils.NewCertificateCredentials(configuration));
		}
		return certificate;
	}
	
	static OkHttpClient httpClientWithCertificate(CertificateCredentials certificate) {
		OkHttpClient client = null;

		try {

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(certificate.getKeyStore(), certificate.getPassword().getPlainText().toCharArray());
			KeyManager[] kms = kmf.getKeyManagers();

			KeyStore trustStore = KeyStore.getInstance("JKS");
			trustStore.load(null, null);
			Enumeration<String> e = certificate.getKeyStore().aliases();
			while (e.hasMoreElements()) {
				String alias = e.nextElement();
				trustStore.setCertificateEntry(alias, certificate.getKeyStore().getCertificate(alias));
			}
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			TrustManager[] tms = tmf.getTrustManagers();

			SSLContext sslContext = null;
			sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(kms, tms, new SecureRandom());

			client = new OkHttpClient.Builder()
					.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tms[0]).build();
		} catch (Exception e) {
			throw new IllegalArgumentException("Error configuring server certificates.", e);
		}

		return client;

	}

	public static OkHttpClient getHttpClient(ConjurConfiguration configuration) {

		CertificateCredentials certificate = certificateFromConfiguration(configuration);

		if (certificate != null) {
			return httpClientWithCertificate(certificate);
		}

		return new OkHttpClient.Builder().build();
	}

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

	public static class NewGlobalConfiguration extends SlaveToMasterCallable<GlobalConjurConfiguration, IOException> {
		/**
		 * Standardize serialization.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * {@inheritDoc}
		 */
		public GlobalConjurConfiguration call() throws IOException {
			GlobalConjurConfiguration result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
			return result;
		}
	}

	public static class NewConjurSecretCredentials extends SlaveToMasterCallable<ConjurSecretCredentials, IOException> {
		/**
		 * Standardize serialization.
		 */
		private static final long serialVersionUID = 1L;

		String credentialID;
		// Run<?, ?> context;

		public NewConjurSecretCredentials(String credentialID) {
			super();
			this.credentialID = credentialID;
			// this.context = context;
		}

		/**
		 * {@inheritDoc}
		 */
		public ConjurSecretCredentials call() throws IOException {
			ConjurSecretCredentials credential = CredentialsMatchers
					.firstOrNull(
							CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.get(),
									ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
							CredentialsMatchers.withId(this.credentialID));

			// if (credential == null && context != null) {
			// 	getLogger().log(Level.INFO, "NOT FOUND at Jenkins Instance Level!");
			// 	Item folder = Jenkins.get().getItemByFullName(context.getParent().getParent().getFullName());
			// 	credential = CredentialsMatchers
			// 			.firstOrNull(
			// 					CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
			// 							Collections.<DomainRequirement>emptyList()),
			// 					CredentialsMatchers.withId(credentialID));
			// }

			return credential;
		}
	}

	public static <T> Object objectFromMaster(Channel channel, SlaveToMasterCallable<T, IOException> callable) {
		// Running from a slave, Get credential entry from master
		try {
			return channel.call(callable);
		} catch (Exception e) {
			getLogger().log(Level.INFO, "Exception getting object from Master", e);
			e.printStackTrace();
		}
		return null;
	}

	public static class NewSecretFromString extends SlaveToMasterCallable<Secret, IOException> {
		/**
		 * Standardize serialization.
		 */
		private static final long serialVersionUID = 1L;

		String secretString;

		public NewSecretFromString(String secretString) {
			super();
			this.secretString = secretString;
		}

		/**
		 * {@inheritDoc}
		 */
		public Secret call() throws IOException {
			return Secret.fromString(secretString);
		}
	}

}
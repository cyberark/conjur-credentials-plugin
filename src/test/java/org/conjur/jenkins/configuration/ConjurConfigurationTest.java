package org.conjur.jenkins.configuration;

import java.io.IOException;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.conjur.jenkins.conjursecrets.ConjurSecretCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.model.GlobalConfiguration;

public class ConjurConfigurationTest {

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Before
	public void setupConjur() {

		CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();

		/*
		// Setup Conjur SSL Certificate
		try {
			byte[] keyStoreBytes = FileUtils.readFileToByteArray(new File("c:\\conjur.p12"));
			byte[] keyStore = Base64.getEncoder().encode(keyStoreBytes);
			CertificateCredentialsImpl credentials = new CertificateCredentialsImpl(CredentialsScope.GLOBAL,
					"Conjur-Master-Certificate", "Certificate for Conjur-master", "Cyberark1",
					new CertificateCredentialsImpl.UploadedKeyStoreSource(SecretBytes.fromBytes(keyStore)));

			store.addCredentials(Domain.global(), credentials);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		*/

		// Setup Conjur login credentials
		UsernamePasswordCredentialsImpl conjurCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
				"conjur-login", "Login Credential to Conjur", "host/frontend/frontend-01",
				"1vpn19h1j621711qm1c9mphkkqw2y35v283h1bccxb028w06t94st");
		try {
			store.addCredentials(Domain.global(), conjurCredentials);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setGlobalConfiguration() {
		GlobalConjurConfiguration globalConfig = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
		ConjurConfiguration conjurConfiguration = new ConjurConfiguration("https://conjur-master.local:8443", "demo");
		conjurConfiguration.setCredentialID("conjur-login");
		conjurConfiguration.setCertificateCredentialID("Conjur-Master-Certificate");
		globalConfig.setConjurConfiguration(conjurConfiguration);
		globalConfig.save();
		System.out.println("Global Configuration setup");
	}

	@Test
	public void addConjurCredential() {
		setGlobalConfiguration();
		CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		ConjurSecretCredentialsImpl cred = new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, "DB_SECRET",
				"db/db_password", "Conjur Secret");
		try {
			store.addCredentials(Domain.global(), cred);
			System.out.println("Conjur Credential Added");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}

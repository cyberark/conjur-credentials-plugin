package org.conjur.jenkins.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ConjurAPI {

	private ConjurAPI() {
		super();
	}

	private static final Logger LOGGER = Logger.getLogger( ConjurAPI.class.getName());

	private static class ConjurAuthnInfo {
		String applianceUrl;
		String account;
		String login;
		String apiKey;
	}

	private static ConjurAuthnInfo getConjurAuthnInfo(ConjurConfiguration configuration, List<UsernamePasswordCredentials> availableCredentials) {
		// Conjur variables
		ConjurAuthnInfo conjurAuthn = new ConjurAuthnInfo();

		if (configuration != null) {

			if (availableCredentials != null) {
				initializeWithCredential(conjurAuthn, configuration.getCredentialID(), availableCredentials);
			}

			String applianceUrl = configuration.getApplianceURL();
			if (applianceUrl != null && !applianceUrl.isEmpty()) {
				conjurAuthn.applianceUrl = applianceUrl;
			}
			String account = configuration.getAccount();
			if (account != null && !account.isEmpty()) {
				conjurAuthn.account = account;
			}
		}

		// Default to Environment variables if not values present
		defaultToEnvironment(conjurAuthn);

		return conjurAuthn;
	}

	private static void initializeWithCredential(ConjurAuthnInfo conjurAuthn, String credentialID, List<UsernamePasswordCredentials> availableCredentials) {
		if (credentialID != null && !credentialID.isEmpty()) {
			LOGGER.log(Level.INFO, "Retrieving Conjur credential stored in Jenkins");
			UsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
					availableCredentials,
					CredentialsMatchers.withId(credentialID)
					);
			if (credential != null) {
				conjurAuthn.login = credential.getUsername();
				conjurAuthn.apiKey = credential.getPassword().getPlainText();
			}
		}
	}

	private static void defaultToEnvironment(ConjurAuthnInfo conjurAuthn) {
		Map<String, String> env = System.getenv();
		if (conjurAuthn.applianceUrl == null && env.containsKey("CONJUR_APPLIANCE_URL")) conjurAuthn.applianceUrl = env.get("CONJUR_APPLIANCE_URL");
		if (conjurAuthn.account == null && env.containsKey("CONJUR_ACCOUNT")) conjurAuthn.account = env.get("CONJUR_ACCOUNT");
		if (conjurAuthn.login == null && env.containsKey("CONJUR_AUTHN_LOGIN")) conjurAuthn.login = env.get("CONJUR_AUTHN_LOGIN");
		if (conjurAuthn.apiKey == null && env.containsKey("CONJUR_AUTHN_API_KEY")) conjurAuthn.apiKey = env.get("CONJUR_AUTHN_API_KEY");
	}

	public static String getAuthorizationToken(OkHttpClient client, ConjurConfiguration configuration, Run<?, ?> context) throws IOException {

		String resultingToken = null;

		List<UsernamePasswordCredentials> availableCredentials = CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
				Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

		if (context != null) {
			availableCredentials.addAll(CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
					context.getParent(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()));
		}

		ConjurAuthnInfo conjurAuthn = getConjurAuthnInfo(configuration, availableCredentials);

		if (conjurAuthn.login != null && conjurAuthn.apiKey != null) {
			LOGGER.log(Level.INFO, "Authenticating with Conjur");
			Request request = new Request.Builder().url(String.format("%s/authn/%s/%s/authenticate", 
					conjurAuthn.applianceUrl,
					conjurAuthn.account, 
					URLEncoder.encode(conjurAuthn.login, "utf-8")))
					.post(RequestBody.create(MediaType.parse("text/plain"), conjurAuthn.apiKey))
					.build();

			Response response = client.newCall(request).execute();
			resultingToken = Base64.getEncoder().withoutPadding().encodeToString(response.body().string().getBytes("UTF-8"));
			LOGGER.log(Level.INFO, () -> "Conjur Authenticate response " + response.code() + " - " + response.message());
			if (response.code() != 200) {
				throw new IOException("Error authenticating to Conjur [" + response.code() +  " - " + response.message() + "\n" + resultingToken);
			}
		}
		else{
			LOGGER.log(Level.INFO,  "Failed to find credentials for conjur authentication");
		}

		return resultingToken;
	}

	public static String getSecret(OkHttpClient client, ConjurConfiguration configuration, String authToken, String variablePath) throws IOException {
		String result = null;

		ConjurAuthnInfo conjurAuthn = getConjurAuthnInfo(configuration, null);

		LOGGER.log(Level.INFO, "Fetching secret from Conjur");
		Request request = new Request.Builder().url(String.format("%s/secrets/%s/variable/%s", 
				conjurAuthn.applianceUrl,
				conjurAuthn.account,
				variablePath))
				.get()
				.addHeader("Authorization", "Token token=\"" + authToken +"\"")
				.build();

		Response response = client.newCall(request).execute();
		result = response.body().string();
		LOGGER.log(Level.INFO, () -> "Fetch secret [" + variablePath + "] from Conjur response " + response.code() + " - " + response.message());
		if (response.code() != 200) {
			throw new IOException("Error fetching secret from Conjur [" + response.code() +  " - " + response.message() + "\n" + result);
		}

		return result;
	}

	public static OkHttpClient getHttpClient(ConjurConfiguration configuration) {

		OkHttpClient client = null;

		CertificateCredentials certificate = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(CertificateCredentials.class,
						Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(configuration.getCertificateCredentialID())
				);

		if (certificate != null) {
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
						.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tms[0])
						.build();
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Error configuring server certificates.", e);
			}
		} else {
			client = new OkHttpClient.Builder().build();
		}

		return client;
	}

}

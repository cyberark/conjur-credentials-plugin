package org.conjur.jenkins.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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

import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.configuration.ConjurJITJobProperty;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import com.fasterxml.jackson.databind.ObjectMapper;

import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConjurAPI {

	private static class ConjurAuthnInfo {
		String applianceUrl;
		String authnPath;
		String account;
		String login;
		String apiKey;
	}

	private static final Logger LOGGER = Logger.getLogger(ConjurAPI.class.getName());

	private static void defaultToEnvironment(ConjurAuthnInfo conjurAuthn) {
		Map<String, String> env = System.getenv();
		if (conjurAuthn.applianceUrl == null && env.containsKey("CONJUR_APPLIANCE_URL"))
			conjurAuthn.applianceUrl = env.get("CONJUR_APPLIANCE_URL");
		if (conjurAuthn.account == null && env.containsKey("CONJUR_ACCOUNT"))
			conjurAuthn.account = env.get("CONJUR_ACCOUNT");
		if (conjurAuthn.login == null && env.containsKey("CONJUR_AUTHN_LOGIN"))
			conjurAuthn.login = env.get("CONJUR_AUTHN_LOGIN");
		if (conjurAuthn.apiKey == null && env.containsKey("CONJUR_AUTHN_API_KEY"))
			conjurAuthn.apiKey = env.get("CONJUR_AUTHN_API_KEY");
	}

	public static String getAuthorizationToken(OkHttpClient client, ConjurConfiguration configuration,
			Run<?, ?> context) throws IOException {

		String resultingToken = null;

		List<UsernamePasswordCredentials> availableCredentials = CredentialsProvider.lookupCredentials(
				UsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
				Collections.<DomainRequirement>emptyList());

		if (context != null) {
			availableCredentials.addAll(CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
					context.getParent(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()));
		}

		ConjurAuthnInfo conjurAuthn = getConjurAuthnInfo(configuration, availableCredentials, context);

		if (conjurAuthn.login != null && conjurAuthn.apiKey != null) {
			LOGGER.log(Level.INFO, "Authenticating with Conjur");
			Request request = new Request.Builder()
					.url(String.format("%s/%s/%s/%s/authenticate", conjurAuthn.applianceUrl, conjurAuthn.authnPath,
							conjurAuthn.account, URLEncoder.encode(conjurAuthn.login, "utf-8")))
					.post(RequestBody.create(MediaType.parse("text/plain"), conjurAuthn.apiKey)).build();

			Response response = client.newCall(request).execute();
			resultingToken = Base64.getEncoder().withoutPadding()
					.encodeToString(response.body().string().getBytes("UTF-8"));
			LOGGER.log(Level.INFO,
					() -> "Conjur Authenticate response " + response.code() + " - " + response.message());
			if (response.code() != 200) {
				throw new IOException("Error authenticating to Conjur [" + response.code() + " - " + response.message()
						+ "\n" + resultingToken);
			}
		} else {
			LOGGER.log(Level.INFO, "Failed to find credentials for conjur authentication");
		}

		return resultingToken;
	}

	private static ConjurAuthnInfo getConjurAuthnInfo(ConjurConfiguration configuration,
			List<UsernamePasswordCredentials> availableCredentials, Run<?, ?> context) {
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
			// Default authentication will be authn
			conjurAuthn.authnPath = "authn";
		}

		// Default to Environment variables if not values present
		defaultToEnvironment(conjurAuthn);

		// Check for Just-In-time Credential Access
		if (context != null) {
			setConjurAuthnForJITCredentialAccess(context, conjurAuthn);
		}

		return conjurAuthn;
	}

	private static String signatureForRequest(String challenge, RSAPrivateKey privateKey) {
		// sign using the private key
		LOGGER.log(Level.INFO, "Challenge: {0}", challenge);
		try {
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initSign(privateKey);
			sig.update(challenge.getBytes("UTF8"));
			String signatureString = Base64.getEncoder().encodeToString(sig.sign());
			LOGGER.log(Level.INFO, "*** SignatureString: {0}", signatureString);
			return signatureString;
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String apiKeyForAuthentication(String prefix, String buildNumber, String signature, String keyAlgorithm) {
		// Build the response Body
		Map<String, String> body = new HashMap<String, String>();
		body.put("buildNumber", buildNumber);
		body.put("signature", signature);
		body.put("keyAlgorithm", keyAlgorithm);
		if (prefix != null && prefix.length() > 0) {
			body.put("jobProperty_hostPrefix", prefix);
		}

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return objectMapper.writeValueAsString(body);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void setConjurAuthnForJITCredentialAccess(Run<?, ?> context, ConjurAuthnInfo conjurAuthn) {

		ConjurJITJobProperty conjurJobConfig = context.getParent().getProperty(ConjurJITJobProperty.class);
		if (conjurJobConfig != null && conjurJobConfig.getUseJustInTime()) {
			String jobName = context.getParent().getFullName();
			int buildNumber = context.getNumber();
			LOGGER.log(Level.INFO, "++++++ JobName: " + jobName + "  Build Number: " + buildNumber);
				String prefix = conjurJobConfig.getHostPrefix();
			LOGGER.log(Level.INFO, "PREFIX: {0}", prefix);
			RSAPrivateKey privateKey = InstanceIdentity.get().getPrivate();
			LOGGER.log(Level.INFO, privateKey.getAlgorithm());
			conjurAuthn.login = "host/" + (prefix != null && prefix.length() > 0 ? prefix + "/" : "") + jobName;
			conjurAuthn.authnPath = "authn-jenkins/" + conjurJobConfig.getAuthWebServiceId();	
			conjurAuthn.apiKey = apiKeyForAuthentication(prefix,
														 String.valueOf(buildNumber), 
														 signatureForRequest(jobName + "-" + buildNumber, privateKey),
														 privateKey.getAlgorithm());
			LOGGER.log(Level.INFO, "*** passwordBody: {0}", conjurAuthn.apiKey);
		}
	}

	public static OkHttpClient getHttpClient(ConjurConfiguration configuration) {

		OkHttpClient client = null;

		CertificateCredentials certificate = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(CertificateCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(configuration.getCertificateCredentialID()));
		
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
						.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tms[0]).build();
			} catch (Exception e) {
				throw new IllegalArgumentException("Error configuring server certificates.", e);
			}
		} else {
			client = new OkHttpClient.Builder().build();
		}

		return client;
	}

	public static String getSecret(OkHttpClient client, ConjurConfiguration configuration, String authToken,
			String variablePath) throws IOException {
		String result = null;

		ConjurAuthnInfo conjurAuthn = getConjurAuthnInfo(configuration, null, null);

		LOGGER.log(Level.INFO, "Fetching secret from Conjur");
		Request request = new Request.Builder().url(
				String.format("%s/secrets/%s/variable/%s", conjurAuthn.applianceUrl, conjurAuthn.account, variablePath))
				.get().addHeader("Authorization", "Token token=\"" + authToken + "\"").build();

		Response response = client.newCall(request).execute();
		result = response.body().string();
		LOGGER.log(Level.INFO, () -> "Fetch secret [" + variablePath + "] from Conjur response " + response.code()
				+ " - " + response.message());
		if (response.code() != 200) {
			throw new IOException("Error fetching secret from Conjur [" + response.code() + " - " + response.message()
					+ "\n" + result);
		}

		return result;
	}

	public static ConjurConfiguration logConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		if (conjurConfiguration != null) {
			LOGGER.log(Level.INFO, "Conjur configuration provided");
			LOGGER.log(Level.INFO, "Conjur Appliance Url: " + conjurConfiguration.getApplianceURL());
			LOGGER.log(Level.INFO, "Conjur Account: " + conjurConfiguration.getAccount());
			LOGGER.log(Level.INFO, "Conjur credential ID: " + conjurConfiguration.getCredentialID());
		}
		return conjurConfiguration;
	}

	private static void initializeWithCredential(ConjurAuthnInfo conjurAuthn, String credentialID,
			List<UsernamePasswordCredentials> availableCredentials) {
		if (credentialID != null && !credentialID.isEmpty()) {
			LOGGER.log(Level.INFO, "Retrieving Conjur credential stored in Jenkins");
			UsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(availableCredentials,
					CredentialsMatchers.withId(credentialID));
			if (credential != null) {
				conjurAuthn.login = credential.getUsername();
				conjurAuthn.apiKey = credential.getPassword().getPlainText();
			}
		}
	}

	private ConjurAPI() {
		super();
	}

}

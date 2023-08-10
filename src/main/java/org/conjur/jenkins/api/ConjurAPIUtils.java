package org.conjur.jenkins.api;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
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
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.security.ACL;
import jenkins.model.Jenkins;
import okhttp3.OkHttpClient;

/**
 * ConjurAPIUtils class used to build the OkHttp Client object and create
 * CertificateCredentials.
 * 
 * @author Jaleela.FaizurRahman
 *
 */
public class ConjurAPIUtils {

	private static final Logger LOGGER = Logger.getLogger(ConjurAPIUtils.class.getName());

	/**
	 * static method to generate CertificateCredentials
	 * 
	 * @param ConjurConfiguration configuration
	 * @return CertificateCredentials
	 */
	static CertificateCredentials certificateFromConfiguration(ConjurConfiguration configuration) {
		LOGGER.log(Level.FINE, "Start of certificateFromConfiguration()");

		CertificateCredentials certificate = null;

		if (configuration.getCertificateCredentialID() == null) {
			LOGGER.log(Level.FINE, "Return null, as CertificationID is null");
			return null;
		}

		certificate = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(CertificateCredentials.class, Jenkins.get(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(configuration.getCertificateCredentialID()));
		LOGGER.log(Level.FINE, "Return CertificateCredential for CertificationCredentialID");
		return certificate;
	}

	/**
	 * static method to return the OkHttpClient with the certificate
	 * 
	 * @param CertificateCredentials certificate
	 * @return OkHttpClient clientf
	 */

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

	/**
	 * static method to get HttpClinet
	 * 
	 * @param ConjurConfiguration configuration
	 * @return OkHttpClient client
	 */
	public static OkHttpClient getHttpClient(ConjurConfiguration configuration) {

		CertificateCredentials certificate = certificateFromConfiguration(configuration);

		if (certificate != null) {
			return httpClientWithCertificate(certificate);
		}

		return new OkHttpClient.Builder().build();
	}

}
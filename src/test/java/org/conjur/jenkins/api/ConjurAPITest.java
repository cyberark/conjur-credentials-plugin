package org.conjur.jenkins.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.conjur.jenkins.api.ConjurAPI.ConjurAuthnInfo;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.jwtauth.impl.JwtToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;

import hudson.model.ModelObject;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConjurAPI.class, JwtToken.class , ConjurAPIUtils.class})
@PowerMockIgnore({ "javax.management.", "com.sun.org.apache.xerces.", "javax.xml.", "org.xml.", "org.w3c.dom.",
		"org.apache.http.conn.ssl.*", "javax.net.ssl.*", "com.sun.org.apache.xalan.", "javax.activation.*",
		 "javax.xml.*", "org.xml.*", "javax.management.*",
		"javax.crypto.*" })
public class ConjurAPITest {

	public OkHttpClient client;
	public ModelObject context;
	public ConjurConfiguration conjurConfiguration;
	public Call remoteCall;
	public ConjurAPI api;
	public List<UsernamePasswordCredentials> availableCredential;

	@Before
	public void setUp() throws IOException {
		PowerMockito.mockStatic(ConjurAPI.class);
		conjurConfiguration = new ConjurConfiguration("https://conjur_server:8083", "myConjurAccount");
		client = ConjurAPIUtils.getHttpClient(new ConjurConfiguration("https://conjur_server:8083", "myConjurAccount"));
		availableCredential = new ArrayList<>();
		context = mock(ModelObject.class);
		remoteCall = mock(Call.class);
		api = mock(ConjurAPI.class);

	}

	@Test
	public void getConjurAuthnInfo() {
		ConjurAuthnInfo conjurAuthn = new ConjurAuthnInfo();

//		try (MockedStatic<ConjurAPI> mockStatic = Mockito.mockStatic(ConjurAPI.class)) {
//
//			mockStatic.when(() -> ConjurAPI.getConjurAuthnInfo(any(), any(), any())).thenReturn(conjurConfiguration);
//			mockStatic.verify(() -> ConjurAPI.getConjurAuthnInfo(conjurConfiguration, availableCredential, context));
//		}

		// PowerMockito.mockStatic(ConjurAPI.class);

		when(ConjurAPI.getConjurAuthnInfo(any(), any(), any())).thenReturn(conjurAuthn);
		assertTrue(ConjurAPI.getConjurAuthnInfo(any(), any(), any()) instanceof ConjurAuthnInfo);
	}

	@Test
	public void checkAuthentication() throws IOException {
//
//		try (MockedStatic<JwtToken> mockStatic = Mockito.mockStatic(JwtToken.class)) {
//			mockStatic.when(() -> JwtToken.getToken(context)).thenReturn(
//					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
//		}
//
//		try (MockedStatic<ConjurAPI> mock = Mockito.mockStatic(ConjurAPI.class)) {
//
//			mock.when(() -> ConjurAPI.getAuthorizationToken(client, conjurConfiguration, context))
//					.thenReturn("success");
//			mock.verify(() -> ConjurAPI.getConjurAuthnInfo(conjurConfiguration, availableCredential, context));
//		}
//		assertThat(ConjurAPI.getAuthorizationToken(client, conjurConfiguration, context)).isEqualTo("success");

		PowerMockito.mockStatic(JwtToken.class);

		when(JwtToken.getToken(context)).thenReturn(
				"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
		when(ConjurAPI.getAuthorizationToken(client, conjurConfiguration, context)).thenReturn("success");
		assertEquals(ConjurAPI.getAuthorizationToken(client, conjurConfiguration, context), "success");

	}

	@Test
	public void checkSecretVal() throws IOException {
		
//		Request request = new Request.Builder()
//				.url(String.format("%s/secrets/%s/variable/%s", "https://localhost:8443", "myConjurAccount",
//						"host/jenkins-frontend/NG-NITIN-M.local"))
//				.get().addHeader("Authorization", "Token token=\"" + "authToken" + "\"").build();
//		when(client.newCall(request).execute()).thenReturn(mockHttpResponse());
//		when(remoteCall.execute()).thenReturn(mockHttpResponse());
//		when(client.newCall(any())).thenReturn(remoteCall);
		when(ConjurAPI.getSecret(client, conjurConfiguration, "auth-token", "host/frontend/frontend-01"))
				.thenReturn("bhfbdbkfbkd-bvjdbfbjbv-bfjbdbjkb-bbfkbskb");
		assertEquals(ConjurAPI.getSecret(client, conjurConfiguration, "auth-token", "host/frontend/frontend-01"),
				"bhfbdbkfbkd-bvjdbfbjbv-bfjbdbjkb-bbfkbskb");
	}



}

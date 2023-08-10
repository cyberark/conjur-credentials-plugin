package org.conjur.jenkins.jwtauthimpl;

import static org.mockito.Mockito.mock;

import org.conjur.jenkins.jwtauth.impl.JwtToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JwtToken.class})
@PowerMockIgnore({ "javax.management.", "com.sun.org.apache.xerces.", "javax.xml.", "org.xml.", "org.w3c.dom.","com.sun.org.apache.xalan.", "javax.activation.*", "jdk.internal.reflect.*,java.util.logging.*" ,"javax.xml.*","org.xml.*", "javax.management.*, org.mockito.*"})
public class JwtTokenTest {

	@Test
	public void mockSign() {
		PowerMockito.mockStatic(JwtToken.class);
	    JwtToken jwtToken= mock(JwtToken.class);
	    PowerMockito.when(jwtToken.sign()).thenReturn("Signing Token");
	}
	
	
	@SuppressWarnings("static-access")
	@Test
	public void mockGetToken() {
	   PowerMockito.mockStatic(JwtToken.class);
	   JwtToken jwtToken1= mock(JwtToken.class);
	   Object context = "secretId";
	   PowerMockito.when(jwtToken1.getToken(context)).thenReturn("secret retrival "+ context);
		
	}
	
	@SuppressWarnings("static-access")
	@Test
	public void mockGetUnsignedToken() {
	   PowerMockito.mockStatic(JwtToken.class);
	   JwtToken jwtToken2= mock(JwtToken.class);
	   
	   String pluginAction= " sdfghjkl";
	   PowerMockito.when(jwtToken2.getUnsignedToken(pluginAction, jwtToken2)).thenReturn(jwtToken2);
				
	}
}






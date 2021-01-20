package org.eclipse.che.security.oauth1;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.eclipse.che.api.factory.server.scm.exception.ScmUnauthorizedException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class BitbucketServerOAuth1AuthorizationHeaderSupplierTest {
  @Mock BitbucketServerOAuthAuthenticator authenticator;
  @InjectMocks BitbucketServerOAuth1AuthorizationHeaderSupplier supplier;

  Subject subject = new SubjectImpl("user", "234234", "t234234", false);

  @BeforeMethod
  public void setUp() {
    EnvironmentContext.getCurrent().setSubject(subject);
  }

  @Test
  public void shouldBeAbleToComputeAuthorizationHeader()
      throws ScmUnauthorizedException, OAuthAuthenticationException {
    // given
    when(authenticator.computeAuthorizationHeader(
            eq(subject.getUserId()), eq("POST"), eq("/api/user")))
        .thenReturn("signature");
    // when
    String actual = supplier.computeAuthorizationHeader("POST", "/api/user");
    // then
    assertEquals(actual, "signature");
  }

  @Test(
      expectedExceptions = ScmUnauthorizedException.class,
      expectedExceptionsMessageRegExp =
          "user is not authorized in bitbucket-server OAuth1 provider")
  public void shouldThrowScmUnauthorizedExceptionIfHeaderIsNull()
      throws OAuthAuthenticationException, ScmUnauthorizedException {
    // given
    when(authenticator.computeAuthorizationHeader(
            eq(subject.getUserId()), eq("POST"), eq("/api/user")))
        .thenReturn(null);
    // when
    supplier.computeAuthorizationHeader("POST", "/api/user");
  }

  @Test(
      expectedExceptions = ScmUnauthorizedException.class,
      expectedExceptionsMessageRegExp =
          "user is not authorized in bitbucket-server OAuth1 provider")
  public void shouldThrowScmUnauthorizedExceptionIfHeaderIsEmpty()
      throws OAuthAuthenticationException, ScmUnauthorizedException {
    // given
    when(authenticator.computeAuthorizationHeader(
            eq(subject.getUserId()), eq("POST"), eq("/api/user")))
        .thenReturn("");
    // when
    supplier.computeAuthorizationHeader("POST", "/api/user");
  }

  @Test(
      expectedExceptions = ScmUnauthorizedException.class,
      expectedExceptionsMessageRegExp = "this is a message")
  public void shouldThrowScmUnauthorizedExceptionOnOAuthAuthenticationException()
      throws OAuthAuthenticationException, ScmUnauthorizedException {
    // given
    when(authenticator.computeAuthorizationHeader(
            eq(subject.getUserId()), eq("POST"), eq("/api/user")))
        .thenThrow(new OAuthAuthenticationException("this is a message"));
    // when
    supplier.computeAuthorizationHeader("POST", "/api/user");
  }
}

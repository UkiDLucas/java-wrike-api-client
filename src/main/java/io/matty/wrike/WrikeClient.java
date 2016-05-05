package io.matty.wrike;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Calendar;

/**
 * Wrike Client
 */
public class WrikeClient implements Serializable {
    private String accessToken = "";
    private String refreshToken = "";
    private String scope = "";
    private long expiresIn = 0;

    private Calendar expireDate;

    private String returnToUrl = "/";

    private final OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

    public String getAccessToken() {
        if (this.accessToken == null
                || this.accessToken.length() == 0) {
            return null;
        }

        return this.accessToken;
    }

    public boolean isTokenValid() {
        if (this.accessToken == null
                || this.accessToken.length() == 0) {
            return false;
        }

        // Check expiry and try refresh token
        Calendar now = Calendar.getInstance();
        if (now.compareTo(this.expireDate) < 0) {
            try {
                this.refreshToken();
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    public OAuthClient getOAuthClient() {
        if (this.accessToken == null
                || this.accessToken.length() == 0) {
            return null;
        }
        return this.oAuthClient;
    }

    public OAuthResourceResponse makeRequest(String token, String uri, String requestMethod) throws OAuthSystemException, OAuthProblemException {
        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(uri)
                .setAccessToken(token).buildQueryMessage();

        OAuthResourceResponse resourceResponse = getOAuthClient().resource(bearerClientRequest, requestMethod, OAuthResourceResponse.class);
        return resourceResponse;
    }

    public boolean wrikeCallback(HttpServletRequest request, HttpServletResponse response) {
        try {
            OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = oar.getCode();

            OAuthClientRequest clientRequest = OAuthClientRequest
                    .tokenLocation("https://www.wrike.com/oauth2/token")
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(WrikeCfg.clientID)
                    .setClientSecret(WrikeCfg.clientSecret)
                    .setRedirectURI(WrikeCfg.callbackUri)
                    .setCode(code)
                    .buildQueryMessage();

            OAuthAccessTokenResponse oAuthResponse = this.oAuthClient.accessToken(clientRequest);

            this.accessToken = oAuthResponse.getAccessToken();
            this.refreshToken = oAuthResponse.getRefreshToken();
            this.scope = oAuthResponse.getScope();
            this.expiresIn = oAuthResponse.getExpiresIn();

            this.expireDate = Calendar.getInstance();
            this.expireDate.add(Calendar.SECOND, Math.toIntExact(oAuthResponse.getExpiresIn()));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Building & returns OAuthClientRequest from WrikeCfg.java
     **/
    public String getWrikeAuthenticationUri() {
        try {
            System.out.println("Building OAuthClientRequest from WrikeCfg.java");

            OAuthClientRequest request = OAuthClientRequest
                    .authorizationLocation("https://www.wrike.com/oauth2/authorize")
                    .setClientId(WrikeCfg.clientID)
                    .setRedirectURI(WrikeCfg.callbackUri)
                    .setParameter("response_type", "code")
                    .buildQueryMessage();

            return request.getLocationUri();
        } catch (OAuthSystemException e) {
            e.printStackTrace();
        }

        return "";
    }

    private boolean refreshToken() throws Exception {
        OAuthClientRequest clientRequest = OAuthClientRequest
                .tokenLocation("https://www.wrike.com/oauth2/token")
                .setGrantType(GrantType.REFRESH_TOKEN)
                .setClientId(WrikeCfg.clientID)
                .setClientSecret(WrikeCfg.clientSecret)
                .setRefreshToken(refreshToken)
                .buildQueryMessage();

        OAuthAccessTokenResponse oAuthResponse = this.oAuthClient.accessToken(clientRequest);

        if (oAuthResponse.getAccessToken().length() == 0
                || oAuthResponse.getExpiresIn() == 0
                || oAuthResponse.getRefreshToken().length() == 0) {
            return false;
        }

        this.accessToken = oAuthResponse.getAccessToken();
        this.refreshToken = oAuthResponse.getRefreshToken();
        this.scope = oAuthResponse.getScope();
        this.expiresIn = oAuthResponse.getExpiresIn();

        this.expireDate = Calendar.getInstance();
        this.expireDate.add(Calendar.SECOND, Math.toIntExact(oAuthResponse.getExpiresIn()));

        return true;
    }

    public static WrikeClient getClientFromSession(HttpServletRequest request) {
        Object wrikeClientObj = request.getSession().getAttribute("WRIKE_CLIENT");

        if (wrikeClientObj instanceof WrikeClient) {
            return (WrikeClient) wrikeClientObj;
        }

        WrikeClient wrikeClient = new WrikeClient();
        request.getSession().setAttribute("WRIKE_CLIENT", wrikeClient);

        return wrikeClient;
    }

    public String getReturnToUrl() {
        return returnToUrl;
    }

    public void setReturnToUrl(String returnToUrl) {
        this.returnToUrl = returnToUrl;
    }
}

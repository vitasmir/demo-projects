import { AuthConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  issuer: 'http://localhost:8180/realms/myrealm',
  redirectUri: window.location.origin,
  postLogoutRedirectUri: window.location.origin,
  clientId: 'myapp-client',
  responseType: 'code',
  scope: 'openid profile email',
  requireHttps: false,
  useSilentRefresh: true,
  showDebugInformation: true
};

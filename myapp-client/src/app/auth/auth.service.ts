import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private static readonly postLoginTargetKey = 'myapp.postLoginTarget';
  private static readonly adminRoleNames = new Set([
    'admin',
    'role_admin',
    'task-management-admin',
    'task_management_admin',
    'task.admin',
    'tm_admin'
  ]);
  private initialized = false;

  constructor(private readonly oauthService: OAuthService) {}

  async initAuth(): Promise<void> {
    this.oauthService.configure(authConfig);
    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      this.oauthService.setupAutomaticSilentRefresh();
      this.restorePostLoginTarget();
    } finally {
      this.initialized = true;
    }
  }

  login(targetUrl?: string): void {
    this.storePostLoginTarget(targetUrl);
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  isInitialized(): boolean {
    return this.initialized;
  }

  private storePostLoginTarget(targetUrl?: string): void {
    const normalizedTarget = (targetUrl ?? `${window.location.pathname}${window.location.search}${window.location.hash}`).trim();
    if (normalizedTarget === '' || normalizedTarget === '/') {
      sessionStorage.removeItem(AuthService.postLoginTargetKey);
      return;
    }

    sessionStorage.setItem(AuthService.postLoginTargetKey, normalizedTarget);
  }

  private restorePostLoginTarget(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const savedTarget = sessionStorage.getItem(AuthService.postLoginTargetKey);
    if (savedTarget == null || savedTarget.trim() === '') {
      return;
    }

    const currentTarget = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    if (currentTarget === '/' || currentTarget === '') {
      sessionStorage.removeItem(AuthService.postLoginTargetKey);
      window.location.replace(savedTarget);
      return;
    }

    if (currentTarget === savedTarget) {
      sessionStorage.removeItem(AuthService.postLoginTargetKey);
    }
  }

  getUsername(): string | null {
    const identityClaims = this.oauthService.getIdentityClaims() as Record<string, unknown> | null;
    const identityUsername = this.readUsernameClaim(identityClaims);
    if (identityUsername !== null) {
      return identityUsername;
    }

    const accessToken = this.oauthService.getAccessToken();
    if (accessToken.trim() === '') {
      return null;
    }

    const accessTokenClaims = this.parseJwtClaims(accessToken);
    return this.readUsernameClaim(accessTokenClaims);
  }

  isAdmin(): boolean {
    const identityClaims = this.oauthService.getIdentityClaims() as Record<string, unknown> | null;
    if (this.hasAdminRole(identityClaims)) {
      return true;
    }

    const accessToken = this.oauthService.getAccessToken();
    if (accessToken.trim() === '') {
      return false;
    }

    return this.hasAdminRole(this.parseJwtClaims(accessToken));
  }

  private readUsernameClaim(claims: Record<string, unknown> | null): string | null {
    if (claims == null) {
      return null;
    }

    const usernameClaim = claims['preferred_username'] ?? claims['name'] ?? claims['email'] ?? claims['sub'];
    return typeof usernameClaim === 'string' && usernameClaim.trim() !== ''
      ? usernameClaim.trim()
      : null;
  }

  private hasAdminRole(claims: Record<string, unknown> | null): boolean {
    if (claims == null) {
      return false;
    }

    const normalizedRoles = new Set<string>();
    this.collectRoleValuesFromClaim(claims['realm_access'], normalizedRoles);

    const resourceAccess = claims['resource_access'];
    if (resourceAccess != null && typeof resourceAccess === 'object') {
      for (const clientAccess of Object.values(resourceAccess as Record<string, unknown>)) {
        this.collectRoleValuesFromClaim(clientAccess, normalizedRoles);
      }
    }

    return Array.from(normalizedRoles).some(role => AuthService.adminRoleNames.has(role));
  }

  private collectRoleValuesFromClaim(claimValue: unknown, roles: Set<string>): void {
    if (claimValue == null || typeof claimValue !== 'object') {
      return;
    }

    const rawRoles = (claimValue as Record<string, unknown>)['roles'];
    if (!Array.isArray(rawRoles)) {
      return;
    }

    for (const rawRole of rawRoles) {
      if (typeof rawRole === 'string' && rawRole.trim() !== '') {
        roles.add(rawRole.trim().toLowerCase());
      }
    }
  }

  private parseJwtClaims(token: string): Record<string, unknown> | null {
    const parts = token.split('.');
    if (parts.length < 2) {
      return null;
    }

    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const paddedBase64 = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      const jsonPayload = decodeURIComponent(
        atob(paddedBase64)
          .split('')
          .map(character => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
          .join('')
      );

      return JSON.parse(jsonPayload) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}

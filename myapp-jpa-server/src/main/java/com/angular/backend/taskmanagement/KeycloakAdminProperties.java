package com.angular.backend.taskmanagement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.keycloak.admin")
public class KeycloakAdminProperties {

    private String serverUrl;
    private String realm;
    private String adminRealm = "master";
    private String clientId = "admin-cli";
    private String username;
    private String password;
    private List<String> bootstrapAdminUsernames = new ArrayList<>(List.of("demo"));

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAdminRealm() {
        return adminRealm;
    }

    public void setAdminRealm(String adminRealm) {
        this.adminRealm = adminRealm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getBootstrapAdminUsernames() {
        return bootstrapAdminUsernames;
    }

    public void setBootstrapAdminUsernames(List<String> bootstrapAdminUsernames) {
        this.bootstrapAdminUsernames = bootstrapAdminUsernames;
    }
}
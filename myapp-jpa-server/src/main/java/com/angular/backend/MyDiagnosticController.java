package com.angular.backend;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyDiagnosticController {

    @GetMapping("/whoami")
    public String whoami() {
        try {
            // This will return the container's hostname (which is its container ID by default)
            return "Hello from container: " + InetAddress.getLocalHost().getHostName() + "\n";
        } catch (UnknownHostException e) {
            return "Could not determine hostname: " + e.getMessage() + "\n";
        }
    }
}

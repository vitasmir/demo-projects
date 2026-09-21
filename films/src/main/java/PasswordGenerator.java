import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.peta.films.SpringFileWatcher;

@Configuration
public class PasswordGenerator {

    @Bean
    public CommandLineRunner generatePasswords() {
        return args -> {
            ;
        };
    }
    
    public static void main(String[] args) {
    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        // Encode the plain text passwords
        String password1 = "FIJEjeievjJVIVJJJEVV39r"; 
        String hash1 = encoder.encode(password1);
        
        String password2 = "OsaodjJDFEfoJVAEVPEA";
        String hash2 = encoder.encode(password2);
        
        System.out.println("------------------------------------------------");
        System.out.println("COPY THESE INTO YOUR users.txt FILE:");
        System.out.println("r=" + hash1);
        System.out.println("p=" + hash2);
        System.out.println("------------------------------------------------");

    }}

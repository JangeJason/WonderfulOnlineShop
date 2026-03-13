import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$X8wL.vjXQ2QJ/8nIq52y9e7W1R4U0r.5NlD1rV0d8pB3sF7q6Z6sK";
        System.out.println("Matches 'admin': " + encoder.matches("admin", hash));
        System.out.println("Matches '123456': " + encoder.matches("123456", hash));
    }
}

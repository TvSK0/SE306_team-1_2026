package lavs.logic;

// AI-assisted: service contract consumed by the application.
public interface Authentication {
    AuthResult login(String username, String password);
    void logout();
    String currentUser();
}

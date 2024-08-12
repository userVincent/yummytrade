package be.kuleuven.dsgt4.domain;

import java.util.Map;

public class User {

    private String email;
    private String role;

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isManager() {
        return this.role != null && this.role.equals("manager");
    }

    public Map<String, Object> toDoc() {
        return Map.of(
                "email", this.email,
                "role", this.role
        );
    }

    public static User fromDoc(Map<String, Object> doc) {
        return new User(
                (String) doc.get("email"),
                (String) doc.get("role")
        );
    }
}

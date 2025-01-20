package com.example.newtracking;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    private final DatabaseReference userDatabase;

    public FirebaseHelper() {
        userDatabase = FirebaseDatabase.getInstance().getReference("Users");
    }

    public void saveUser(String userId, String email, String role) {
        userDatabase.child(userId).setValue(new User(email, role));
    }
}

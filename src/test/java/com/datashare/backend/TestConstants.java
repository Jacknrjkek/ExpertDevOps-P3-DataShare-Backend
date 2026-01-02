package com.datashare.backend;

public class TestConstants {
    // START: SUPPRESS WARNINGS (Test Credentials)
    // nosonar
    // Snyk: deliberately hardcoded for testing purposes
    public static final String TEST_USER_PASSWORD = "password123";
    public static final String TEST_FILE_PASSWORD = "securePassword";
    public static final String TEST_FILE_PASSWORD_HASH = "$2a$10$hashedpasswordplaceholder";
    public static final String TEST_JWT_SECRET = "ThisIsASecretKeyForTestThatIsLongEnoughToSatisfyHS256Requirements";
    public static final String TEST_WRONG_PASSWORD = "wrongpassword";
    // END: SUPPRESS WARNINGS
}

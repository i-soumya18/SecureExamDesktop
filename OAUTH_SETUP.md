# Setting Up Google OAuth for SecureExamDesktop

This guide explains how to set up Google OAuth for the SecureExamDesktop application.

## Prerequisites

1. A Google Cloud Platform (GCP) account
2. Firebase project with Authentication enabled

## Steps to Configure Google OAuth

### 1. Create a Google Cloud Platform Project (if not already done)

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Click on "Select a project" at the top of the page
3. Click on "New Project"
4. Enter a name for your project and click "Create"

### 2. Configure OAuth Consent Screen

1. In the Google Cloud Console, navigate to "APIs & Services" > "OAuth consent screen"
2. Select "External" user type (unless you're using Google Workspace)
3. Fill in the required information:
   - App name: "SecureExamDesktop"
   - User support email: Your email address
   - Developer contact information: Your email address
4. Click "Save and Continue"
5. Add the following scopes:
   - `email`
   - `profile`
   - `openid`
6. Click "Save and Continue"
7. Add test users if you're still in testing mode
8. Click "Save and Continue"
9. Review your settings and click "Back to Dashboard"

### 3. Create OAuth Client ID

1. In the Google Cloud Console, navigate to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Select "Desktop app" as the application type
4. Enter a name for your client ID (e.g., "SecureExamDesktop Client")
5. Click "Create"
6. Note down the Client ID (you'll need it for the application)

### 4. Configure Firebase Authentication

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to "Authentication" > "Sign-in method"
4. Enable "Google" as a sign-in provider
5. Configure the Google sign-in provider with the OAuth client ID you created

### 5. Update the Application Code

1. Open `GoogleAuthHelper.java`
2. Replace the placeholder `YOUR_GOOGLE_CLIENT_ID` with your actual client ID:
   ```java
   private static final String CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID"; // Replace with your actual client ID
   ```

## Testing the OAuth Flow

1. Build and run the application
2. Click the "Sign in with Google" button
3. Your default browser should open with the Google sign-in page
4. After signing in, you'll be redirected back to the application

## Troubleshooting

- If the browser doesn't open, the application will display the URL to manually open
- Check the application logs for any error messages
- Ensure that the redirect URI (`http://localhost:8888/callback`) is correctly configured in your OAuth client settings
- If you're getting "invalid_client" errors, double-check your client ID

## Security Considerations

- The client ID is considered public information and can be included in the client-side code
- Never include the client secret in the desktop application code
- The application uses PKCE (Proof Key for Code Exchange) to secure the authorization flow without a client secret
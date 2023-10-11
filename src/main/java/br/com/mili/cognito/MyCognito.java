package br.com.mili.cognito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class MyCognito {

    @Value("${aws.region.static}")
    private String region;

    public CognitoIdentityProviderClient getClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create("COGNITO-PROMOCAO"))
                .build();
    }

    public void listAllUserPools(){

        //System.out.println("=== listAllUserPools ===");

        CognitoIdentityProviderClient client = getClient();

        ListUserPoolsRequest request = ListUserPoolsRequest.builder()
                .maxResults(10)
                .build();

        ListUserPoolsResponse response = client.listUserPools(request);

        response.userPools().forEach(userPool -> {
            System.out.println("User Pool: " + userPool.name() + ", ID " + userPool.id());

            listAllPoolClients(client, userPool.id());
            listAllUsers(client, userPool.id());
            //initateAuth(client);

            System.out.println("---------------------------------------------------------");

        });



        client.close();

    }

    private void listAllPoolClients(CognitoIdentityProviderClient client, String userPoolId) {

        //System.out.println("\t=== listAllPoolClients ===");

        ListUserPoolClientsRequest request = ListUserPoolClientsRequest.builder()
                .maxResults(10)
                .userPoolId(userPoolId)
                .build();
        ListUserPoolClientsResponse response = client.listUserPoolClients(request);

        response.userPoolClients().forEach(userPoolClient -> {
            System.out.println("\t\tClient: " + userPoolClient.clientName() + ", Client Id: " + userPoolClient.clientId());
        });

    }

    private void listAllUsers(CognitoIdentityProviderClient client, String userPoolId) {

        //System.out.println("\t\t=== listAllUsers ===");

        ListUsersRequest request = ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .limit(10)
                .build();

        ListUsersResponse response = client.listUsers(request);

        response.users().forEach(user -> {
            System.out.println("\t\t\tUsername: " + user.username() + " Status " + user.userStatus());
        });

    }

    /**
     *
     * Usado para fazer a autenticação.
     * Na primeira vez que for utlizado, é retornado um session e então é necessário executar RespondToAuthChallengeRequest, quando for necessário alterar senha
     * @param client
     * @param clientId
     * @param pass
     */
    public InitiateAuthResponse initateAuth(CognitoIdentityProviderClient client, String clientId, String email, String pass) {

        System.out.println("=== initateAuth ===");

        Map<String,String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", email);
        authParameters.put("PASSWORD", pass);
        authParameters.put("SECRET_HASH", calculateSecretHash(email, clientId, "155u0r31d5903m54mb8n5an8lh510i63gelbq3vnosavmlvnrlav"));

        InitiateAuthRequest request = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();
        InitiateAuthResponse response = client.initiateAuth(request);

        System.out.println("accessToken: " + ((response.authenticationResult() == null)? "" : response.authenticationResult().accessToken()));
        System.out.println("idToken: " + ((response.authenticationResult() == null)? "" : response.authenticationResult().idToken()));
        System.out.println("refreshToken: " + ((response.authenticationResult() == null)? "" : response.authenticationResult().refreshToken()));
        System.out.println("result: " + ((response.authenticationResult() == null)? "" : response.authenticationResult().toString()));



        System.out.println("session: " + response.session());
        return response;
    }

    /**
     * 2º passo Faz a alteração da senha do primeiro login
     * @param client
     * @param userPoolId
     * @param clientId
     * @param username
     * @param pass
     * @param session
     */
    public void respondToAuthChallenge(CognitoIdentityProviderClient client, String userPoolId, String clientId, String username, String pass, String session) {

        Map<String,String> challengeResponses = new HashMap<>();
        challengeResponses.put("USERNAME", username);
        challengeResponses.put("NEW_PASSWORD", pass);

        RespondToAuthChallengeRequest request = RespondToAuthChallengeRequest.builder()
                .clientId(clientId)
                .session(session)
                .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                .challengeResponses(challengeResponses)
                .build();

        RespondToAuthChallengeResponse response = client.respondToAuthChallenge(request);
        System.out.println("Result " + response.authenticationResult().toString());
    }

    public void changeUserPassword(CognitoIdentityProviderClient client, String accessToken, String previousPassword, String proposedPassword) {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .accessToken(accessToken)
                .previousPassword(previousPassword)
                .proposedPassword(proposedPassword)
                .build();

        ChangePasswordResponse response = client.changePassword(request);
        System.out.println(response.toString());
        System.out.println(response.sdkHttpResponse().statusCode());


    }

    /**
     * Para cadastrar novo usuario
     *
     * @return
     */
    public SignUpResponse signUp(CognitoIdentityProviderClient identityProviderClient, String clientId,
                                 String email,
                                 String name,
                                 String lastName,
                                 String password) {


        List<AttributeType> userAttrsList = new ArrayList<>();
        userAttrsList.add(AttributeType.builder()
                .name("email")
                .value(email)
                .build());

        userAttrsList.add(AttributeType.builder()
                .name("given_name")
                .value(name)
                .build());

        userAttrsList.add(AttributeType.builder()
                .name("family_name")
                .value(name)
                .build());
        try {
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .secretHash(calculateSecretHash(email, clientId, "155u0r31d5903m54mb8n5an8lh510i63gelbq3vnosavmlvnrlav"))
                    .userAttributes(userAttrsList)
                    .username(email)
                    .clientId(clientId)
                    .password(password)
                    .build();

            SignUpResponse response = identityProviderClient.signUp(signUpRequest);
            System.out.println("User has been signed up ");
            return response;

        } catch(CognitoIdentityProviderException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Apos cadastrar o usuário é necessário confirmar o cadastro com o código que vai por email
     *
     * @return
     */
    public ConfirmSignUpResponse confirmationSignUp(CognitoIdentityProviderClient identityProviderClient, String clientId, String email, String verificationCode) {
        try {
            ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                    .secretHash(calculateSecretHash(email, clientId, "155u0r31d5903m54mb8n5an8lh510i63gelbq3vnosavmlvnrlav"))
                    .clientId(clientId)
                    .username(email)
                    .confirmationCode(verificationCode)
                    .build();

            ConfirmSignUpResponse response = identityProviderClient.confirmSignUp(confirmSignUpRequest);
            System.out.println("User has been confirmed");
            return response;
        } catch (CognitoIdentityProviderException e) {
            e.printStackTrace();
        }
        return null;

    }

    public ResendConfirmationCodeResponse resendConfirmationCode(CognitoIdentityProviderClient identityProviderClient, String clientId, String email) {
        try {
            ResendConfirmationCodeRequest codeRequest = ResendConfirmationCodeRequest.builder()
                    .secretHash(calculateSecretHash(email, clientId, "155u0r31d5903m54mb8n5an8lh510i63gelbq3vnosavmlvnrlav"))
                    .clientId(clientId)
                    .username(email)
                    .build();

            return identityProviderClient.resendConfirmationCode(codeRequest);


        } catch(CognitoIdentityProviderException e) {
            e.printStackTrace();
        }
        return null;
    }



    public String calculateSecretHash(String username, String clientId, String clientSecret) {
        try {
            String message = username + clientId;
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hashBytes = sha256HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }


}

package br.com.mili.cognito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class MyCognito {

    @Value("${aws.region.static}")
    private String region;

    public CognitoIdentityProviderClient getClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create("cognito"))
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
     * Na primeira vez que for utlizado, é retornado um session e então é necessário executar RespondToAuthChallengeRequest
     * @param client
     * @param clientId
     * @param username
     * @param pass
     */
    public InitiateAuthResponse initateAuth(CognitoIdentityProviderClient client, String clientId, String username, String pass) {

        System.out.println("=== initateAuth ===");

        Map<String,String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", pass);

        InitiateAuthRequest request = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();
        InitiateAuthResponse response = client.initiateAuth(request);

        System.out.println("accessToken: " + ((response.authenticationResult() == null)? "" : response.authenticationResult().accessToken()));

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


}

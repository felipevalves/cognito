package br.com.mili.cognito;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

@SpringBootTest
class CognitoApplicationTests {

	@Autowired
	private MyCognito cognito;

	private CognitoIdentityProviderClient client;


	@BeforeEach
	void init() {
		client = cognito.getClient();
	}

	@Test
	void test_list_users_pool() {

		cognito.listAllUserPools();

	}


	@Test
	void test_init_auth_pool_live_demo() {

		String clientId = "3a4cqdeg67aeklo9iuve8ubpch";
		String user = "felipe.alves@mili.com.br";
		String pass = "123456Ab#";

		cognito.initateAuth(client, clientId, user, pass);

	}

	@Test
	void test_init_auth_pool_my_user_pool() {

		String clientId = "2ur7r0o6gr21oobcsqmhgqdhp2";
		String user = "felipe.alves";
		String pass = "PassUpdated22#";

		cognito.initateAuth(client, clientId, user, pass);

	}

	@Test
	void test_respond_auth_challenge_my_user_pool() {

		String poolId = "sa-east-1_WjcNeuDAG";
		String clientId = "2ur7r0o6gr21oobcsqmhgqdhp2";
		String user = "felipe.alves";
		String pass = "123456Ab#";
		String newPass = "PassUpdated22#";

		InitiateAuthResponse response = cognito.initateAuth(client, clientId, user, pass);

		cognito.respondToAuthChallenge(client, poolId, clientId, user, newPass, response.session());
	}

	@Test
	void test_change_password_my_user_pool() {

		String poolId = "sa-east-1_WjcNeuDAG";
		String clientId = "2ur7r0o6gr21oobcsqmhgqdhp2";
		String user = "test@mili.com.br";
		String proposed = "Changed22#";
		String previous = "123456Updated#";

		InitiateAuthResponse response = cognito.initateAuth(client, clientId, user, previous);

		cognito.changeUserPassword(client, response.authenticationResult().accessToken(), previous,  proposed);
	}

}

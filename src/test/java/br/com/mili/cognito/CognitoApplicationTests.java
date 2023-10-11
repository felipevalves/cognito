package br.com.mili.cognito;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

@SpringBootTest
class CognitoApplicationTests {

	@Autowired
	private MyCognito cognito;

	private CognitoIdentityProviderClient client;

	private static final String GRUPO_PROMOCAO = "dev-promocao-01";
	private static final String CLIENT_ID_PROMOCAO = "3q2rns92m8afoofr64n7vdqq3q";

	private static final String EMAIL = "vieira.felipe.alves@gmail.com";
	private static final String PASS = "123@Abcd";

	@BeforeEach
	void init() {
		client = cognito.getClient();
	}

	@Test
	void test_list_users_pool() {

		cognito.listAllUserPools();

	}

	@Test
	void test_create_new_user() {
		SignUpResponse response = cognito.signUp(client, CLIENT_ID_PROMOCAO, "douglas.silverio@mili.com.br", "Douglas", "Silverio", PASS);

		Assertions.assertNotNull(response);
		System.out.println("test_create_new_user: " + response.toString());
	}

	@Test
	void test_confirmation_new_user() {
		ConfirmSignUpResponse response = cognito.confirmationSignUp(client, CLIENT_ID_PROMOCAO, "douglas.silverio@mili.com.br", "746381");

		Assertions.assertNotNull(response);
		System.out.println("test_create_new_user: " + response.toString());
	}

	@Test
	void test_resend_confirmation_code() {
		ResendConfirmationCodeResponse response = cognito.resendConfirmationCode(client, CLIENT_ID_PROMOCAO, EMAIL);

		Assertions.assertNotNull(response);
		System.out.println("test_resend_confirmation_code: Method of delivery is "+response.codeDeliveryDetails().deliveryMediumAsString());
	}


	@Test
	void test_init_auth_pool_live_demo() {

		String clientId = CLIENT_ID_PROMOCAO;
		String user = EMAIL;
		String pass = PASS;

		cognito.initateAuth(client, clientId, "douglas.silverio@mili.com.br", pass);

	}

	@Test
	void test_init_auth_pool_my_user_pool() {

		String clientId = CLIENT_ID_PROMOCAO;
		String user = EMAIL;
		String pass =  PASS;
//		String user = "felipe.alves";
//		String pass = "PassUpdated22#";

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

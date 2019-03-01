package com.holonplatform.example.test;

import static com.holonplatform.example.Product.DESCRIPTION;
import static com.holonplatform.example.Product.PRODUCT;
import static com.holonplatform.example.Product.SKU;

import java.net.URI;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.holonplatform.auth.Authentication;
import com.holonplatform.auth.jwt.JwtConfiguration;
import com.holonplatform.auth.jwt.JwtTokenBuilder;
import com.holonplatform.core.property.PropertyBox;
import com.holonplatform.example.Application;
import com.holonplatform.http.rest.RequestEntity;
import com.holonplatform.http.rest.RestClient;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TestClient {

	@LocalServerPort
	private int serverPort;

	@Autowired
	private JwtConfiguration jwtConfiguration;

	@Test
	public void testMultiTenancy() {

		RestClient client = RestClient.forTarget("http://localhost:" + serverPort + "/api/");

		final PropertyBox product1 = PropertyBox.builder(PRODUCT).set(DESCRIPTION, "Product 1")
				.set(SKU, "tenant1-product1").build();

		// Build JWT using tenant1 as tenant id
		String jwt = JwtTokenBuilder.get().buildJwt(jwtConfiguration,
				Authentication.builder("aSubject").withParameter(Application.TENANT_ID_JWT_CLAIM, "tenant1").build());

		// [tenant1] add using POST
		URI location = client.request().path("products") //
				.authorizationBearer(jwt) // set JWT bearer with tenant id claim
				.postForLocation(RequestEntity.json(product1)).orElseThrow(() -> new RuntimeException("Missing URI"));

		// [tenant1] get the product
		PropertyBox created = client.request().target(location) //
				.authorizationBearer(jwt) // set JWT bearer with tenant id claim
				.propertySet(PRODUCT).getForEntity(PropertyBox.class)
				.orElseThrow(() -> new RuntimeException("Missing product"));

		Assert.assertEquals("tenant1-product1", created.getValue(SKU));

		// Build JWT using tenant2 as tenant id
		jwt = JwtTokenBuilder.get().buildJwt(jwtConfiguration,
				Authentication.builder("aSubject").withParameter(Application.TENANT_ID_JWT_CLAIM, "tenant2").build());

		// [tenant2] get all products
		List<PropertyBox> values = client.request().path("products") //
				.authorizationBearer(jwt) // set JWT bearer with tenant id claim
				.propertySet(PRODUCT).getAsList(PropertyBox.class);

		Assert.assertEquals(1, values.size());

		final PropertyBox product2 = PropertyBox.builder(PRODUCT).set(DESCRIPTION, "Product 1")
				.set(SKU, "tenant2-product1").build();

		// [tenant2] add using POST
		location = client.request().path("products") //
				.authorizationBearer(jwt) // set JWT bearer with tenant id claim
				.postForLocation(RequestEntity.json(product2)).orElseThrow(() -> new RuntimeException("Missing URI"));

		// [tenant2] get the product
		created = client.request().target(location) //
				.authorizationBearer(jwt) // set JWT bearer with tenant id claim
				.propertySet(PRODUCT).getForEntity(PropertyBox.class)
				.orElseThrow(() -> new RuntimeException("Missing product"));

		Assert.assertEquals("tenant2-product1", created.getValue(SKU));

	}

}

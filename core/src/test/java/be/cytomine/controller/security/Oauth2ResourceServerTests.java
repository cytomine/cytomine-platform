package be.cytomine.controller.security;

/*
 * Copyright (c) 2009-2022. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.CytomineCoreApplication;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.utils.AuthenticationSuccessListener;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class Oauth2ResourceServerTests {

    @Autowired
    private MockMvc allProtectedMockMvc;

    @Autowired
    AuthenticationSuccessListener authenticationSuccessListener;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private UserRepository userRepository;

    private static final WireMockServer wireMockServer = new WireMockServer(8888);

    private static RSAKey rsaKey;

    private static final String KEY_ID = "some random string";

    public static void configureWireMock(WireMockServer wireMockServer) throws JOSEException {
        rsaKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(new Algorithm("RS256"))
                .keyID(KEY_ID)
                .generate();

        RSAKey rsaPublicJWK = rsaKey.toPublicJWK();
        String jwkResponse = String.format("{\"keys\": [%s]}", rsaPublicJWK.toJSONString());

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlMatching("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jwkResponse)));


    }

    @BeforeAll
    public static void beforeAll() throws JOSEException {
        configureWireMock(wireMockServer);
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception ignored) {
        }
    }

    @Test
    public void whenNoTokenProvided_thenUnauthorized() throws Exception {
        allProtectedMockMvc.perform(get("/api/project/45.json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenInvalidTokenProvided_thenUnauthorized() throws Exception {
        allProtectedMockMvc.perform(get("/api/project/45.json")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenValidTokenProvided_thenNotFoundAsOk() throws Exception {
        // get a valid cytomine access token using password grant from iam microservice
        allProtectedMockMvc.perform(get("/api/project/45.json")
                        .header("Authorization", "Bearer " + getSignedNotExpiredJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void whenExpiredTokenProvided_thenUnauthorized() throws Exception {
        // get a valid cytomine access token using password grant from iam microservice
        allProtectedMockMvc.perform(get("/api/project/45.json")
                        .header("Authorization", "Bearer " + getSignedExpiredJwt()))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void whenAuthenticationSuccessEventPublished_thenUserAddedIfDoesNotExist() {
        // user not already in the database
        String sub = UUID.randomUUID().toString();
        Assertions.assertTrue(userRepository.findByReference(sub).isEmpty());

        // given a valid token authentication
        String tokenString = "eyJhbGciOiJSUzI1NiIsInI1ojGFg";
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(10, ChronoUnit.MINUTES);
        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", "6mTJJxdp_uUSC9eK8vuCrmMQ1WvgN2oG5xh4GGSskDg");
        headers.put("typ", "JWT");
        headers.put("alg", "RS256");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("name", "Some User");
        claims.put("preferred_username", "test_user_from_token");
        Jwt jwt = new Jwt(tokenString, issuedAt, expiresAt, headers, claims);
        List<GrantedAuthority> authorities = new ArrayList<>();
        GrantedAuthority userRole = new SimpleGrantedAuthority("ROLE_USER");
        authorities.add(userRole);
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, authorities);
        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(jwtAuthenticationToken));

        // assert a user with this sub is created
        Assertions.assertTrue(userRepository.findByReference(sub).isPresent());
    }

    private String getSignedNotExpiredJwt() throws Exception {
        return getSignedJwt(Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    private String getSignedExpiredJwt() throws Exception {
        return getSignedJwt(Instant.now().minus(10, ChronoUnit.MINUTES));
    }

    private String getSignedJwt(Instant expiresAt) throws Exception {

        RSASSASigner signer = new RSASSASigner(rsaKey);
        Instant issuedAt = Instant.now();
        Map<String, Object> resourceAccessClaim = new HashMap<>();
        Map<String, Object> resource = new HashMap<>();
        List<String> resourceRoles = List.of("ADMIN");
        resource.put("roles", resourceRoles);
        resourceAccessClaim.put("core" , resource);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .issuer("http://localhost:8888/")
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(issuedAt))
                .claim("iss", "http://localhost:8888/")
                .claim("sub", UUID.randomUUID())
                .claim("name", "Some User")
                .claim("preferred_username", "test_user_from_token")
                .claim("resource_access" , resourceAccessClaim)
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .build(), claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

}

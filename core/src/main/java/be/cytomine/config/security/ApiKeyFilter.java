package be.cytomine.config.security;

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

import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AuthenticationException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
public class ApiKeyFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final UserRepository secUserRepository;


    public ApiKeyFilter(UserRepository secUserRepository) {
        this.secUserRepository = secUserRepository;
    }

    public static String generateKeys(String method, String content_md5, String content_type, String date, String queryString, String path, User user) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String canonicalHeaders = method + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n";
        String canonicalExtensionHeaders = "";
        String canonicalResource = path + queryString;
        String messageToSign = canonicalHeaders + canonicalExtensionHeaders + canonicalResource;

        String key = user.getPrivateKey();
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes());

        // base64-encode the hmac
        byte[] signatureBytes = Base64.encodeBase64(rawHmac);

        return new String(signatureBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        tryAPIAuthentification(request);
        filterChain.doFilter(request, response);
    }

    private boolean tryAPIAuthentification(HttpServletRequest request) {
        // http://code.google.com/apis/storage/docs/reference/v1/developer-guidev1.html#authentication
        if (request.getHeader("date") == null) {
            return false;
        }
        if (request.getHeader("host") == null) {
            return false;
        }
        String authorization = request.getHeader("authorization");
        if (authorization == null) {
            return false;
        }
        if (!authorization.startsWith("CYTOMINE") || !authorization.contains(" ") || !authorization.contains(":")) {
            return false;
        }
        try {

            String content_md5 = (request.getHeader("content-MD5") != null) ? request.getHeader("content-MD5") : "";

            String content_type = (request.getHeader("content-type") != null) ? request.getHeader("content-type") : "";
            content_type = (request.getHeader("Content-Type") != null) ? request.getHeader("Content-Type") : content_type;
            String date = (request.getHeader("date") != null) ? request.getHeader("date") : "";

            String queryString = (request.getQueryString() != null) ? "?" + request.getQueryString() : "";

            String path = request.getRequestURI();

            String accessKey = authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":"));
            String authorizationSign = authorization.substring(authorization.indexOf(":") + 1);

            Optional<User> user = secUserRepository.findByPublicKeyAndEnabled(accessKey,true);

            if (user.isEmpty()) {
                log.debug("User cannot be extracted with accessKey {}", accessKey);
                throw new AuthenticationException("User cannot be extracted with accessKey " + accessKey);
            } else {
                String signature = generateKeys(request.getMethod(),content_md5, content_type,date,queryString,path,user.get());
                if (authorizationSign.equals(signature)) {
                    this.reauthenticate(user.get());
                    return true;
                } else {
                    // the java client does not set content-type, so we override the header to application/json BEFORE this authentication.
                    // So the client thinks content-type is "" while spring boot set it to application/json. In order to match the client signature, we generate it
                    // with an empty value.
                    // => it would be better to improve the java client to set a valid content type.
                    String signatureWithEmptyContentType = generateKeys(request.getMethod(),content_md5, "",date,queryString,path,user.get());
                    if (authorizationSign.equals(signatureWithEmptyContentType)) {
                        this.reauthenticate(user.get());
                        return true;
                    }


                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Rebuild an Authentication for the given username and register it in the security context.
     * Typically used after updating a user's authorities or other auth-cached info.
     * <p/>
     * Also removes the user from the user cache to force a refresh at next login.
     */
    private void reauthenticate(final User secUser) {
        UserDetails userDetails = createSpringSecurityUser(secUser);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        usernamePasswordAuthenticationToken.setDetails(secUser);

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(User user) {
        if (!user.getEnabled()) {
            throw new ForbiddenException("User with access key " + user.getPublicKey() + "is not enabled.");
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                "null",
                user.getRoles().stream().map(x -> new SimpleGrantedAuthority(x.getAuthority())).collect(Collectors.toList())
        );
    }
}

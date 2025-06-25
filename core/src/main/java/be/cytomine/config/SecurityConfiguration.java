package be.cytomine.config;

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

import be.cytomine.config.security.ApiKeyFilter;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.utils.JwtAuthConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JwtAuthConverter customJwtAuthConverter;
    private final UserRepository userRepository;

    public SecurityConfiguration(UserRepository userRepository, JwtAuthConverter customJwtAuthConverter) {
        this.userRepository = userRepository;
        this.customJwtAuthConverter = customJwtAuthConverter;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ApiKeyFilter(userRepository), BasicAuthenticationFilter.class) // Deprecated. Kept as transitional in 2024.2
                .exceptionHandling((exceptionHandling) ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .authorizeHttpRequests((authorizeHttpRequests) ->
                        authorizeHttpRequests
                                .requestMatchers(new AntPathRequestMatcher("/api/abstractimage/**")).permitAll()
                                .requestMatchers(new AntPathRequestMatcher("/api/imageinstance/**")).permitAll()
                                .requestMatchers(new AntPathRequestMatcher("/api/userannotation/**")).permitAll()
                                .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                                .requestMatchers("/session/admin/info.json").authenticated()
                                .requestMatchers("/session/admin/open.json").authenticated()
                                .requestMatchers("/session/admin/close.json").authenticated()

                                .requestMatchers(HttpMethod.GET, "/server/ping").permitAll() // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
                                .requestMatchers(HttpMethod.GET, "/server/ping.json").permitAll() // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
                                .requestMatchers(HttpMethod.POST, "/server/ping").permitAll() // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
                                .requestMatchers(HttpMethod.POST, "/server/ping.json").permitAll() // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
                                .requestMatchers(new AntPathRequestMatcher("/**")).permitAll() // TODO IAM: remove ?
                );
        http.oauth2ResourceServer((oauth2) -> oauth2
                .jwt(jwtAuthConverter -> jwtAuthConverter.jwtAuthenticationConverter(customJwtAuthConverter)));
        return http.build();
    }
}



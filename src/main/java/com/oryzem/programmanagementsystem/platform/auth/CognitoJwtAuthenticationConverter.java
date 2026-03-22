package com.oryzem.programmanagementsystem.platform.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String USERNAME_CLAIM = "cognito:username";

    private final JwtAuthenticationConverter delegate;

    public CognitoJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        scopeAuthoritiesConverter.setAuthorityPrefix("SCOPE_");
        scopeAuthoritiesConverter.setAuthoritiesClaimName("scope");

        this.delegate = new JwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(jwt -> mergeAuthorities(jwt, scopeAuthoritiesConverter));
        this.delegate.setPrincipalClaimName(USERNAME_CLAIM);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        return delegate.convert(source);
    }

    private Collection<GrantedAuthority> mergeAuthorities(
            Jwt jwt,
            Converter<Jwt, Collection<GrantedAuthority>> scopeAuthoritiesConverter) {

        Set<GrantedAuthority> mergedAuthorities = new LinkedHashSet<>(scopeAuthoritiesConverter.convert(jwt));
        mergedAuthorities.addAll(extractGroupAuthorities(jwt));
        return mergedAuthorities;
    }

    private Collection<GrantedAuthority> extractGroupAuthorities(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList(GROUPS_CLAIM);
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>(groups.size());
        for (String group : groups) {
            if (group == null || group.isBlank()) {
                continue;
            }

            String normalizedGroup = group.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedGroup));
        }
        return authorities;
    }
}

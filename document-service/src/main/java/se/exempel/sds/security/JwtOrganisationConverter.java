package se.exempel.sds.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

/**
 * Converts a validated {@link Jwt} into a Spring Security authentication token
 * that carries a {@link OrganisationPrincipal} as its principal.
 *
 * <p>Expected JWT claims:
 * <ul>
 *   <li>{@code sub}       – user identifier (standard)</li>
 *   <li>{@code organisation_id} – required; absence is treated as an authentication failure</li>
 *   <li>{@code roles}     – optional list of role strings, mapped to {@code ROLE_<name>} authorities</li>
 * </ul>
 *
 * <p>The converter also populates {@link OrganisationContext} so downstream components
 * (services, entity listeners) can access the principal without receiving it
 * as a method parameter.
 */
public class JwtOrganisationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String organisationId = jwt.getClaim("organisation_id");

        if (organisationId == null || organisationId.isBlank()) {
            throw new BadCredentialsException(
                    "JWT is missing the required 'organisation_id' claim");
        }

        List<GrantedAuthority> authorities = Optional
                .ofNullable(jwt.getClaimAsStringList("roles"))
                .orElse(List.of())
                .stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        OrganisationPrincipal principal = new OrganisationPrincipal(jwt.getSubject(), organisationId, authorities);

        // Populate ThreadLocal so services can call OrganisationContext.get() directly.
        // Cleared by SecurityConfig#organisationContextClearFilter after the response is written.
        OrganisationContext.set(principal);

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}

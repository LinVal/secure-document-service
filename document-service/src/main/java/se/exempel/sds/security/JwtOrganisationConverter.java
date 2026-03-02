package se.exempel.sds.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Converts a validated {@link Jwt} into a Spring Security authentication token
 * that carries a {@link OrganisationPrincipal} as its principal.
 */
public class JwtOrganisationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        //
        // TODO (Uppgift A)
        //
        // Bygg ett UsernamePasswordAuthenticationToken med en OrganisationPrincipal som principal.
        //
        // Claims att läsa från jwt:
        //   jwt.getSubject()                       – användarens ID  (sub-claim)
        //   jwt.getClaim("organisation_id")         – organisationens ID (obligatorisk)
        //   jwt.getClaimAsStringList("roles")       – lista med rollnamn, t.ex. ["USER", "ADMIN"]
        //
        // Önskat resultat:
        //   - Saknas eller är organisation_id tom ska BadCredentialsException kastas.
        //   - Roller mappas till GrantedAuthority med prefixet "ROLE_"
        //     t.ex. "ADMIN" → new SimpleGrantedAuthority("ROLE_ADMIN")
        //   - Anropa OrganisationContext.set(principal) efter att principal skapats.
        //   - Returnera new UsernamePasswordAuthenticationToken(principal, jwt, authorities)
        throw new UnsupportedOperationException("Not implemented – se Uppgift A");
    }
}

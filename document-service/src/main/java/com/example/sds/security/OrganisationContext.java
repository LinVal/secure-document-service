package com.example.sds.security;

/**
 * ThreadLocal holder for the current request's OrganisationPrincipal.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Set by {@link JwtOrganisationConverter} during authentication.</li>
 *   <li>Cleared by the cleanup filter registered in {@link SecurityConfig}
 *       in a {@code finally} block after the response is written.</li>
 * </ol>
 *
 * <p>Using {@link ThreadLocal#remove()} (not {@code set(null)}) prevents
 * memory leaks in thread-pool environments such as Tomcat.
 */
public final class OrganisationContext {

    private static final ThreadLocal<OrganisationPrincipal> HOLDER = new ThreadLocal<>();

    private OrganisationContext() {}

    public static void set(OrganisationPrincipal principal) {
        HOLDER.set(principal);
    }

    /**
     * Returns the current principal.
     *
     * @throws IllegalStateException if called outside an authenticated request.
     */
    public static OrganisationPrincipal get() {
        OrganisationPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new IllegalStateException(
                    "No OrganisationPrincipal bound to the current thread – " +
                    "is this being called outside a JWT-authenticated request?");
        }
        return principal;
    }

    /** Must be called at end of every request to avoid ThreadLocal leaks. */
    public static void clear() {
        HOLDER.remove();
    }
}

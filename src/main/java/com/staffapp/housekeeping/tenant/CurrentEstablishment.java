package com.staffapp.housekeeping.tenant;

/**
 * Holds the establishment the current request works on, set by {@link TenantFilter}.
 */
public final class CurrentEstablishment {

    private static final ThreadLocal<Long> ESTABLISHMENT_ID = new ThreadLocal<>();

    private CurrentEstablishment() {
    }

    public static Long getId() {
        var id = ESTABLISHMENT_ID.get();
        if (id == null) {
            throw new IllegalStateException("No establishment in the current request");
        }
        return id;
    }

    static void set(Long id) {
        ESTABLISHMENT_ID.set(id);
    }

    static void clear() {
        ESTABLISHMENT_ID.remove();
    }
}

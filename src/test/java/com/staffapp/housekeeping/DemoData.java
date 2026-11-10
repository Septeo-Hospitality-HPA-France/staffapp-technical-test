package com.staffapp.housekeeping;

/**
 * Ids of the demo data loaded by {@code V2__insert_demo_data.sql}.
 */
public final class DemoData {

    /** Camping Les Pins: rooms A01 to A10 (ids 1 to 10), alice and bob (STAFF), marc (MANAGER). */
    public static final Long ESTABLISHMENT_A = 1L;

    /** Camping La Plage: rooms B01 to B10 (ids 11 to 20), claire (STAFF), nina (MANAGER). */
    public static final Long ESTABLISHMENT_B = 2L;

    /** Rooms A01 and A02 of Camping Les Pins, B05 of Camping La Plage. */
    public static final Long ROOM_A01 = 1L;
    public static final Long ROOM_A02 = 2L;
    public static final Long ROOM_B05 = 15L;

    /** Staff ids, in the order of the demo data. */
    public static final Long ALICE = 1L;
    public static final Long BOB = 2L;
    public static final Long CLAIRE = 4L;

    private DemoData() {
    }
}

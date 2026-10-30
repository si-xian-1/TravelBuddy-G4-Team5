package com.TravelBuddy.acceptance;

/** Runs shared requirements independently for hotel records. */
class HotelAcceptanceTest extends ItemAcceptanceContract {
    @Override Item item() { return Item.HOTEL; }
}

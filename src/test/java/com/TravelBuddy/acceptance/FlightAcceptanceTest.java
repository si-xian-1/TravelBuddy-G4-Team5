package com.TravelBuddy.acceptance;

/** Runs shared requirements independently for flight records. */
class FlightAcceptanceTest extends ItemAcceptanceContract {
    @Override Item item() { return Item.FLIGHT; }
}

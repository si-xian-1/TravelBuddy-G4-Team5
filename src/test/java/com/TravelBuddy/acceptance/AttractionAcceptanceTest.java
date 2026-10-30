package com.TravelBuddy.acceptance;

/** Runs shared requirements independently for attraction records. */
class AttractionAcceptanceTest extends ItemAcceptanceContract {
    @Override Item item() { return Item.ATTRACTION; }
}

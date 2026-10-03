package com.ridelink.account.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    @SuppressWarnings("deprecation")
    void legacyCustomer_isNormalisedToPassenger_otherRolesAreUnchanged() {
        assertThat(Role.CUSTOMER.normalised()).isEqualTo(Role.PASSENGER);
        assertThat(Role.PASSENGER.normalised()).isEqualTo(Role.PASSENGER);
        assertThat(Role.DRIVER.normalised()).isEqualTo(Role.DRIVER);
    }

    @Test
    void accountWithoutStoredStatus_getsTheRegistrationDefault() {
        Account driver = new Account("1", "d@x.lk", "D", null, "h", Role.DRIVER, null, null, null);
        Account passenger = new Account("2", "p@x.lk", "P", null, "h", Role.PASSENGER, null, null, null);

        assertThat(driver.effectiveStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(passenger.effectiveStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}

package com.ridelink.driver.service;

import com.ridelink.driver.client.AccountClient;
import com.ridelink.driver.client.AccountSummary;
import com.ridelink.driver.dto.AvailableDriverResponse;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.exception.BusinessRuleException;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.AvailabilityStatus;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.Location;
import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final AuthenticatedUser DRIVER_1 = new AuthenticatedUser("acc-1", "kasun@ridelink.test", "DRIVER");
    private static final AuthenticatedUser DRIVER_2 = new AuthenticatedUser("acc-2", "other@ridelink.test", "DRIVER");
    private static final AuthenticatedUser RIDE_SERVICE = new AuthenticatedUser("ride-management-service", null, "SERVICE");
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser("admin", null, "ADMIN");

    @Mock private DriverRepository driverRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private AccountClient accountClient;

    private DriverService driverService;

    private Driver driver;

    @BeforeEach
    void setUp() {
        driverService = new DriverService(driverRepository, vehicleRepository, accountClient,
                Duration.ofMinutes(30), Clock.fixed(NOW, ZoneOffset.UTC));
        driver = new Driver();
        driver.setId("d1");
        driver.setAccountId("acc-1");
        driver.setAvailability(AvailabilityStatus.OFFLINE);
    }

    // ---- profile creation (calls Account Service) ----

    @Test
    void createProfile_succeeds_whenAccountIsAnApprovedDriverAccount() {
        CreateDriverRequest request = new CreateDriverRequest("acc-1", "LIC-001", "Colombo");
        when(driverRepository.existsByAccountId("acc-1")).thenReturn(false);
        when(accountClient.fetchAccount("acc-1", "Bearer token")).thenReturn(
                new AccountSummary("acc-1", "a@b.com", "DRIVER", "ACTIVE"));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        Driver result = driverService.createProfile(request, DRIVER_1, "Bearer token");

        assertThat(result.getAccountId()).isEqualTo("acc-1");
        assertThat(result.getAvailability()).isEqualTo(AvailabilityStatus.OFFLINE);
        verify(accountClient).fetchAccount("acc-1", "Bearer token");
    }

    @Test
    void createProfile_defaultsAccountId_toTheCallingDriver() {
        when(driverRepository.existsByAccountId("acc-1")).thenReturn(false);
        when(accountClient.fetchAccount("acc-1", "Bearer token")).thenReturn(
                new AccountSummary("acc-1", "a@b.com", "DRIVER", "ACTIVE"));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        Driver result = driverService.createProfile(new CreateDriverRequest(null, "LIC-001", " Negombo "),
                DRIVER_1, "Bearer token");

        assertThat(result.getAccountId()).isEqualTo("acc-1");
        assertThat(result.getServiceArea()).isEqualTo("Negombo");
    }

    @Test
    void createProfile_rejects_duplicateAccount() {
        CreateDriverRequest request = new CreateDriverRequest("acc-1", "LIC-001", "Colombo");
        when(driverRepository.existsByAccountId("acc-1")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createProfile(request, DRIVER_1, "Bearer token"))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(accountClient);
    }

    @Test
    void createProfile_rejects_accountWithoutDriverRole() {
        CreateDriverRequest request = new CreateDriverRequest("acc-1", "LIC-001", "Colombo");
        when(driverRepository.existsByAccountId("acc-1")).thenReturn(false);
        when(accountClient.fetchAccount("acc-1", "Bearer token")).thenReturn(
                new AccountSummary("acc-1", "a@b.com", "PASSENGER", "ACTIVE"));

        assertThatThrownBy(() -> driverService.createProfile(request, DRIVER_1, "Bearer token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRIVER role");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void createProfile_rejects_driverAccountPendingApproval() {
        when(driverRepository.existsByAccountId("acc-1")).thenReturn(false);
        when(accountClient.fetchAccount("acc-1", "Bearer token")).thenReturn(
                new AccountSummary("acc-1", "a@b.com", "DRIVER", "PENDING"));

        assertThatThrownBy(() -> driverService.createProfile(new CreateDriverRequest(null, "LIC-001", "Colombo"),
                DRIVER_1, "Bearer token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("approve");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void createProfile_rejects_driverCreatingAProfileForSomeoneElse() {
        assertThatThrownBy(() -> driverService.createProfile(new CreateDriverRequest("acc-2", "LIC-001", "Colombo"),
                DRIVER_1, "Bearer token"))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(accountClient, driverRepository);
    }

    // ---- availability rules ----

    @Test
    void updateAvailability_rejects_goingAvailable_withoutActiveVehicle() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId("d1")).thenReturn(List.of());

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.AVAILABLE, DRIVER_1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active vehicle");
    }

    @Test
    void updateAvailability_rejects_goingAvailable_withoutLocation() {
        Vehicle activeVehicle = new Vehicle();
        activeVehicle.setActive(true);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId("d1")).thenReturn(List.of(activeVehicle));

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.AVAILABLE, DRIVER_1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("location");
    }

    @Test
    void updateAvailability_succeeds_whenVehicleAndLocationArePresent() {
        driver.setCurrentLocation(new Location(6.9271, 79.8612));
        Vehicle activeVehicle = new Vehicle();
        activeVehicle.setActive(true);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId("d1")).thenReturn(List.of(activeVehicle));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        Driver result = driverService.updateAvailability("d1", AvailabilityStatus.AVAILABLE, DRIVER_1);

        assertThat(result.getAvailability()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void updateAvailability_rejects_onTripToOffline() {
        driver.setAvailability(AvailabilityStatus.ON_TRIP);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.OFFLINE, DRIVER_1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ON_TRIP");
    }

    @Test
    void updateAvailability_onTrip_canOnlyBeSetByTheRideService() {
        driver.setAvailability(AvailabilityStatus.AVAILABLE);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.ON_TRIP, DRIVER_1))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void updateAvailability_rideService_reservesAnAvailableDriver_andLaterReleasesThem() {
        driver.setAvailability(AvailabilityStatus.AVAILABLE);
        driver.setCurrentLocation(new Location(7.2087, 79.8358));
        Vehicle activeVehicle = new Vehicle();
        activeVehicle.setActive(true);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId("d1")).thenReturn(List.of(activeVehicle));
        when(driverRepository.reserveIfAvailable(eq("d1"), any(Instant.class))).thenReturn(true);

        assertThat(driverService.updateAvailability("d1", AvailabilityStatus.ON_TRIP, RIDE_SERVICE).getAvailability())
                .isEqualTo(AvailabilityStatus.ON_TRIP);
        assertThat(driverService.updateAvailability("d1", AvailabilityStatus.AVAILABLE, RIDE_SERVICE).getAvailability())
                .isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void updateAvailability_reservationRaceLost_isConflict() {
        driver.setAvailability(AvailabilityStatus.AVAILABLE);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(driverRepository.reserveIfAvailable(eq("d1"), any(Instant.class))).thenReturn(false);

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.ON_TRIP, RIDE_SERVICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("just reserved by another ride");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void updateAvailability_rejects_reservingADriverWhoIsNotAvailable() {
        driver.setAvailability(AvailabilityStatus.OFFLINE);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.ON_TRIP, RIDE_SERVICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be reserved");
    }

    @Test
    void updateAvailability_driverOnTrip_cannotReleaseThemselves() {
        driver.setAvailability(AvailabilityStatus.ON_TRIP);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.AVAILABLE, DRIVER_1))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("released automatically");
    }

    @Test
    void updateAvailability_rejects_anotherDriversProfile() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.updateAvailability("d1", AvailabilityStatus.AVAILABLE, DRIVER_2))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(driverRepository, never()).save(any());
    }

    @Test
    void updateAvailability_sameStatus_isANoOp() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThat(driverService.updateAvailability("d1", AvailabilityStatus.OFFLINE, ADMIN).getAvailability())
                .isEqualTo(AvailabilityStatus.OFFLINE);
        verify(driverRepository, never()).save(any());
    }

    // ---- location and eligibility ----

    @Test
    void updateLocation_storesPositionAndTimestamp() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        Driver result = driverService.updateLocation("d1", new UpdateLocationRequest(7.2087, 79.8358), DRIVER_1);

        assertThat(result.getCurrentLocation().getLatitude()).isEqualTo(7.2087);
        assertThat(result.getLocationUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void findEligibleDrivers_keepsOnlyFreshDriversWithAMatchingActiveVehicle() {
        Driver fresh = available("d1", NOW.minusSeconds(60));
        Driver stale = available("d2", NOW.minus(Duration.ofHours(2)));
        Driver noMatchingVehicle = available("d3", NOW.minusSeconds(30));
        when(driverRepository.findByAvailabilityAndServiceAreaIgnoreCase(AvailabilityStatus.AVAILABLE, "Negombo"))
                .thenReturn(List.of(fresh, stale, noMatchingVehicle));
        Vehicle sedan = new Vehicle();
        sedan.setId("v1");
        sedan.setDriverId("d1");
        sedan.setPlateNumber("CAB-1234");
        sedan.setVehicleType(VehicleType.SEDAN);
        sedan.setSeats(4);
        when(vehicleRepository.findByDriverIdInAndVehicleTypeAndActiveTrue(List.of("d1", "d3"), VehicleType.SEDAN))
                .thenReturn(List.of(sedan));

        List<AvailableDriverResponse> result = driverService.findEligibleDrivers(" Negombo ", VehicleType.SEDAN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).driverId()).isEqualTo("d1");
        assertThat(result.get(0).accountId()).isEqualTo("acc-d1");
        assertThat(result.get(0).plateNumber()).isEqualTo("CAB-1234");
        assertThat(result.get(0).vehicleId()).isEqualTo("v1");
    }

    @Test
    void findEligibleDrivers_returnsEmpty_whenNobodyIsAvailable() {
        when(driverRepository.findByAvailabilityAndServiceAreaIgnoreCase(AvailabilityStatus.AVAILABLE, "Jaffna"))
                .thenReturn(List.of());

        assertThat(driverService.findEligibleDrivers("Jaffna", VehicleType.VAN)).isEmpty();
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void getById_throws_whenDriverMissing() {
        when(driverRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyProfile_throws_whenDriverHasNoProfileYet() {
        when(driverRepository.findByAccountId("acc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getMyProfile(DRIVER_1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("POST /api/v1/drivers");
    }

    private static Driver available(String id, Instant locationAt) {
        Driver d = new Driver();
        d.setId(id);
        d.setAccountId("acc-" + id);
        d.setServiceArea("Negombo");
        d.setAvailability(AvailabilityStatus.AVAILABLE);
        d.setCurrentLocation(new Location(7.2087, 79.8358));
        d.setLocationUpdatedAt(locationAt);
        return d;
    }
}

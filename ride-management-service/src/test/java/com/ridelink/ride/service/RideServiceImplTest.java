package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverClient;
import com.ridelink.ride.client.FareClient;
import com.ridelink.ride.client.dto.AvailableDriver;
import com.ridelink.ride.client.dto.FareQuote;
import com.ridelink.ride.client.dto.FinalFare;
import com.ridelink.ride.client.dto.FinalFareRequest;
import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.LocationDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.DownstreamServiceException;
import com.ridelink.ride.exception.InvalidRequestException;
import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.exception.NoDriverAvailableException;
import com.ridelink.ride.exception.RideAccessDeniedException;
import com.ridelink.ride.exception.RideConflictException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.matching.NearestDriverSelector;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.model.VehicleType;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.AuthenticatedUser;
import com.ridelink.ride.service.impl.RideServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final AuthenticatedUser PASSENGER = new AuthenticatedUser("rider-101", "n@ridelink.test", "PASSENGER");
    private static final AuthenticatedUser OTHER_PASSENGER = new AuthenticatedUser("rider-999", null, "PASSENGER");
    private static final AuthenticatedUser DRIVER = new AuthenticatedUser("acc-driver-55", "k@ridelink.test", "DRIVER");
    private static final AuthenticatedUser OTHER_DRIVER = new AuthenticatedUser("acc-driver-77", null, "DRIVER");
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser("admin", null, "ADMIN");

    @Mock
    private RideRepository rideRepository;
    @Mock
    private DriverClient driverClient;
    @Mock
    private FareClient fareClient;

    private RideServiceImpl rideService;

    @BeforeEach
    void setUp() {
        rideService = new RideServiceImpl(rideRepository, driverClient, fareClient, new NearestDriverSelector(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ------------------------------------------------------------------ create

    @Test
    void createRide_shouldCreateRide_withServerSideFareEstimate() {
        when(rideRepository.existsByRiderIdAndStatusIn(ArgumentMatchers.eq("rider-101"),
                ArgumentMatchers.anyCollection())).thenReturn(false);
        when(fareClient.estimate(any(Location.class), any(Location.class), eq(VehicleType.SEDAN)))
                .thenReturn(new FareQuote(new BigDecimal("8.05"), 20, new BigDecimal("1326.00"), "LKR"));
        stubRideSave();

        RideResponse response = rideService.createRide(createRequest(), PASSENGER);

        assertThat(response.getId()).isEqualTo("ride-123");
        assertThat(response.getRiderId()).isEqualTo("rider-101");
        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(response.getEstimatedFare()).isEqualTo(1326.00);
        assertThat(response.getEstimatedDistanceKm()).isEqualTo(8.05);
        assertThat(response.getCurrency()).isEqualTo("LKR");
        assertThat(response.getRequestedAt()).isEqualTo(NOW);
    }

    @Test
    void createRide_rejectsRiderWithAnActiveRide() {
        when(rideRepository.existsByRiderIdAndStatusIn(ArgumentMatchers.eq("rider-101"),
                ArgumentMatchers.anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> rideService.createRide(createRequest(), PASSENGER))
                .isInstanceOf(RideConflictException.class)
                .hasMessageContaining("already has an active ride");
        verifyNoInteractions(fareClient);
    }

    @Test
    void createRide_rejectsIdenticalPickupAndDestination() {
        when(rideRepository.existsByRiderIdAndStatusIn(anyString(), ArgumentMatchers.anyCollection())).thenReturn(false);
        CreateRideRequest request = createRequest();
        request.setDestinationLocation(request.getPickupLocation());

        assertThatThrownBy(() -> rideService.createRide(request, PASSENGER))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(fareClient);
    }

    @Test
    void createRide_whenFareServiceIsDown_failsWithoutSavingAnything() {
        when(rideRepository.existsByRiderIdAndStatusIn(anyString(), ArgumentMatchers.anyCollection())).thenReturn(false);
        when(fareClient.estimate(any(), any(), any()))
                .thenThrow(new DownstreamServiceException("Fare & Payment Service is unavailable"));

        assertThatThrownBy(() -> rideService.createRide(createRequest(), PASSENGER))
                .isInstanceOf(DownstreamServiceException.class);
        verify(rideRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ assign

    @Test
    void assignDriver_picksTheNearestDriver_andReservesThem() {
        Ride ride = ride(RideStatus.REQUESTED);
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride));
        when(driverClient.findAvailableDrivers("Negombo", VehicleType.SEDAN)).thenReturn(List.of(
                candidate("far", 7.1808, 79.8842), candidate("near", 7.2090, 79.8360)));
        when(rideRepository.existsByDriverIdAndStatusIn(anyString(), ArgumentMatchers.anyCollection())).thenReturn(false);
        when(driverClient.reserve("near")).thenReturn(true);
        stubRideSave();

        RideResponse response = rideService.assignDriver("ride-123", null, PASSENGER);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(response.getDriverId()).isEqualTo("near");
        assertThat(response.getDriverAccountId()).isEqualTo("acc-near");
        assertThat(response.getVehicleNumber()).isEqualTo("CAB-near");
        assertThat(response.getAssignedAt()).isEqualTo(NOW);
        verify(driverClient, never()).reserve("far");
    }

    @Test
    void assignDriver_fallsBackToTheNextDriver_whenTheNearestWasJustTaken() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));
        when(driverClient.findAvailableDrivers("Negombo", VehicleType.SEDAN)).thenReturn(List.of(
                candidate("near", 7.2090, 79.8360), candidate("far", 7.1808, 79.8842)));
        when(rideRepository.existsByDriverIdAndStatusIn(anyString(), ArgumentMatchers.anyCollection())).thenReturn(false);
        doReturn(false).when(driverClient).reserve("near");
        doReturn(true).when(driverClient).reserve("far");
        stubRideSave();

        assertThat(rideService.assignDriver("ride-123", null, PASSENGER).getDriverId()).isEqualTo("far");
    }

    @Test
    void assignDriver_noEligibleDriver_throws_andRideStaysRequested() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));
        when(driverClient.findAvailableDrivers("Negombo", VehicleType.SEDAN)).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.assignDriver("ride-123", null, PASSENGER))
                .isInstanceOf(NoDriverAvailableException.class)
                .hasMessageContaining("No available SEDAN driver in Negombo");
        verify(rideRepository, never()).save(any());
    }

    @Test
    void assignDriver_skipsDriversWhoAlreadyHaveAnActiveRide() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));
        when(driverClient.findAvailableDrivers("Negombo", VehicleType.SEDAN))
                .thenReturn(List.of(candidate("busy", 7.2090, 79.8360)));
        when(rideRepository.existsByDriverIdAndStatusIn(eq("busy"), ArgumentMatchers.anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> rideService.assignDriver("ride-123", null, PASSENGER))
                .isInstanceOf(NoDriverAvailableException.class);
        verify(driverClient, never()).reserve(anyString());
    }

    @Test
    void assignDriver_byAnotherPassenger_isForbidden() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));

        assertThatThrownBy(() -> rideService.assignDriver("ride-123", null, OTHER_PASSENGER))
                .isInstanceOf(RideAccessDeniedException.class);
        verifyNoInteractions(driverClient);
    }

    @Test
    void assignDriver_passengerCannotChooseASpecificDriver() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));

        assertThatThrownBy(() -> rideService.assignDriver("ride-123", new AssignDriverRequest("near"), PASSENGER))
                .isInstanceOf(RideAccessDeniedException.class)
                .hasMessageContaining("administrator");
    }

    @Test
    void assignDriver_adminCanDispatchASpecificEligibleDriver() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));
        when(driverClient.findAvailableDrivers("Negombo", VehicleType.SEDAN)).thenReturn(List.of(
                candidate("near", 7.2090, 79.8360), candidate("far", 7.1808, 79.8842)));
        when(rideRepository.existsByDriverIdAndStatusIn(eq("far"), ArgumentMatchers.anyCollection())).thenReturn(false);
        when(driverClient.reserve("far")).thenReturn(true);
        stubRideSave();

        assertThat(rideService.assignDriver("ride-123", new AssignDriverRequest("far"), ADMIN).getDriverId())
                .isEqualTo("far");
    }

    @Test
    void assignDriver_onAnAlreadyAssignedRide_isAnInvalidTransition() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED)));

        assertThatThrownBy(() -> rideService.assignDriver("ride-123", null, PASSENGER))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("cannot move from ASSIGNED to ASSIGNED");
    }

    // ------------------------------------------------------------------ lifecycle

    @Test
    void acceptRide_byTheAssignedDriver_movesToAccepted() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.ASSIGNED)));
        stubRideSave();

        RideResponse response = rideService.acceptRide("ride-123", DRIVER);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(response.getAcceptedAt()).isEqualTo(NOW);
    }

    @Test
    void acceptRide_byADifferentDriver_isForbidden() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.ASSIGNED)));

        assertThatThrownBy(() -> rideService.acceptRide("ride-123", OTHER_DRIVER))
                .isInstanceOf(RideAccessDeniedException.class);
        verify(rideRepository, never()).save(any());
    }

    @Test
    void startRide_beforeAccepting_isAnInvalidTransition() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.ASSIGNED)));

        assertThatThrownBy(() -> rideService.startRide("ride-123", DRIVER))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("cannot move from ASSIGNED to IN_PROGRESS");
    }

    @Test
    void declineRide_returnsRideToRequested_andReleasesTheDriver() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.ASSIGNED)));
        stubRideSave();

        RideResponse response = rideService.declineRide("ride-123", DRIVER);

        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(response.getDriverId()).isNull();
        verify(driverClient).release("driver-55");
    }

    @Test
    void completeRide_callsFareServiceWithDistanceAndDuration_thenReleasesTheDriver() {
        Ride inProgress = assignedRide(RideStatus.IN_PROGRESS);
        inProgress.setStartedAt(NOW.minusSeconds(14 * 60 + 5));
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(inProgress));
        when(fareClient.calculateFinalFare(any(FinalFareRequest.class)))
                .thenReturn(new FinalFare("pay-1", "ride-123", new BigDecimal("1460.00"), "LKR", "PENDING"));
        stubRideSave();

        RideResponse response = rideService.completeRide("ride-123", new CompleteRideRequest(9.5), DRIVER);

        ArgumentCaptor<FinalFareRequest> sent = ArgumentCaptor.forClass(FinalFareRequest.class);
        verify(fareClient).calculateFinalFare(sent.capture());
        assertThat(sent.getValue().distanceKm()).isEqualTo(9.5);
        assertThat(sent.getValue().durationMinutes()).isEqualTo(15);
        assertThat(sent.getValue().passengerId()).isEqualTo("rider-101");
        assertThat(sent.getValue().driverAccountId()).isEqualTo("acc-driver-55");
        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(response.getFinalFare()).isEqualTo(1460.00);
        assertThat(response.getPaymentId()).isEqualTo("pay-1");
        verify(driverClient).release("driver-55");
    }

    @Test
    void completeRide_withoutDistance_usesTheEstimatedDistance() {
        Ride inProgress = assignedRide(RideStatus.IN_PROGRESS);
        inProgress.setStartedAt(NOW.minusSeconds(30));
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(inProgress));
        when(fareClient.calculateFinalFare(any(FinalFareRequest.class)))
                .thenReturn(new FinalFare("pay-1", "ride-123", new BigDecimal("1170.00"), "LKR", "PENDING"));
        stubRideSave();

        rideService.completeRide("ride-123", null, DRIVER);

        ArgumentCaptor<FinalFareRequest> sent = ArgumentCaptor.forClass(FinalFareRequest.class);
        verify(fareClient).calculateFinalFare(sent.capture());
        assertThat(sent.getValue().distanceKm()).isEqualTo(8.05);
        assertThat(sent.getValue().durationMinutes()).isEqualTo(1);
    }

    @Test
    void completeRide_whenFareServiceIsDown_rideStaysInProgress() {
        Ride inProgress = assignedRide(RideStatus.IN_PROGRESS);
        inProgress.setStartedAt(NOW.minusSeconds(600));
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(inProgress));
        when(fareClient.calculateFinalFare(any())).thenThrow(new DownstreamServiceException("down"));

        assertThatThrownBy(() -> rideService.completeRide("ride-123", null, DRIVER))
                .isInstanceOf(DownstreamServiceException.class);
        verify(rideRepository, never()).save(any());
        verify(driverClient, never()).release(anyString());
        assertThat(inProgress.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    void completeRide_succeeds_evenIfTheDriverCannotBeReleased() {
        Ride inProgress = assignedRide(RideStatus.IN_PROGRESS);
        inProgress.setStartedAt(NOW.minusSeconds(600));
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(inProgress));
        when(fareClient.calculateFinalFare(any()))
                .thenReturn(new FinalFare("pay-1", "ride-123", new BigDecimal("1200.00"), "LKR", "PENDING"));
        stubRideSave();
        doThrow(new DownstreamServiceException("driver service down")).when(driverClient).release("driver-55");

        assertThat(rideService.completeRide("ride-123", null, DRIVER).getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    @Test
    void cancelRide_afterAssignment_recordsWhoCancelled_andReleasesTheDriver() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.ASSIGNED)));
        stubRideSave();

        RideResponse response = rideService.cancelRide("ride-123", new CancelRideRequest("Changed travel plans"),
                PASSENGER);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(response.getCancelledBy()).isEqualTo("PASSENGER");
        assertThat(response.getCancellationReason()).isEqualTo("Changed travel plans");
        verify(driverClient).release("driver-55");
    }

    @Test
    void cancelRide_beforeAssignment_doesNotCallDriverService() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));
        stubRideSave();

        assertThat(rideService.cancelRide("ride-123", null, PASSENGER).getStatus()).isEqualTo(RideStatus.CANCELLED);
        verifyNoInteractions(driverClient);
    }

    @Test
    void cancelRide_onceInProgress_isRejected() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> rideService.cancelRide("ride-123", null, PASSENGER))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cancelRide_byAStranger_isForbidden() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(ride(RideStatus.REQUESTED)));

        assertThatThrownBy(() -> rideService.cancelRide("ride-123", null, OTHER_PASSENGER))
                .isInstanceOf(RideAccessDeniedException.class);
    }

    // ------------------------------------------------------------------ retrieval

    @Test
    void getRideById_unknownId_isNotFound() {
        when(rideRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getRideById("nope", ADMIN)).isInstanceOf(RideNotFoundException.class);
    }

    @Test
    void getRideById_isVisibleToRiderAndAssignedDriver_butNotToOthers() {
        when(rideRepository.findById("ride-123")).thenReturn(Optional.of(assignedRide(RideStatus.ACCEPTED)));

        assertThat(rideService.getRideById("ride-123", PASSENGER).getId()).isEqualTo("ride-123");
        assertThat(rideService.getRideById("ride-123", DRIVER).getId()).isEqualTo("ride-123");
        assertThatThrownBy(() -> rideService.getRideById("ride-123", OTHER_DRIVER))
                .isInstanceOf(RideAccessDeniedException.class);
    }

    @Test
    void getMyRides_forADriver_queriesByDriverAccount() {
        when(rideRepository.findByDriverAccountIdOrderByCreatedAtDesc("acc-driver-55"))
                .thenReturn(List.of(assignedRide(RideStatus.COMPLETED)));

        assertThat(rideService.getMyRides(DRIVER)).hasSize(1);
    }

    // ------------------------------------------------------------------ helpers

    private void stubRideSave() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            if (ride.getId() == null) {
                ride.setId("ride-123");
            }
            return ride;
        });
    }

    private static CreateRideRequest createRequest() {
        return CreateRideRequest.builder()
                .riderName("Nadeesha Perera")
                .riderPhone("+94771234567")
                .serviceArea("Negombo")
                .vehicleType(VehicleType.SEDAN)
                .pickupLocation(LocationDto.builder()
                        .address("Negombo Beach Road")
                        .latitude(7.2087)
                        .longitude(79.8358)
                        .landmark("Near Beach Park")
                        .build())
                .destinationLocation(LocationDto.builder()
                        .address("Bandaranaike International Airport")
                        .latitude(7.1808)
                        .longitude(79.8842)
                        .build())
                .build();
    }

    private static Ride ride(RideStatus status) {
        return Ride.builder()
                .id("ride-123")
                .riderId("rider-101")
                .serviceArea("Negombo")
                .vehicleType(VehicleType.SEDAN)
                .pickupLocation(Location.builder().address("Negombo Beach Road").latitude(7.2087).longitude(79.8358).build())
                .destinationLocation(Location.builder().address("BIA").latitude(7.1808).longitude(79.8842).build())
                .estimatedDistanceKm(8.05)
                .status(status)
                .build();
    }

    private static Ride assignedRide(RideStatus status) {
        Ride ride = ride(status);
        ride.setDriverId("driver-55");
        ride.setDriverAccountId("acc-driver-55");
        ride.setVehicleNumber("WP CAB-4521");
        return ride;
    }

    private static AvailableDriver candidate(String id, double lat, double lng) {
        return new AvailableDriver(id, "acc-" + id, "Negombo", lat, lng, NOW.minusSeconds(60), "v-" + id,
                "CAB-" + id, VehicleType.SEDAN, 4);
    }
}

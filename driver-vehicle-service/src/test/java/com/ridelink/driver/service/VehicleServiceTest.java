package com.ridelink.driver.service;

import com.ridelink.driver.dto.CreateVehicleRequest;
import com.ridelink.driver.dto.UpdateVehicleRequest;
import com.ridelink.driver.exception.BusinessRuleException;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    private static final AuthenticatedUser OWNER = new AuthenticatedUser("acc-1", null, "DRIVER");
    private static final AuthenticatedUser OTHER_DRIVER = new AuthenticatedUser("acc-2", null, "DRIVER");

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle vehicle;
    private Driver driver;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId("v1");
        vehicle.setDriverId("d1");
        vehicle.setPlateNumber("CAB-1234");
        vehicle.setActive(true);
        driver = new Driver();
        driver.setId("d1");
        driver.setAccountId("acc-1");
    }

    @Test
    void createVehicle_succeeds_forOwnProfile_andNormalisesThePlate() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "d1", " cab-1234 ", "Toyota", "Prius", 2019, "White", VehicleType.SEDAN, 4);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByPlateNumber("CAB-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.createVehicle(request, OWNER);

        assertThat(result.getPlateNumber()).isEqualTo("CAB-1234");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createVehicle_rejects_unknownDriver() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "missing-driver", "CAB-1234", "Toyota", "Prius", 2019, "White", VehicleType.SEDAN, 4);
        when(driverRepository.findById("missing-driver")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.createVehicle(request, OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createVehicle_rejects_duplicatePlateNumber() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "d1", "CAB-1234", "Toyota", "Prius", 2019, "White", VehicleType.SEDAN, 4);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByPlateNumber("CAB-1234")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(request, OWNER))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createVehicle_rejects_anotherDriversProfile() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "d1", "CAB-1234", "Toyota", "Prius", 2019, "White", VehicleType.SEDAN, 4);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> vehicleService.createVehicle(request, OTHER_DRIVER))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createVehicle_rejects_aModelYearInTheFuture() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "d1", "CAB-1234", "Toyota", "Prius", 2999, "White", VehicleType.SEDAN, 4);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> vehicleService.createVehicle(request, OWNER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("year");
    }

    @Test
    void updateDetails_updatesEditableFields() {
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.updateDetails("v1",
                new UpdateVehicleRequest("Toyota", "Aqua", 2020, "Silver", 4), OWNER);

        assertThat(result.getModel()).isEqualTo("Aqua");
        assertThat(result.getColour()).isEqualTo("Silver");
    }

    @Test
    void setActive_rejects_deactivatingTheLastActiveVehicle() {
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.countByDriverIdAndActiveTrue("d1")).thenReturn(1L);

        assertThatThrownBy(() -> vehicleService.setActive("v1", false, OWNER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last active vehicle");
    }

    @Test
    void setActive_allows_deactivating_whenAnotherActiveVehicleRemains() {
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        when(vehicleRepository.countByDriverIdAndActiveTrue("d1")).thenReturn(2L);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.setActive("v1", false, OWNER);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void getById_throws_whenVehicleMissing() {
        when(vehicleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

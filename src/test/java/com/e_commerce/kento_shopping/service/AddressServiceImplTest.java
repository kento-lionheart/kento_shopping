package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.AddressRequest;
import com.e_commerce.kento_shopping.dto.response.AddressResponse;
import com.e_commerce.kento_shopping.entity.Address;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.exception.AddressAlreadyExistsException;
import com.e_commerce.kento_shopping.exception.AddressNotFoundException;
import com.e_commerce.kento_shopping.repository.AddressRepository;
import com.e_commerce.kento_shopping.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User user;

    @BeforeEach
    void setUp() {
        user = newUser(7L);
    }

    @Test
    void getAddressReturnsEveryFieldOfTheStoredAddress() {
        when(addressRepository.findByUser(user)).thenReturn(Optional.of(newAddress()));

        AddressResponse response = addressService.getAddress(user);

        assertThat(response.getRecipientName()).isEqualTo("Nguyen Van An");
        assertThat(response.getPhone()).isEqualTo("0912345678");
        assertThat(response.getStreet()).isEqualTo("12 Le Loi");
        assertThat(response.getWard()).isEqualTo("Ben Nghe");
        assertThat(response.getDistrict()).isEqualTo("District 1");
        assertThat(response.getCity()).isEqualTo("Ho Chi Minh");
        assertThat(response.getPostalCode()).isEqualTo("700000");
    }

    @Test
    void getAddressThrowsWhenCustomerHasNoAddressYet() {
        when(addressRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddress(user))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessage("Address not found");
    }

    @Test
    void createAddressPersistsEveryFieldLinkedToTheUser() {
        when(addressRepository.existsByUser(user)).thenReturn(false);

        addressService.createAddress(user, addressRequest("Nguyen Van An", "0912345678",
                "12 Le Loi", "Ben Nghe", "District 1", "Ho Chi Minh", "700000"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertThat(saved.getRecipientName()).isEqualTo("Nguyen Van An");
        assertThat(saved.getPhone()).isEqualTo("0912345678");
        assertThat(saved.getStreet()).isEqualTo("12 Le Loi");
        assertThat(saved.getWard()).isEqualTo("Ben Nghe");
        assertThat(saved.getDistrict()).isEqualTo("District 1");
        assertThat(saved.getCity()).isEqualTo("Ho Chi Minh");
        assertThat(saved.getPostalCode()).isEqualTo("700000");
        assertThat(saved.getUser()).isSameAs(user);
    }

    @Test
    void createAddressAllowsNullPostalCode() {
        when(addressRepository.existsByUser(user)).thenReturn(false);

        addressService.createAddress(user, addressRequest("Nguyen Van An", "0912345678",
                "12 Le Loi", "Ben Nghe", "District 1", "Ho Chi Minh", null));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getPostalCode()).isNull();
    }

    @Test
    void createAddressThrowsWhenUserAlreadyHasOne() {
        when(addressRepository.existsByUser(user)).thenReturn(true);

        assertThatThrownBy(() -> addressService.createAddress(user, addressRequest("Nguyen Van An",
                "0912345678", "12 Le Loi", "Ben Nghe", "District 1", "Ho Chi Minh", "700000")))
                .isInstanceOf(AddressAlreadyExistsException.class)
                .hasMessage("The address is existed for this user");
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void updateAddressMutatesTheExistingAddressInPlace() {
        Address existing = newAddress();
        when(addressRepository.findByUser(user)).thenReturn(Optional.of(existing));

        addressService.updateAddress(user, addressRequest("Tran Thi Binh", "0987654321",
                "99 Nguyen Hue", "Da Kao", "District 3", "Ha Noi", "100000"));

        assertThat(existing.getRecipientName()).isEqualTo("Tran Thi Binh");
        assertThat(existing.getPhone()).isEqualTo("0987654321");
        assertThat(existing.getStreet()).isEqualTo("99 Nguyen Hue");
        assertThat(existing.getWard()).isEqualTo("Da Kao");
        assertThat(existing.getDistrict()).isEqualTo("District 3");
        assertThat(existing.getCity()).isEqualTo("Ha Noi");
        assertThat(existing.getPostalCode()).isEqualTo("100000");
        assertThat(existing.getUser()).isSameAs(user);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void updateAddressThrowsWhenCustomerHasNoAddressYet() {
        when(addressRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(user, addressRequest("Tran Thi Binh",
                "0987654321", "99 Nguyen Hue", "Da Kao", "District 3", "Ha Noi", "100000")))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessage("Address not found");
    }

    private User newUser(Long id) {
        User u = User.builder()
                .email("customer@kento.com")
                .password("secret")
                .fullName("Customer")
                .phoneNumber("0900000000")
                .build();
        u.setId(id);
        return u;
    }

    private Address newAddress() {
        Address address = Address.builder()
                .recipientName("Nguyen Van An")
                .phone("0912345678")
                .street("12 Le Loi")
                .ward("Ben Nghe")
                .district("District 1")
                .city("Ho Chi Minh")
                .postalCode("700000")
                .user(user)
                .build();
        address.setId(1L);
        return address;
    }

    private AddressRequest addressRequest(String recipientName, String phone, String street,
                                          String ward, String district, String city, String postalCode) {
        AddressRequest request = new AddressRequest();
        ReflectionTestUtils.setField(request, "recipientName", recipientName);
        ReflectionTestUtils.setField(request, "phone", phone);
        ReflectionTestUtils.setField(request, "street", street);
        ReflectionTestUtils.setField(request, "ward", ward);
        ReflectionTestUtils.setField(request, "district", district);
        ReflectionTestUtils.setField(request, "city", city);
        ReflectionTestUtils.setField(request, "postalCode", postalCode);
        return request;
    }
}

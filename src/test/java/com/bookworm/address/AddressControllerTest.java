package com.bookworm.address;

import static com.bookworm.support.TestAuth.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.support.WebSecurityTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressController.class)
@Import(WebSecurityTestConfig.class)
class AddressControllerTest {

    private static final String VALID_BODY = """
            {"firstName":"Arjun","lastName":"Patel","street":"12 MG Road, Flat 4B","city":"Mumbai",
             "state":"Maharashtra","postalCode":"400001","country":"India","phone":"+919876543210",
             "email":"arjun.patel@example.com"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    private static AddressResponse address() {
        return new AddressResponse(5L, "Arjun", "Patel", "12 MG Road, Flat 4B", "Mumbai", "Maharashtra",
                "400001", "India", "+919876543210", "arjun.patel@example.com");
    }

    @Test
    void listReturnsAddresses() throws Exception {
        given(addressService.list(7L)).willReturn(List.of(address()));

        mockMvc.perform(get("/addresses").with(user(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postalCode").value("400001"));
    }

    @Test
    void createReturns201() throws Exception {
        given(addressService.create(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any()))
                .willReturn(address());

        mockMvc.perform(post("/addresses").with(user(7)).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createRejectsBadPostalCodeAndPhone() throws Exception {
        String body = VALID_BODY.replace("400001", "12AB").replace("+919876543210", "123");

        mockMvc.perform(post("/addresses").with(user(7)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/addresses").with(user(7)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        String body = VALID_BODY.replace("arjun.patel@example.com", "not-an-email");

        mockMvc.perform(post("/addresses").with(user(7)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void getMapsForeignAddressTo403() throws Exception {
        given(addressService.get(7L, 5L)).willThrow(new ForbiddenException("no"));

        mockMvc.perform(get("/addresses/5").with(user(7))).andExpect(status().isForbidden());
    }

    @Test
    void getMapsMissingAddressTo404() throws Exception {
        given(addressService.get(7L, 5L)).willThrow(ResourceNotFoundException.of("Address", 5L));

        mockMvc.perform(get("/addresses/5").with(user(7))).andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsAddress() throws Exception {
        given(addressService.update(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.any())).willReturn(address());

        mockMvc.perform(put("/addresses/5").with(user(7)).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/addresses/5").with(user(7))).andExpect(status().isNoContent());

        verify(addressService).delete(7L, 5L);
    }

    @Test
    void deleteOfAddressUsedByOrderReturns409() throws Exception {
        doThrow(new ConflictException("in use")).when(addressService).delete(7L, 5L);

        mockMvc.perform(delete("/addresses/5").with(user(7))).andExpect(status().isConflict());
    }

    @Test
    void requiresIdentity() throws Exception {
        mockMvc.perform(get("/addresses")).andExpect(status().isUnauthorized());
    }
}

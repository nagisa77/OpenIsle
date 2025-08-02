package com.openisle.controller;

import com.openisle.service.PushSubscriptionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PushSubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PushSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PushSubscriptionService pushSubscriptionService;

    @Test
    void subscribeEndpoint() throws Exception {
        mockMvc.perform(post("/api/push/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"e\",\"p256dh\":\"p\",\"auth\":\"a\"}")
                        .principal(new UsernamePasswordAuthenticationToken("u","p")))
                .andExpect(status().isOk());
        Mockito.verify(pushSubscriptionService).saveSubscription("u","e","p","a");
    }
}

package com.pokemontracker.controller;

import com.pokemontracker.dto.DealResponse;
import com.pokemontracker.exception.DealNotFoundException;
import com.pokemontracker.exception.SpecialTradeAlreadyUsedException;
import com.pokemontracker.model.DealStatus;
import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;
import com.pokemontracker.service.DealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealController.class)
class DealControllerTest {

    private static final String BASE = "/api/v1/deals";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DealService dealService;

    @Test
    void createReturns201WithDaysLeft() throws Exception {
        when(dealService.create(any())).thenReturn(sample(UUID.randomUUID()));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.friendName").value("Ash"))
                .andExpect(jsonPath("$.daysLeft").value(7))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void blankFriendNameReturnsValidationError() throws Exception {
        String body = validBody().replace("\"Ash\"", "\"\"");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("friendName")));
    }

    @Test
    void missingTradeTypeReturnsValidationError() throws Exception {
        String body = """
                {"friendName":"Ash","pokemonGive":"Pikachu","pokemonGet":"Eevee"}
                """;

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("tradeType")));
    }

    @Test
    void unknownEnumValueReturnsValidationError() throws Exception {
        String body = validBody().replace("DAYS_7", "NEXT_WEEK");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealService.get(id)).thenThrow(new DealNotFoundException(id));

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void malformedIdReturns400() throws Exception {
        mockMvc.perform(get(BASE + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void listPassesFilterAndSortParametersToTheService() throws Exception {
        when(dealService.list(eq(DealStatus.PLANNED), eq(Priority.HIGH), isNull(), eq("WhatsApp"),
                eq(true), eq("priority"))).thenReturn(List.of(sample(UUID.randomUUID())));

        mockMvc.perform(get(BASE)
                        .param("status", "PLANNED")
                        .param("priority", "HIGH")
                        .param("platform", "WhatsApp")
                        .param("overdue", "true")
                        .param("sort", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pokemonGet").value("Eevee"));

        verify(dealService).list(DealStatus.PLANNED, Priority.HIGH, null, "WhatsApp", true, "priority");
    }

    @Test
    void invalidFilterValueReturns400() throws Exception {
        mockMvc.perform(get(BASE).param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void priorityPatchUpdatesPriorityOnly() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealService.updatePriority(id, Priority.LOW)).thenReturn(sample(id));

        mockMvc.perform(patch(BASE + "/" + id + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(dealService).updatePriority(id, Priority.LOW);
    }

    @Test
    void priorityPatchWithoutValueReturns400() throws Exception {
        mockMvc.perform(patch(BASE + "/" + UUID.randomUUID() + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void secondSpecialTradeReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealService.updateStatus(id, DealStatus.DONE))
                .thenThrow(new SpecialTradeAlreadyUsedException(LocalDate.of(2026, 10, 19)));

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPECIAL_TRADE_ALREADY_USED"));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());

        verify(dealService).delete(id);
    }

    private static String validBody() {
        return """
                {
                  "friendName": "Ash",
                  "pokemonGive": "Pikachu",
                  "pokemonGet": "Eevee",
                  "tradeType": "DAYS_7",
                  "priority": "HIGH",
                  "chatLink": "https://wa.me/12345"
                }
                """;
    }

    private static DealResponse sample(UUID id) {
        LocalDate today = LocalDate.of(2026, 10, 19);
        return new DealResponse(id, "Ash", null, "Pikachu", "Eevee", TradeType.DAYS_7, 7, today,
                today.plusDays(7), Priority.HIGH, DealStatus.PLANNED, false, "WhatsApp",
                "https://wa.me/12345", null, null, LocalDateTime.of(2026, 10, 19, 11, 30),
                LocalDateTime.of(2026, 10, 19, 11, 30), 7, false);
    }
}

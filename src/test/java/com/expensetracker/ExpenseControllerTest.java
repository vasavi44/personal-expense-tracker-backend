package com.expensetracker;

import com.expensetracker.controller.ExpenseController;
import com.expensetracker.controller.HealthController;
import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.dto.TotalResponse;
import com.expensetracker.exception.GlobalExceptionHandler;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.service.ExpenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ExpenseController.class, HealthController.class})
@Import(GlobalExceptionHandler.class)
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/health should return UP status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("expense-tracker-backend")));
    }

    @Test
    @DisplayName("GET /api/expenses should return list of expenses")
    void testGetAllExpenses() throws Exception {
        ExpenseResponse e1 = new ExpenseResponse(1L, "Metro Pass", new BigDecimal("500.00"), "Travel", LocalDate.now());
        when(expenseService.getAllExpenses()).thenReturn(Arrays.asList(e1));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Metro Pass")))
                .andExpect(jsonPath("$[0].amount", is(500.00)))
                .andExpect(jsonPath("$[0].category", is("Travel")));
    }

    @Test
    @DisplayName("GET /api/expenses/{id} should return 404 when not found")
    void testGetExpenseById_NotFound() throws Exception {
        when(expenseService.getExpenseById(999L))
                .thenThrow(new ResourceNotFoundException("Expense not found with id: 999"));

        mockMvc.perform(get("/api/expenses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Expense not found with id: 999")));
    }

    @Test
    @DisplayName("POST /api/expenses with valid body should return 201 Created")
    void testCreateExpense_Success() throws Exception {
        ExpenseRequest request = new ExpenseRequest("Cafe Coffee", new BigDecimal("250.00"), "Food", LocalDate.now());
        ExpenseResponse response = new ExpenseResponse(1L, "Cafe Coffee", new BigDecimal("250.00"), "Food", LocalDate.now());

        when(expenseService.createExpense(any(ExpenseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Cafe Coffee")));
    }

    @Test
    @DisplayName("POST /api/expenses with invalid payload should return 400 Bad Request")
    void testCreateExpense_ValidationErrors() throws Exception {
        // Blank title, zero amount, invalid category, null date
        ExpenseRequest invalid = new ExpenseRequest("", BigDecimal.ZERO, "InvalidCat", null);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Error")));
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} should return 200 on successful update")
    void testUpdateExpense_Success() throws Exception {
        ExpenseRequest request = new ExpenseRequest("Updated Title", new BigDecimal("350.00"), "Food", LocalDate.now());
        ExpenseResponse response = new ExpenseResponse(1L, "Updated Title", new BigDecimal("350.00"), "Food", LocalDate.now());

        when(expenseService.updateExpense(eq(1L), any(ExpenseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} should return 200 when deleted")
    void testDeleteExpense_Success() throws Exception {
        doNothing().when(expenseService).deleteExpense(1L);

        mockMvc.perform(delete("/api/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Expense deleted successfully")));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} should return 404 when non-existent")
    void testDeleteExpense_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Expense not found with id: 99"))
                .when(expenseService).deleteExpense(99L);

        mockMvc.perform(delete("/api/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/expenses/total should return total and count")
    void testGetTotalExpenses() throws Exception {
        TotalResponse total = new TotalResponse(new BigDecimal("7500.50"), 12);
        when(expenseService.getTotalExpenses()).thenReturn(total);

        mockMvc.perform(get("/api/expenses/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(7500.50)))
                .andExpect(jsonPath("$.count", is(12)));
    }

    @Test
    @DisplayName("GET /api/expenses/category/{category} should return filtered expenses")
    void testGetExpensesByCategory() throws Exception {
        ExpenseResponse response = new ExpenseResponse(2L, "Train Ticket", new BigDecimal("650.00"), "Travel", LocalDate.now());
        when(expenseService.getExpensesByCategory("Travel")).thenReturn(Arrays.asList(response));

        mockMvc.perform(get("/api/expenses/category/Travel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category", is("Travel")));
    }
}

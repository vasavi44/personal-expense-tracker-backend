package com.expensetracker;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.dto.TotalResponse;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.expensetracker.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(new User(null, "User A", "userA@example.com", "hashA"));
        userB = userRepository.save(new User(null, "User B", "userB@example.com", "hashB"));
    }

    @Test
    @DisplayName("Anonymous users cannot access /api/expenses (returns 401)")
    void testAnonymousAccessDenied() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous users can access /api/health (returns 200)")
    void testAnonymousHealthAllowed() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous users can register via POST /api/auth/register (returns 201 with JWT token)")
    void testAnonymousRegistrationAllowed() throws Exception {
        RegisterRequest request = new RegisterRequest("New User", "newuser@example.com", "securePassword123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("newuser@example.com")))
                .andExpect(jsonPath("$.name", is("New User")));

        assertThat(userRepository.existsByEmail("newuser@example.com")).isTrue();
    }

    @Test
    @DisplayName("Registration rejects duplicate email with 400 Bad Request")
    void testRegistrationDuplicateEmailRejection() throws Exception {
        // userA is already in the database
        RegisterRequest request = new RegisterRequest("User A Duplicate", "userA@example.com", "securePassword123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Email is already registered. Please login or use a different email.")));
    }

    @Test
    @DisplayName("Registration rejects invalid payload with 400 Validation Error")
    void testRegistrationValidationErrors() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", notNullValue()));
    }

    @Test
    @DisplayName("User expenses are strictly isolated in service layer")
    void testUserDataIsolation() {
        // User A creates 2 expenses
        ExpenseResponse expA1 = expenseService.createExpense(
                new ExpenseRequest("User A Coffee", new BigDecimal("150.00"), "Food", LocalDate.now()),
                userA
        );
        expenseService.createExpense(
                new ExpenseRequest("User A Flight", new BigDecimal("4500.00"), "Travel", LocalDate.now()),
                userA
        );

        // User B creates 1 expense
        ExpenseResponse expB1 = expenseService.createExpense(
                new ExpenseRequest("User B Books", new BigDecimal("800.00"), "Education", LocalDate.now()),
                userB
        );

        // User A list should have only 2 items
        List<ExpenseResponse> userAList = expenseService.getAllExpenses(userA);
        assertThat(userAList).hasSize(2);
        assertThat(userAList).extracting(ExpenseResponse::getTitle)
                .containsExactlyInAnyOrder("User A Coffee", "User A Flight");

        // User B list should have only 1 item
        List<ExpenseResponse> userBList = expenseService.getAllExpenses(userB);
        assertThat(userBList).hasSize(1);
        assertThat(userBList.get(0).getTitle()).isEqualTo("User B Books");

        // User B cannot get User A's expense by ID
        assertThatThrownBy(() -> expenseService.getExpenseById(expA1.getId(), userB))
                .isInstanceOf(ResourceNotFoundException.class);

        // User B cannot update User A's expense
        ExpenseRequest updateAttempt = new ExpenseRequest("Hacked", new BigDecimal("1.00"), "Food", LocalDate.now());
        assertThatThrownBy(() -> expenseService.updateExpense(expA1.getId(), updateAttempt, userB))
                .isInstanceOf(ResourceNotFoundException.class);

        // User B cannot delete User A's expense
        assertThatThrownBy(() -> expenseService.deleteExpense(expA1.getId(), userB))
                .isInstanceOf(ResourceNotFoundException.class);

        // Totals are strictly isolated
        TotalResponse totalA = expenseService.getTotalExpenses(userA);
        assertThat(totalA.getTotalAmount()).isEqualByComparingTo("4650.00");
        assertThat(totalA.getCount()).isEqualTo(2);

        TotalResponse totalB = expenseService.getTotalExpenses(userB);
        assertThat(totalB.getTotalAmount()).isEqualByComparingTo("800.00");
        assertThat(totalB.getCount()).isEqualTo(1);

        // Category filter is strictly isolated
        List<ExpenseResponse> foodA = expenseService.getExpensesByCategory("Food", userA);
        assertThat(foodA).hasSize(1);

        List<ExpenseResponse> foodB = expenseService.getExpensesByCategory("Food", userB);
        assertThat(foodB).isEmpty();
    }
}

package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.dto.TotalResponse;
import com.expensetracker.entity.User;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final AuthService authService;

    public ExpenseController(ExpenseService expenseService, AuthService authService) {
        this.expenseService = expenseService;
        this.authService = authService;
    }

    private User resolveUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return authService.getAuthenticatedUser(userDetails.getUsername());
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        List<ExpenseResponse> expenses = user != null ? expenseService.getAllExpenses(user) : expenseService.getAllExpenses();
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = resolveUser(userDetails);
        ExpenseResponse expense = user != null ? expenseService.getExpenseById(id, user) : expenseService.getExpenseById(id);
        return ResponseEntity.ok(expense);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = resolveUser(userDetails);
        ExpenseResponse created = user != null ? expenseService.createExpense(request, user) : expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = resolveUser(userDetails);
        ExpenseResponse updated = user != null ? expenseService.updateExpense(id, request, user) : expenseService.updateExpense(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = resolveUser(userDetails);
        if (user != null) {
            expenseService.deleteExpense(id, user);
        } else {
            expenseService.deleteExpense(id);
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "Expense deleted successfully"));
    }

    @GetMapping("/total")
    public ResponseEntity<TotalResponse> getTotalExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        TotalResponse total = user != null ? expenseService.getTotalExpenses(user) : expenseService.getTotalExpenses();
        return ResponseEntity.ok(total);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = resolveUser(userDetails);
        List<ExpenseResponse> expenses = user != null ? expenseService.getExpensesByCategory(category, user) : expenseService.getExpensesByCategory(category);
        return ResponseEntity.ok(expenses);
    }
}

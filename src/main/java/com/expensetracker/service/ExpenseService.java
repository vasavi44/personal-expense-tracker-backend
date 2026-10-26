package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.dto.TotalResponse;
import com.expensetracker.entity.Expense;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseService {

    public static final List<String> VALID_CATEGORIES = Arrays.asList(
            "Food", "Travel", "Shopping", "Bills", "Entertainment", "Health", "Education", "Other"
    );

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAllByOrderByDateDescIdDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return mapToResponse(expense);
    }

    public ExpenseResponse createExpense(ExpenseRequest request) {
        validateRequest(request);

        Expense expense = new Expense();
        expense.setTitle(request.getTitle().trim());
        expense.setAmount(request.getAmount());
        expense.setCategory(normalizeCategory(request.getCategory()));
        expense.setDate(request.getDate());

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        validateRequest(request);

        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        existing.setTitle(request.getTitle().trim());
        existing.setAmount(request.getAmount());
        existing.setCategory(normalizeCategory(request.getCategory()));
        existing.setDate(request.getDate());

        Expense updated = expenseRepository.save(existing);
        return mapToResponse(updated);
    }

    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense not found with id: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TotalResponse getTotalExpenses() {
        BigDecimal total = expenseRepository.sumTotalAmount();
        long count = expenseRepository.count();
        return new TotalResponse(total != null ? total : BigDecimal.ZERO, count);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getAllExpenses();
        }
        return expenseRepository.findByCategoryIgnoreCaseOrderByDateDescIdDesc(category.trim())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateRequest(ExpenseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Expense payload cannot be null");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Expense title cannot be empty or whitespace");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a valid number greater than zero");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Expense date is required");
        }
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        String normalized = normalizeCategory(request.getCategory());
        if (normalized == null) {
            throw new IllegalArgumentException("Invalid category. Must be one of: " + String.join(", ", VALID_CATEGORIES));
        }
    }

    private String normalizeCategory(String category) {
        if (category == null) return null;
        for (String valid : VALID_CATEGORIES) {
            if (valid.equalsIgnoreCase(category.trim())) {
                return valid;
            }
        }
        return null;
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDate()
        );
    }
}

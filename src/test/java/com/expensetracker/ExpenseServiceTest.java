package com.expensetracker;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.dto.TotalResponse;
import com.expensetracker.entity.Expense;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense sampleExpense;
    private ExpenseRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleExpense = new Expense(1L, "Electricity Bill", new BigDecimal("1850.00"), "Bills", LocalDate.of(2026, 10, 20));
        sampleRequest = new ExpenseRequest("Electricity Bill", new BigDecimal("1850.00"), "Bills", LocalDate.of(2026, 10, 20));
    }

    @Test
    @DisplayName("Should create expense when valid")
    void testCreateExpense_Success() {
        when(expenseRepository.save(any(Expense.class))).thenReturn(sampleExpense);

        ExpenseResponse response = expenseService.createExpense(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Electricity Bill");
        assertThat(response.getAmount()).isEqualByComparingTo("1850.00");
    }

    @Test
    @DisplayName("Should reject blank or whitespace title")
    void testCreateExpense_BlankTitle() {
        ExpenseRequest request = new ExpenseRequest("   ", new BigDecimal("100.00"), "Food", LocalDate.now());
        assertThatThrownBy(() -> expenseService.createExpense(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title cannot be empty");
    }

    @Test
    @DisplayName("Should reject zero or negative amount")
    void testCreateExpense_InvalidAmount() {
        ExpenseRequest zeroReq = new ExpenseRequest("Snacks", BigDecimal.ZERO, "Food", LocalDate.now());
        assertThatThrownBy(() -> expenseService.createExpense(zeroReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        ExpenseRequest negativeReq = new ExpenseRequest("Snacks", new BigDecimal("-50.00"), "Food", LocalDate.now());
        assertThatThrownBy(() -> expenseService.createExpense(negativeReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("Should reject invalid category")
    void testCreateExpense_InvalidCategory() {
        ExpenseRequest invalidCat = new ExpenseRequest("Item", new BigDecimal("100.00"), "Crypto", LocalDate.now());
        assertThatThrownBy(() -> expenseService.createExpense(invalidCat))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid category");
    }

    @Test
    @DisplayName("Should get total amount and count correctly")
    void testGetTotalExpenses() {
        when(expenseRepository.sumTotalAmount()).thenReturn(new BigDecimal("4500.75"));
        when(expenseRepository.count()).thenReturn(3L);

        TotalResponse total = expenseService.getTotalExpenses();
        assertThat(total.getTotalAmount()).isEqualByComparingTo("4500.75");
        assertThat(total.getCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown ID on update")
    void testUpdateExpense_NotFound() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.updateExpense(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expense not found with id: 99");
    }

    @Test
    @DisplayName("Should delete expense when existing")
    void testDeleteExpense_Success() {
        when(expenseRepository.existsById(1L)).thenReturn(true);

        expenseService.deleteExpense(1L);

        verify(expenseRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent expense")
    void testDeleteExpense_NotFound() {
        when(expenseRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.deleteExpense(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

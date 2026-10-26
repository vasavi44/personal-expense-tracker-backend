package com.expensetracker;

import com.expensetracker.entity.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @DisplayName("Should save and retrieve expense")
    void testSaveAndFind() {
        Expense expense = new Expense(null, "Groceries", new BigDecimal("1250.50"), "Food", LocalDate.of(2026, 10, 20));
        Expense saved = expenseRepository.save(expense);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Groceries");
        assertThat(saved.getAmount()).isEqualByComparingTo("1250.50");
    }

    @Test
    @DisplayName("Should retrieve expenses ordered by date descending")
    void testOrderByDateDesc() {
        Expense e1 = new Expense(null, "Old Flight", new BigDecimal("3500.00"), "Travel", LocalDate.of(2026, 10, 10));
        Expense e2 = new Expense(null, "Recent Dinner", new BigDecimal("850.00"), "Food", LocalDate.of(2026, 10, 25));
        expenseRepository.save(e1);
        expenseRepository.save(e2);

        List<Expense> list = expenseRepository.findAllByOrderByDateDescIdDesc();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getTitle()).isEqualTo("Recent Dinner");
    }

    @Test
    @DisplayName("Should filter by category case-insensitively")
    void testFilterByCategory() {
        Expense e1 = new Expense(null, "Doctor Visit", new BigDecimal("500.00"), "Health", LocalDate.of(2026, 10, 15));
        Expense e2 = new Expense(null, "Cinema Ticket", new BigDecimal("300.00"), "Entertainment", LocalDate.of(2026, 10, 18));
        expenseRepository.save(e1);
        expenseRepository.save(e2);

        List<Expense> healthExpenses = expenseRepository.findByCategoryIgnoreCaseOrderByDateDescIdDesc("health");
        assertThat(healthExpenses).hasSize(1);
        assertThat(healthExpenses.get(0).getTitle()).isEqualTo("Doctor Visit");
    }

    @Test
    @DisplayName("Should correctly calculate sum of all expenses")
    void testSumTotalAmount() {
        expenseRepository.deleteAll();
        BigDecimal emptyTotal = expenseRepository.sumTotalAmount();
        assertThat(emptyTotal).isEqualByComparingTo(BigDecimal.ZERO);

        expenseRepository.save(new Expense(null, "Milk", new BigDecimal("60.00"), "Food", LocalDate.now()));
        expenseRepository.save(new Expense(null, "Book", new BigDecimal("440.00"), "Education", LocalDate.now()));

        BigDecimal total = expenseRepository.sumTotalAmount();
        assertThat(total).isEqualByComparingTo(new BigDecimal("500.00"));
    }
}

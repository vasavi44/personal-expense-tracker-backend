package com.expensetracker.repository;

import com.expensetracker.entity.Expense;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // User-scoped queries (Strict data isolation)
    List<Expense> findAllByUserOrderByDateDescIdDesc(User user);

    Optional<Expense> findByIdAndUser(Long id, User user);

    boolean existsByIdAndUser(Long id, User user);

    List<Expense> findByUserAndCategoryIgnoreCaseOrderByDateDescIdDesc(User user, String category);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user")
    BigDecimal sumTotalAmountByUser(@Param("user") User user);

    long countByUser(User user);

    // Legacy un-scoped queries (preserved for tests/migrations)
    List<Expense> findAllByOrderByDateDescIdDesc();

    List<Expense> findByCategoryIgnoreCaseOrderByDateDescIdDesc(String category);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumTotalAmount();
}

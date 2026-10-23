package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.query.request.UserBalanceMutationDto;
import com.example.inventory_api_springboot.dto.request.UserBalanceDepositRequestDto;
import com.example.inventory_api_springboot.dto.request.UserBalanceWithdrawRequestDto;
import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.entity.UserBalance;
import com.example.inventory_api_springboot.entity.UserBalanceLog;
import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import com.example.inventory_api_springboot.projection.UserBalanceCardProjection;
import com.example.inventory_api_springboot.repository.UserBalanceLogRepository;
import com.example.inventory_api_springboot.repository.UserBalanceRepository;
import com.example.inventory_api_springboot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBalanceService {
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final UserBalanceLogRepository userBalanceLogRepository;

    @Transactional(readOnly = true)
    public UserBalance getReferenceById(UUID id) {
        return userBalanceRepository.getReferenceById(id);
    }

    public ResponseEntity<UserBalanceCardProjection> getBalanceCard(UUID userId) {
        UserBalanceCardProjection userBalance = userBalanceRepository.getUserBalanceCardByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User balance not found"));

        return ResponseEntity.ok(userBalance);
    }

    @Transactional
    public void mutation(
        UserBalanceMutationDto userBalanceMutationData
    ) {
        UserBalance userBalance =
            userBalanceRepository.findWithLockByUserId(userBalanceMutationData.userId())
                .orElseThrow(() -> new EntityNotFoundException("User balance not found"));

        userBalanceMutationData.type().validate(
            userBalanceMutationData.balance(),
            userBalanceMutationData.reservedBalance()
        );

        User loggedCreatorProxy = null;
        if (userBalanceMutationData.logCreatorId() != null)
            loggedCreatorProxy = userRepository.getReferenceById(userBalanceMutationData.logCreatorId());

        BigDecimal balanceAfter = userBalance.getBalance().add(userBalanceMutationData.balance());
        BigDecimal reservedBalanceAfter =
            userBalance.getReservedBalance().add(userBalanceMutationData.reservedBalance());

        UserBalanceLog userBalanceLog = UserBalanceLog.builder()
            .balanceBefore(userBalance.getBalance())
            .balanceAfter(balanceAfter)
            .balanceChange(userBalanceMutationData.balance())
            .reservedBalanceBefore(userBalance.getReservedBalance())
            .reservedBalanceAfter(reservedBalanceAfter)
            .reservedBalanceChange(userBalanceMutationData.reservedBalance())
            .type(userBalanceMutationData.type())
            .referenceId(userBalanceMutationData.referenceId())
            .user(loggedCreatorProxy)
            .build();

        userBalance.setBalance(balanceAfter);
        userBalance.setReservedBalance(reservedBalanceAfter);

        userBalanceRepository.save(userBalance);
        userBalanceLogRepository.save(userBalanceLog);
    }

    @Transactional
    public void mutation(
        UUID orderId,
        UUID userId,
        UserBalanceLogTypeEnum type
    ) {
        BigDecimal reservedAmount =
            userBalanceLogRepository
                .sumReservedBalanceChange(orderId, UserBalanceLogTypeEnum.ORDER_RESERVED)
                .orElseThrow(
                    () -> new EntityNotFoundException(
                        "Reservation log for order " + orderId + " not found"
                    )
                );

        mutation(orderId, userId, type, reservedAmount);
    }

    @Transactional
    public void mutation(
        UUID orderId,
        UUID userId,
        UserBalanceLogTypeEnum type,
        BigDecimal reservedAmount
    ) {
        if (reservedAmount == null || reservedAmount.signum() <= 0)
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order " + orderId + " has no reserved balance to settle " +
                "(reservedAmount=" + reservedAmount + ")"
            );

        UserBalance userBalance =
            userBalanceRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User balance not found"));

        BigDecimal balanceBefore = userBalance.getBalance();
        BigDecimal reservedBalanceBefore = userBalance.getReservedBalance();

        BigDecimal balanceChange =
            switch (type) {
                case ORDER_CANCELLED -> BigDecimal.ZERO;
                case ORDER_SHIPPED -> reservedAmount.negate();
                default -> throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unsupported balance log type for order settlement: " + type
                );
            };
        BigDecimal reservedBalanceChange = reservedAmount.negate();
        userBalance.mutateBalances(balanceChange, reservedBalanceChange);

        BigDecimal balanceAfter = userBalance.getBalance();
        BigDecimal reservedBalanceAfter = userBalance.getReservedBalance();

        userBalanceRepository.save(userBalance);

        int insertedRows =
            switch (type) {
                case ORDER_CANCELLED ->
                    userBalanceLogRepository.insertReversedLog(
                        userId,
                        balanceBefore,
                        balanceAfter,
                        reservedBalanceBefore,
                        reservedBalanceAfter,
                        orderId
                    );
                case ORDER_SHIPPED ->
                    userBalanceLogRepository.insertSwitchedLog(
                        userId,
                        balanceBefore,
                        balanceAfter,
                        reservedBalanceBefore,
                        reservedBalanceAfter,
                        orderId
                    );
                default -> 0;
            };

        if (insertedRows != 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order " + orderId + " has already been cancelled or shipped, so the balance log was not inserted"
            );
        }
    }

    @Transactional
    public ResponseEntity<String> deposit(UserBalanceDepositRequestDto depositData, UUID userId) {
        mutation(
            new UserBalanceMutationDto(
                depositData.balance(),
                BigDecimal.ZERO,
                UserBalanceLogTypeEnum.DEPOSIT,
                userId,
                userId,
                null
            )
        );

        return ResponseEntity.ok("User balance successfully deposited");
    }

    @Transactional
    public ResponseEntity<String> withdraw(UserBalanceWithdrawRequestDto withdrawData, UUID userId) {
        mutation(
            new UserBalanceMutationDto(
                withdrawData.balance(),
                BigDecimal.ZERO,
                UserBalanceLogTypeEnum.WITHDRAW,
                userId,
                userId,
                null
            )
        );

        return ResponseEntity.ok("User balance successfully withdrawn");
    }

    @Transactional
    public void refundMultipleUsers(List<UserBalanceMutationDto> listUserBalanceMutationData) {
        for (UserBalanceMutationDto userBalanceMutation : listUserBalanceMutationData) {
            mutation(userBalanceMutation);
        }
    }

    @Transactional
    public void refund(UserBalanceMutationDto userBalanceMutation) {
        mutation(userBalanceMutation);
    }
}

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            userRepository.getReferenceById(userBalanceMutationData.logCreatorId());

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

//        userBalanceRepository.updateBalanceAndReservedBalance(
//            userBalanceMutationData.balance(),
//            userBalanceMutationData.reservedBalance(),
//            userBalanceMutationData.userId()
//        );
        userBalanceRepository.save(userBalance);
        userBalanceLogRepository.save(userBalanceLog);
    }

    @Transactional
    public void mutation(
        UUID orderId,
        UserBalanceLogTypeEnum type
    ) {
        switch (type) {
            case ORDER_CANCELLED : {
                userBalanceRepository.cancelOrder(orderId);
                userBalanceLogRepository.insertReversedLog(
                    orderId,
                    UserBalanceLogTypeEnum.ORDER_CANCELLED
                );
                break;
            }
            case ORDER_SHIPPED: {
                userBalanceRepository.finishOrder(orderId);
                userBalanceLogRepository.insertSwitchedLog(
                    orderId,
                    UserBalanceLogTypeEnum.ORDER_SHIPPED
                );
                break;
            }
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

    @Transactional
    public void insertReversedLog(
        UUID orderId,
        UserBalanceLogTypeEnum balanceLogType
    ) {
        userBalanceLogRepository.insertReversedLog(
            orderId,
            balanceLogType
        );
    }
}

package com.blockchain.earn.domain.models.interest

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

data class InterestAccountBalance(
    val totalBalance: Money,
    val pendingInterest: Money,
    val pendingDeposit: Money,
    val totalInterest: Money,
    val lockedBalance: Money,
    val hasTransactions: Boolean = false
) {
    val actionableBalance: CryptoValue
        get() = (totalBalance - lockedBalance) as CryptoValue

    companion object {
        fun zeroBalance(currency: Currency): InterestAccountBalance =
            InterestAccountBalance(
                totalBalance = Money.zero(currency),
                lockedBalance = Money.zero(currency),
                pendingDeposit = Money.zero(currency),
                pendingInterest = Money.zero(currency),
                totalInterest = Money.zero(currency),
                hasTransactions = false
            )
    }
}

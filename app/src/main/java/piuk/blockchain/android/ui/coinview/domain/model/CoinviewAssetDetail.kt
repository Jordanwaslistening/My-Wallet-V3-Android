package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.AssetFilter
import info.blockchain.balance.Money

sealed interface CoinviewAssetDetail {
    val isAddedToWatchlist: Boolean
    val totalBalance: CoinviewAssetTotalBalance

    data class Tradeable(
        override val isAddedToWatchlist: Boolean,
        val accounts: CoinviewAccounts,
        override val totalBalance: CoinviewAssetTotalBalance
    ) : CoinviewAssetDetail

    data class NonTradeable(
        override val isAddedToWatchlist: Boolean,
        override val totalBalance: CoinviewAssetTotalBalance
    ) : CoinviewAssetDetail
}

data class CoinviewAssetTotalBalance(
    val totalCryptoBalance: Map<AssetFilter, Money>,
    val totalFiatBalance: Money
)

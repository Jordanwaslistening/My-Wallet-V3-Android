package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.blockchain.componentlib.system.ShimmerLoadingBox
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.BalanceDifferenceConfig
import com.blockchain.home.presentation.allassets.WalletBalance
import com.blockchain.koin.payloadScope
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import org.koin.androidx.compose.getViewModel

@Composable
fun Balance(
    modifier: Modifier = Modifier,
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope),
    scrollRange: Float,
    hideBalance: Boolean
) {
    val viewState: AssetsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    BalanceScreen(
        modifier = modifier,
        walletBalance = viewState.balance,
        balanceAlpha = scrollRange,
        hideBalance = hideBalance
    )
}

@Composable
fun BalanceScreen(
    modifier: Modifier = Modifier,
    walletBalance: WalletBalance,
    balanceAlpha: Float,
    hideBalance: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = AppTheme.dimensions.smallSpacing
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TotalBalance(
            balance = walletBalance.balance,
            alpha = balanceAlpha,
            hide = hideBalance
        )
        BalanceDifference(
            balanceDifference = walletBalance.balanceDifference,
        )
    }
}

@Composable
fun TotalBalance(
    alpha: Float,
    hide: Boolean,
    balance: DataResource<Money>
) {
    val balanceOffset by animateIntAsState(
        targetValue = if (hide) -BALANCE_OFFSET_TARGET else 0,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    when (balance) {
        DataResource.Loading -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1F))
                ShimmerLoadingBox(
                    modifier = Modifier
                        .height(AppTheme.dimensions.largeSpacing)
                        .weight(1F)
                )
                Spacer(modifier = Modifier.weight(1F))
            }
        }

        is DataResource.Data -> {
            Text(
                modifier = Modifier
                    .clipToBounds()
                    .alpha(alpha)
                    .scale((alpha * 1.6F).coerceIn(0F, 1F))
                    .offset {
                        IntOffset(
                            x = 0,
                            y = balanceOffset
                        )
                    },
                text = balance.data.toStringWithSymbol(),
                style = AppTheme.typography.title1,
                color = AppTheme.colors.title
            )
        }

        is DataResource.Error -> {
            // todo(othman) checking with Ethan
        }
    }
}

@Composable
fun BalanceDifference(
    balanceDifference: BalanceDifferenceConfig,
) {
    if (balanceDifference.text.isNotEmpty()) {
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        Text(
            text = balanceDifference.text,
            style = AppTheme.typography.paragraph2,
            color = balanceDifference.color
        )
    }
}

@Preview
@Composable
fun PreviewBalanceScreen() {
    AppTheme {
        BalanceScreen(
            walletBalance =
            WalletBalance(
                balance = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
                cryptoBalanceDifference24h = DataResource.Data(
                    Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())
                ),
                cryptoBalanceNow = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
            ),
            balanceAlpha = 1F,
            hideBalance = false
        )
    }
}

@Preview
@Composable
fun PreviewBalanceScreenLoading() {
    BalanceScreen(
        walletBalance = WalletBalance(
            balance = DataResource.Loading,
            cryptoBalanceDifference24h = DataResource.Loading,
            cryptoBalanceNow = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
        ),
        balanceAlpha = 1F,
        hideBalance = false
    )
}

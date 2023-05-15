package com.blockchain.earn.staking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBarWithSmallText
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.custom.TextWithTooltipTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.earn.R
import com.blockchain.earn.common.EarnFieldExplainer
import com.blockchain.earn.common.EarnPendingWithdrawals
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewState
import com.blockchain.extensions.safeLet
import kotlinx.coroutines.delay

@Composable
fun StakingSummarySheet(
    state: StakingSummaryViewState,
    onWithdrawPressed: (sourceAccount: BlockchainAccount, targetAccount: CustodialTradingAccount) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Staking) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onExplainerClicked: (EarnFieldExplainer) -> Unit,
    onClosePressed: () -> Unit
) {
    Box {
        Column {
            SheetHeader(
                title = stringResource(
                    id = com.blockchain.stringResources.R.string.staking_summary_title,
                    state.balanceCrypto?.currency?.networkTicker.orEmpty()
                ),
                startImageResource = ImageResource.Remote(state.balanceCrypto?.currency?.logo.orEmpty()),
                shouldShowDivider = false,
                onClosePress = onClosePressed
            )

            Column(
                modifier = Modifier
                    .background(color = AppTheme.colors.light)
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LargeVerticalSpacer()

                state.balanceFiat?.let { balance ->
                    SimpleText(
                        text = balance.toStringWithSymbol(),
                        style = ComposeTypographies.Title1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )
                    TinyVerticalSpacer()
                }

                state.balanceCrypto?.let { balance ->
                    SimpleText(
                        text = balance.toStringWithSymbol(),
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )
                }

                LargeVerticalSpacer()

                Card(
                    backgroundColor = AppTheme.colors.background,
                    shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TinyVerticalSpacer()

                        safeLet(state.earnedFiat, state.earnedCrypto) { earnedFiat, earnedCrypto ->
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    com.blockchain.stringResources.R.string.staking_summary_total_earned
                                ),
                                endTitle = earnedFiat.toStringWithSymbol(),
                                endSubtitle = earnedCrypto.toStringWithSymbol()
                            )

                            TinyVerticalSpacer()
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                            TinyVerticalSpacer()
                        }

                        TextWithTooltipTableRow(
                            startText = stringResource(com.blockchain.stringResources.R.string.rewards_summary_rate),
                            endTitle = "${state.stakingRate}%",
                            onClick = {
                                onExplainerClicked(EarnFieldExplainer.StakingEarnRate)
                            }
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(com.blockchain.stringResources.R.string.earn_payment_frequency),
                            endTitle = when (state.earnFrequency) {
                                EarnRewardsFrequency.Daily ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_daily
                                    )

                                EarnRewardsFrequency.Weekly ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_weekly
                                    )

                                EarnRewardsFrequency.Monthly ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_monthly
                                    )

                                else ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_unknown
                                    )
                            }
                        )

                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()

                val hasPendingWithdrawals by remember(state.pendingWithdrawals) {
                    mutableStateOf(state.pendingWithdrawals.isNotEmpty())
                }

                var withdrawalsLocked by remember { mutableStateOf(false) }

                if (state.shouldShowWithdrawWarning()) {
                    StakingWithdrawalNotice(onLearnMorePressed = withdrawDisabledLearnMore)
                } else {
                    if (hasPendingWithdrawals) {
                        EarnPendingWithdrawals(pendingWithdrawals = state.pendingWithdrawals)
                        StandardVerticalSpacer()
                    }

                    StakingWithdrawalQueueNotice(
                        unbondingDays = state.unbondingDays,
                        onLearnMorePressed = withdrawDisabledLearnMore
                    )
                    LargeVerticalSpacer()
                }

                LargeVerticalSpacer()

                if (hasPendingWithdrawals) {
                    val withdrawalTimestamp = state.pendingWithdrawals.last().withdrawalTimestamp?.time ?: 0L
                    val timeBetweenWithdrawals = 5 * 60 * 1000 // 5 minutes in milliseconds
                    val unlockTime = withdrawalTimestamp + timeBetweenWithdrawals

                    val timeUntilUnlock = remember {
                        mutableStateOf(unlockTime - System.currentTimeMillis())
                    }

                    LaunchedEffect(Unit) {
                        withdrawalsLocked = true
                        while (true) {
                            delay(1000L)
                            val newTimeUntilUnlock = unlockTime - System.currentTimeMillis()
                            if (newTimeUntilUnlock != timeUntilUnlock.value) {
                                timeUntilUnlock.value = newTimeUntilUnlock
                            }
                        }
                    }

                    if (timeUntilUnlock.value > 0) {
                        val progress = (timeUntilUnlock.value.toFloat() / timeBetweenWithdrawals.toFloat())
                            .coerceIn(0f, 1f)
                        CircularProgressBarWithSmallText(
                            progress = progress,
                            text = stringResource(
                                id =
                                com.blockchain.stringResources.R
                                    .string.earn_staking_withdrawal_locked_countdown_message,
                                formatDuration(timeUntilUnlock.value)
                            )
                        )
                        SmallVerticalSpacer()
                    } else {
                        withdrawalsLocked = false
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = com.blockchain.stringResources.R.string.common_withdraw),
                        icon = ImageResource.Local(
                            com.blockchain.componentlib.icons.R.drawable.send_off,
                            colorFilter = ColorFilter.tint(Color.White)
                        ),
                        state = if (state.canWithdraw && !withdrawalsLocked) {
                            ButtonState.Enabled
                        } else ButtonState.Disabled,
                        onClick = {
                            safeLet(state.account, state.tradingAccount) { account, tradingAccount ->
                                onWithdrawPressed(account, tradingAccount)
                            }
                        }
                    )

                    TinyHorizontalSpacer()

                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = com.blockchain.stringResources.R.string.common_add),
                        icon = ImageResource.Local(
                            com.blockchain.componentlib.icons.R.drawable.receive_off,
                            colorFilter = ColorFilter.tint(Color.White)
                        ),
                        onClick = { state.account?.let { onDepositPressed(it as EarnRewardsAccount.Staking) } },
                        state = if (state.canDeposit) ButtonState.Enabled else ButtonState.Disabled
                    )
                }

                LargeVerticalSpacer()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StakingSummarySheetPreview() {
    AppTheme {
        StakingSummarySheet(
            state = StakingSummaryViewState(
                account = null,
                tradingAccount = null,
                errorState = StakingError.None,
                isLoading = false,
                balanceCrypto = null,
                balanceFiat = null,
                stakedCrypto = null,
                stakedFiat = null,
                bondingCrypto = null,
                bondingFiat = null,
                earnedCrypto = null,
                earnedFiat = null,
                stakingRate = 5.0,
                commissionRate = 1.0,
                earnFrequency = EarnRewardsFrequency.Weekly,
                canDeposit = true,
                canWithdraw = true,
                pendingWithdrawals = emptyList(),
                unbondingDays = 2
            ),
            onWithdrawPressed = { _, _ -> },
            onDepositPressed = {},
            withdrawDisabledLearnMore = {},
            onClosePressed = {},
            onExplainerClicked = {}
        )
    }
}

@Composable
fun StakingWithdrawalNotice(onLearnMorePressed: () -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        border = BorderStroke(
            width = 1.dp,
            color = AppTheme.colors.warning
        ),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.common_important_information),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Warning,
                gravity = ComposeGravities.Start
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.earn_staking_withdrawal_blocked),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            SmallVerticalSpacer()

            SecondaryButton(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMorePressed
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StakingWithdrawalNoticePreview() {
    AppTheme {
        StakingWithdrawalNotice(onLearnMorePressed = {})
    }
}

@Composable
fun StakingWithdrawalQueueNotice(unbondingDays: Int, onLearnMorePressed: () -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(
                    com.blockchain.stringResources.R.string.earn_staking_withdrawal_queue_notice_title
                ),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(
                    com.blockchain.stringResources.R.string.earn_staking_withdrawal_queue_notice_description,
                    unbondingDays
                ),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            SmallVerticalSpacer()

            SecondaryButton(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMorePressed
            )
        }
    }
}

@Composable
fun SummarySheetLoading() {
    Column {
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
    }
}

private fun StakingSummaryViewState.shouldShowWithdrawWarning(): Boolean =
    // TODO (labreu): remove hardcoded ETH once staking is available for other networks
    !canWithdraw && balanceCrypto?.currency?.networkTicker == "ETH"

private fun formatDuration(duration: Long): String {
    val minutes = (duration / 1000) / 60
    val seconds = (duration / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

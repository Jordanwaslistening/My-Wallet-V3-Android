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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.custom.TextWithTooltipTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.earn.R
import com.blockchain.earn.common.EarnFieldExplainer
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewState
import com.blockchain.extensions.safeLet

@Composable
fun StakingSummarySheet(
    state: StakingSummaryViewState,
    onWithdrawPressed: (currency: EarnRewardsAccount.Staking) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Staking) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onExplainerClicked: (EarnFieldExplainer) -> Unit,
    onClosePressed: () -> Unit,
) {
    Box {
        Column {
            SheetHeader(
                title = stringResource(
                    id = R.string.staking_summary_title, state.balanceCrypto?.currency?.networkTicker.orEmpty()
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
                    elevation = 0.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TinyVerticalSpacer()

                        safeLet(state.earnedFiat, state.earnedCrypto) { earnedFiat, earnedCrypto ->
                            TextWithTooltipTableRow(
                                startText = stringResource(R.string.staking_summary_total_earned),
                                endTitle = earnedFiat.toStringWithSymbol(),
                                endSubtitle = earnedCrypto.toStringWithSymbol(),
                            )

                            TinyVerticalSpacer()
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                            TinyVerticalSpacer()
                        }

                        TextWithTooltipTableRow(
                            startText = stringResource(R.string.rewards_summary_rate),
                            endTitle = "${state.stakingRate}%",
                            onClick = {
                                onExplainerClicked(EarnFieldExplainer.StakingEarnRate)
                            },
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(R.string.earn_payment_frequency),
                            endTitle = when (state.earnFrequency) {
                                EarnRewardsFrequency.Daily ->
                                    stringResource(id = R.string.earn_payment_frequency_daily)

                                EarnRewardsFrequency.Weekly ->
                                    stringResource(id = R.string.earn_payment_frequency_weekly)

                                EarnRewardsFrequency.Monthly ->
                                    stringResource(id = R.string.earn_payment_frequency_monthly)

                                else ->
                                    stringResource(id = R.string.earn_payment_frequency_unknown)
                            }
                        )

                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()
                StakingWithdrawalNotice(onLearnMorePressed = withdrawDisabledLearnMore)
                LargeVerticalSpacer()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = R.string.common_withdraw),
                        icon = ImageResource.Local(R.drawable.send_off, colorFilter = ColorFilter.tint(Color.White)),
                        state = ButtonState.Disabled,
                        onClick = {},
                    )

                    TinyHorizontalSpacer()

                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = R.string.common_add),
                        icon = ImageResource.Local(R.drawable.receive_off, colorFilter = ColorFilter.tint(Color.White)),
                        onClick = { state.account?.let { onDepositPressed(it) } },
                        state = if (state.canDeposit) ButtonState.Enabled else ButtonState.Disabled,
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
                isWithdrawable = false,
                earnFrequency = EarnRewardsFrequency.Weekly,
                canDeposit = false
            ),
            onWithdrawPressed = {},
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
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(R.string.common_important_information),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Warning,
                gravity = ComposeGravities.Start
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(R.string.earn_staking_withdrawal_blocked),
                style = ComposeTypographies.Caption1, color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            SmallVerticalSpacer()

            SecondaryButton(
                text = stringResource(id = R.string.common_learn_more),
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
fun SummarySheetLoading() {
    Column {
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
    }
}

private fun StakingSummaryViewState.shouldShowWithdrawWarning(): Boolean =
    !isWithdrawable && balanceCrypto?.currency?.networkTicker == "ETH"

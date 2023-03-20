package piuk.blockchain.android.ui.dashboard

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.CustomBackgroundCardView
import com.blockchain.componentlib.viewextensions.configureWithPinnedView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.earn.interest.InterestSummaryBottomSheet
import com.blockchain.extensions.minus
import com.blockchain.fiatActions.BankLinkingHost
import com.blockchain.fiatActions.QuestionnaireSheetHost
import com.blockchain.fiatActions.fiatactions.KycBenefitsSheetHost
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.home.presentation.navigation.HomeLaunch.ACCOUNT_EDIT
import com.blockchain.home.presentation.navigation.HomeLaunch.SETTINGS_EDIT
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.customviews.BlockchainListDividerDecor
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentPortfolioBinding
import piuk.blockchain.android.rating.presentaion.AppRatingFragment
import piuk.blockchain.android.rating.presentaion.AppRatingTriggerSource
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.sheets.SimpleBuyCancelOrderBottomSheet
import piuk.blockchain.android.ui.cowboys.CowboysAnalytics
import piuk.blockchain.android.ui.cowboys.CowboysFlowActivity
import piuk.blockchain.android.ui.cowboys.FlowStep
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.adapter.PortfolioDelegateAdapter
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.dashboard.model.BrokerageFiatAsset
import piuk.blockchain.android.ui.dashboard.model.DashboardAsset
import piuk.blockchain.android.ui.dashboard.model.DashboardCowboysState
import piuk.blockchain.android.ui.dashboard.model.DashboardIntent
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardOnboardingState
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.model.DashboardUIState
import piuk.blockchain.android.ui.dashboard.model.FiatBalanceInfo
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingAnalytics
import piuk.blockchain.android.ui.dashboard.onboarding.toCurrentStepIndex
import piuk.blockchain.android.ui.dashboard.sheets.FiatFundsDetailSheet
import piuk.blockchain.android.ui.dashboard.sheets.ForceBackupForSendSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.home.WalletClientAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.alias.BankAliasLinkContract
import piuk.blockchain.android.ui.locks.LocksDetailsActivity
import piuk.blockchain.android.ui.referral.presentation.ReferralSheet
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class PortfolioFragment :
    HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState, FragmentPortfolioBinding>(),
    ForceBackupForSendSheet.Host,
    KycBenefitsSheetHost,
    QuestionnaireSheetHost,
    BankLinkingHost {

    override val model: DashboardModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPortfolioBinding =
        FragmentPortfolioBinding.inflate(inflater, container, false)

    private val analyticsReporter: BalanceAnalyticsReporter by scopedInject()
    private val assetResources: AssetResources by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val momentLogger: MomentLogger by inject()

    private var activeFiat = currencyPrefs.selectedFiatCurrency

    private val theAdapter: PortfolioDelegateAdapter by lazy {
        PortfolioDelegateAdapter(
            prefs = get(),
            assetCatalogue = get(),
            onCardClicked = { },
            analytics = get(),
            onFundsItemClicked = { onFundsClicked(it) },
            assetResources = assetResources,
            onHoldAmountClicked = { onHoldAmountClicked(it) }
        )
    }

    private val theLayoutManager: RecyclerView.LayoutManager by unsafeLazy {
        SafeLayoutManager(requireContext())
    }

    private val compositeDisposable = CompositeDisposable()

    private val flowToLaunch: AssetAction? by unsafeLazy {
        arguments?.getSerializable(FLOW_TO_LAUNCH) as? AssetAction
    }

    private val flowCurrency: String? by unsafeLazy {
        arguments?.getString(FLOW_FIAT_CURRENCY)
    }

    private var state: DashboardState? =
        null // Hold the 'current' display state, to enable optimising of state updates

    private var questionnaireCallbackIntent: DashboardIntent.LaunchBankTransferFlow? = null

    private val bankAliasLinkLauncher = registerForActivityResult(BankAliasLinkContract()) { linkSuccess ->
        if (linkSuccess) {
            (state?.dashboardNavigationAction as? DashboardNavigationAction.LinkWithAlias)?.let { action ->
                action.fiatAccount?.let {
                    model.process(
                        DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatWithdraw, false)
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)
        analytics.logEvent(WalletClientAnalytics.WalletHomeViewed)

        model.process(DashboardIntent.UpdateDepositButton)
        setupSwipeRefresh()
        setupRecycler()

        if (flowToLaunch != null && flowCurrency != null) {
            when (flowToLaunch) {
                AssetAction.FiatDeposit,
                AssetAction.FiatWithdraw -> model.process(
                    DashboardIntent.StartBankTransferFlow(
                        action = AssetAction.FiatWithdraw
                    )
                )
                else -> throw IllegalStateException("Unsupported flow launch for action $flowToLaunch")
            }
        }
    }

    // For the split dashboard, this onResume is called only once. When the fragment is created.
    // To fix that we need to use a different PagerAdapter (FragmentStateAdapter) with the corresponding behavior
    override fun onResume() {
        super.onResume()

        if (activeFiat != currencyPrefs.selectedFiatCurrency) {
            activeFiat = currencyPrefs.selectedFiatCurrency
            model.process(DashboardIntent.ResetDashboardAssets)
        }
        if (isHidden) return

        model.process(DashboardIntent.FetchOnboardingSteps)
        model.process(DashboardIntent.CheckCowboysFlow)
        model.process(DashboardIntent.GetActiveAssets(loadSilently = true))
        model.process(DashboardIntent.FetchReferralSuccess)
    }

    @UiThread
    override fun render(newState: DashboardState) {
        binding.swipe.isRefreshing = newState.isSwipingToRefresh
        updateDisplayList(newState)
        verifyAppRating(newState)

        if (this.state?.dashboardNavigationAction != newState.dashboardNavigationAction) {
            newState.dashboardNavigationAction?.let { dashboardNavigationAction ->
                handleStateNavigation(dashboardNavigationAction)
            }
        }

        // Update/show announcement
        if (this.state?.announcement != newState.announcement) {
            showAnnouncement(newState.announcement)
        }

        updateAnalytics(this.state, newState)
        updateOnboarding(newState.onboardingState)
        newState.referralSuccessData?.let {
            showReferralSuccess(it)
        }

        renderCowboysFlow(newState.dashboardCowboysState)

        this.state = newState
    }

    private fun updateDisplayList(newState: DashboardState) {
        val items = listOfNotNull(
            newState.dashboardBalance,
            newState.locks.fundsLocks?.let {
                newState.locks
            },
            newState.fiatDashboardAssets.takeIf { it.isNotEmpty() }?.let { FiatBalanceInfo(it) }
        )

        val cryptoAssets = newState.displayableAssets.filterNot { it is BrokerageFiatAsset }.sortedWith(
            compareByDescending<DashboardAsset> {
                it.fiatBalance(useDisplayBalance = it.assetDisplayBalanceFFEnabled)?.toBigInteger()
            }
                .thenByDescending {
                    it.currency.index
                }
                .thenBy { it.currency.name }
        )

        renderState(newState.uiState)
        theAdapter.items = theAdapter.items.filterIsInstance<AnnouncementCard>().plus(items).plus(cryptoAssets)
    }

    private fun renderState(uiState: DashboardUIState) {
        Timber.i("Rendering state $uiState")
        with(binding) {
            when (uiState) {
                DashboardUIState.ASSETS -> {
                    portfolioRecyclerView.visible()
                    dashboardProgress.gone()
                    emptyPortfolioGroup.gone()
                    momentLogger.endEvent(MomentEvent.PIN_TO_DASHBOARD)
                }
                DashboardUIState.EMPTY -> {
                    portfolioRecyclerView.gone()
                    emptyPortfolioGroup.visible()
                    dashboardProgress.gone()
                    momentLogger.endEvent(MomentEvent.PIN_TO_DASHBOARD)
                }
                DashboardUIState.LOADING -> {
                    portfolioRecyclerView.gone()
                    emptyPortfolioGroup.gone()
                    dashboardProgress.visible()
                }
            }
        }
    }

    private fun isDashboardLoading(state: DashboardState): Boolean {
        val atLeastOneAssetIsLoading = state.activeAssets.values.any { it.isUILoading }
        val dashboardLoading = state.isLoadingAssets
        return dashboardLoading || atLeastOneAssetIsLoading
    }

    /**
     * Once the dashboard is fully loaded, and there is money on account
     * -> verify app rating
     */
    private fun verifyAppRating(state: DashboardState) {
        if (isDashboardLoading(state).not() && state.dashboardBalance?.fiatBalance?.isPositive == true) {
            model.process(DashboardIntent.VerifyAppRating)
        }
    }

    private fun handleStateNavigation(navigationAction: DashboardNavigationAction) {
        when (navigationAction) {
            DashboardNavigationAction.AppRating -> {
                showAppRating()
            }
            is DashboardNavigationAction.BottomSheet -> {
                handleBottomSheet(navigationAction)
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.LinkBankWithPartner -> {
                startBankLinking(navigationAction)
            }
            is DashboardNavigationAction.DashboardOnboarding -> {
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.TransactionFlow -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = requireActivity(),
                        sourceAccount = navigationAction.sourceAccount,
                        target = navigationAction.target,
                        action = navigationAction.action
                    )
                )
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.LinkWithAlias -> {
                bankAliasLinkLauncher.launch(
                    navigationAction.fiatAccount?.currency?.networkTicker
                        ?: currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
            is DashboardNavigationAction.BackUpBeforeSend,
            is DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions,
            is DashboardNavigationAction.FiatFundsDetails,
            DashboardNavigationAction.FiatFundsNoKyc,
            is DashboardNavigationAction.InterestSummary,
            is DashboardNavigationAction.LinkOrDeposit,
            is DashboardNavigationAction.PaymentMethods,
            DashboardNavigationAction.SimpleBuyCancelOrder,
            is DashboardNavigationAction.DepositQuestionnaire ->
                Timber.e("Unhandled navigation event $navigationAction")
        }
    }

    private fun showAppRating() {
        AppRatingFragment.newInstance(AppRatingTriggerSource.DASHBOARD)
            .show(childFragmentManager, AppRatingFragment.TAG)
    }

    private fun startBankLinking(action: DashboardNavigationAction.LinkBankWithPartner) {
        activityResultLinkBank.launch(
            BankAuthActivity.newInstance(
                action.linkBankTransfer,
                when (action.assetAction) {
                    AssetAction.FiatDeposit -> {
                        BankAuthSource.DEPOSIT
                    }
                    AssetAction.FiatWithdraw -> {
                        BankAuthSource.WITHDRAW
                    }
                    else -> {
                        throw IllegalStateException("Attempting to link from an unsupported action")
                    }
                },
                requireContext()
            )
        )
    }

    private fun handleBottomSheet(navigationAction: DashboardNavigationAction) {
        showBottomSheet(
            when (navigationAction) {
                is DashboardNavigationAction.BackUpBeforeSend -> ForceBackupForSendSheet.newInstance(
                    navigationAction.backupSheetDetails
                )
                DashboardNavigationAction.SimpleBuyCancelOrder -> {
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_PROMPT)
                    SimpleBuyCancelOrderBottomSheet.newInstance(true)
                }
                is DashboardNavigationAction.FiatFundsDetails -> FiatFundsDetailSheet.newInstance(
                    navigationAction.fiatAccount
                )
                is DashboardNavigationAction.LinkOrDeposit -> {
                    navigationAction.fiatAccount?.let {
                        WireTransferAccountDetailsBottomSheet.newInstance(it, false)
                    } ?: WireTransferAccountDetailsBottomSheet.newInstance()
                }
                is DashboardNavigationAction.PaymentMethods -> {
                    LinkBankMethodChooserBottomSheet.newInstance(
                        navigationAction.paymentMethodsForAction
                    )
                }
                DashboardNavigationAction.FiatFundsNoKyc -> showFiatFundsKyc()
                is DashboardNavigationAction.InterestSummary -> InterestSummaryBottomSheet.newInstance(
                    navigationAction.account.currency.networkTicker
                )
                is DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions ->
                    BlockedDueToSanctionsSheet.newInstance(navigationAction.reason)
                is DashboardNavigationAction.DepositQuestionnaire -> {
                    // todo othman see fiatactionsviewmodel
                    questionnaireCallbackIntent = navigationAction.callbackIntent
                    QuestionnaireSheet.newInstance(navigationAction.questionnaire, true)
                }
                else -> null
            }
        )
    }

    private fun showFiatFundsKyc(): BottomSheetDialogFragment {
        return KycBenefitsBottomSheet.newInstance(
            KycBenefitsBottomSheet.BenefitsDetails(
                title = getString(R.string.fiat_funds_no_kyc_announcement_title),
                description = getString(R.string.fiat_funds_no_kyc_announcement_description),
                listOfBenefits = listOf(
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_1_title),
                        getString(R.string.fiat_funds_no_kyc_step_1_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_2_title),
                        getString(R.string.fiat_funds_no_kyc_step_2_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_3_title),
                        getString(R.string.fiat_funds_no_kyc_step_3_description)
                    )
                ),
                icon = currencyPrefs.selectedFiatCurrency.logo
            )
        )
    }

    private fun showAnnouncement(card: AnnouncementCard?) {
        card?.let {
            theAdapter.items = theAdapter.items.plus(it)
        } ?: kotlin.run {
            theAdapter.items = theAdapter.items.minus { it is AnnouncementCard }
        }
    }

    private fun updateAnalytics(oldState: DashboardState?, newState: DashboardState) {
        analyticsReporter.updateFiatTotal(newState.dashboardBalance?.fiatBalance)

        newState.activeAssets.forEach { (asset, state) ->
            val newBalance = state.accountBalance?.total
            if (newBalance != null && newBalance != oldState?.activeAssets?.getOrNull(asset)?.accountBalance?.total) {
                // If we have the full set, this will fire
                (asset as? AssetInfo)?.let {
                    analyticsReporter.gotAssetBalance(asset, newBalance, newState.activeAssets.size)
                }
            }
        }
    }

    private fun updateOnboarding(newState: DashboardOnboardingState) {
        with(binding.cardOnboarding) {
            binding.portfolioRecyclerView.configureWithPinnedView(this, newState is DashboardOnboardingState.Visible)
            if (newState is DashboardOnboardingState.Visible) {
                val totalSteps = newState.steps.size
                val completeSteps = newState.steps.count { it.isCompleted }
                setTotalSteps(totalSteps)
                setCompleteSteps(completeSteps)
                setOnClickListener {
                    model.process(DashboardIntent.LaunchDashboardOnboarding(newState.steps))
                    newState.steps.toCurrentStepIndex()?.let {
                        analytics.logEvent(DashboardOnboardingAnalytics.CardClicked(it))
                    }
                }
            }
        }
    }

    private fun renderCowboysFlow(cowboysState: DashboardCowboysState) {
        with(binding.cardCowboys) {
            when (cowboysState) {
                is DashboardCowboysState.CowboyWelcomeCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.VerifyEmailAnnouncementClicked)
                            startActivity(
                                CowboysFlowActivity.newIntent(requireContext(), FlowStep.EmailVerification)
                            )
                        }
                    )
                is DashboardCowboysState.CowboyRaffleCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.CompleteSignupAnnouncementClicked)
                            startActivity(
                                CowboysFlowActivity.newIntent(requireContext(), FlowStep.Welcome)
                            )
                        }
                    )
                is DashboardCowboysState.CowboyKycInProgressCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.KycInProgressAnnouncementClicked)
                        }
                    )
                is DashboardCowboysState.CowboyIdentityCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.VerifyIdAnnouncementClicked)
                            startActivity(
                                CowboysFlowActivity.newIntent(requireContext(), FlowStep.Verify)
                            )
                        }
                    )
                is DashboardCowboysState.CowboyReferFriendsCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            if (cowboysState.referralData is ReferralInfo.Data) {
                                analytics.logEvent(CowboysAnalytics.ReferFriendAnnouncementClicked)
                                showBottomSheet(ReferralSheet.newInstance())
                            }
                        },
                        isDismissable = true,
                        onDismiss = {
                            model.process(DashboardIntent.CowboysReferralCardClosed)
                            gone()
                        }
                    )
                is DashboardCowboysState.Hidden -> gone()
            }
        }
    }

    private fun CustomBackgroundCardView.showCowboysCard(
        cardInfo: PromotionStyleInfo,
        onClick: () -> Unit,
        isDismissable: Boolean = false,
        onDismiss: () -> Unit = {}
    ) {
        visible()
        title = cardInfo.title
        subtitle = cardInfo.message
        if (cardInfo.backgroundUrl.isNotEmpty()) {
            backgroundResource = ImageResource.Remote(cardInfo.backgroundUrl)
        }
        if (cardInfo.iconUrl.isNotEmpty()) {
            iconResource = ImageResource.Remote(cardInfo.iconUrl)
        }
        isCloseable = isDismissable
        this.onClick = onClick
        onClose = onDismiss
    }

    private fun setupRecycler() {
        binding.portfolioRecyclerView.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter

            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener {
                model.process(DashboardIntent.OnSwipeToRefresh)
                model.process(DashboardIntent.LoadFundsLocked)
            }

            // Configure the refreshing colors
            swipe.setColorSchemeResources(
                R.color.blue_800,
                R.color.blue_600,
                R.color.blue_400,
                R.color.blue_200
            )
        }
    }

    override fun onPause() {
        saveAssetOrderingLegacy()
        model.process(DashboardIntent.DisposePricesAndBalances)
        compositeDisposable.clear()
        super.onPause()
    }

    @Deprecated("use balances directly for sorting")
    private fun saveAssetOrderingLegacy() {
        // Save the sort order for use elsewhere, so that other asset lists can have the same
        // ordering. Storing this through prefs is a bit of a hack, um, "optimisation" - we don't
        // want to be getting all the balances every time we want to display assets in balance order.
    }

    override fun questionnaireSubmittedSuccessfully() {
        questionnaireCallbackIntent?.let {
            model.process(it)
        }
    }

    override fun questionnaireSkipped() {
        questionnaireCallbackIntent?.let {
            model.process(it)
        }
    }

    private val activityResultLinkBank =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                (state?.dashboardNavigationAction as? DashboardNavigationAction.LinkBankWithPartner)?.let {
                    model.process(
                        DashboardIntent.LaunchBankTransferFlow(
                            it.fiatAccount,
                            it.assetAction,
                            true
                        )
                    )
                }
            }
            model.process(DashboardIntent.ResetNavigation)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SETTINGS_EDIT,
            ACCOUNT_EDIT,
            -> model.process(DashboardIntent.GetActiveAssets(false))
            BACKUP_FUNDS_REQUEST_CODE -> {
                state?.backupSheetDetails?.let {
                    model.process(DashboardIntent.CheckBackupStatus(it.account, it.action))
                }
            }
        }

        model.process(DashboardIntent.ResetNavigation)
    }

    private fun onFundsClicked(fiatAccount: FiatAccount) {
        analytics.logEvent(
            fiatAssetAction(AssetDetailsAnalytics.FIAT_DETAIL_CLICKED, fiatAccount.currency.networkTicker)
        )
        model.process(DashboardIntent.ShowFiatAssetDetails(fiatAccount))
    }

    private fun onHoldAmountClicked(locks: Locks) {
        require(locks.fundsLocks != null) { "funds are null" }
        LocksDetailsActivity.start(requireContext(), locks.fundsLocks)
    }

    private fun showReferralSuccess(successData: Pair<String, String>) {
        binding.referralSuccess.apply {
            title = successData.first
            subtitle = successData.second
            onClose = {
                model.process(DashboardIntent.DismissReferralSuccess)
                gone()
            }
            visible()
        }
    }

    // DialogBottomSheet.Host
    override fun onSheetClosed() {
        model.process(DashboardIntent.ClearActiveFlow)
    }

    // BankLinkingHost
    override fun onBankWireTransferSelected(currency: FiatCurrency) {
        state?.selectedFiatAccount?.let {
            model.process(DashboardIntent.ShowBankLinkingSheet(it))
        }
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        state?.selectedFiatAccount?.let {
            if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit) {
                model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatDeposit, true))
            } else if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw) {
                model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatWithdraw, true))
            }
        }
    }

    // KycBenefitsBottomSheet.Host
    override fun verificationCtaClicked() {
        navigator().launchKyc(CampaignType.FiatFunds)
    }

    // ForceBackupForSendSheet.Host
    override fun startBackupForTransfer() {
        navigator().launchBackupFunds(this, BACKUP_FUNDS_REQUEST_CODE)
    }

    override fun startTransferFunds(account: SingleAccount, action: AssetAction) {
        if (account is CryptoAccount) {
            model.process(
                DashboardIntent.UpdateNavigationAction(
                    DashboardNavigationAction.TransactionFlow(
                        sourceAccount = account,
                        action = action
                    )
                )
            )
        }
    }

    companion object {
        fun newInstance(
            flowToLaunch: AssetAction? = null,
            fiatCurrency: String? = null,
        ) = PortfolioFragment().apply {
            arguments = Bundle().apply {
                if (flowToLaunch != null && fiatCurrency != null) {
                    putSerializable(FLOW_TO_LAUNCH, flowToLaunch)
                    putString(FLOW_FIAT_CURRENCY, fiatCurrency)
                }
            }
        }

        internal const val FLOW_TO_LAUNCH = "FLOW_TO_LAUNCH"
        internal const val FLOW_FIAT_CURRENCY = "FLOW_FIAT_CURRENCY"

        const val BACKUP_FUNDS_REQUEST_CODE = 8265
    }
}

internal class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}

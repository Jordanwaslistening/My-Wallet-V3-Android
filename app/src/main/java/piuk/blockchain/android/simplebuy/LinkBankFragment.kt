package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import kotlinx.android.synthetic.main.fragment_link_a_bank.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class LinkBankFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(), SimpleBuyScreen {

    override val model: SimpleBuyModel by scopedInject()

    private val accountProviderId: String by lazy {
        arguments?.getString(ACCOUNT_PROVIDER_ID) ?: ""
    }

    private val errorState: ErrorState? by unsafeLazy {
        arguments?.getSerializable(ERROR_STATE) as? ErrorState
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_link_a_bank)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null && accountProviderId.isNotEmpty()) {
            model.process(SimpleBuyIntent.UpdateAccountProvider(accountProviderId))
        }
        activity.setupToolbar(R.string.link_a_bank, false)
    }

    override fun render(newState: SimpleBuyState) {

        if (newState.isLoading) {
            showLinkingInProgress()
        }

        val error = newState.errorState ?: errorState
        error?.let {
            showErrorState(it)
        }

        if (!newState.isLoading && error == null) {
            newState.selectedPaymentMethod?.label?.let {
                if (it.isNotEmpty()) {
                    showLinkingSuccess(it)
                }
            }
        }
    }

    private fun showErrorState(state: ErrorState) {
        // TODO add states for bank already linked and other API failures
        val errorText = when (state) {
            ErrorState.BankLinkingUpdateFailed -> {
                getString(R.string.yodlee_linking_generic_error_subtitle)
            }
            ErrorState.BankLinkingFailed -> {
                getString(R.string.yodlee_linking_generic_error_subtitle)
            }
            ErrorState.BankLinkingTimeout -> {
                getString(R.string.yodlee_linking_timeout_error_subtitle)
            }
            ErrorState.LinkedBankAlreadyLinked -> {
                getString(R.string.yodlee_linking_already_linked_error_subtitle)
            }
            ErrorState.LinkedBankNotSupported -> {
                getString(R.string.yodlee_linking_generic_error_subtitle)
            }
            else -> {
                getString(R.string.yodlee_linking_generic_error_subtitle)
            }
        }

        link_bank_icon.setImageResource(R.drawable.ic_bank_details_big)
        link_bank_progress.gone()
        link_bank_state_indicator.setImageResource(R.drawable.ic_alert_white_bkgd)
        link_bank_state_indicator.visible()
        link_bank_btn.text = getString(R.string.common_try_again)
        link_bank_btn.visible()
        link_bank_btn.setOnClickListener {
            navigator().pop()
        }
        link_bank_cancel.visible()
        link_bank_cancel.setOnClickListener {
            navigator().exitSimpleBuyFlow()
        }
        link_bank_title.text = getString(R.string.yodlee_linking_generic_error_title)
        link_bank_subtitle.text = errorText
    }

    private fun showLinkingInProgress() {
        link_bank_icon.setImageResource(R.drawable.ic_blockchain_blue_circle)
        link_bank_progress.visible()
        link_bank_state_indicator.gone()
        link_bank_btn.gone()
        link_bank_title.text = getString(R.string.yodlee_linking_title)
        link_bank_subtitle.text = getString(R.string.yodlee_linking_subtitle)
    }

    private fun showLinkingSuccess(label: String) {
        link_bank_icon.setImageResource(R.drawable.ic_bank_details_big)
        link_bank_progress.gone()
        link_bank_state_indicator.setImageResource(R.drawable.ic_check_circle)
        link_bank_state_indicator.visible()
        link_bank_btn.visible()
        link_bank_btn.setOnClickListener {
            navigator().goToCheckOutScreen(false)
        }
        link_bank_title.text = getString(R.string.yodlee_linking_success_title)
        link_bank_subtitle.text = getString(R.string.yodlee_linking_success_subtitle, label)
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator)
            ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    companion object {
        private const val ACCOUNT_PROVIDER_ID = "ACCOUNT_PROVIDER_ID"
        private const val ERROR_STATE = "ERROR_STATE"

        fun newInstance(accountProviderId: String) = LinkBankFragment().apply {
            arguments = Bundle().apply {
                putString(ACCOUNT_PROVIDER_ID, accountProviderId)
            }
        }

        fun newInstance(errorState: ErrorState) = LinkBankFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ERROR_STATE, errorState)
            }
        }
    }
}
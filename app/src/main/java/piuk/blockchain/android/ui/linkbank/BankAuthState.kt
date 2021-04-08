package piuk.blockchain.android.ui.linkbank

import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.google.gson.Gson
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod
import piuk.blockchain.android.ui.base.mvi.MviState
import java.io.Serializable

data class BankAuthState(
    val id: String? = null,
    val linkedBank: LinkedBank? = null,
    val linkBankTransfer: LinkBankTransfer? = null,
    val linkBankUrl: String? = null,
    val bankLinkingProcessState: BankLinkingProcessState = BankLinkingProcessState.NONE,
    val errorState: ErrorState? = null,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val authorisePaymentUrl: String? = null
) : MviState

enum class BankLinkingProcessState {
    LINKING,
    IN_EXTERNAL_FLOW,
    APPROVAL,
    APPROVAL_WAIT,
    ACTIVATING,
    LINKING_SUCCESS,
    CANCELED,
    NONE
}

data class BankLinkingInfo(
    val linkingId: String,
    val bankAuthSource: BankAuthSource
) {

    companion object {
        private val gson = Gson()
        fun fromJson(json: String): BankLinkingInfo = gson.fromJson(json, BankLinkingInfo::class.java)
        fun toJson(bankLinkingInfo: BankLinkingInfo): String = gson.toJson(bankLinkingInfo, BankLinkingInfo::class.java)
    }
}

enum class BankAuthSource {
    SIMPLE_BUY,
    SETTINGS,
    DEPOSIT,
    WITHDRAW
}

data class BankPaymentApproval(
    val authorisationUrl: String,
    val linkedBank: LinkedBank,
    val orderValue: FiatValue
) : Serializable {
    companion object {
        private val gson = Gson()
        fun fromJson(json: String): BankPaymentApproval = gson.fromJson(json, BankPaymentApproval::class.java)
        fun toJson(bankPaymentData: BankPaymentApproval): String =
            gson.toJson(bankPaymentData, BankPaymentApproval::class.java)
    }
}

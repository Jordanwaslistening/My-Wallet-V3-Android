package piuk.blockchain.android.ui.base.mvi

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.blockchain.notifications.analytics.Analytics
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.BlockchainActivity
import timber.log.Timber
import java.lang.IllegalStateException
import java.util.Locale

abstract class MviFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState> : Fragment() {

    protected abstract val model: M

    var subscription: Disposable? = null

    override fun onResume() {
        super.onResume()
        subscription?.dispose()
        subscription = model.state.subscribeBy(
            onNext = { render(it) },
            onError = { Timber.e(it) },
            onComplete = { Timber.d("***> State on complete!!") }
        )
    }

    override fun onPause() {
        subscription?.dispose()
        subscription = null
        super.onPause()
    }

    override fun onDestroy() {
        model.destroy()
        super.onDestroy()
    }

    protected abstract fun render(newState: S)

    protected val activity: BlockchainActivity
        get() = requireActivity() as? BlockchainActivity
            ?: throw IllegalStateException("Root activity is not a BlockchainActivity")

    protected val analytics: Analytics
        get() = activity.analytics

    protected val locale: Locale
        get() = activity.locale

    @UiThread
    protected fun showAlert(dlg: AlertDialog) = activity.showAlert(dlg)

    @UiThread
    protected fun clearAlert() = activity.clearAlert()

    @UiThread
    fun showProgressDialog(@StringRes messageId: Int, onCancel: (() -> Unit)? = null) =
        activity.showProgressDialog(messageId, onCancel)

    @UiThread
    fun dismissProgressDialog() = activity.dismissProgressDialog()

    @UiThread
    fun updateProgressDialog(msg: String) = activity.updateProgressDialog(msg)

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment) =
        bottomSheet.show(childFragmentManager, "BOTTOM_SHEET")
}

package org.github.keepasscompose

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import io.github.aakira.napier.Napier

/**
 * Android Autofill Service that provides KeePass credential suggestions
 * to autofill-enabled apps and browsers.
 *
 * Users must enable this service in Settings > Passwords & accounts > Autofill service.
 */
class KeePassAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        // TODO: Parse the AssistStructure from request.fillContexts to identify
        //  autofill-eligible fields (username, password, etc.)
        // TODO: Look up matching credentials from the unlocked KeePass database
        // TODO: Build Dataset objects with RemoteViews for the autofill dropdown
        // TODO: Return a FillResponse via callback.onSuccess()
        Napier.d(tag = TAG) { "onFillRequest received" }
        callback.onSuccess(null)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // TODO: Extract filled field values from request.fillContexts
        // TODO: Prompt the user to save new or updated credentials to the KeePass database
        Napier.d(tag = TAG) { "onSaveRequest received" }
        callback.onSuccess()
    }

    companion object {
        private const val TAG = "KeePassAutofillService"
    }
}

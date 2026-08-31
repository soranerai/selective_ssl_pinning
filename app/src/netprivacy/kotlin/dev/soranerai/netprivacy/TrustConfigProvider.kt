package dev.soranerai.netprivacy

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import dev.soranerai.netprivacy.data.TrustConfigStore
import dev.soranerai.netprivacy.data.toBundle

/** Exported read-only endpoint; CA files remain private to this module's storage. */
class TrustConfigProvider : ContentProvider() {
    private lateinit var store: TrustConfigStore
    override fun onCreate(): Boolean { store = TrustConfigStore(requireNotNull(context)); return true }
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = if (method == "read_config") store.read().toBundle() else null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

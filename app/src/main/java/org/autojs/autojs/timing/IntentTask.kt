package org.autojs.autojs.timing

import org.autojs.autojs.storage.database.BaseModel
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException

class IntentTask : BaseModel() {
    var scriptPath: String? = null
    var action: String? = null
    var category: String? = null
    var dataType: String? = null
    var isLocal = false
    val intentFilter: IntentFilter
        get() {
            val filter = IntentFilter()
            if (action != null) {
                filter.addAction(action)
            }
            if (this.category != null) {
                filter.addCategory(this.category)
            }
            if (dataType != null) {
                try {
                    filter.addDataType(dataType)
                } catch (e: MalformedMimeTypeException) {
                    e.printStackTrace()
                }
            }
            return filter
        }

    companion object {
        const val TABLE = "IntentTask"
    }
}
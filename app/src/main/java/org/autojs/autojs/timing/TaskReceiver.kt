package org.autojs.autojs.timing


import org.autojs.autojs.external.ScriptIntents
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


/**
 * Created by Stardust on 2017/11/27.
 */
class TaskReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ScriptIntents.handleIntent(context, intent)
        val id = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (id >= 0) {
            TimedTaskManager.instance?.notifyTaskFinished(id)
        }
    }

    companion object {
        const val ACTION_TASK = "com.stardust.autojs.action.task"
        const val EXTRA_TASK_ID = "task_id"
    }
}
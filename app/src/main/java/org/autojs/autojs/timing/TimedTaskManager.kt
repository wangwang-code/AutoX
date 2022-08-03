package org.autojs.autojs.timing


import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.stardust.app.GlobalAppContext.get
import io.reactivex.Flowable
import io.reactivex.Observable
import org.autojs.autojs.App.Companion.app
import org.autojs.autojs.storage.database.IntentTaskDatabase
import org.autojs.autojs.storage.database.ModelChange
import org.autojs.autojs.storage.database.TimedTaskDatabase
import org.autojs.autojs.tool.Observers

/**
 * Created by Stardust on 2017/11/27.
 */
//TODO rx
class TimedTaskManager @SuppressLint("CheckResult") constructor(private val mContext: Context) {
    private val mTimedTaskDatabase: TimedTaskDatabase
    private val mIntentTaskDatabase: IntentTaskDatabase
    @SuppressLint("CheckResult")
    fun notifyTaskFinished(id: Long) {
        val task = getTimedTask(id) ?: return
        if (task.isDisposable) {
            mTimedTaskDatabase.delete(task)
                .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
        } else {
            task.isScheduled = false
            mTimedTaskDatabase.update(task)
                .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
        }
    }

    @SuppressLint("CheckResult")
    fun removeTask(timedTask: TimedTask) {
        TimedTaskScheduler.cancel(timedTask)
        mTimedTaskDatabase.delete(timedTask)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun addTask(timedTask: TimedTask) {
        mTimedTaskDatabase.insert(timedTask)
            .subscribe({ id: Long? ->
                timedTask.id = id!!
                TimedTaskScheduler.scheduleTaskIfNeeded(mContext, timedTask, false)
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun addTask(intentTask: IntentTask) {
        mIntentTaskDatabase.insert(intentTask)
            .subscribe({ i: Long? ->
                if (!TextUtils.isEmpty(intentTask.action)) {
                    app.dynamicBroadcastReceivers
                        .register(intentTask)
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun removeTask(intentTask: IntentTask) {
        mIntentTaskDatabase.delete(intentTask)
            .subscribe({ i: Int? ->
                if (!TextUtils.isEmpty(intentTask.action)) {
                    app.dynamicBroadcastReceivers
                        .unregister(intentTask.action)
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    val allTasks: Flowable<TimedTask>
        get() = mTimedTaskDatabase.queryAllAsFlowable()

    fun getIntentTaskOfAction(action: String?): Flowable<IntentTask> {
        return mIntentTaskDatabase.query("action = ?", action)
    }

    val timeTaskChanges: Observable<ModelChange<TimedTask>>
        get() = mTimedTaskDatabase.modelChange

    @SuppressLint("CheckResult")
    fun notifyTaskScheduled(timedTask: TimedTask) {
        timedTask.isScheduled = true
        mTimedTaskDatabase.update(timedTask)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
    }

    val allTasksAsList: List<TimedTask>
        get() = mTimedTaskDatabase.queryAll()

    fun getTimedTask(taskId: Long): TimedTask {
        return mTimedTaskDatabase.queryById(taskId)
    }

    @SuppressLint("CheckResult")
    fun updateTask(task: TimedTask) {
        mTimedTaskDatabase.update(task)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
        TimedTaskScheduler.cancel(task)
        TimedTaskScheduler.scheduleTaskIfNeeded(mContext, task, false)
    }

    @SuppressLint("CheckResult")
    fun updateTaskWithoutReScheduling(task: TimedTask) {
        mTimedTaskDatabase.update(task)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun updateTask(task: IntentTask) {
        mIntentTaskDatabase.update(task)
            .subscribe({ i: Int ->
                if (i > 0 && !TextUtils.isEmpty(task.action)) {
                    app.dynamicBroadcastReceivers
                        .register(task)
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    fun countTasks(): Long {
        return mTimedTaskDatabase.count()
    }

    val allIntentTasksAsList: List<IntentTask>
        get() = mIntentTaskDatabase.queryAll()
    val intentTaskChanges: Observable<ModelChange<IntentTask>>
        get() = mIntentTaskDatabase.modelChange

    fun getIntentTask(intentTaskId: Long): IntentTask {
        return mIntentTaskDatabase.queryById(intentTaskId)
    }

    val allIntentTasks: Flowable<IntentTask>
        get() = mIntentTaskDatabase.queryAllAsFlowable()

    companion object {
        private var sInstance: TimedTaskManager? = null
        @JvmStatic
        val instance: TimedTaskManager?
            get() {
                if (sInstance == null) {
                    sInstance = TimedTaskManager(get())
                }
                return sInstance
            }
    }

    init {
        mTimedTaskDatabase = TimedTaskDatabase(mContext)
        mIntentTaskDatabase = IntentTaskDatabase(mContext)
    }
}
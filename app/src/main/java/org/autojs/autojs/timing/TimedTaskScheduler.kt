package org.autojs.autojs.timing

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.external.ScriptIntents
import java.util.concurrent.TimeUnit

/**
 * Created by Stardust on 2017/11/27.
 */
object TimedTaskScheduler {
    private const val LOG_TAG = "TimedTaskScheduler"
    private val SCHEDULE_TASK_MIN_TIME = TimeUnit.DAYS.toMillis(2)
    private const val JOB_TAG_CHECK_TASKS = "checkTasks"
    @SuppressLint("CheckResult")
    fun checkTasks(context: Context?, force: Boolean) {
        Log.d(LOG_TAG, "check tasks: force = $force")
        TimedTaskManager.instance?.allTasks
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(Consumer { timedTask: TimedTask ->
                scheduleTaskIfNeeded(
                    context,
                    timedTask,
                    force
                )
            })
    }

    fun scheduleTaskIfNeeded(context: Context?, timedTask: TimedTask, force: Boolean) {
        val millis = timedTask.nextTime
        if (!force && timedTask.isScheduled || millis - System.currentTimeMillis() > SCHEDULE_TASK_MIN_TIME) {
            return
        }
        scheduleTask(context, timedTask, millis, force)
        TimedTaskManager.instance
            ?.notifyTaskScheduled(timedTask)
    }

    @Synchronized
    private fun scheduleTask(
        context: Context?,
        timedTask: TimedTask,
        millis: Long,
        force: Boolean
    ) {
        if (!force && timedTask.isScheduled) {
            return
        }
        val timeWindow = millis - System.currentTimeMillis()
        timedTask.isScheduled = true
        TimedTaskManager.instance?.updateTaskWithoutReScheduling(timedTask)
        if (timeWindow <= 0) {
            runTask(context, timedTask)
            return
        }
        cancel(timedTask)
        Log.d(
            LOG_TAG,
            "schedule task: task = $timedTask, millis = $millis, timeWindow = $timeWindow"
        )
        JobRequest.Builder(timedTask.id.toString())
            .setExact(timeWindow)
            .build()
            .schedule()
    }

    fun cancel(timedTask: TimedTask) {
        val cancelCount = JobManager.instance().cancelAllForTag(timedTask.id.toString())
        Log.d(LOG_TAG, "cancel task: task = $timedTask, cancel = $cancelCount")
    }

    fun init(context: Context) {
        JobManager.create(context).addJobCreator { tag: String ->
            if (tag == JOB_TAG_CHECK_TASKS) {
                return@addJobCreator CheckTasksJob(context)
            } else {
                return@addJobCreator TimedTaskJob(context)
            }
        }
        JobRequest.Builder(JOB_TAG_CHECK_TASKS)
            .setPeriodic(TimeUnit.MINUTES.toMillis(20))
            .build()
            .scheduleAsync()
        checkTasks(context, true)
    }

    fun runTask(context: Context?, task: TimedTask) {
        Log.d(LOG_TAG, "run task: task = $task")
        val intent = task.createIntent()
        ScriptIntents.handleIntent(context, intent)
        TimedTaskManager.Companion.instance?.notifyTaskFinished(task.id)
    }

    private class TimedTaskJob internal constructor(private val mContext: Context) : Job() {
        override fun onRunJob(params: Params): Result {
            val id = params.tag.toLong()
            val task: TimedTask = TimedTaskManager.instance?.getTimedTask(id)!!
            Log.d(LOG_TAG, "onRunJob: id = $id, task = $task")
            if (task == null) {
                return Result.FAILURE
            }
            runTask(mContext, task)
            return Result.SUCCESS
        }
    }

    private class CheckTasksJob internal constructor(private val mContext: Context) : Job() {
        override fun onRunJob(params: Params): Result {
            checkTasks(mContext, false)
            return Result.SUCCESS
        }
    }
}
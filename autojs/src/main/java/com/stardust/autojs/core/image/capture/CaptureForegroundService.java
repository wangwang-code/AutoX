package com.stardust.autojs.core.image.capture;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.stardust.autojs.R;
/**
 * by zhang_senlin https://blog.csdn.net/zhang_senlin/article/details/119481636
 */
public class CaptureForegroundService extends Service {
    private final static int SERVICE_ID = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // 4.3以下
            startForeground(SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 7.0以下
            startForeground(SERVICE_ID, new Notification());
            // 删除通知栏
            startService(new Intent(this, InnerService.class));
        } else {
            // 8.0以上
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // NotificationManager.IMPORTANCE_MIN 通知栏消息的重要级别 最低，不让弹出
            // IMPORTANCE_MIN 前台时，在阴影区能看到，后台时 阴影区不消失，增加显示 IMPORTANCE_NONE时 一样的提示
            // IMPORTANCE_NONE app在前台没有通知显示，后台时有
            NotificationChannel channel = new NotificationChannel("channel", "keep",
                    NotificationManager.IMPORTANCE_NONE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Notification notification = new Notification.Builder(this, "channel").build();
                startForeground(SERVICE_ID, notification);
            }

        }
    }

    private static class InnerService extends Service {
        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            // Log.d(KeepAliveApp.TAG, "onCreate: ");
            startForeground(SERVICE_ID, new Notification());
            stopSelf();
        }
    }
}
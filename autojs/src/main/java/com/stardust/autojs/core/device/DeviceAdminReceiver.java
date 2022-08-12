package com.stardust.autojs.core.device;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "设备管理权限已激活");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "设备管理权限未激活");
    }

}


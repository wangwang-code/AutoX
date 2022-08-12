"ui";
ui.layout(
    <vertical>
        <text text="Auto.Js" textSize="24sp"  w="*" gravity="center">
        </text>
        <button id="isAdminActive" textSize="40sp">
             检测设备管理权限
         </button>
        <button id="requestAdmin" textSize="40sp">
             申请设备管理权限
         </button>
        <button id="lockNow" textSize="56sp">
            一键锁屏
        </button>
        <button id="lockLater" textSize="56sp">
            延时锁屏
        </button>
        <button id="removeActiveAdmin" bg="#f5222d" textSize="40sp">
            移除设备管理权限
        </button>
    </vertical>
);
importClass("android.app.admin.DevicePolicyManager")
importClass("android.content.ComponentName")
importClass("com.stardust.autojs.core.device.DeviceAdminReceiver")
importClass("android.content.Context")
var adminReceiver = new ComponentName(activity, DeviceAdminReceiver);
var devicePolicyManager = activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
ui.isAdminActive.click(function() {
    if (devicePolicyManager.isAdminActive(adminReceiver)) {//判断超级管理员是否激活
        toastLog("设备管理权限已激活");
    } else {
        toastLog("设备管理权限未激活");
    }
})
ui.requestAdmin.click(function() {
    let intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "设备管理权限");
    activity.startActivityForResult(intent, 284);
})
ui.lockNow.click(function() {
    if (devicePolicyManager.isAdminActive(adminReceiver)) {
        devicePolicyManager.lockNow();
    } else {
        toastLog("请先激活设备管理权限！");
    }
})
ui.lockLater.click(function() {
    if (devicePolicyManager.isAdminActive(adminReceiver)) {
        let seconds = 10
        toastLog(seconds + "秒后锁屏！");
        setTimeout(function () {
            devicePolicyManager.lockNow();
        }, seconds * 1000);
    } else {
        toastLog("请先激活设备管理权限！");
    }
})
ui.removeActiveAdmin.click(function() {
    devicePolicyManager.removeActiveAdmin(adminReceiver);
})
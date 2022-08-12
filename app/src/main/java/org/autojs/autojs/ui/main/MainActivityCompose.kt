package org.autojs.autojs.ui.main

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.gson.Gson
import com.stardust.app.permission.DrawOverlaysPermission
import com.stardust.app.permission.PermissionsSettingsUtil
import com.stardust.util.IntentUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.ui.compose.theme.AutoXJsTheme
import org.autojs.autojs.ui.compose.widget.MyIcon
import org.autojs.autojs.ui.compose.widget.SearchBox2
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.log.LogActivityKt
import org.autojs.autojs.ui.main.drawer.DrawerPage
import org.autojs.autojs.ui.main.scripts.ScriptListFragment
import org.autojs.autojs.ui.main.task.TaskManagerFragmentKt
import org.autojs.autojs.ui.main.web.WebData
import org.autojs.autojs.ui.main.web.WebViewFragment
import org.autojs.autojs.ui.widget.fillMaxSize


data class BottomNavigationItem(val icon: Int, val label: String)

class MainActivityCompose : FragmentActivity() {

    companion object {
        @JvmStatic
        fun getIntent(context: Context) = Intent(context, MainActivityCompose::class.java)
    }

    private val scriptListFragment by lazy { ScriptListFragment() }
    private val taskManagerFragment by lazy { TaskManagerFragmentKt() }
    private val webViewFragment by lazy { WebViewFragment() }
    private var lastBackPressedTime = 0L
    private var drawerState: DrawerState? = null
    private val viewPager: ViewPager2 by lazy { ViewPager2(this) }
    private var scope: CoroutineScope? = null

    @RequiresApi(Build.VERSION_CODES.M)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Pref.isForegroundServiceEnabled()) ForegroundService.start(this)
        else ForegroundService.stop(this)

        if (Pref.isFloatingMenuShown() && !FloatyWindowManger.isCircularMenuShowing()) {
            if (DrawOverlaysPermission.isCanDrawOverlays(this)) FloatyWindowManger.showCircularMenu()
            else Pref.setFloatingMenuShown(false)
        }
        setContent {
            scope = rememberCoroutineScope()
            AutoXJsTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val permission = rememberExternalStoragePermissionsState {
                        if (it) {
                            scriptListFragment.explorerView.onRefresh()
                        }
                    }
                    LaunchedEffect(key1 = Unit, block = {
                        permission.launchMultiplePermissionRequest()
                    })
                    MainPage(
                        activity = this,
                        scriptListFragment = scriptListFragment,
                        taskManagerFragment = taskManagerFragment,
                        webViewFragment = webViewFragment,
                        onDrawerState = {
                            this.drawerState = it
                        },
                        viewPager = viewPager
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerState?.isOpen == true) {
            scope?.launch { drawerState?.close() }
            return
        }
        if (viewPager.currentItem == 0 && scriptListFragment.onBackPressed()) {
            return
        } else if (viewPager.currentItem == 2 && webViewFragment.onBackPressed()) {
            return
        }
        back()
    }

    private fun back() {
        val currentTime = System.currentTimeMillis()
        val interval = currentTime - lastBackPressedTime
        if (interval > 2000) {
            lastBackPressedTime = currentTime
            Toast.makeText(
                this,
                getString(R.string.text_press_again_to_exit),
                Toast.LENGTH_SHORT
            ).show()
        } else super.onBackPressed()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun MainPage(
    activity: FragmentActivity,
    scriptListFragment: ScriptListFragment,
    taskManagerFragment: TaskManagerFragmentKt,
    webViewFragment: WebViewFragment,
    onDrawerState: (DrawerState) -> Unit,
    viewPager: ViewPager2
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    onDrawerState(scaffoldState.drawerState)
    val scope = rememberCoroutineScope()

    val bottomBarItems = remember {
        getBottomItems(context)
    }
    var currentPage by remember {
        mutableStateOf(0)
    }

    SetSystemUI(scaffoldState)
    viewPager.offscreenPageLimit = 3; //设置ViewPager的缓存界面数，此方案适用于界面数较少的情况，滑动切换界面时非活动界面销毁过于频繁。
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        topBar = {
            Surface(elevation = 0.dp, color = MaterialTheme.colors.primarySurface) {
                Column() {
                    Spacer(
                        modifier = Modifier
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                    )
                    TopBar(
                        currentPage = currentPage,
                        requestOpenDrawer = {
                            scope.launch { scaffoldState.drawerState.open() }
                        },
                        onSearch = { keyword ->
                            scriptListFragment.explorerView.setFilter { it.name.contains(keyword) }
                        },
                        scriptListFragment = scriptListFragment,
                        webViewFragment = webViewFragment,
                    )
                }
            }
        },
        bottomBar = {
            Surface(elevation = 0.dp, color = MaterialTheme.colors.surface) {
                Column {
                    BottomBar(bottomBarItems, currentPage, onSelectedChange = { currentPage = it })
                    Spacer(
                        modifier = Modifier
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    )
                }
            }
        },
        drawerContent = {
            DrawerPage()
        },

        ) {
        AndroidView(
            modifier = Modifier.padding(it),
            factory = {
                viewPager.apply {
                    fillMaxSize()
                    adapter = ViewPager2Adapter(
                        activity,
                        scriptListFragment,
                        taskManagerFragment,
                        webViewFragment
                    )
                    isUserInputEnabled = false
                    ViewCompat.setNestedScrollingEnabled(this, true)
                }
            },
            update = { viewPager0 ->
                viewPager0.currentItem = currentPage
            }
        )
    }
}

fun showExternalStoragePermissionToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.text_please_enable_external_storage),
        Toast.LENGTH_SHORT
    ).show()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberExternalStoragePermissionsState(onPermissionsResult: (allAllow: Boolean) -> Unit) =
    rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        onPermissionsResult = { map ->
            onPermissionsResult(map.all { it.value })
        })

@Composable
private fun SetSystemUI(scaffoldState: ScaffoldState) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons =
        if (MaterialTheme.colors.isLight) {
            scaffoldState.drawerState.isOpen || scaffoldState.drawerState.isAnimationRunning
        } else false

    val navigationUseDarkIcons = MaterialTheme.colors.isLight
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
        systemUiController.setNavigationBarColor(
            Color.Transparent,
            darkIcons = navigationUseDarkIcons
        )
    }
}

private fun getBottomItems(context: Context) = mutableStateListOf(
    BottomNavigationItem(
        R.drawable.ic_home,
        context.getString(R.string.text_home)
    ),
    BottomNavigationItem(
        R.drawable.ic_manage,
        context.getString(R.string.text_management)
    ),
    BottomNavigationItem(
        R.drawable.ic_web,
        context.getString(R.string.text_document)
    )
)

@Composable
fun BottomBar(
    items: List<BottomNavigationItem>,
    currentSelected: Int,
    onSelectedChange: (Int) -> Unit
) {
    BottomNavigation(elevation = 0.dp, backgroundColor = MaterialTheme.colors.background) {
        items.forEachIndexed { index, item ->
            val selected = currentSelected == index
            val color = if (selected) MaterialTheme.colors.primary else Color.Gray
            BottomNavigationItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onSelectedChange(index)
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                        tint = color
                    )
                },
                label = {
                    Text(text = item.label, color = color)
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
private fun TopBar(
    currentPage: Int,
    requestOpenDrawer: () -> Unit,
    onSearch: (String) -> Unit,
    scriptListFragment: ScriptListFragment,
    webViewFragment: WebViewFragment,
) {
    var isSearch by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    TopAppBar(elevation = 0.dp) {
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.high,
        ) {
            if (!isSearch) {
                IconButton(onClick = requestOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.text_menu),
                    )
                }

                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.app_name)
                    )
                }

                IconButton(onClick = { isSearch = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(id = R.string.text_search)
                    )
                }
            } else {
                IconButton(onClick = { isSearch = false }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.text_exit_search)
                    )
                }

                var keyword by remember {
                    mutableStateOf("")
                }
                SearchBox2(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(text = stringResource(id = R.string.text_search)) },
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearch(keyword)
                    })
                )
            }
            IconButton(onClick = { LogActivityKt.start(context) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logcat),
                    contentDescription = stringResource(id = R.string.text_logcat)
                )
            }
            var expanded by remember {
                mutableStateOf(false)
            }
            Box() {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.desc_more)
                    )
                }
                TopAppBarMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    scriptListFragment = scriptListFragment,
                    webViewFragment = webViewFragment,
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TopAppBarMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    scriptListFragment: ScriptListFragment,
    webViewFragment: WebViewFragment
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, offset = offset) {
        val context = LocalContext.current
        WebKernel(context)
        TtsSetting(context)
        TtsEngineUpdate(context)
        FileManagerPermission(context)
        IgnoreBatteryOptimizations(context)
        AppDetailSettings(context)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun WebKernel(context: Context) {
    lateinit var mWebData: WebData
    val gson = Gson()
    if (Pref.getWebData().contains("isTbs")) {
        mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
    } else {
        mWebData = WebData()
        Pref.setWebData(gson.toJson(mWebData))
    }
    DropdownMenuItem(onClick = {
        mWebData.isTbs = !mWebData.isTbs;
        Pref.setWebData(gson.toJson(mWebData));
        if (mWebData.isTbs) {
            Toast.makeText(context, "默认Web内核已切换为：TBS WebView，重启APP后生效！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "默认Web内核已切换为：系统 WebView，重启APP后生效！", Toast.LENGTH_SHORT).show();
        }
    }) {
//        MyIcon(
//            painter = painterResource(id = R.drawable.ic_web),
//            contentDescription = null
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_switch_web_kernel))
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun IgnoreBatteryOptimizations(context: Context) {
    lateinit var mWebData: WebData
    val gson = Gson()
    if (Pref.getWebData().contains("isTbs")) {
        mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
    } else {
        mWebData = WebData()
        Pref.setWebData(gson.toJson(mWebData))
    }
    DropdownMenuItem(onClick = {
        try {
            val intent0 = Intent()
            val isIgnoring =
                (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                    context.packageName
                )
            if (isIgnoring) {
                intent0.action =
                    android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            } else {
                intent0.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).data =
                    Uri.parse("package:" + context.packageName)
            }
            context.startActivity(intent0)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }) {
//        MyIcon(
//            painter = painterResource(id = R.drawable.ic_battery),
//            contentDescription = null
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_ignore_battery_optimizations))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FileManagerPermission(context: Context) {
    lateinit var mWebData: WebData
    val gson = Gson()
    if (Pref.getWebData().contains("isTbs")) {
        mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
    } else {
        mWebData = WebData()
        Pref.setWebData(gson.toJson(mWebData))
    }
    DropdownMenuItem(onClick = {
        if (Build.VERSION.SDK_INT >= 30) {
            MaterialDialog.Builder(context)
                .title("所有文件访问权限")
                .content("在Android 11及以上的系统中，读写非应用目录外文件需要授予“所有文件访问权限”（右侧侧滑菜单中设置），部分设备授予后可能出现文件读写异常，建议仅在无法读写文件时授予。请选择是否授予该权限：")
                .positiveText("前往授权")
                .negativeText("取消")
                .onPositive { dialog: MaterialDialog, which: DialogAction? ->
                    dialog.dismiss()
                    val intent: Intent =
                        Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                    dialog.dismiss()
                }
                .show()
        } else {
            Toast.makeText(context, "Android 10 及以下系统无需设置该项", Toast.LENGTH_SHORT).show()
        }
    }) {
//        MyIcon(
//            painter = painterResource(id = R.drawable.ic_floating_action_menu_file),
//            contentDescription = null
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_file_manager_permission))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun TtsSetting(context: Context) {
    DropdownMenuItem(onClick = {
        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
    }) {
//        MyIcon(
//            painter = painterResource(id = R.drawable.ic_speaker_black_48dp),
//            contentDescription = null
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.tts_setting))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun TtsEngineUpdate(context: Context) {
    DropdownMenuItem(onClick = {
        IntentUtil.browse(
            context,
            "https://github.com/ag2s20150909/TTS/releases"
        )
    }) {
//        MyIcon(
//            painter = painterResource(id = R.drawable.ic_check_for_updates),
//            contentDescription = null
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.tts_engine_update))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AppDetailSettings(context: Context) {
    DropdownMenuItem(onClick = {
        context.startActivity(PermissionsSettingsUtil.getAppDetailSettingIntent(context.packageName))
    }) {
//        MyIcon(
//            painter = painterResource(id = R.drawable.ic_setting_fill),
//            contentDescription = null
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_app_detail_settings))
    }
}

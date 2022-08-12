package org.autojs.autojs.ui.main.web

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialScope
import com.leinardi.android.speeddial.compose.SpeedDialState
import com.tencent.smtt.sdk.QbSdk
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.ui.widget.CallbackBundle
import org.autojs.autojs.ui.widget.fillMaxSize
import java.io.File
import java.util.*


class WebViewFragment : Fragment() {
    private lateinit var mWebData: WebData
    val gson = Gson()
    lateinit var mEWebView: EWebView
    lateinit var mWebViewTbs: com.tencent.smtt.sdk.WebView
    lateinit var mWebView: android.webkit.WebView

    companion object {
        var mDialog: Dialog? = null
        var sRoot: String = Environment.getExternalStorageDirectory().absolutePath
        const val sParent = ".."
        const val sFolder = "."
        const val sEmpty = ""
        const val sOnErrorMsg = "No rights to access!"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
        } else {
            mWebData = WebData()
            Pref.setWebData(gson.toJson(mWebData))
        }
        return ComposeView(requireContext()).apply {
            setContent {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingButton()
                    },
                ) {
                    AndroidView(
                        modifier = Modifier.padding(it),
                        factory = {
                            EWebView(requireContext()).apply {
                                mEWebView = this
                                if (getIsTbs()) {
                                    mWebViewTbs = getWebViewTbs()
                                    if (this@WebViewFragment.arguments != null) {
                                        mWebViewTbs.restoreState(this@WebViewFragment.arguments)
                                    }
                                    if (mWebViewTbs!!.originalUrl.isNullOrBlank()){
                                        mWebViewTbs!!.loadUrl(mWebData!!.homepage)
                                        mWebViewTbs.saveState(this@WebViewFragment.arguments)
                                    }
                                } else {
                                    mWebView = getWebView()
                                    if (this@WebViewFragment.arguments != null) {
                                        this@WebViewFragment.arguments?.let { it1 ->
                                            mWebView.restoreState(it1)
                                        }
                                    }
                                    if (mWebView!!.originalUrl.isNullOrBlank()){
                                        mWebView!!.loadUrl(mWebData!!.homepage)
                                        this@WebViewFragment.arguments?.let { it1 ->
                                            mWebView.saveState(it1)
                                        }
                                    }
                                }
                                getSwipeRefreshLayout().setOnRefreshListener {
                                    onRefresh()
                                }
                                fillMaxSize()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mEWebView.getIsTbs()) {
            mWebViewTbs.saveState(this@WebViewFragment.arguments)
        } else {
            this@WebViewFragment.arguments?.let { mWebView.saveState(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mEWebView.getIsTbs()) {
            mWebViewTbs.restoreState(this@WebViewFragment.arguments)
        } else {
            this@WebViewFragment.arguments?.let { mWebView.restoreState(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mEWebView.getIsTbs()) {
            mWebViewTbs.saveState(this@WebViewFragment.arguments)
        } else {
            this@WebViewFragment.arguments?.let { mWebView.saveState(it) }
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class
    )
    @Composable
    private fun FloatingButton() {
        var speedDialState by rememberSaveable { mutableStateOf(SpeedDialState.Collapsed) }
        val context = LocalContext.current
        SpeedDial(
            state = speedDialState,
            onFabClick = { expanded ->
                speedDialState =
                    if (expanded) SpeedDialState.Collapsed else SpeedDialState.Expanded
            },
            fabClosedContent = {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colors.onSecondary
                )
            },
            fabOpenedContent = {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = MaterialTheme.colors.onSecondary
                )
            },
        ) {
            this.items(context)
        }
    }

    private fun SpeedDialScope.items(context: Context) {
        item {
            LoadHomepage(context)
        }
        item {
            LoadBookmark(context)
        }
        item {
            BrowseMode(context)
        }
        item {
            SwitchConsole(context)
        }
        item {
            AddressOrSearch(context)
        }
        item {
            HtmlSource(context)
        }
        item {
            OpenFile(context)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun LoadHomepage(context: Context) {
        FabWithLabel(
            onClick = {
                if (mEWebView.getIsTbs()) {
                    mWebViewTbs.loadUrl(mWebData.homepage)
                } else {
                    mWebView.loadUrl(mWebData.homepage)
                }
            },
            labelContent = { Text(text = stringResource(id = R.string.text_home)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_homepage), null)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun LoadBookmark(context: Context) {
        FabWithLabel(
            onClick = {
                MaterialDialog.Builder(requireContext())
                    .title("设置当前页或选择书签：")
                    .positiveText("添加书签")
                    .negativeText("删除")
                    .neutralText("设为主页")
                    .items(*mWebData.bookmarkLabels)
                    .itemsCallback { dialog, itemView, which, text ->
                        if (mEWebView.getIsTbs()) {
                            mWebViewTbs.loadUrl(mWebData.bookmarks[which])
                        } else {
                            mWebView.loadUrl(mWebData.bookmarks[which])
                        }
                        dialog.dismiss()
                    }
                    .onPositive { dialog, which ->
                        val strList =
                            arrayOfNulls<String>(mWebData.bookmarks.size + 1)
                        val strLabelList =
                            arrayOfNulls<String>(mWebData.bookmarks.size + 1)
                        var j = 0
                        for (i in 0 until mWebData.bookmarks.size) {
                            strList[j] = mWebData.bookmarks[i]
                            strLabelList[j] = mWebData.bookmarkLabels[i]
                            j += 1
                        }
                        strList[j] = if (mEWebView.getIsTbs()) {
                            mWebViewTbs.originalUrl
                        } else {
                            mWebView.originalUrl
                        }
                        strLabelList[j] = if (mEWebView.getIsTbs()) {
                            mWebViewTbs.title
                        } else {
                            mWebView.title
                        }
                        mWebData.bookmarks = strList as Array<String>
                        mWebData.bookmarkLabels = strLabelList as Array<String>
                        Pref.setWebData(gson.toJson(mWebData))
                        dialog.dismiss()
                    }
                    .onNegative { dialog, which ->
                        MaterialDialog.Builder(requireContext())
                            .title("请选择书签：")
                            .positiveText("删除(多选)")
                            .negativeText("取消")
                            .items(*mWebData.bookmarkLabels)
                            .itemsCallbackMultiChoice(
                                null
                            ) { dialog, which, text -> true }
                            .onPositive { dialog, which ->
                                if (Objects.requireNonNull(dialog.selectedIndices).size >= mWebData.bookmarks.size) {
                                    mWebData.bookmarks = arrayOf()
                                    mWebData.bookmarkLabels =
                                        arrayOf()
                                    mWebData = gson.fromJson(
                                        Pref.getWebData(),
                                        WebData::class.java
                                    )
                                } else if (Objects.requireNonNull(dialog.selectedIndices)
                                        .isNotEmpty()
                                ) {
                                    val strList =
                                        arrayOfNulls<String>(mWebData.bookmarks.size - dialog.selectedIndices!!.size)
                                    val strLabelList =
                                        arrayOfNulls<String>(mWebData.bookmarks.size - dialog.selectedIndices!!.size)
                                    var j = 0
                                    for (i in 0 until mWebData.bookmarks.size) {
                                        var flag = true
                                        for (index in dialog.selectedIndices!!) {
                                            if (i == index) {
                                                flag = false
                                                break
                                            }
                                        }
                                        if (flag) {
                                            strList[j] = mWebData.bookmarks[i]
                                            strLabelList[j] =
                                                mWebData.bookmarkLabels[i]
                                            j += 1
                                        }
                                    }
                                    mWebData.bookmarks = strList as Array<String>
                                    mWebData.bookmarkLabels = strLabelList as Array<String>
                                    Pref.setWebData(
                                        gson.toJson(
                                            mWebData,
                                            WebData::class.java
                                        )
                                    )
                                }
                                dialog.dismiss()
                            }
                            .show()
                        dialog.dismiss()
                    }
                    .onNeutral { dialog, which ->
                        mWebData.homepage = if (mEWebView.getIsTbs()) {
                            mWebViewTbs.originalUrl
                        } else {
                            mWebView.originalUrl.toString()
                        }
                        Pref.setWebData(gson.toJson(mWebData))
                        Toast.makeText(
                            getContext(),
                            if (mEWebView!!.getIsTbs()) {
                                "设置为主页："+ mWebViewTbs!!.title
                            } else {
                                "设置为主页："+ mWebView!!.title
                            },
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                    }
                    .show()
            },
            labelContent = { Text(text = stringResource(id = R.string.bookmark)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_star), null)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun BrowseMode(context: Context) {
        FabWithLabel(
            onClick = {
                MaterialDialog.Builder(requireContext())
                    .title("请选择默认的User-Agent及缩放设置：")
                    .positiveText("强制缩放")
                    .negativeText("确定")
                    .neutralText("关闭强制缩放")
                    .items(*mWebData.userAgentLabels)
                    .itemsCallbackSingleChoice(
                        -1
                    ) { dialog, view, which, text -> true }
                    .onPositive { dialog, which ->
                        if (dialog.selectedIndex >= 0) {
                            mWebData.userAgent =
                                mWebData.userAgents[dialog.selectedIndex]
                            Pref.setWebData(
                                gson.toJson(
                                    mWebData
                                )
                            )
                        }
                        mEWebView.setEnableRescale(true)
                        dialog.dismiss()
                    }
                    .onNegative { dialog, which ->
                        if (dialog.selectedIndex >= 0) {
                            mWebData.userAgent =
                                mWebData.userAgents[dialog.selectedIndex]
                            Pref.setWebData(
                                gson.toJson(
                                    mWebData
                                )
                            )
                        }
                        dialog.dismiss()
                    }
                    .onNeutral { dialog, which ->
                        if (dialog.selectedIndex >= 0) {
                            mWebData.userAgent =
                                mWebData.userAgents[dialog.selectedIndex]
                            Pref.setWebData(
                                gson.toJson(
                                    mWebData
                                )
                            )
                        }
                        mEWebView.setEnableRescale(false)
                        dialog.dismiss()
                    }
                    .show()
                if (mEWebView.getIsTbs()) {
                    mWebViewTbs.reload()
                } else {
                    mWebView.reload()
                }
            },
            labelContent = { Text(text = stringResource(id = R.string.browse_mode)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_pc_mode), null)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun SwitchConsole(context: Context) {
        FabWithLabel(
            onClick = {
                mEWebView.switchConsole();
                if (mEWebView.getIsTbs()) {
                    mWebViewTbs.reload()
                } else {
                    mWebView.reload()
                }
            },
            labelContent = { Text(text = stringResource(id = R.string.text_console)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_console), null)
        }
    }

    @SuppressLint("ServiceCast")
    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun AddressOrSearch(context: Context) {
        FabWithLabel(
            onClick = {
                val et = EditText(getContext())
                MaterialDialog.Builder(requireContext())
                    .title(
                        if (mEWebView.getIsTbs()) {
                            mWebViewTbs.originalUrl
                        } else {
                            mWebView.originalUrl.toString()
                        }
                    )
                    .customView(et, false)
                    .positiveText("打开")
                    .negativeText("复制网址")
                    .neutralText("搜索")
                    .onNeutral { dialog, which ->
                        MaterialDialog.Builder(requireContext())
                            .title("选择搜索引擎：")
                            .negativeText("取消")
                            .items(*mWebData.searchEngineLabels)
                            .itemsCallback { dialog, view, which, text ->
                                if (mEWebView.getIsTbs()) {
                                    mWebViewTbs.loadUrl(mWebData.searchEngines[which] + et.text.toString())
                                } else {
                                    mWebView.loadUrl(mWebData.searchEngines[which] + et.text.toString())
                                }
                                dialog.dismiss()
                            }
                            .show()
                        dialog.dismiss()
                    }
                    .onPositive { dialog, which ->
                        if (mEWebView.getIsTbs()) {
                            mWebViewTbs.loadUrl(et.text.toString())
                        } else {
                            mWebView.loadUrl(et.text.toString())
                        }
                        dialog.dismiss()
                    }
                    .onNegative { dialog, which ->
                        val mClipboardManager: ClipboardManager =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val mClipData = ClipData.newPlainText(
                            "Label", if (mEWebView.getIsTbs()) {
                                mWebViewTbs.originalUrl
                            } else {
                                mWebView.originalUrl.toString()
                            }
                        )
                        mClipboardManager.setPrimaryClip(mClipData)
                        Toast.makeText(
                            getContext(),
                            "当前网址已复制到剪贴板！",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .show()
            },
            labelContent = { Text(text = stringResource(id = R.string.address_or_search)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_web), null)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun HtmlSource(context: Context) {
        FabWithLabel(
            onClick = {
                if (mEWebView.getIsTbs()) {
                    mWebViewTbs.loadUrl(
                        "file://" + (requireContext().getExternalFilesDir(
                            null
                        )?.path) + File.separator + "html_source.txt"
                    );
                } else {
                    mWebView.loadUrl(
                        "file://" + (requireContext().getExternalFilesDir(
                            null
                        )?.path) + File.separator + "html_source.txt"
                    );
                }
            },
            labelContent = { Text(text = stringResource(id = R.string.html_source)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_code_black_48dp), null)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun OpenFile(context: Context) {
        FabWithLabel(
            onClick = {
                val images = HashMap<String, Int>()
                images[sRoot] = R.drawable.filedialog_root // 根目录图标
                images[sParent] = R.drawable.filedialog_folder_up //返回上一层的图标
                images[sFolder] = R.drawable.filedialog_folder //文件夹图标
                images[sEmpty] = R.drawable.filedialog_file
                mDialog = createDialog(
                    activity, "打开本地文档", CallbackBundle { bundle ->
                        val filepath = bundle.getString("filepath")!!.lowercase(Locale.getDefault())
                        if (filepath.endsWith(".html") || filepath.endsWith(".htm") || filepath.endsWith(
                                ".xhtml"
                            ) || filepath.endsWith(
                                ".xml"
                            ) || filepath.endsWith(
                                ".mhtml"
                            ) || filepath.endsWith(
                                ".mht"
                            ) || filepath.endsWith(
                                ".txt"
                            ) || filepath.endsWith(
                                ".js"
                            ) || filepath.endsWith(
                                ".log"
                            )
                        ) {
                            if (mEWebView.getIsTbs()) {
                                mWebViewTbs.loadUrl("file://$filepath")
                            } else {
                                mWebView.loadUrl("file://$filepath")
                            }
                        } else {
                            if (mEWebView.getIsTbs()) {
                                val extraParams = HashMap<String, String>() //define empty hashmap
                                extraParams["style"] = "1"
                                extraParams["local"] = "true"
                                QbSdk.openFileReader(
                                    getContext(),
                                    filepath,
                                    extraParams
                                ) { it ->
                                    if ("openFileReader open in QB" === it || "filepath error" === it || "TbsReaderDialogClosed" === it || "fileReaderClosed" === it
                                    ) {
                                        QbSdk.closeFileReader(
                                            activity
                                        )
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    getContext(),
                                    "系统Web内核不支持查看该格式文档！",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    "html;htm;xhtml;xml;mhtml;mht;doc;docx;ppt;pptx;xls;xlsx;pdf;txt;js;log;epub",
                    images, sRoot
                )
                mDialog!!.show()
            },
            labelContent = { Text(text = stringResource(id = R.string.text_open)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_floating_action_menu_open), null)
        }
    }

    fun onBackPressed(): Boolean {
        if (mEWebView.getIsTbs()) {
            if (mWebViewTbs.canGoBack()) {
                mWebViewTbs.goBack()
                return true
            }
        } else {
            if (mWebView.canGoBack()) {
                mWebView.goBack()
                return true
            }
        }
        return false
    }

    fun createDialog(
        context: Context?,
        title: String?,
        callback: CallbackBundle?,
        suffix: String?,
        images: Map<String, Int>,
        rootDir: String
    ): Dialog? {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setView(FileSelectView(context, callback, suffix, images, rootDir))
        mDialog = builder.create()
        mDialog!!.setTitle(title)
        return mDialog
    }

    internal class FileSelectView(
        context: Context?,
        callback: CallbackBundle?,
        suffix: String?,
        images: Map<String, Int>?,
        rootDir: String
    ) : ListView(context), AdapterView.OnItemClickListener {
        private var callback: CallbackBundle? = null
        private var path = if (rootDir.isNotEmpty() && File(rootDir).isDirectory) {
            rootDir
        } else {
            sRoot
        }
        private var list: MutableList<Map<String, Any?>>? = null
        private var suffix: String? = null
        private var imageMap: Map<String, Int>? = null

        private fun getSuffix(filename: String): String {
            val dix = filename.lastIndexOf('.')
            return if (dix < 0) {
                ""
            } else {
                filename.substring(dix)
            }
        }

        private fun getImageId(s: String): Int {
            return if (imageMap == null) {
                0
            } else if (imageMap!!.containsKey(s)) {
                imageMap!![s]!!
            } else if (imageMap!!.containsKey(sEmpty)) {
                imageMap!![sEmpty]!!
            } else {
                0
            }
        }

        private fun refreshFileList(): Int {
            // 刷新文件列表
            var files: Array<File>? = null
            files = try {
                File(path).listFiles()
            } catch (e: Exception) {
                null
            }
            if (files == null) {
                // 访问出错
                Toast.makeText(context, sOnErrorMsg, Toast.LENGTH_SHORT).show()
                return -1
            }
            if (list != null) {
                list!!.clear()
            } else {
                list = ArrayList(files.size)
            }

            // 用来先保存文件夹和文件夹的两个列表
            val lFolders = ArrayList<Map<String, Any?>>()
            val lFiles = ArrayList<Map<String, Any?>>()
            if (path != sRoot) {
                // 添加根目录 和 上一层目录
                var map: MutableMap<String, Any?> = HashMap()
                map["name"] = sRoot
                map["path"] = sRoot
                map["img"] = getImageId(sRoot)
                list!!.add(map)
                map = HashMap()
                map["name"] = sParent
                map["path"] = path
                map["img"] = getImageId(sParent)
                list!!.add(map)
            }
            for (file in files) {
                if (file.isDirectory && file.listFiles() != null) {
                    // 添加文件夹
                    val map: MutableMap<String, Any?> = HashMap()
                    map["name"] = file.name
                    map["path"] = file.path
                    map["img"] = getImageId(sFolder)
                    lFolders.add(map)
                } else if (file.isFile) {
                    // 添加文件
                    val sf = getSuffix(file.name).lowercase(Locale.getDefault())
                    val suffixList = suffix!!.split(";".toRegex()).toTypedArray()
                    var hasSuffix = false
                    for (item in suffixList) {
                        if (sf.contains(".$item")) {
                            hasSuffix = true
                            break
                        }
                    }
                    if (suffix == null || suffix!!.isEmpty() || sf.isNotEmpty() && hasSuffix) {
                        val map: MutableMap<String, Any?> = HashMap()
                        map["name"] = file.name
                        map["path"] = file.path
                        map["img"] = getImageId(suffixList[0])
                        lFiles.add(map)
                    }
                }
            }
            list!!.addAll(lFolders) // 先添加文件夹，确保文件夹显示在上面
            list!!.addAll(lFiles) //再添加文件
            adapter = SimpleAdapter(
                context,
                list,
                R.layout.filedialogitem,
                arrayOf("img", "name", "path"),
                intArrayOf(
                    R.id.filedialogitem_img, R.id.filedialogitem_name, R.id.filedialogitem_path
                )
            )
            return files.size
        }

        override fun onItemClick(parent: AdapterView<*>?, v: View, position: Int, id: Long) {
            // 条目选择
            val pt = list!![position]["path"] as String?
            val fn = list!![position]["name"] as String?
            if (fn == sRoot || fn == sParent) {
                // 如果是更目录或者上一层
                val fl = File(pt)
                val ppt = fl.parent
                path = // 返回上一层
                    ppt ?: // 返回更目录
                            sRoot
            } else {
                val fl = File(pt)
                if (fl.isFile) {
                    // 如果是文件
                    mDialog!!.dismiss() // 让文件夹对话框消失

                    // 设置回调的返回值
                    val bundle = Bundle()
                    bundle.putString("filepath", pt)
                    bundle.putString("filename", fn)
                    // 调用事先设置的回调函数
                    callback!!.callback(bundle)
                    return
                } else if (fl.isDirectory) {
                    // 如果是文件夹
                    // 那么进入选中的文件夹
                    if (pt != null) {
                        path = pt
                    }
                }
            }
            refreshFileList()
        }

        init {
            imageMap = images
            this.suffix = suffix?.lowercase(Locale.getDefault()) ?: ""
            this.callback = callback
            this.onItemClickListener = this
            refreshFileList()
        }
    }

}

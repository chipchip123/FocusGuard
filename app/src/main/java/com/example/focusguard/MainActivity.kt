package com.example.focusguard

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class InstalledApp(val pkg: String, val name: String)

class MainActivity : ComponentActivity() {
    private var resumed by mutableIntStateOf(0)
    override fun onResume() { super.onResume(); resumed++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FocusRepository(this).initialize(this)
        setContent { FocusTheme { FocusApp(resumed) } }
    }

    private fun openSettings(action: String, packageUri: Boolean = false) {
        runCatching { startActivity(Intent(action).apply { if (packageUri) data = Uri.parse("package:$packageName") }) }
            .onFailure { Toast.makeText(this, "Hãy mở mục này trong Cài đặt của điện thoại.", Toast.LENGTH_LONG).show() }
    }

    @Composable
    private fun FocusApp(refresh: Int) {
        val repo = remember { FocusRepository(this) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var enabled by remember { mutableStateOf(repo.enabled) }
        var strict by remember { mutableStateOf(repo.strict) }
        var managed by remember { mutableStateOf(repo.apps()) }
        var installed by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
        var catalogLoaded by remember { mutableStateOf(false) }
        var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
        var history by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
        var usageAllowed by remember { mutableStateOf(false) }
        var overlayAllowed by remember { mutableStateOf(false) }
        var serviceAllowed by remember { mutableStateOf(false) }
        var completed by remember { mutableIntStateOf(0) }
        var consent by remember { mutableStateOf(false) }
        var strictInfo by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<ManagedApp?>(null) }
        var search by rememberSaveable { mutableStateOf("") }
        var loading by remember { mutableStateOf(true) }
        var readError by remember { mutableStateOf(false) }
        var demoPackages by remember { mutableStateOf(repo.demoPackages()) }
        var demoConfirm by remember { mutableStateOf<List<ManagedApp>?>(null) }

        LaunchedEffect(refresh) {
            enabled = repo.enabled; strict = repo.strict; managed = repo.apps()
            while (true) {
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    delay(3000)
                    continue
                }
                usageAllowed = UsageReader.permitted(this@MainActivity)
                overlayAllowed = Settings.canDrawOverlays(this@MainActivity)
                serviceAllowed = FocusAccessibilityService.connected
                completed = repo.completedToday()
                demoPackages = repo.demoPackages()
                usage = withContext(Dispatchers.IO) {
                    runCatching { UsageReader.read(this@MainActivity) }.onFailure { readError = true }.getOrDefault(emptyMap())
                }
                loading = false
                delay(3000)
            }
        }
        LaunchedEffect(Unit) {
            installed = withContext(Dispatchers.IO) {
                val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val launcher = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
                    .filter { it.pkg != packageName && it.pkg != launcher && it.pkg != "com.android.settings" && !it.pkg.contains("permissioncontroller") }
                    .distinctBy { it.pkg }.sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
            catalogLoaded = true
        }
        LaunchedEffect(tab, refresh, managed) {
            if (tab == 2) {
                history = withContext(Dispatchers.IO) {
                    val packages = managed.map { it.packageName }.toSet()
                    (6 downTo 0).map { ago ->
                        val cal = Calendar.getInstance().apply { timeInMillis = FocusRepository.dayStart(); add(Calendar.DAY_OF_YEAR, -ago) }
                        val start = cal.timeInMillis
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val values = runCatching { UsageReader.read(this@MainActivity, start, minOf(cal.timeInMillis, System.currentTimeMillis())) }.getOrDefault(emptyMap())
                        SimpleDateFormat("dd/MM", Locale.getDefault()).format(start) to values.filterKeys { it in packages }.values.sum()
                    }
                }
            }
        }
        fun save(apps: List<ManagedApp>) { repo.saveApps(apps); managed = repo.apps() }
        fun guardEdit(action: () -> Unit) {
            if (strict) Toast.makeText(this, "Tắt chế độ nghiêm ngặt trong Cài đặt trước khi sửa giới hạn.", Toast.LENGTH_LONG).show()
            else action()
        }
        val ready = usageAllowed && overlayAllowed && serviceAllowed
        fun prepareDemo(targets: List<ManagedApp>) {
            when {
                !ready -> { tab = 3; Toast.makeText(this, "Cấp đủ 3 quyền trước khi bắt đầu demo.", Toast.LENGTH_LONG).show() }
                targets.isEmpty() -> { tab = 1; Toast.makeText(this, "Cài TikTok/Facebook hoặc chọn một ứng dụng để demo.", Toast.LENGTH_LONG).show() }
                else -> demoConfirm = targets
            }
        }
        Scaffold(containerColor = Mist, bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Tổng quan" to "◷", "Ứng dụng" to "▦", "Thống kê" to "▥", "Cài đặt" to "⚙").forEachIndexed { i, item ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(item.second) }, label = { Text(item.first) })
                }
            }
        }) { insets ->
            LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("F O C U S G U A R D", color = Forest, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                when (tab) {
                    0 -> {
                        item { Heading("Dành thời gian cho bạn", "Giữ tập trung, từng phút một.") }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(28.dp)) {
                                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("MẠNG XÃ HỘI HÔM NAY", color = Color(0xFFD7EDDF), style = MaterialTheme.typography.labelLarge)
                                    Text(if (!usageAllowed) "Chưa có dữ liệu" else if (loading) "Đang tải…" else timeLabel(managed.sumOf { usage[it.packageName] ?: 0 }), color = Color.White, style = MaterialTheme.typography.headlineLarge)
                                    Text("${managed.size} ứng dụng quản lý  ·  $completed thử thách hoàn thành", color = Color.White)
                                }
                            }
                        }
                        item { Panel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Bảo vệ thời gian", fontWeight = FontWeight.Bold)
                                    Text(if (!enabled) "Đang tắt" else if (ready) "Đang hoạt động" else "Cần cấp đủ quyền", color = Forest)
                                }
                                Switch(enabled, onCheckedChange = { value -> guardEdit { enabled = value; repo.enabled = value; if (value && !ready) tab = 3 } })
                            }
                            Text("Khi hết thời gian, hoàn thành thử thách để mở thêm 5 phút.", style = MaterialTheme.typography.bodyMedium)
                        } }
                        if (!ready) item { Panel {
                            Text("Hoàn tất thiết lập", fontWeight = FontWeight.Bold)
                            Text("Cấp 3 quyền để FocusGuard có thể đo thời gian và hiển thị màn hình khóa.")
                            Button(onClick = { tab = 3 }) { Text("Thiết lập quyền") }
                        } }
                        item { Panel {
                            Text("Demo khi thuyết trình", fontWeight = FontWeight.Bold)
                            if (demoPackages.isEmpty()) {
                                Text("Khóa ngay TikTok/Facebook đã cài, không cần chờ 30 phút. Thử thách vẫn mở thưởng 5 phút như bình thường.")
                                Button(onClick = { prepareDemo(repo.installedSocialApps(this@MainActivity)) }) { Text("Demo TikTok / Facebook") }
                                if (managed.isNotEmpty()) TextButton(onClick = { prepareDemo(managed) }) { Text("Demo ứng dụng đã chọn") }
                            } else {
                                Text("DEMO đang bật cho: " + managed.filter { it.packageName in demoPackages }.joinToString { it.name })
                                Text("Mở ứng dụng trên màn hình chính để xem khóa. Nút khóa lại xóa thưởng demo để trình diễn lần nữa. Tự hết chế độ khi sang ngày mới.")
                                Button(onClick = { prepareDemo(managed.filter { it.packageName in demoPackages }) }) { Text("Khóa lại để demo") }
                                OutlinedButton(onClick = {
                                    repo.stopDemo(); demoPackages = repo.demoPackages(); enabled = repo.enabled
                                }) { Text("Kết thúc demo") }
                            }
                        } }
                        item { Text("Ứng dụng của bạn", style = MaterialTheme.typography.titleLarge) }
                        if (managed.isEmpty()) item { Panel {
                            Text("Bắt đầu với một giới hạn nhỏ")
                            Text("Chọn TikTok, Facebook hoặc ứng dụng bạn muốn dành ít thời gian hơn.")
                            Button(onClick = { tab = 1 }) { Text("Chọn ứng dụng") }
                        } }
                        items(managed, key = { it.packageName }) { app ->
                            AppUsageCard(app, usage[app.packageName] ?: 0, usageAllowed, repo.rewardUntil(app.packageName), demo = app.packageName in demoPackages, onEdit = { guardEdit { editing = app } })
                        }
                    }
                    1 -> {
                        item { Heading("Giới hạn vừa đủ", "Chọn ứng dụng và đặt ngân sách thời gian mỗi ngày.") }
                        item { OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Tìm ứng dụng") }, singleLine = true, shape = RoundedCornerShape(18.dp)) }
                        if (!catalogLoaded) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        if (strict) item { Text("Chế độ nghiêm ngặt đang bảo vệ danh sách này.", color = Forest) }
                        val filtered = installed.filter { it.name.contains(search, true) || it.pkg.contains(search, true) }
                        if (catalogLoaded && filtered.isEmpty()) item { Text("Không tìm thấy ứng dụng phù hợp.") }
                        items(filtered, key = { it.pkg }) { app ->
                            val selected = managed.find { it.packageName == app.pkg }
                            Panel {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(app.name, fontWeight = FontWeight.Bold)
                                        Text(if (selected != null) "${selected.limitMinutes} phút / ngày" else "Chưa quản lý", style = MaterialTheme.typography.bodySmall, color = Forest)
                                    }
                                    Checkbox(selected != null, onCheckedChange = { yes -> guardEdit {
                                        save(if (yes) managed + ManagedApp(app.pkg, app.name) else managed.filter { it.packageName != app.pkg })
                                    } })
                                }
                                if (selected != null) TextButton(onClick = { guardEdit { editing = selected } }) { Text("Điều chỉnh giới hạn") }
                            }
                        }
                        item { Text("Thay đổi được lưu tự động trên thiết bị. Ứng dụng Cài đặt và màn hình chính được loại khỏi danh sách.", style = MaterialTheme.typography.bodySmall) }
                    }
                    2 -> {
                        item { Heading("Nhìn lại thói quen", "Thời gian của những ứng dụng đang được quản lý.") }
                        if (!usageAllowed) item { Panel { Text("Cần quyền truy cập thời gian sử dụng để xem thống kê."); Button(onClick = { tab = 3 }) { Text("Cấp quyền") } } }
                        item { Panel {
                            Text("7 ngày gần nhất", style = MaterialTheme.typography.titleLarge)
                            Text("Dựa trên lịch sử còn được Android lưu giữ.", style = MaterialTheme.typography.bodySmall)
                            if (history.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                            val max = history.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                            history.forEach { (label, value) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(label, Modifier.width(46.dp), style = MaterialTheme.typography.bodySmall)
                                    LinearProgressIndicator(progress = { (value.toFloat() / max).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(10.dp), color = Forest)
                                    Text("${value / 60_000}p", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } }
                        item { Panel { Text("Hôm nay bạn đã hoàn thành", style = MaterialTheme.typography.titleMedium); Text("$completed thử thách", style = MaterialTheme.typography.headlineMedium, color = Forest); Text("Mỗi lần hoàn thành mở thêm 5 phút theo đồng hồ; thời gian thưởng vẫn trôi khi bạn chuyển sang ứng dụng khác.") } }
                        items(managed.sortedByDescending { usage[it.packageName] ?: 0 }, key = { it.packageName }) { app ->
                            AppUsageCard(app, usage[app.packageName] ?: 0, usageAllowed, repo.rewardUntil(app.packageName), demo = app.packageName in demoPackages) { guardEdit { editing = app } }
                        }
                    }
                    3 -> {
                        item { Heading("Thiết lập tập trung", "Bạn luôn kiểm soát quyền và dữ liệu của mình.") }
                        item { PermissionCard("1. Thời gian sử dụng", "Đọc thời gian sử dụng từng ứng dụng trong ngày.", usageAllowed) { openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS) } }
                        item { PermissionCard("2. Hiển thị trên ứng dụng khác", "Hiện màn hình nghỉ khi ứng dụng hết giờ.", overlayAllowed) { openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, true) } }
                        item { PermissionCard("3. Dịch vụ hỗ trợ tiếp cận", "Chỉ nhận tên ứng dụng đang mở để áp dụng giới hạn.", serviceAllowed) { consent = true } }
                        item { Panel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Chế độ nghiêm ngặt", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Switch(strict, onCheckedChange = {
                                    if (it) strictInfo = true else { strict = false; repo.strict = false }
                                })
                            }
                            Text("Bảo vệ giới hạn trong FocusGuard và nhắc bạn trước khi vào Cài đặt Android. Vẫn có nút tạm mở Cài đặt hoặc tắt chế độ này.", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                val admin = getSystemService(DevicePolicyManager::class.java)
                                val component = ComponentName(this@MainActivity, FocusAdminReceiver::class.java)
                                if (admin.isAdminActive(component)) {
                                    admin.removeActiveAdmin(component)
                                    Toast.makeText(this@MainActivity, "Đã tắt quản trị thiết bị", Toast.LENGTH_SHORT).show()
                                } else runCatching {
                                    startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Tùy chọn: Android yêu cầu tắt quản trị thiết bị trước khi gỡ FocusGuard. Không ngăn gỡ tuyệt đối."))
                                }
                            }) { Text("Bật / tắt quản trị thiết bị (tùy chọn)") }
                        } }
                        item { Panel {
                            Text("Thử trước khi sử dụng", fontWeight = FontWeight.Bold)
                            Text("Chạy thử màn hình thử thách, không cấp thời gian thưởng.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { startActivity(Intent(this@MainActivity, ChallengeActivity::class.java).putExtra("mode", "quiz")) }) { Text("Từ vựng") }
                                OutlinedButton(onClick = { startActivity(Intent(this@MainActivity, ChallengeActivity::class.java).putExtra("mode", "shake")) }) { Text("Vận động") }
                            }
                        } }
                        item { Panel {
                            Text("Riêng tư theo thiết kế", fontWeight = FontWeight.Bold)
                            Text("Hoạt động ngoại tuyến, không tài khoản, không quảng cáo. Cấu hình chỉ lưu trên máy; FocusGuard không yêu cầu quyền Internet và không đọc nội dung màn hình.")
                            Text("Nếu cài APK và Android chặn bật dịch vụ: mở Thông tin ứng dụng → menu ⋮ → Cho phép cài đặt bị hạn chế, rồi quay lại cấp quyền. Tên mục có thể khác tùy máy.", style = MaterialTheme.typography.bodySmall)
                            Text("Nếu dịch vụ bị dừng bởi tiết kiệm pin, cho phép FocusGuard chạy nền trong cài đặt pin của thiết bị.", style = MaterialTheme.typography.bodySmall)
                            Text("FocusGuard 1.0 · Kotlin & Jetpack Compose", color = Forest, style = MaterialTheme.typography.labelMedium)
                        } }
                    }
                }
                if (readError) item { Text("Chưa đọc được dữ liệu sử dụng. Hãy kiểm tra quyền và mở lại ứng dụng.", color = MaterialTheme.colorScheme.error) }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        demoConfirm?.let { targets ->
            AlertDialog(onDismissRequest = { demoConfirm = null }, title = { Text("Bắt đầu demo khóa ngay?") }, text = {
                Text("Ứng dụng: ${targets.joinToString { it.name }}.\n\nBật bảo vệ và khóa ngay khi mở các ứng dụng này; xóa thời gian thưởng còn lại của chúng. Giới hạn hằng ngày và số liệu sử dụng thật được giữ nguyên. Hoàn thành thử thách vẫn nhận 5 phút. Bấm Kết thúc demo để trở về chế độ thường.")
            }, confirmButton = { TextButton(onClick = {
                repo.startDemo(targets); managed = repo.apps(); enabled = repo.enabled; demoPackages = repo.demoPackages(); demoConfirm = null
                Toast.makeText(this@MainActivity, "Demo đã sẵn sàng. Hãy mở ${targets.first().name}.", Toast.LENGTH_LONG).show()
            }) { Text("Bắt đầu demo") } }, dismissButton = { TextButton(onClick = { demoConfirm = null }) { Text("Hủy") } })
        }
        if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("Cho phép nhận biết ứng dụng?") }, text = {
            Text("FocusGuard sử dụng dịch vụ hỗ trợ tiếp cận để nhận tên gói của ứng dụng đang mở và hiện màn hình khóa khi hết giới hạn. Không đọc văn bản trên màn hình, không ghi thao tác và không gửi dữ liệu ra ngoài.\n\nBạn tự bật FocusGuard ở mục Ứng dụng đã cài đặt / Dịch vụ đã tải xuống trong màn hình tiếp theo.")
        }, confirmButton = { TextButton(onClick = { consent = false; openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text("Đồng ý, mở cài đặt") } }, dismissButton = { TextButton(onClick = { consent = false }) { Text("Để sau") } })
        if (strictInfo) AlertDialog(onDismissRequest = { strictInfo = false }, title = { Text("Bật chế độ nghiêm ngặt?") }, text = { Text("Chế độ này ngăn sửa giới hạn trong ứng dụng và hiển thị lời nhắc khi mở Cài đặt Android. Bạn vẫn có thể tạm mở Cài đặt 60 giây hoặc tắt chế độ. Cần bật bảo vệ và cấp đủ 3 quyền để lời nhắc hoạt động.") }, confirmButton = { TextButton(onClick = { strict = true; repo.strict = true; strictInfo = false }) { Text("Bật") } }, dismissButton = { TextButton(onClick = { strictInfo = false }) { Text("Hủy") } })
        editing?.let { app ->
            var limit by remember(app.packageName) { mutableFloatStateOf(app.limitMinutes.toFloat()) }
            AlertDialog(onDismissRequest = { editing = null }, title = { Text(app.name) }, text = {
                Column { Text("${limit.toInt()} phút mỗi ngày", style = MaterialTheme.typography.titleLarge); Slider(limit, { limit = it }, valueRange = 5f..180f, steps = 34); Text("Giới hạn tính từ 00:00; bao gồm thời gian sử dụng trước khi bạn bật FocusGuard.", style = MaterialTheme.typography.bodySmall) }
            }, confirmButton = { TextButton(onClick = { save(managed.map { if (it.packageName == app.packageName) it.copy(limitMinutes = (kotlin.math.round(limit / 5) * 5).toInt()) else it }); editing = null }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Hủy") } })
        }
    }
}

@Composable
private fun PermissionCard(title: String, detail: String, granted: Boolean, onClick: () -> Unit) {
    Panel {
        Text(title, fontWeight = FontWeight.Bold)
        Text(detail, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onClick) { Text(if (granted) "Đã cấp · Xem cài đặt" else "Cấp quyền") }
    }
}

@Composable
private fun AppUsageCard(app: ManagedApp, used: Long, permitted: Boolean, rewardUntil: Long, demo: Boolean = false, onEdit: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text(app.name.take(1).uppercase(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.titleLarge, color = Forest)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(app.name, fontWeight = FontWeight.Bold)
                Text(if (permitted) "${timeLabel(used)} / ${app.limitMinutes} phút" else "Cần quyền xem thời gian sử dụng", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onEdit) { Text("Sửa") }
        }
        LinearProgressIndicator(progress = { if (permitted) (used.toFloat() / (app.limitMinutes * 60_000L)).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Forest)
        val rewardLeft = rewardUntil - System.currentTimeMillis()
        Text(when {
            !permitted -> "Chưa có dữ liệu"
            rewardLeft > 0 -> "Đang mở thưởng · còn ${(rewardLeft + 999) / 1000} giây"
            demo -> "DEMO · Khóa ngay khi mở ứng dụng"
            used >= app.limitMinutes * 60_000L -> "Đã đạt giới hạn hôm nay"
            else -> "Còn khoảng ${(app.limitMinutes * 60_000L - used + 59_999) / 60_000} phút"
        }, style = MaterialTheme.typography.bodySmall, color = Forest)
    }
}

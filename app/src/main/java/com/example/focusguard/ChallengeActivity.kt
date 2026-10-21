package com.example.focusguard

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt
import kotlin.random.Random

class ChallengeActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private val counter = ShakeCounter()
    private var shakes by mutableIntStateOf(0)
    private var sensorPresent by mutableStateOf(true)
    private var completed by mutableStateOf(false)
    private var registrationFailed by mutableStateOf(false)
    private var mode = "quiz"
    private var target = ""
    private var rewarded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sensors = getSystemService(SensorManager::class.java)
        mode = intent.getStringExtra("mode") ?: "quiz"
        target = intent.getStringExtra("package") ?: ""
        shakes = savedInstanceState?.getInt("shakes", 0) ?: 0
        completed = savedInstanceState?.getBoolean("completed", false) ?: false
        rewarded = savedInstanceState?.getBoolean("rewarded", false) ?: false
        sensorPresent = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        setContent { FocusTheme { ChallengeScreen() } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("shakes", shakes); outState.putBoolean("completed", completed); outState.putBoolean("rewarded", rewarded)
    }

    override fun onResume() {
        super.onResume()
        FocusAccessibilityService.challengeActive = true
        if (mode == "shake" && !completed) registerSensor()
    }
    override fun onPause() {
        sensors.unregisterListener(this)
        FocusAccessibilityService.challengeActive = false
        super.onPause()
    }

    private fun registerSensor() {
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorPresent = sensor != null
        if (sensor != null) registrationFailed = !sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (completed || mode != "shake") return
        val (x, y, z) = event.values
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (counter.sample(g, SystemClock.elapsedRealtime())) {
            shakes++
            if (shakes >= 30) finishChallenge()
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun finishChallenge() {
        if (completed) return
        val repo = FocusRepository(this)
        if (!rewarded && target.isNotBlank() && repo.apps().any { it.packageName == target }) {
            repo.grantReward(target)
            rewarded = true
        }
        completed = true
        sensors.unregisterListener(this)
    }

    private fun leave(openApp: Boolean) {
        if (openApp && rewarded) {
            val launch = packageManager.getLaunchIntentForPackage(target)
            if (launch != null) runCatching { startActivity(launch) }
        } else if (target.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    @Composable
    private fun ChallengeScreen() {
        var words by remember { mutableStateOf<List<Word>>(emptyList()) }
        var dataError by remember { mutableStateOf(false) }
        var streak by rememberSaveable { mutableIntStateOf(0) }
        var seed by rememberSaveable { mutableIntStateOf(Random.nextInt()) }
        var chosen by rememberSaveable { mutableStateOf<String?>(null) }
        var correct by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            words = withContext(Dispatchers.IO) { runCatching { Vocabulary.load(this@ChallengeActivity) }.onFailure { dataError = true }.getOrDefault(emptyList()) }
            if (words.map { it.vietnamese }.distinct().size < 4) dataError = true
        }
        Scaffold(containerColor = Mist) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("F O C U S G U A R D", color = Forest, style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { leave(false) }) { Text("← Dừng thử thách") }
                if (completed) {
                    Spacer(Modifier.height(30.dp))
                    Heading("Bạn làm được rồi!", if (rewarded) "Bạn đã mở thêm 5 phút. Khoảng thưởng bắt đầu ngay bây giờ và kết thúc chậm nhất lúc 00:00." else "Hoàn thành buổi luyện tập. Chế độ thử không cấp thời gian thưởng.")
                    Panel {
                        Text(if (rewarded) "+5 phút" else "Hoàn thành", style = MaterialTheme.typography.displaySmall, color = Forest)
                        Text("Thêm một bước nhỏ để sử dụng điện thoại chủ động hơn.")
                        Button(onClick = { leave(rewarded) }, modifier = Modifier.fillMaxWidth()) { Text(if (rewarded) "Tiếp tục dùng ứng dụng" else "Quay lại FocusGuard") }
                    }
                } else if (mode == "shake") {
                    Heading("Nghỉ tay, vận động nhẹ", "Cầm chắc điện thoại, lắc nhẹ và đều 30 lần.")
                    Panel {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text("$shakes / 30", style = MaterialTheme.typography.displayMedium, color = Forest)
                            LinearProgressIndicator(progress = { shakes / 30f }, modifier = Modifier.fillMaxWidth().height(12.dp))
                            Text("Mỗi chuyển động cần trở về trạng thái ổn định trước khi tính lần kế tiếp.")
                        }
                    }
                    if (!sensorPresent || registrationFailed) {
                        Text("Cảm biến gia tốc không khả dụng. Bạn có thể chuyển sang thử thách từ vựng.")
                        Button(onClick = {
                            sensors.unregisterListener(this@ChallengeActivity)
                            mode = "quiz"
                            startActivity(Intent(this@ChallengeActivity, ChallengeActivity::class.java).putExtra("package", target).putExtra("mode", "quiz"))
                            finish()
                        }) { Text("Chuyển sang từ vựng") }
                    }
                    Text("Nếu không tiện vận động, hãy dừng và chọn thử thách từ vựng.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Heading("Một chút tiếng Anh", "Trả lời đúng liên tiếp 5 câu. Sai một câu, chuỗi đúng bắt đầu lại.")
                    Panel {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CHUỖI ĐÚNG", style = MaterialTheme.typography.labelLarge)
                            Text("$streak / 5", color = Forest, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { streak / 5f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        if (dataError) {
                            Text("Không đọc được bộ từ vựng. Hãy cài lại bản APK đầy đủ hoặc chọn thử thách vận động.")
                        } else if (words.isEmpty()) {
                            Text("Đang chuẩn bị câu hỏi…")
                        } else {
                            val question = remember(words, seed) { Vocabulary.question(words, seed) }
                            Text(question.prompt, style = MaterialTheme.typography.headlineSmall)
                            question.choices.forEach { answer ->
                                OutlinedButton(onClick = {
                                    if (chosen != null || completed) return@OutlinedButton
                                    chosen = answer
                                    correct = answer == question.correct
                                    streak = if (correct) streak + 1 else 0
                                    if (streak >= 5) finishChallenge()
                                }, enabled = chosen == null, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) { Text(answer) }
                            }
                            if (chosen != null) {
                                Text(if (correct) "Chính xác! Tiếp tục nhé." else "Đáp án đúng: ${question.correct}. Chuỗi đúng đã về 0.", color = if (correct) Forest else MaterialTheme.colorScheme.error)
                                Button(onClick = { seed = Random.nextInt(); chosen = null }, modifier = Modifier.fillMaxWidth()) { Text("Câu tiếp theo") }
                            }
                            Text("${words.size} từ vựng · Anh → Việt / Việt → Anh", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

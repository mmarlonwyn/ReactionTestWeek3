package com.example.reactiontestweek3

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GameState { START, WAITING, READY, TRIAL_DONE, FAIL, FINAL }

enum class Category(val message: String, val bg: Color, val imageRes: Int) {
    FAST("DANG YOU ARE SO FAST BRO!", Color(0xFF00E676), R.drawable.img_salute),
    GOOD("YOUR REFLEX IS GOOD", Color(0xFF2196F3), R.drawable.img_thumbs_up),
    MEH("MEH LIKE OTHER PERSON", Color(0xFFFF9800), R.drawable.img_standard),
    SNAIL("YOU LIKE A SNAIL BRO", Color(0xFFFF5722), R.drawable.img_snail);

    companion object {
        fun from(avg: Long?): Category = when {
            avg == null -> SNAIL          // semua trial gagal
            avg < 180 -> FAST
            avg < 280 -> GOOD
            avg < 450 -> MEH
            else -> SNAIL
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { ReactionTestApp() }
        }
    }
}

@Composable
fun ReactionTestApp() {
    var state by remember { mutableStateOf(GameState.START) }
    var trial by remember { mutableIntStateOf(0) }          // 0..2
    var startTime by remember { mutableLongStateOf(0L) }
    var lastTime by remember { mutableLongStateOf(0L) }
    val results = remember { mutableStateListOf<Long?>(null, null, null) }

    // Random delay 500 - 4500 ms memakai delay()
    LaunchedEffect(state, trial) {
        if (state == GameState.WAITING) {
            delay(Random.nextLong(500L, 4501L))
            startTime = SystemClock.elapsedRealtime()
            state = GameState.READY
        }
    }

    val valid = results.filterNotNull()
    val average: Long? = if (valid.isEmpty()) null else valid.average().toLong()
    val category = Category.from(average)

    val bg = when (state) {
        GameState.START -> Color(0xFF6FCDDD)
        GameState.WAITING -> Color(0xFFD6D6D6)
        GameState.READY, GameState.TRIAL_DONE -> Color(0xFF4CAF50)
        GameState.FAIL -> Color(0xFFF94144)
        GameState.FINAL -> category.bg
    }

    fun onScreenTap() {
        when (state) {
            GameState.START -> state = GameState.WAITING
            GameState.WAITING -> {               // klik terlalu cepat = gagal
                results[trial] = null
                state = GameState.FAIL
            }
            GameState.READY -> {
                val t = SystemClock.elapsedRealtime() - startTime
                lastTime = t
                results[trial] = t
                state = if (trial == 2) GameState.FINAL else GameState.TRIAL_DONE
            }
            GameState.TRIAL_DONE -> {
                trial++
                state = GameState.WAITING
            }
            GameState.FAIL -> {
                if (trial == 2) state = GameState.FINAL
                else {
                    trial++
                    state = GameState.START
                }
            }
            GameState.FINAL -> {                 // mulai tes baru
                for (i in 0..2) results[i] = null
                trial = 0
                state = GameState.START
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onScreenTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                GameState.START -> {
                    Title("Reaction")
                    Emoji("⚡")
                    Line("Test", 16)
                    Line("Click to Start", 12)
                    if (trial > 0) ResultCard(results, null)
                }
                GameState.WAITING -> {
                    Title("Get Ready")
                    Emoji("⚠️")
                    Line("Wait for green light...", 14)
                    Line("DON'T CLICK YET!", 11)
                }
                GameState.READY -> {
                    Title("GO!")
                    Emoji("🏃")
                    Line("CLICK NOW!", 16)
                    Line("TAP AS FAST AS YOU CAN!", 11)
                }
                GameState.TRIAL_DONE -> {
                    Title("Trial ${trial + 1} Complete!")
                    Emoji("✅")
                    Line("Time: ${lastTime}ms", 16)
                    Line("Continue to Trial ${trial + 2}", 12)
                    ResultCard(results, null)
                }
                GameState.FAIL -> {
                    Title("FAIL!")
                    Emoji("👎")
                    Line("You clicked too early, TRY TO READ THE RULE BRO", 14)
                    Line("TRY AGAIN", 12)
                    ResultCard(results, null)
                }
                GameState.FINAL -> {
                    Title(category.message)
                    Spacer(Modifier.height(16.dp))
                    Image(
                        painter = painterResource(category.imageRes),
                        contentDescription = category.message,
                        modifier = Modifier.size(150.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Line("Average: ${average?.toString() ?: "-"}ms", 16)
                    Line("Click to Start New Test", 12)
                    ResultCard(results, average)
                }
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(
        text, color = Color.White, fontSize = 26.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
    )
}

@Composable
fun Emoji(e: String) {
    Text(e, fontSize = 90.sp, modifier = Modifier.padding(vertical = 24.dp))
}

@Composable
fun Line(text: String, size: Int) {
    Text(
        text, color = Color.White, fontSize = size.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ResultCard(results: List<Long?>, average: Long?) {
    Card(
        modifier = Modifier.padding(top = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Trial Results", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                results.forEachIndexed { i, r ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${i + 1}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        Text(if (r == null) "-" else "${r}ms", fontSize = 12.sp, color = Color.Black)
                    }
                }
            }
            if (average != null) {
                Spacer(Modifier.height(8.dp))
                Text("Average Score", color = Color(0xFF1976D2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${average}ms", color = Color(0xFFE65100), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReactionTestWeek3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ReactionTestWeek3Theme {
        Greeting("Android")
    }
}
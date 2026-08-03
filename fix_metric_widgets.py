import re

with open("app/src/main/java/com/example/ui/components/MetricWidgets.kt", "r") as f:
    content = f.read()

# Add imports
if "import androidx.lifecycle.compose.LocalLifecycleOwner" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.lifecycle.compose.LocalLifecycleOwner\nimport androidx.lifecycle.Lifecycle\nimport androidx.lifecycle.LifecycleEventObserver")

# Replace MetricItemWithProgress implementation
old_anim = """    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = ""
    )"""

new_anim = """    var trigger by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                trigger = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(trigger) {
        if (!trigger) {
            kotlinx.coroutines.delay(50)
            trigger = true
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (trigger) progress else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = ""
    )"""

content = content.replace(old_anim, new_anim)

# Replace drawArc in Canvas to use LavaBrush
canvas_block = """            Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                drawArc(
                    color = progressColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }"""

new_canvas_block = """            val lavaBrush = rememberLavaBrush()
            Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                drawArc(
                    color = progressColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = lavaBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }"""

content = content.replace(canvas_block, new_canvas_block)

with open("app/src/main/java/com/example/ui/components/MetricWidgets.kt", "w") as f:
    f.write(content)


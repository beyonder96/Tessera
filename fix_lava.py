import os

with open("app/src/main/java/com/example/ui/components/SharedComponents.kt", "r") as f:
    content = f.read()

new_func = """
@Composable
fun rememberLavaBrush(): Brush {
    val isDark = MaterialTheme.colorScheme.background == Color.Black || MaterialTheme.colorScheme.background == Color(0xFF000000)
    
    val infiniteTransition = rememberInfiniteTransition(label = "lavaLamp")
    
    val targetColor1 = if (isDark) Color(0xFFFF5722) else Color(0xFF00BCD4) // Thermal: Deep Orange | Frosty: Cyan
    val targetColor2 = if (isDark) Color(0xFFFFC107) else Color(0xFF81D4FA) // Thermal: Amber | Frosty: Light Blue
    
    val targetColor3 = if (isDark) Color(0xFFFF0000) else Color(0xFF03A9F4) // Thermal: Red | Frosty: Light Blue
    val targetColor4 = if (isDark) Color(0xFFFF9800) else Color(0xFF4FC3F7) // Thermal: Orange | Frosty: Blue
    
    val color1 by infiniteTransition.animateColor(
        initialValue = targetColor1,
        targetValue = targetColor2,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = targetColor3,
        targetValue = targetColor4,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    return Brush.linearGradient(
        colors = listOf(color1, color2),
        start = Offset(0f, 0f),
        end = Offset(500f, 500f)
    )
}
"""

if "fun rememberLavaBrush" not in content:
    content += new_func
    
    # ensure imports are present
    if "import androidx.compose.animation.animateColor" not in content:
        content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.animation.animateColor\nimport androidx.compose.animation.core.*\nimport androidx.compose.ui.graphics.Brush")
    
    with open("app/src/main/java/com/example/ui/components/SharedComponents.kt", "w") as f:
        f.write(content)


import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove the old lavaBrush definition block
old_lava_block = re.search(r'val infiniteTransition = rememberInfiniteTransition\(label = "lavaLamp"\).*?end = Offset\(500f, 500f\)\n    \)', content, re.DOTALL)
if old_lava_block:
    content = content.replace(old_lava_block.group(0), "val lavaBrush = com.example.ui.components.rememberLavaBrush()")

# Apply aura to photo
content = content.replace("Modifier\n                            .size(28.dp)\n                            .clip(CircleShape)\n                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)", 
                          "Modifier.size(32.dp).clip(CircleShape).border(2.dp, lavaBrush, CircleShape).drawBehind { drawCircle(brush = lavaBrush, radius = size.width/2 + 4.dp.toPx(), alpha = 0.4f) }")

# Apply aura to Metro icon
content = content.replace("""Icon(
                    imageVector = Icons.Outlined.DirectionsTransit,
                    contentDescription = "Status do Metrô",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp)
                )""", 
"""Box(modifier = Modifier.size(36.dp).drawBehind { drawCircle(brush = lavaBrush, radius = size.width/2, alpha = 0.4f) }, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsTransit,
                        contentDescription = "Status do Metrô",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }""")

# TESSERA Text resizing
content = content.replace("""Text(
                text = "TESSERA",
                fontSize = 16.sp, // Made smaller
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(brush = lavaBrush), // Applied lava lamp
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )""",
"""Text(
                text = "TESSERA",
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(brush = lavaBrush),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )""")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)


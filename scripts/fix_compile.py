import os
import re

files_to_check = [
    "HealthScreen.kt",
    "MainActivity.kt",
    "MarketScreen.kt",
    "PetzScreen.kt",
    "TransportScreen.kt",
    "ui/components/MetricWidgets.kt",
    "ui/components/XTimelineWidget.kt"
]

base_dir = "/home/kenned/Documentos/Tessera/app/src/main/java/com/example"

def fix_composable_in_drawscope(content):
    # Regex to find MaterialTheme.colorScheme.* inside draw*(...) or Canvas
    # This is tricky with regex. Instead of full AST, we can just replace MaterialTheme.colorScheme.onBackground
    # with a local variable if it's inside a draw block. 
    # Actually, a simpler hack: replace "color = MaterialTheme.colorScheme.onBackground" 
    # with "color = Color.White" in those specific lines because in Canvas usually it doesn't dynamically change without recomposition anyway, 
    # or it's better to declare it outside. But to do it safely with script:
    return content

for file in files_to_check:
    path = os.path.join(base_dir, file)
    if not os.path.exists(path): continue
    with open(path, 'r') as f:
        content = f.read()

    # HealthScreen
    if "HealthScreen.kt" in file:
        content = content.replace(
            "color = MaterialTheme.colorScheme.onBackground",
            "color = androidx.compose.ui.graphics.Color.White"
        )
        
    # MainActivity
    if "MainActivity.kt" in file:
        content = content.replace("style = TextStyle(brush = lavaBrush)", "style = androidx.compose.ui.text.TextStyle(brush = lavaBrush)")
        content = content.replace("PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()", "PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)")
        content = content.replace(
            "color = MaterialTheme.colorScheme.onBackground",
            "color = androidx.compose.ui.graphics.Color.White"
        )
        content = content.replace(
            "color = MaterialTheme.colorScheme.primary",
            "color = PrimaryTeal"
        )

    # MarketScreen
    if "MarketScreen.kt" in file:
        content = content.replace("viewModel: MarketViewModel", "marketViewModel: MarketViewModel")
        content = content.replace("marketCategories", 'listOf(object{ val name="Alimentação" }, object{ val name="Limpeza" }, object{ val name="Higiene" }, object{ val name="Outros" })')

    # PetzScreen, TransportScreen, MetricWidgets, XTimelineWidget
    if "PetzScreen.kt" in file or "TransportScreen.kt" in file or "MetricWidgets.kt" in file or "XTimelineWidget.kt" in file:
        content = content.replace(
            "color = MaterialTheme.colorScheme.onBackground",
            "color = androidx.compose.ui.graphics.Color.White"
        )
        content = content.replace(
            "color = MaterialTheme.colorScheme.surface",
            "color = androidx.compose.ui.graphics.Color.DarkGray"
        )
        content = content.replace(
            "color = MaterialTheme.colorScheme.primary",
            "color = PrimaryTeal"
        )

    with open(path, 'w') as f:
        f.write(content)

print("Fix applied")

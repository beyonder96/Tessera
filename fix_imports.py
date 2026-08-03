import re

# Fix SharedComponents.kt
with open("app/src/main/java/com/example/ui/components/SharedComponents.kt", "r") as f:
    content = f.read()

# Add getValue import
if "import androidx.compose.runtime.getValue" not in content:
    content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue")

# Fix ambiguous Brush import (we might have two `import androidx.compose.ui.graphics.Brush`)
lines = content.split('\n')
unique_lines = []
for line in lines:
    if line.startswith("import androidx.compose.ui.graphics.Brush"):
        if line not in unique_lines:
            unique_lines.append(line)
    else:
        unique_lines.append(line)
        
content = '\n'.join(unique_lines)

with open("app/src/main/java/com/example/ui/components/SharedComponents.kt", "w") as f:
    f.write(content)


# Fix MetricWidgets.kt
with open("app/src/main/java/com/example/ui/components/MetricWidgets.kt", "r") as f:
    content = f.read()
    
if "import androidx.compose.runtime.remember" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\nimport androidx.compose.runtime.LaunchedEffect\nimport androidx.compose.runtime.DisposableEffect")

with open("app/src/main/java/com/example/ui/components/MetricWidgets.kt", "w") as f:
    f.write(content)


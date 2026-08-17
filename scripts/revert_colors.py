import os

base_dir = "/home/kenned/Documentos/Tessera/app/src/main/java/com/example"

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            if "androidx.compose.ui.graphics.Color.White" in content:
                content = content.replace("androidx.compose.ui.graphics.Color.White", "MaterialTheme.colorScheme.onBackground")
                with open(path, 'w') as f:
                    f.write(content)

print("Reverted!")

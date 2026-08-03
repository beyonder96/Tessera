import os

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Replacements
    content = content.replace("BackgroundDark", "MaterialTheme.colorScheme.background")
    content = content.replace("OnBackgroundDark", "MaterialTheme.colorScheme.onBackground")
    content = content.replace("SurfaceVariantDark", "MaterialTheme.colorScheme.surfaceVariant")
    content = content.replace("SurfaceGlassDark", "MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)")
    content = content.replace("BorderGlassDark", "MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)")
    
    with open(filepath, 'w') as f:
        f.write(content)

base_dir = "app/src/main/java/com/example"
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            replace_in_file(os.path.join(root, file))

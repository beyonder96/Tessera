import os

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
        
    old_content = content
    content = content.replace("OnMaterialTheme.colorScheme.background", "MaterialTheme.colorScheme.onBackground")
    
    if content != old_content:
        with open(filepath, 'w') as f:
            f.write(content)

base_dir = "app/src/main/java/com/example"
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            replace_in_file(os.path.join(root, file))

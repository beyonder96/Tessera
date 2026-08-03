with open("app/src/main/java/com/example/HealthScreen.kt", "r") as f:
    lines = f.readlines()

# 1387: Canvas for progress
lines[1386] = "                val sysOnBackground = MaterialTheme.colorScheme.onBackground\n" + lines[1386]

# 1551: Canvas for weight chart
lines[1550] = "            val sysOnBackground = MaterialTheme.colorScheme.onBackground\n" + lines[1550]

# 1729: Canvas for moon
lines[1728] = "                val sysOnBackground = MaterialTheme.colorScheme.onBackground\n" + lines[1728]

for i in range(len(lines)):
    if "color = MaterialTheme.colorScheme.onBackground," in lines[i] and i > 1551 and i < 1620:
        lines[i] = lines[i].replace("color = MaterialTheme.colorScheme.onBackground", "color = sysOnBackground")
    if "color = MaterialTheme.colorScheme.onBackground" in lines[i] and i > 1729 and i < 1760:
        lines[i] = lines[i].replace("color = MaterialTheme.colorScheme.onBackground", "color = sysOnBackground")

with open("app/src/main/java/com/example/HealthScreen.kt", "w") as f:
    f.writelines(lines)

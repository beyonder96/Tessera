$content = Get-Content app/src/main/java/com/example/MainActivity.kt
$level = 0
$lineNum = 1
foreach ($line in $content) {
    foreach ($char in $line.ToCharArray()) {
        if ($char -eq '{') { $level++ }
        elseif ($char -eq '}') { $level-- }
    }
    if ($level -lt 0) {
        Write-Host "Negative level at line ${lineNum}: $line"
        $level = 0
    }
    $lineNum++
}
Write-Host "Final level: $level"

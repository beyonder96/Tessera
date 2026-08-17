sed -i '/animateItems = true/a \
\
    var financeIndex by remember { mutableStateOf(0) }\
    var healthIndex by remember { mutableStateOf(0) }\
    LaunchedEffect(Unit) {\
        while(true) {\
            kotlinx.coroutines.delay(4000L)\
            financeIndex = (financeIndex + 1) % 4\
            healthIndex = (healthIndex + 1) % 3\
        }\
    }\
    val netWorth = totalIncome - totalExpense\
    val todayStart = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis\
    val todayEnd = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 23); set(java.util.Calendar.MINUTE, 59); set(java.util.Calendar.SECOND, 59); set(java.util.Calendar.MILLISECOND, 999) }.timeInMillis\
    val todaySteps = stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }\
    val latestWeight = weightRecords.lastOrNull()?.weightKg ?: 70.0\
    val aptProgress = sharedPrefs.getFloat("apartment_progress", 0.75f)\
' app/src/main/java/com/example/DailyScreen.kt

sed -i '/HeaderGreetingSection(/,/)/a \
                        // 1.5. HOME SCREEN METRICS WIDGETS\
                        Row(\
                            modifier = Modifier\
                                .fillMaxWidth(),\
                            horizontalArrangement = Arrangement.SpaceEvenly,\
                            verticalAlignment = Alignment.CenterVertically\
                        ) {\
                            // Widget 1 (Finanças)\
                            Box(modifier = Modifier.width(76.dp)) {\
                                Crossfade(targetState = financeIndex, animationSpec = tween(500), label = "FinanceRotation") { idx ->\
                                    val valIdx = when(idx) {\
                                        0 -> totalPatrimony\
                                        1 -> netWorth\
                                        2 -> totalIncome\
                                        else -> totalExpense\
                                    }\
                                    val labelIdx = when(idx) {\
                                        0 -> "PATRIMÔNIO"\
                                        1 -> "SALDO"\
                                        2 -> "RECEITAS"\
                                        else -> "DESPESAS"\
                                    }\
                                    val iconIdx = when(idx) {\
                                        0 -> Icons.Outlined.AccountBalance\
                                        1 -> Icons.Outlined.AccountBalanceWallet\
                                        2 -> Icons.Outlined.ArrowUpward\
                                        else -> Icons.Outlined.ArrowDownward\
                                    }\
                                    val formattedIdx = if (valIdx >= 1000) "${(valIdx / 1000).toInt()}k" else valIdx.toInt().toString()\
                                    MetricItem(iconIdx, formattedIdx, labelIdx, onClick = { onNavigate("finance") })\
                                }\
                            }\
\
                            // Widget 2 (Saúde)\
                            Box(modifier = Modifier.width(76.dp)) {\
                                Crossfade(targetState = healthIndex, animationSpec = tween(500), label = "HealthRotation") { idx ->\
                                    val iconIdx = when (idx) {\
                                        0 -> Icons.Outlined.Bedtime\
                                        1 -> Icons.Outlined.MonitorWeight\
                                        else -> Icons.Outlined.DirectionsWalk\
                                    }\
                                    val valIdx = when (idx) {\
                                        0 -> String.format(java.util.Locale("pt", "BR"), "%.1fh", latestSleep)\
                                        1 -> String.format(java.util.Locale("pt", "BR"), "%.1f", latestWeight)\
                                        else -> todaySteps.toString()\
                                    }\
                                    val labelIdx = when (idx) {\
                                        0 -> "SONO"\
                                        1 -> "PESO"\
                                        else -> "PASSOS"\
                                    }\
                                    val progressIdx = when (idx) {\
                                        0 -> (latestSleep / 10.0).toFloat().coerceIn(0f, 1f)\
                                        1 -> (latestWeight / 120.0f).toFloat().coerceIn(0f, 1f)\
                                        else -> (todaySteps.toFloat() / 10000f).coerceIn(0f, 1f)\
                                    }\
                                    val colorIdx = when (idx) {\
                                        0 -> PrimaryTeal\
                                        1 -> TertiaryPurple\
                                        else -> Color(0xFF4D96FF)\
                                    }\
                                    MetricItemWithProgress(iconIdx, valIdx, labelIdx, colorIdx, progressIdx, onClick = { onNavigate("health") })\
                                }\
                            }\
\
                            // Widget 3 (Apartamento)\
                            MetricItemWithProgress(Icons.Outlined.Construction, "${(aptProgress * 100).toInt()}%", "OBRA", SecondaryGold, aptProgress, onClick = { onNavigate("apartment") })\
                        }\
' app/src/main/java/com/example/DailyScreen.kt

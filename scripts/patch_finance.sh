#!/bin/bash
sed -i '/Row(/,/Text("3 \/ 3", fontSize = 10.sp, color = Color(0x99FFFFFF), fontWeight = FontWeight.Bold)\n                                    }/d' app/src/main/java/com/example/FinanceScreen.kt

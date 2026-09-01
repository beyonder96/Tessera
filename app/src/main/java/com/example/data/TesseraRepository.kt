package com.example.data

import kotlinx.coroutines.flow.Flow

class TesseraRepository(private val dao: TesseraDao) {
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val pendingMarketItems: Flow<List<MarketItem>> = dao.getPendingMarketItems()
    val shoppingMarketItems: Flow<List<MarketItem>> = dao.getShoppingMarketItems()
    val boughtMarketItems: Flow<List<MarketItem>> = dao.getBoughtMarketItems()
    val allPetEvents: Flow<List<PetEvent>> = dao.getAllPetEvents()

    suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        dao.insertTransactions(transactions)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactions(transactions: List<Transaction>) {
        dao.deleteTransactions(transactions)
    }

    suspend fun updateTransactionsCategory(ids: List<Int>, category: String) {
        dao.updateTransactionsCategory(ids, category)
    }

    suspend fun insertMarketItem(item: MarketItem) {
        dao.insertMarketItem(item)
    }

    suspend fun updateMarketItem(item: MarketItem) {
        dao.updateMarketItem(item)
    }

    suspend fun deleteMarketItem(item: MarketItem) {
        dao.deleteMarketItem(item)
    }

    suspend fun deletePlanningItemsByNames(names: List<String>) {
        dao.deletePlanningItemsByNames(names)
    }

    suspend fun syncMarketItems(
        itemsToInsert: List<MarketItem>,
        itemsToUpdate: List<MarketItem>,
        itemsToDelete: List<MarketItem>
    ) {
        dao.syncMarketItems(itemsToInsert, itemsToUpdate, itemsToDelete)
    }

    suspend fun updatePetEvent(event: PetEvent) {
        dao.updatePetEvent(event)
    }
    
    suspend fun getPetEventsCount(): Int {
        return dao.getPetEventsCount()
    }

    suspend fun insertPetEvents(events: List<PetEvent>) {
        dao.insertPetEvents(events)
    }

    suspend fun insertPetEvent(event: PetEvent) {
        dao.insertPetEvent(event)
    }

    suspend fun deletePetEvent(event: PetEvent) {
        dao.deletePetEvent(event)
    }

    val allBankAccounts: Flow<List<BankAccount>> = dao.getAllBankAccounts()
    val allCreditCards: Flow<List<CreditCard>> = dao.getAllCreditCards()

    suspend fun insertBankAccount(account: BankAccount) {
        dao.insertBankAccount(account)
    }

    suspend fun deleteBankAccount(account: BankAccount) {
        dao.deleteBankAccount(account)
    }

    suspend fun insertCreditCard(card: CreditCard) {
        dao.insertCreditCard(card)
    }

    suspend fun deleteCreditCard(card: CreditCard) {
        dao.deleteCreditCard(card)
    }

    suspend fun payInvoice(cardId: Int) {
        dao.payInvoice(cardId)
    }

    // Benefit Cards
    val allBenefitCards: Flow<List<BenefitCard>> = dao.getAllBenefitCards()

    suspend fun insertBenefitCard(card: BenefitCard) {
        dao.insertBenefitCard(card)
    }

    suspend fun deleteBenefitCard(card: BenefitCard) {
        dao.deleteBenefitCard(card)
    }

    suspend fun clearAllFinances() {
        dao.clearAllFinances()
    }

    val allHabits: Flow<List<Habit>> = dao.getAllHabits()
    val allPurchaseGoals: Flow<List<PurchaseGoal>> = dao.getAllPurchaseGoals()

    suspend fun insertHabit(habit: Habit) {
        dao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        dao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        dao.deleteHabit(habit)
    }

    suspend fun insertPurchaseGoal(goal: PurchaseGoal) {
        dao.insertPurchaseGoal(goal)
    }

    suspend fun updatePurchaseGoal(goal: PurchaseGoal) {
        dao.updatePurchaseGoal(goal)
    }

    suspend fun deletePurchaseGoal(goal: PurchaseGoal) {
        dao.deletePurchaseGoal(goal)
    }

    // Health
    val healthProfile: Flow<HealthProfile?> = dao.getHealthProfile()
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()
    val allWeightRecords: Flow<List<WeightRecord>> = dao.getAllWeightRecords()
    val allSleepRecords: Flow<List<SleepRecord>> = dao.getAllSleepRecords()

    suspend fun insertHealthProfile(profile: HealthProfile) = dao.insertHealthProfile(profile)
    suspend fun insertMedication(medication: Medication) = dao.insertMedication(medication)
    suspend fun updateMedication(medication: Medication) = dao.updateMedication(medication)
    suspend fun deleteMedication(medication: Medication) = dao.deleteMedication(medication)
    
    suspend fun insertWeightRecord(record: WeightRecord) = dao.insertWeightRecord(record)
    suspend fun clearHealthConnectWeightRecords() = dao.clearHealthConnectWeightRecords()
    
    suspend fun insertSleepRecord(record: SleepRecord) = dao.insertSleepRecord(record)
    suspend fun clearHealthConnectSleepRecords() = dao.clearHealthConnectSleepRecords()
    suspend fun clearManualSleepForDay(startOfDay: Long, endOfDay: Long) = dao.clearManualSleepForDay(startOfDay, endOfDay)

    val allPets: Flow<List<PetEntity>> = dao.getAllPets()

    suspend fun insertPet(pet: PetEntity): Long {
        return dao.insertPet(pet)
    }

    suspend fun updatePet(pet: PetEntity) {
        dao.updatePet(pet)
    }

    suspend fun deletePet(pet: PetEntity) {
        dao.deletePet(pet)
    }

    fun getWeightHistoryForPet(petId: Int): Flow<List<PetWeightHistoryEntity>> {
        return dao.getWeightHistoryForPet(petId)
    }

    suspend fun insertWeightHistory(record: PetWeightHistoryEntity) {
        dao.insertWeightHistory(record)
    }

    suspend fun deleteWeightHistory(record: PetWeightHistoryEntity) {
        dao.deleteWeightHistory(record)
    }

    fun getMedicationLogsForRange(start: Long, end: Long): Flow<List<MedicationLog>> {
        return dao.getMedicationLogsForRange(start, end)
    }

    fun getLogsForMedication(medicationId: Int, start: Long, end: Long): Flow<List<MedicationLog>> {
        return dao.getLogsForMedication(medicationId, start, end)
    }

    suspend fun insertMedicationLog(log: MedicationLog) {
        dao.insertMedicationLog(log)
    }

    suspend fun deleteMedicationLog(log: MedicationLog) {
        dao.deleteMedicationLog(log)
    }

    val allStepsRecords: Flow<List<StepsRecord>> = dao.getAllStepsRecords()

    suspend fun insertStepsRecord(record: StepsRecord) {
        dao.insertStepsRecord(record)
    }

    suspend fun clearHealthConnectStepsRecords() {
        dao.clearHealthConnectStepsRecords()
    }

    suspend fun clearManualStepsForDay(startOfDay: Long, endOfDay: Long) {
        dao.clearManualStepsForDay(startOfDay, endOfDay)
    }

    // Routines
    val allRoutines: Flow<List<Routine>> = dao.getAllRoutines()

    fun getStepsForRoutine(routineId: Int): Flow<List<RoutineStep>> {
        return dao.getStepsForRoutine(routineId)
    }

    suspend fun insertRoutine(routine: Routine): Long {
        return dao.insertRoutine(routine)
    }

    suspend fun insertRoutineStep(step: RoutineStep) {
        dao.insertRoutineStep(step)
    }

    suspend fun clearStepsForRoutine(routineId: Int) {
        dao.clearStepsForRoutine(routineId)
    }

    suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) {
        dao.saveRoutineWithSteps(routine, steps)
    }

    suspend fun deleteRoutine(routine: Routine) {
        dao.deleteRoutine(routine)
    }

    val allDebts: Flow<List<Debt>> = dao.getAllDebts()

    suspend fun insertDebt(debt: Debt) {
        dao.insertDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        dao.deleteDebt(debt)
    }

    // Nutrition & Meal Records
    val allMealRecords: Flow<List<MealRecord>> = dao.getAllMealRecords()

    fun getMealRecordsForDate(date: String): Flow<List<MealRecord>> {
        return dao.getMealRecordsForDate(date)
    }

    suspend fun insertMealRecord(meal: MealRecord): Long {
        return dao.insertMealRecord(meal)
    }

    suspend fun updateMealRecord(meal: MealRecord) {
        dao.updateMealRecord(meal)
    }

    suspend fun deleteMealRecord(meal: MealRecord) {
        dao.deleteMealRecord(meal)
    }

    suspend fun deleteMealRecordById(id: Int) {
        dao.deleteMealRecordById(id)
    }

    // Water Intake Records
    val allWaterRecords: Flow<List<WaterRecord>> = dao.getAllWaterRecords()

    fun getWaterRecordsForDate(date: String): Flow<List<WaterRecord>> {
        return dao.getWaterRecordsForDate(date)
    }

    suspend fun insertWaterRecord(record: WaterRecord): Long {
        return dao.insertWaterRecord(record)
    }

    suspend fun deleteWaterRecord(record: WaterRecord) {
        dao.deleteWaterRecord(record)
    }

    suspend fun deleteWaterRecordById(id: Int) {
        dao.deleteWaterRecordById(id)
    }

    private val bibliaApiKey = "bapi_cyd65a70b4cmbin97bcojzs3vmq7cpxnhyo2lgfjogiup9d5"

    private val bibliaOkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $bibliaApiKey")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val bibleMoshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val openBibleOkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val openBibleRetrofit = retrofit2.Retrofit.Builder()
        .baseUrl("https://bible-api.com/")
        .client(openBibleOkHttpClient)
        .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(bibleMoshi))
        .build()

    private val openBibleApi = openBibleRetrofit.create(OpenBibleApi::class.java)

    private val offlineVerses = listOf(
        BibleVerseResponse(book = ABibliaBook(name = "Salmos", version = "Almeida"), chapter = 23, verse = 1, text = "O SENHOR é meu pastor, e nada me faltará.", bookAbbrev = "sl", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Filipenses", version = "Almeida"), chapter = 4, verse = 13, text = "Posso todas as coisas por meio de Cristo, que me dá forças.", bookAbbrev = "fp", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Josué", version = "Almeida"), chapter = 1, verse = 9, text = "Esta é minha ordem: Seja forte e corajoso! Não tenha medo nem desanime, pois o SENHOR, seu Deus, estará com você por onde você andar.", bookAbbrev = "js", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Jeremias", version = "Almeida"), chapter = 29, verse = 11, text = "Porque eu sei os planos que tenho para vocês, diz o SENHOR. São planos de bem, e não de mal, para lhes dar um futuro e uma esperança.", bookAbbrev = "jr", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Provérbios", version = "Almeida"), chapter = 3, verse = 5, text = "Confie no SENHOR de todo o coração; não dependa de seu próprio entendimento.", bookAbbrev = "pv", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Isaías", version = "Almeida"), chapter = 41, verse = 10, text = "Não tenha medo, pois estou com você; não desanime, pois sou o seu Deus. Eu o fortalecerei e o ajudarei; eu o sustentarei com minha mão direita vitoriosa.", bookAbbrev = "is", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Mateus", version = "Almeida"), chapter = 11, verse = 28, text = "Venham a mim todos vocês que estão cansados e sobrecarregados, e eu lhes darei descanso.", bookAbbrev = "mt", versionCode = "Almeida"),
        BibleVerseResponse(book = ABibliaBook(name = "Romanos", version = "Almeida"), chapter = 8, verse = 28, text = "E sabemos que Deus faz todas as coisas cooperarem para o bem daqueles que o amam e que são chamados de acordo com seu propósito.", bookAbbrev = "rm", versionCode = "Almeida")
    )

    private val bookAbbrevToSearchName = mapOf(
        "gn" to "genesis", "ex" to "exodo", "lv" to "levitico", "nm" to "numeros", "dt" to "deuteronomio",
        "js" to "josue", "jz" to "juizes", "rt" to "rute", "1sm" to "1 samuel", "2sm" to "2 samuel",
        "1rs" to "1 reis", "2rs" to "2 reis", "1cr" to "1 cronicas", "2cr" to "2 cronicas",
        "ed" to "esdras", "ne" to "neemias", "et" to "ester", "job" to "jo", "sl" to "salmos",
        "pv" to "proverbios", "ec" to "eclesiastes", "ct" to "canticos", "is" to "isaias", "jr" to "jeremias",
        "lm" to "lamentacoes", "ez" to "ezequiel", "dn" to "daniel", "os" to "oseias", "jl" to "joel",
        "am" to "amos", "ob" to "obadias", "jn" to "jonas", "mq" to "miqueias", "na" to "naum",
        "hc" to "habacuque", "sf" to "sofonias", "ag" to "ageu", "zc" to "zacarias", "ml" to "malaquias",
        "mt" to "mateus", "mc" to "marcos", "lc" to "lucas", "jo" to "joao", "atos" to "atos",
        "rm" to "romanos", "1co" to "1 corintios", "2co" to "2 corintios", "gl" to "galatas", "ef" to "efesios",
        "fp" to "filipenses", "cl" to "colossenses", "1ts" to "1 tessalonicenses", "2ts" to "2 tessalonicenses",
        "1tm" to "1 timoteo", "2tm" to "2 timoteo", "tt" to "tito", "fm" to "filemom", "hb" to "hebreus",
        "tg" to "tiago", "1pe" to "1 pedro", "2pe" to "2 pedro", "1jo" to "1 joao", "2jo" to "2 joao",
        "3jo" to "3 joao", "jd" to "judas", "ap" to "apocalipse"
    )

    suspend fun getRandomBibleVerse(version: String = "Almeida"): BibleVerseResponse {
        return try {
            val popularPassages = listOf(
                "salmos+23:1", "filipenses+4:13", "josue+1:9", "jeremias+29:11",
                "proverbios+3:5", "isaias+41:10", "mateus+11:28", "romanos+8:28",
                "joao+3:16", "salmos+91:1", "mateus+6:33", "1+corintios+13:13"
            )
            val randomPassage = popularPassages.random()
            val response = openBibleApi.getPassage(randomPassage, "almeida")
            val firstVerse = response.verses.firstOrNull()
            val cleanText = (firstVerse?.text ?: response.text ?: "").trim()
            val bookName = firstVerse?.bookName ?: response.reference?.split(" ")?.firstOrNull() ?: "Salmos"
            val chapter = firstVerse?.chapter ?: 1
            val verseNum = firstVerse?.verse ?: 1
            val bookAbbrev = defaultBooksList.find { it.name.equals(bookName, ignoreCase = true) }?.abbrev ?: "sl"

            if (cleanText.isNotBlank()) {
                BibleVerseResponse(
                    book = ABibliaBook(name = bookName, version = response.translationName ?: "Almeida"),
                    chapter = chapter,
                    verse = verseNum,
                    text = cleanText,
                    bookAbbrev = bookAbbrev,
                    versionCode = "Almeida"
                )
            } else {
                offlineVerses.random()
            }
        } catch (e: Exception) {
            offlineVerses.random()
        }
    }

    suspend fun getBibleVersions(): List<BibliaVersionItem> {
        return defaultVersionsList
    }

    suspend fun getBibleBooks(): List<BibliaBookItem> {
        return defaultBooksList
    }

    suspend fun getBibleChapter(version: String, bookAbbrev: String, chapter: Int): Result<BibliaChapterData> {
        return try {
            val book = defaultBooksList.find { it.abbrev.equals(bookAbbrev, ignoreCase = true) }
            val searchBookName = bookAbbrevToSearchName[bookAbbrev.lowercase().trim()] 
                ?: book?.name?.lowercase()?.trim() 
                ?: bookAbbrev.lowercase().trim()

            val queryPassage = "${searchBookName.replace(" ", "+")}+$chapter"
            val response = openBibleApi.getPassage(queryPassage, "almeida")

            if (response.verses.isNotEmpty()) {
                val chapterData = BibliaChapterData(
                    reference = response.reference ?: "${book?.name ?: searchBookName.capitalize()} $chapter",
                    version = response.translationName ?: "Almeida",
                    book = book ?: BibliaBookItem(id = 0, name = response.verses.firstOrNull()?.bookName ?: searchBookName, abbrev = bookAbbrev, testament = "VT"),
                    chapter = BibliaChapterInfo(number = chapter, totalVerses = response.verses.size),
                    verses = response.verses.map { v ->
                        BibliaVerseItem(number = v.verse, text = v.text.trim())
                    }
                )
                Result.success(chapterData)
            } else {
                Result.failure(Exception("Nenhum versículo encontrado para este capítulo."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível carregar o capítulo. Verifique sua conexão."))
        }
    }

    private val defaultVersionsList = listOf(
        BibliaVersionItem("NVT", "Nova Versão Transformadora - 2016", "Mundo Cristão", "pt-BR"),
        BibliaVersionItem("NVI", "Nova Versão Internacional - 2000", "Biblica", "pt-BR"),
        BibliaVersionItem("ACF", "Almeida Corrigida e Fiel - 1994", "SBTB", "pt-BR"),
        BibliaVersionItem("ARA", "Almeida Revista e Atualizada - 1993", "SBB", "pt-BR"),
        BibliaVersionItem("ARC", "Almeida Revista e Corrigida - 1995", "SBB", "pt-BR"),
        BibliaVersionItem("NAA", "Nova Almeida Atualizada - 2017", "SBB", "pt-BR"),
        BibliaVersionItem("NTLH", "Nova Tradução na Linguagem de Hoje - 1988", "SBB", "pt-BR")
    )

    private val defaultBooksList = listOf(
        BibliaBookItem(1, "Gênesis", "gn", "VT"),
        BibliaBookItem(2, "Êxodo", "ex", "VT"),
        BibliaBookItem(3, "Levítico", "lv", "VT"),
        BibliaBookItem(4, "Números", "nm", "VT"),
        BibliaBookItem(5, "Deuteronômio", "dt", "VT"),
        BibliaBookItem(6, "Josué", "js", "VT"),
        BibliaBookItem(7, "Juízes", "jz", "VT"),
        BibliaBookItem(8, "Rute", "rt", "VT"),
        BibliaBookItem(9, "1 Samuel", "1sm", "VT"),
        BibliaBookItem(10, "2 Samuel", "2sm", "VT"),
        BibliaBookItem(11, "1 Reis", "1rs", "VT"),
        BibliaBookItem(12, "2 Reis", "2rs", "VT"),
        BibliaBookItem(13, "1 Crônicas", "1cr", "VT"),
        BibliaBookItem(14, "2 Crônicas", "2cr", "VT"),
        BibliaBookItem(15, "Esdras", "ed", "VT"),
        BibliaBookItem(16, "Neemias", "ne", "VT"),
        BibliaBookItem(17, "Ester", "et", "VT"),
        BibliaBookItem(18, "Jó", "job", "VT"),
        BibliaBookItem(19, "Salmos", "sl", "VT"),
        BibliaBookItem(20, "Provérbios", "pv", "VT"),
        BibliaBookItem(21, "Eclesiastes", "ec", "VT"),
        BibliaBookItem(22, "Cânticos", "ct", "VT"),
        BibliaBookItem(23, "Isaías", "is", "VT"),
        BibliaBookItem(24, "Jeremias", "jr", "VT"),
        BibliaBookItem(25, "Lamentações", "lm", "VT"),
        BibliaBookItem(26, "Ezequiel", "ez", "VT"),
        BibliaBookItem(27, "Daniel", "dn", "VT"),
        BibliaBookItem(28, "Oséias", "os", "VT"),
        BibliaBookItem(29, "Joel", "jl", "VT"),
        BibliaBookItem(30, "Amós", "am", "VT"),
        BibliaBookItem(31, "Obadias", "ob", "VT"),
        BibliaBookItem(32, "Jonas", "jn", "VT"),
        BibliaBookItem(33, "Miquéias", "mq", "VT"),
        BibliaBookItem(34, "Naum", "na", "VT"),
        BibliaBookItem(35, "Habacuque", "hc", "VT"),
        BibliaBookItem(36, "Sofonias", "sf", "VT"),
        BibliaBookItem(37, "Ageu", "ag", "VT"),
        BibliaBookItem(38, "Zacarias", "zc", "VT"),
        BibliaBookItem(39, "Malaquias", "ml", "VT"),
        BibliaBookItem(40, "Mateus", "mt", "NT"),
        BibliaBookItem(41, "Marcos", "mc", "NT"),
        BibliaBookItem(42, "Lucas", "lc", "NT"),
        BibliaBookItem(43, "João", "jo", "NT"),
        BibliaBookItem(44, "Atos dos Apóstolos", "atos", "NT"),
        BibliaBookItem(45, "Romanos", "rm", "NT"),
        BibliaBookItem(46, "1 Coríntios", "1co", "NT"),
        BibliaBookItem(47, "2 Coríntios", "2co", "NT"),
        BibliaBookItem(48, "Gálatas", "gl", "NT"),
        BibliaBookItem(49, "Efésios", "ef", "NT"),
        BibliaBookItem(50, "Filipenses", "fp", "NT"),
        BibliaBookItem(51, "Colossenses", "cl", "NT"),
        BibliaBookItem(52, "1 Tessalonicenses", "1ts", "NT"),
        BibliaBookItem(53, "2 Tessalonicenses", "2ts", "NT"),
        BibliaBookItem(54, "1 Timóteo", "1tm", "NT"),
        BibliaBookItem(55, "2 Timóteo", "2tm", "NT"),
        BibliaBookItem(56, "Tito", "tt", "NT"),
        BibliaBookItem(57, "Filemom", "fm", "NT"),
        BibliaBookItem(58, "Hebreus", "hb", "NT"),
        BibliaBookItem(59, "Tiago", "tg", "NT"),
        BibliaBookItem(60, "1 Pedro", "1pe", "NT"),
        BibliaBookItem(61, "2 Pedro", "2pe", "NT"),
        BibliaBookItem(62, "1 João", "1jo", "NT"),
        BibliaBookItem(63, "2 João", "2jo", "NT"),
        BibliaBookItem(64, "3 João", "3jo", "NT"),
        BibliaBookItem(65, "Judas", "jd", "NT"),
        BibliaBookItem(66, "Apocalipse", "ap", "NT")
    )
}

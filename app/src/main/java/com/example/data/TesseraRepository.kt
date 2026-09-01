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

    private val bibleRetrofit = retrofit2.Retrofit.Builder()
        .baseUrl("https://bibliaapi.com.br/api/v2/")
        .client(bibliaOkHttpClient)
        .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(bibleMoshi))
        .build()

    private val bibleApi = bibleRetrofit.create(BibleApi::class.java)

    private val offlineVerses = listOf(
        BibleVerseResponse(book = ABibliaBook(name = "Salmos", version = "NVT"), chapter = 23, verse = 1, text = "O SENHOR é meu pastor, e nada me faltará.", bookAbbrev = "sl", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Filipenses", version = "NVT"), chapter = 4, verse = 13, text = "Posso todas as coisas por meio de Cristo, que me dá forças.", bookAbbrev = "fp", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Josué", version = "NVT"), chapter = 1, verse = 9, text = "Esta é minha ordem: Seja forte e corajoso! Não tenha medo nem desanime, pois o SENHOR, seu Deus, estará com você por onde você andar.", bookAbbrev = "js", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Jeremias", version = "NVT"), chapter = 29, verse = 11, text = "Porque eu sei os planos que tenho para vocês, diz o SENHOR. São planos de bem, e não de mal, para lhes dar um futuro e uma esperança.", bookAbbrev = "jr", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Provérbios", version = "NVT"), chapter = 3, verse = 5, text = "Confie no SENHOR de todo o coração; não dependa de seu próprio entendimento.", bookAbbrev = "pv", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Isaías", version = "NVT"), chapter = 41, verse = 10, text = "Não tenha medo, pois estou com você; não desanime, pois sou o seu Deus. Eu o fortalecerei e o ajudarei; eu o sustentarei com minha mão direita vitoriosa.", bookAbbrev = "is", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Mateus", version = "NVT"), chapter = 11, verse = 28, text = "Venham a mim todos vocês que estão cansados e sobrecarregados, e eu lhes darei descanso.", bookAbbrev = "mt", versionCode = "NVT"),
        BibleVerseResponse(book = ABibliaBook(name = "Romanos", version = "NVT"), chapter = 8, verse = 28, text = "E sabemos que Deus faz todas as coisas cooperarem para o bem daqueles que o amam e que são chamados de acordo com seu propósito.", bookAbbrev = "rm", versionCode = "NVT")
    )

    suspend fun getRandomBibleVerse(version: String = "NVT"): BibleVerseResponse {
        return try {
            val response = bibleApi.getRandomVerse(version)
            val verseData = response.data
            val cleanText = (verseData.text ?: "").trim()
            val bookName = verseData.book?.name ?: verseData.reference?.split(" ")?.firstOrNull() ?: "Bíblia"
            val chapter = verseData.chapter ?: 1
            val verseNum = verseData.verse ?: 1
            val bookAbbrev = verseData.book?.abbrev ?: "sl"

            if (cleanText.isNotBlank()) {
                BibleVerseResponse(
                    book = ABibliaBook(name = bookName, version = verseData.version ?: version),
                    chapter = chapter,
                    verse = verseNum,
                    text = cleanText,
                    bookAbbrev = bookAbbrev,
                    versionCode = verseData.version ?: version
                )
            } else {
                offlineVerses.random()
            }
        } catch (e: Exception) {
            offlineVerses.random()
        }
    }

    suspend fun getBibleVersions(): List<BibliaVersionItem> {
        return try {
            val response = bibleApi.getVersions()
            if (response.data.isNotEmpty()) response.data else defaultVersionsList
        } catch (e: Exception) {
            defaultVersionsList
        }
    }

    suspend fun getBibleBooks(): List<BibliaBookItem> {
        return try {
            val response = bibleApi.getBooks()
            if (response.data.isNotEmpty()) response.data else defaultBooksList
        } catch (e: Exception) {
            defaultBooksList
        }
    }

    suspend fun getBibleChapter(version: String, bookAbbrev: String, chapter: Int): Result<BibliaChapterData> {
        return try {
            val response = bibleApi.getChapter(version, bookAbbrev, chapter)
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
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

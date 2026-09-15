package com.example

import android.content.SharedPreferences
import com.example.viewmodel.ApartmentViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val map: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any>()
        override fun putString(key: String?, value: String?): SharedPreferences.Editor { value?.let { temp[key!!] = it }; return this }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { values?.let { temp[key!!] = it }; return this }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { temp[key!!] = value; return this }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { temp[key!!] = value; return this }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { temp[key!!] = value; return this }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { temp[key!!] = value; return this }
        override fun remove(key: String?): SharedPreferences.Editor { key?.let { map.remove(it); temp.remove(it) }; return this }
        override fun clear(): SharedPreferences.Editor { map.clear(); temp.clear(); return this }
        override fun commit(): Boolean { map.putAll(temp); return true }
        override fun apply() { map.putAll(temp) }
    }
}

class ApartmentViewModelTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var viewModel: ApartmentViewModel

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        fakePrefs.edit()
            .putFloat("apartment_progress", 0.65f)
            .putString("apartment_date", "Nov 2026")
            .apply()

        viewModel = ApartmentViewModel(fakePrefs, autoRefresh = false)
    }

    @Test
    fun initialState_loadsCorrectlyFromSharedPreferences() {
        val state = viewModel.uiState.value
        assertEquals(0.65f, state.progress, 0.001f)
        assertEquals("Nov 2026", state.expectedDate)
        assertEquals("Nov 2026", state.tempDate)
        assertFalse(state.isPlaying)
        assertFalse(state.isSyncing)
        assertFalse(state.showDateDialog)
    }

    @Test
    fun sliderProgressChange_updatesStateAndPersists() {
        viewModel.onSliderProgressChanged(0.85f)

        val state = viewModel.uiState.value
        assertEquals(0.85f, state.progress, 0.001f)
        assertEquals(0.85f, fakePrefs.getFloat("apartment_progress", 0f), 0.001f)
    }

    @Test
    fun dateDialogFlow_opensUpdatesAndSavesDate() {
        viewModel.onOpenDateDialog()
        assertTrue(viewModel.uiState.value.showDateDialog)

        viewModel.onTempDateChanged("Janeiro 2027")
        assertEquals("Janeiro 2027", viewModel.uiState.value.tempDate)
        // Original expected date should not change yet
        assertEquals("Nov 2026", viewModel.uiState.value.expectedDate)

        viewModel.onSaveExpectedDate()
        assertFalse(viewModel.uiState.value.showDateDialog)
        assertEquals("Janeiro 2027", viewModel.uiState.value.expectedDate)
        assertEquals("Janeiro 2027", fakePrefs.getString("apartment_date", null))
    }

    @Test
    fun dateDialogDismiss_closesWithoutSaving() {
        viewModel.onOpenDateDialog()
        assertTrue(viewModel.uiState.value.showDateDialog)

        viewModel.onTempDateChanged("Marco 2030")
        viewModel.onDismissDateDialog()

        assertFalse(viewModel.uiState.value.showDateDialog)
        assertEquals("Nov 2026", viewModel.uiState.value.expectedDate)
    }

    @Test
    fun playPause_togglesSimulationState() {
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.onPlayPauseClicked()
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.onPlayPauseClicked()
        assertFalse(viewModel.uiState.value.isPlaying)
    }
}

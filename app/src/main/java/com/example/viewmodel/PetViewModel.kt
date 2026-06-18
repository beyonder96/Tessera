package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PetEntity
import com.example.data.PetSex
import com.example.data.PetWeightHistoryEntity
import com.example.data.TesseraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(private val repository: TesseraRepository) : ViewModel() {

    val allPets: StateFlow<List<PetEntity>> = repository.allPets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                val pets = repository.allPets.first()
                if (pets.isEmpty()) {
                    // Seed Marie
                    val marieId = repository.insertPet(
                        PetEntity(
                            name = "Marie",
                            breed = "Golden Retriever",
                            birthDate = System.currentTimeMillis() - 4L * 365 * 24 * 3600 * 1000, // ~4 years ago
                            photoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuC-nfJPLwsDCoZAPRnyoFfm-kb7-YGKFlZERj6GnvfsPRWF04QUeCIX1WhZHhCQLUF4_4wKhJZZ_Pjz7Q86FxU0IpCdNNwQFjU5MHMRrs5lQl4cD1DJTeYqV574VjOoD3xOAusiBniyTZI0VWBYGbhi0NUc57PSZP_6rU7yVmXK85XXkeVqYgYA6Z_-kIeU4PINEX9lZBUfcgobmRvse9pFNN-27sq-IuJzPyavZxsCKJk7pXdnHy5vLrP8xPsnWkGmCE1VhtBiXRw",
                            rga = "1234567",
                            microchip = "123456789012345",
                            sex = PetSex.FEMEA,
                            isCastrated = true,
                            lastV4VaccineDate = System.currentTimeMillis() - 10L * 24 * 3600 * 1000, // 10 days ago (Valid)
                            lastRaivaVaccineDate = System.currentTimeMillis() - 12L * 24 * 3600 * 1000, // 12 days ago (Valid)
                            lastAntipulgasDate = System.currentTimeMillis() - 15L * 24 * 3600 * 1000, // 15 days ago (Valid)
                            lastVermifugoDate = System.currentTimeMillis() - 20L * 24 * 3600 * 1000, // 20 days ago (Valid)
                            lastConsultaDate = System.currentTimeMillis() - 30L * 24 * 3600 * 1000, // 30 days ago (Valid)
                            notes = "Nenhuma Alergia"
                        )
                    ).toInt()

                    // Seed Churchill
                    val churchillId = repository.insertPet(
                        PetEntity(
                            name = "Churchill",
                            breed = "Buldogue Francês",
                            birthDate = System.currentTimeMillis() - 2L * 365 * 24 * 3600 * 1000, // ~2 years ago
                            photoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBwE9mkw-3Q01XMMJNCBsgQYL4vceyVCaIpNVZLlNpqFxq56lIYShGa2Y2Ayd2cWilSsA1Sh7N8EhEeP0UmPiTX1Jxrt5v-bwMd7go8hp_GMPk-ujDr-jURbRlfoI92fsudTavmulIvwmwVFRX5oy5pq4tLAm0ouBfSkwAy2knOwtJPymqKdo2ZhqgGc_eH8IPceKSvI0ugGLLmnBGc5BIGL9mwFb4JUYULZY9PQ4BuBWZGmIU3n7lN0G86yPzXd3Zi58hh3NsMgjw",
                            rga = "7654321",
                            microchip = "987654321098765",
                            sex = PetSex.MACHO,
                            isCastrated = true,
                            lastV4VaccineDate = System.currentTimeMillis() - 400L * 24 * 3600 * 1000, // 400 days ago (Expired!)
                            lastRaivaVaccineDate = System.currentTimeMillis() - 15L * 24 * 3600 * 1000, // 15 days ago (Valid)
                            lastAntipulgasDate = System.currentTimeMillis() - 120L * 24 * 3600 * 1000, // 120 days ago (Expired!)
                            lastVermifugoDate = System.currentTimeMillis() - 200L * 24 * 3600 * 1000, // 200 days ago (Expired!)
                            lastConsultaDate = System.currentTimeMillis() - 500L * 24 * 3600 * 1000, // 500 days ago (Expired!)
                            notes = "Dieta de Controle"
                        )
                    ).toInt()

                    // Seed Weight history for Marie
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = marieId, date = System.currentTimeMillis() - 90L * 24 * 3600 * 1000, weight = 26.5))
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = marieId, date = System.currentTimeMillis() - 60L * 24 * 3600 * 1000, weight = 27.2))
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = marieId, date = System.currentTimeMillis() - 30L * 24 * 3600 * 1000, weight = 28.0))
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = marieId, date = System.currentTimeMillis(), weight = 28.5))

                    // Seed Weight history for Churchill
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = churchillId, date = System.currentTimeMillis() - 90L * 24 * 3600 * 1000, weight = 11.0))
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = churchillId, date = System.currentTimeMillis() - 60L * 24 * 3600 * 1000, weight = 11.5))
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = churchillId, date = System.currentTimeMillis() - 30L * 24 * 3600 * 1000, weight = 11.8))
                    repository.insertWeightHistory(PetWeightHistoryEntity(petId = churchillId, date = System.currentTimeMillis(), weight = 12.2))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun validateRga(rga: String): Boolean {
        return rga.length == 7 && rga.all { it.isDigit() }
    }

    fun validateMicrochip(microchip: String): Boolean {
        return microchip.length == 15 && microchip.all { it.isDigit() }
    }

    fun isVaccineExpired(lastAppliedDate: Long?): Boolean {
        if (lastAppliedDate == null) return true
        val diffMillis = System.currentTimeMillis() - lastAppliedDate
        val diffDays = diffMillis / (24L * 60 * 60 * 1000)
        return diffDays >= 365
    }

    fun isAntipulgasExpired(lastAppliedDate: Long?): Boolean {
        if (lastAppliedDate == null) return true
        val diffMillis = System.currentTimeMillis() - lastAppliedDate
        val diffDays = diffMillis / (24L * 60 * 60 * 1000)
        return diffDays >= 90 // 3 months
    }

    fun isVermifugoExpired(lastAppliedDate: Long?): Boolean {
        if (lastAppliedDate == null) return true
        val diffMillis = System.currentTimeMillis() - lastAppliedDate
        val diffDays = diffMillis / (24L * 60 * 60 * 1000)
        return diffDays >= 180 // 6 months
    }

    fun isConsultaExpired(lastAppliedDate: Long?): Boolean {
        if (lastAppliedDate == null) return true
        val diffMillis = System.currentTimeMillis() - lastAppliedDate
        val diffDays = diffMillis / (24L * 60 * 60 * 1000)
        return diffDays >= 365 // 1 year
    }

    fun insertPet(pet: PetEntity) {
        viewModelScope.launch {
            if (pet.id == 0) {
                repository.insertPet(pet)
            } else {
                repository.updatePet(pet)
            }
        }
    }

    fun insertPetWithInitialWeight(pet: PetEntity, weight: Double, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            val newId = repository.insertPet(pet).toInt()
            if (weight > 0.0) {
                repository.insertWeightHistory(PetWeightHistoryEntity(petId = newId, date = System.currentTimeMillis(), weight = weight))
            }
            onCompleted()
        }
    }

    fun deletePet(pet: PetEntity) {
        viewModelScope.launch {
            repository.deletePet(pet)
        }
    }

    fun getWeightHistory(petId: Int): Flow<List<PetWeightHistoryEntity>> {
        return repository.getWeightHistoryForPet(petId)
    }

    fun addWeightHistoryRecord(petId: Int, date: Long, weight: Double) {
        viewModelScope.launch {
            repository.insertWeightHistory(PetWeightHistoryEntity(petId = petId, date = date, weight = weight))
        }
    }

    fun deleteWeightHistoryRecord(record: PetWeightHistoryEntity) {
        viewModelScope.launch {
            repository.deleteWeightHistory(record)
        }
    }
}

class PetViewModelFactory(private val repository: TesseraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

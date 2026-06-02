package com.tessera.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tessera.app.data.local.dao.TesseraDao
import com.tessera.app.data.local.entity.FinanceEntity
import com.tessera.app.data.local.entity.PetRoutineEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val dao: TesseraDao) : ViewModel() {

    // Calcula o Patrimônio Total em tempo real
    val totalNetWorth: StateFlow<Double> = dao.getAllFinances()
        .map { finances -> 
            finances.sumOf { if (it.type == "INCOME") it.amount else -it.amount } 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = dao.getAllFinances()
        .map { finances -> finances.filter { it.type == "INCOME" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = dao.getAllFinances()
        .map { finances -> finances.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    // Escuta a rotina dos pets
    val petRoutines: StateFlow<List<PetRoutineEntity>> = dao.getPetRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // Adicionar transação de teste
    fun addTestFinance() {
        viewModelScope.launch {
            dao.insertFinance(
                FinanceEntity(description = "Salário", amount = 5000.0, type = "INCOME", timestamp = System.currentTimeMillis())
            )
        }
    }

    // Fábrica para passar o DAO ao ViewModel
    class Factory(private val dao: TesseraDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(dao) as T
            }
            throw IllegalArgumentException("ViewModel desconhecido")
        }
    }
}

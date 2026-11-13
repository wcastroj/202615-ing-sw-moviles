package com.example.vinyls_application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Reemplaza Dispatchers.Main (el que usa viewModelScope) por un TestDispatcher durante cada prueba.
 *
 * Se usa UnconfinedTestDispatcher: las corrutinas corren de inmediato en el hilo de la prueba
 * hasta que suspenden. Así, al crear el ViewModel la petición ya queda encolada, y al entregar la
 * respuesta de Volley la corrutina se reanuda en el acto y actualiza los LiveData.
 *
 * El mismo [testDispatcher] se inyecta en NetworkServiceAdapter para que el parseo (y el
 * ordenamiento de PerformerRepository, que usa el dispatcher del adapter) tampoco salte a un hilo real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

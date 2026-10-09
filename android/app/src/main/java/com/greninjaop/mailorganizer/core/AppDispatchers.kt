package com.greninjaop.mailorganizer.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Injectable dispatchers so IO-bound work (database, network, parsing) is
 * testable and never accidentally runs on Main.
 */
data class AppDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val main: CoroutineDispatcher = Dispatchers.Main,
)

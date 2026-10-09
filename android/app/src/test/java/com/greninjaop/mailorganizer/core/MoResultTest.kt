package com.greninjaop.mailorganizer

import org.junit.Assert.assertEquals
import org.junit.Test

class MoResultTest {

    @Test
    fun `map transforms Success value`() {
        val result: MoResult<Int> = MoResult.Success(2)
        assertEquals(MoResult.Success(4), result.map { it * 2 })
    }

    @Test
    fun `map passes Failure through untouched`() {
        val error = MoError.Network("offline")
        val result: MoResult<Int> = MoResult.Failure(error)
        assertEquals(MoResult.Failure(error), result.map { it * 2 })
    }

    @Test
    fun `fold routes Success to onSuccess`() {
        val result: MoResult<Int> = MoResult.Success(1)
        assertEquals("ok:1", result.fold({ "ok:$it" }, { "err" }))
    }

    @Test
    fun `fold routes Failure to onFailure`() {
        val result: MoResult<Int> = MoResult.Failure(MoError.Unexpected())
        assertEquals("err", result.fold({ "ok:$it" }, { "err" }))
    }
}

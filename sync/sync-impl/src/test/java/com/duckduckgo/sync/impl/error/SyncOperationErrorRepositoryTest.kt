/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.sync.impl.error

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.common.utils.formatters.time.DatabaseDateFormatter
import com.duckduckgo.sync.impl.pixels.SyncPixelParameters
import com.duckduckgo.sync.store.SyncDatabase
import com.duckduckgo.sync.store.model.GENERIC_FEATURE
import com.duckduckgo.sync.store.model.SyncOperationErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncOperationErrorRepositoryTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, SyncDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    private val dao = db.syncOperationErrorsDao()

    private val testee = RealSyncOperationErrorRepository(dao)

    @After
    fun after() {
        db.close()
    }

    @Test
    fun whenOperationErrorAddedAndNotPresentThenNewEntryAdded() {
        val errorType = SyncOperationErrorType.DATA_ENCRYPT
        val date = DatabaseDateFormatter.getUtcIsoLocalDate()

        testee.addError(errorType)

        val error = dao.featureErrorByDate(GENERIC_FEATURE, errorType.name, date)

        Assert.assertTrue(error != null)
        Assert.assertTrue(error!!.count == 1)
    }

    @Test
    fun whenOperationErrorAddedAndPresentThenCountUpdated() {
        val errorType = SyncOperationErrorType.DATA_ENCRYPT
        val date = DatabaseDateFormatter.getUtcIsoLocalDate()

        testee.addError(errorType)
        testee.addError(errorType)

        val error = dao.featureErrorByDate(GENERIC_FEATURE, errorType.name, date)

        Assert.assertTrue(error != null)
        Assert.assertTrue(error!!.count == 2)
    }

    @Test
    fun whenNoErrorsStoredThenGettingErrorsReturnsEmpty() {
        val date = DatabaseDateFormatter.getUtcIsoLocalDate()

        val errors = testee.getErrorsByDate(date)

        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun whenNoErrorsStoredFromYesterdayThenGettingErrorsFromYesterdayReturnsEmpty() {
        val errorType = SyncOperationErrorType.DATA_ENCRYPT
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        testee.addError(errorType)

        val errors = testee.getErrorsByDate(yesterday)

        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun whenErrorsStoredThenGettingErrorsReturnsData() {
        val errorType = SyncOperationErrorType.DATA_ENCRYPT
        val today = DatabaseDateFormatter.getUtcIsoLocalDate()

        testee.addError(errorType)

        val errors = testee.getErrorsByDate(today)
        Assert.assertTrue(errors.isNotEmpty())

        val error = errors.first()
        Assert.assertTrue(error.name == SyncPixelParameters.DATA_ENCRYPT_ERROR)
        Assert.assertTrue(error.count == "1")
    }
}

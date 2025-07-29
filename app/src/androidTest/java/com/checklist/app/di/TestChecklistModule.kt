package com.checklist.app.di

import android.content.Context
import com.checklist.app.presentation.utils.HapticManager
import com.checklist.app.presentation.utils.SoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Test module that provides test doubles for HapticManager and SoundManager
 * to allow verification of method calls during tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestChecklistModule {
    
    @Provides
    @Singleton
    fun provideTestHapticManager(@ApplicationContext context: Context): HapticManager {
        return TestHapticManager(context)
    }
    
    @Provides
    @Singleton
    fun provideTestSoundManager(@ApplicationContext context: Context): SoundManager {
        return TestSoundManager(context)
    }
}

/**
 * Test implementation of HapticManager that tracks method calls
 */
class TestHapticManager(context: Context) : HapticManager(context) {
    var singleBuzzCount = 0
        private set
    var tripleBuzzCount = 0
        private set
    
    override fun singleBuzz() {
        singleBuzzCount++
        // No actual vibration in tests
    }
    
    override fun tripleBuzz() {
        tripleBuzzCount++
        // No actual vibration in tests
    }
    
    fun reset() {
        singleBuzzCount = 0
        tripleBuzzCount = 0
    }
}

/**
 * Test implementation of SoundManager that tracks method calls
 */
class TestSoundManager(context: Context) : SoundManager(context) {
    var playCompletionChimeCount = 0
        private set
    
    override fun playCompletionChime() {
        playCompletionChimeCount++
        // No actual sound in tests
    }
    
    fun reset() {
        playCompletionChimeCount = 0
    }
}
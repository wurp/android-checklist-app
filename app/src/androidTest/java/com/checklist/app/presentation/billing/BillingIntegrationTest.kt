package com.checklist.app.presentation.billing

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.preferences.PreferencesManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Integration test for the "Throw Dev a Bone" billing feature.
 * 
 * Note: This test verifies UI behavior only. Actual billing transactions
 * cannot be tested in instrumented tests as they require:
 * 1. Installation through Google Play Store
 * 2. Valid Google Play account with payment method
 * 3. Product configuration in Google Play Console
 * 
 * When sideloading APKs, the billing flow will fail silently as the
 * Google Play Billing service cannot connect.
 */
@HiltAndroidTest
class BillingIntegrationTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var templateRepository: TemplateRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Reset purchase state
        runBlocking {
            preferencesManager.setHasPurchased(false)
        }
    }
    
    @Test
    fun testThrowDevABoneButtonVisibility() {
        // The heart icon should be visible in the top app bar
        composeTestRule.onNodeWithContentDescription("Throw Dev a Bone").assertIsDisplayed()
        
        // The icon should be unfilled (FavoriteBorder) when not purchased
        composeTestRule.onNodeWithContentDescription("Throw Dev a Bone").assertExists()
    }
    
    @Test
    fun testThrowDevABoneButtonClickable() {
        // The button should be clickable
        composeTestRule.onNodeWithContentDescription("Throw Dev a Bone").assertHasClickAction()
        
        // Click the button
        composeTestRule.onNodeWithContentDescription("Throw Dev a Bone").performClick()
        
        // In a real environment with Google Play, this would launch the billing flow
        // In sideloaded APK, nothing visible happens (billing service not connected)
        
        // Button should still be there
        composeTestRule.onNodeWithContentDescription("Throw Dev a Bone").assertIsDisplayed()
    }
    
    @Test
    fun testPurchasedStateUI() {
        // Simulate a purchased state
        runBlocking {
            preferencesManager.setHasPurchased(true)
        }
        
        // Wait for UI to update
        composeTestRule.waitForIdle()
        
        // The icon should now show "Thank you!" with filled heart
        composeTestRule.onNodeWithContentDescription("Thank you!").assertIsDisplayed()
    }
    
    @Test
    fun testPurchaseStatePersistence() {
        // Initially not purchased
        composeTestRule.onNodeWithContentDescription("Throw Dev a Bone").assertExists()
        
        // Simulate purchase
        runBlocking {
            preferencesManager.setHasPurchased(true)
        }
        
        // Restart the activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Should show purchased state
        composeTestRule.onNodeWithContentDescription("Thank you!").assertIsDisplayed()
    }
}
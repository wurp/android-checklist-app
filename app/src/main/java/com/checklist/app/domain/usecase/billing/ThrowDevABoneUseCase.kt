package com.checklist.app.domain.usecase.billing

import android.app.Activity
import com.checklist.app.data.repository.BillingRepository
import javax.inject.Inject

class ThrowDevABoneUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    val hasPurchased = billingRepository.hasPurchased
    
    suspend operator fun invoke(activity: Activity) {
        billingRepository.launchBillingFlow(activity)
    }
}
# Testing In-App Purchases Without Production Deployment

## Overview
Google Play provides several methods to test in-app purchases before releasing to production. Here's a comprehensive guide.

## Method 1: Internal Testing Track (Recommended)

### Steps:
1. **Build a signed release APK or AAB**
   ```bash
   ./gradlew bundleRelease
   ```

2. **Upload to Google Play Console**
   - Go to Play Console → Your App → Testing → Internal testing
   - Create a new release
   - Upload your AAB/APK
   - Add a release name and notes

3. **Configure In-App Product**
   - Go to Monetization → In-app products
   - Create new product with ID: `throw_dev_a_bone`
   - Set price to $0.99
   - Activate the product

4. **Add Test Users**
   - In Internal testing → Testers tab
   - Create an email list with your test accounts
   - These accounts will see "Test Card" payment options

5. **Install via Testing Link**
   - Copy the opt-in URL from Internal testing
   - Open on your test device
   - Accept the invitation
   - Install from Play Store

### Testing Payment Methods:
- Test accounts will see special test payment methods:
  - "Test card, always approves"
  - "Test card, always declines"
  - "Test card, slow processing"

## Method 2: License Testing (For Development)

### Setup:
1. **Add License Testers in Play Console**
   - Go to Settings → License testing
   - Add Gmail accounts as license testers
   - These accounts can make test purchases in ANY version of your app

2. **Install Your Debug APK**
   - The app must be signed with a certificate uploaded to Play Console
   - Can be debug or release signing

3. **Test Purchases**
   - License testers will see "(Test)" next to prices
   - No charges are made to their payment methods

## Method 3: Local Testing with Static Responses

For basic UI testing without server connection:

### Create a Test Implementation:
```kotlin
// TestBillingRepository.kt
@Singleton
class TestBillingRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) : BillingRepository {
    
    override val hasPurchased = preferencesManager.hasPurchased
    override val isConnected = MutableStateFlow(true)
    
    override suspend fun launchBillingFlow(activity: Activity) {
        // Simulate successful purchase after delay
        delay(2000)
        preferencesManager.setHasPurchased(true)
        
        // Or show a test dialog
        AlertDialog.Builder(activity)
            .setTitle("Test Purchase")
            .setMessage("Simulate purchase success?")
            .setPositiveButton("Success") { _, _ ->
                runBlocking { preferencesManager.setHasPurchased(true) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
```

### Inject Test Implementation:
```kotlin
// TestBillingModule.kt
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BillingModule::class]
)
abstract class TestBillingModule {
    @Binds
    abstract fun bindBillingRepository(
        testBillingRepository: TestBillingRepository
    ): BillingRepository
}
```

## Method 4: Google Play Points Test Cards

For more realistic testing:

1. **Enable test cards in Play Console**
   - Go to Setup → License testing
   - Enable "Test card payments"

2. **Use specific test card numbers**:
   - 4111 1111 1111 1111 - Always approves
   - 4000 0000 0000 0002 - Always declines
   - 4000 0000 0000 0010 - Insufficient funds

## Quick Testing Checklist

### For Internal Testing:
- [ ] Build signed release APK/AAB
- [ ] Upload to Internal testing track
- [ ] Create in-app product with ID `throw_dev_a_bone`
- [ ] Add test email accounts
- [ ] Install via testing opt-in link
- [ ] Test with "Test card, always approves"

### For License Testing:
- [ ] Add Gmail accounts as license testers
- [ ] Install debug APK signed with Play Console certificate
- [ ] Look for "(Test)" label on prices
- [ ] Make test purchases without charges

## Important Notes

1. **Version Code**: Uploaded version must have higher versionCode than production
2. **Package Name**: Must match exactly (`com.eschaton.checklists`)
3. **Signing**: Must be signed with same certificate as Play Console
4. **Product State**: In-app products must be "Active"
5. **Processing Time**: New products/testers may take 15-30 minutes to propagate

## Troubleshooting

### "Item not found" error:
- Ensure product ID matches exactly
- Wait 30 minutes for propagation
- Check product is activated

### Billing service not connected:
- Ensure Play Store is logged in
- Clear Play Store cache
- Check device has internet connection

### No test payment options:
- Verify account is added as tester
- Ensure installed via testing link, not sideloaded
- Check Play Store account matches tester email

## Testing Timeline

1. **Immediate**: Local testing with mock implementation
2. **Same day**: License testing with debug builds
3. **Next day**: Internal testing track with real flow
4. **Before release**: Closed/Open testing with broader audience
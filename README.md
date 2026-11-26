# Puzzles Kids Premium Unlock (v02)

Target: com.gamesforkids.animals.puzzles.jigsaw (Unity IL2CPP + Play Billing)

What this does:
- Forces Purchase.getPurchaseState() → PURCHASED (1)
- Forces Purchase.isAcknowledged() → true
- Lets Unity IAP complete IAPRemoveAdsComplete / IAPFullVersionComplete
  and set isFullVersion + Remove Ads flags

Files:
  app/morphe/patches/puzzleskids/misc/premium/UnlockPremiumPatch.kt
  docs/APPLY.md
  docs/NATIVE_IL2CPP.md

Test after install:
  Open shop / Remove Ads / Full Version — should show owned or unlock immediately
  after a restore/query. Restart app once so PlayerPrefs persist.

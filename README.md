# CampKit

CampKit is an Android camping trip planner, gear locker inventory, and pack weight calculator built with Kotlin, Jetpack Compose, Material 3, and Firebase Cloud Firestore.

## Releases

### Latest Release: **v2.0.1**

- **Download APK (Repository Artifact):** [CampKit-v2.0.1.apk](releases/CampKit-v2.0.1.apk)
- **Direct Raw Download:** [CampKit-v2.0.1.apk (GitHub Raw)](https://github.com/gianrufin/CampKit/raw/main/releases/CampKit-v2.0.1.apk)
- **GitHub Release Page:** [v2.0.1 Release](https://github.com/gianrufin/CampKit/releases/tag/v2.0.1)

#### Changelog (`v2.0.1`)
- Set semantic release versioning starting at `2.0.1` (`versionCode = 20001`).
- Added **Settings** screen with account management, weight unit preference (`Metric g/kg` or `Imperial oz/lb`), and automatic **Check for Update** against `gianrufin/CampKit` (`releases/latest`, `version.json`, and tags).
- Bundled signed debug APK in `releases/CampKit-v2.0.1.apk` and `version.json` metadata.

## Features

- **Camp Trips & Packing Checklists**: Plan trips (Backpacking, Car Camping, Bikepacking, Day Hike), load curated starter kits, and track packed progress with real-time cloud sync.
- **Gear Locker**: Maintain a searchable, category-filtered inventory of camping equipment with weights, quantities, and core essential markers, then import items directly into any trip.
- **Weight Lab**: Inspect base weight vs. worn weight, target pack weight progress, category distribution charts, and heaviest items in Metric (`g / kg`) or Imperial (`oz / lb`).
- **Settings & Update Checker**: Automatically checks `gianrufin/CampKit` for newer `X.Y.Z` versions and opens the release/APK download link when an update is available.
- **Google Sign-In & Zero-Trust Firestore**: Authenticated via Jetpack Credential Manager (`GetSignInWithGoogleOption`) with per-user isolated Cloud Firestore collections.
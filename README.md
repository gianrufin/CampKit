# CampKit

CampKit is an Android camping trip planner, gear locker inventory, and pack weight calculator built with Kotlin, Jetpack Compose, Material 3, and Firebase Cloud Firestore.

## Features

- **Camp Trips & Packing Checklists**: Plan trips (Backpacking, Car Camping, Bikepacking, Day Hike), load curated starter kits, and track packed progress with real-time cloud sync.
- **Gear Locker**: Maintain a searchable, category-filtered inventory of camping equipment with weights, quantities, and core essential markers, then import items directly into any trip.
- **Weight Lab**: Inspect base weight vs. worn weight, target pack weight progress, category distribution charts, and heaviest items in Metric (`g / kg`) or Imperial (`oz / lb`).
- **Google Sign-In & Zero-Trust Firestore**: Authenticated via Jetpack Credential Manager (`GetSignInWithGoogleOption`) with per-user isolated Cloud Firestore collections.
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_camp_123";
const BOB_UID = "bob_camp_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write gear, trips, or packing items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("gear").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("trips").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_1").set({
      name: "MSR PocketRocket",
      category: "COOKING",
      weightGrams: 73,
      quantityOwned: 1,
      essential: true,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Authenticated owner: valid Kotlin gear create, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_1").set({
      name: "Big Agnes Copper Spur HV UL2",
      category: "SHELTER",
      weightGrams: 1190,
      quantityOwned: 1,
      essential: true,
      notes: "2-person backpacking tent",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update omitting immutable createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_1").update({
      weightGrams: 1210,
      essential: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List query matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").get()
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId 'id' leak
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_2").set({
      id: "gear_2",
      name: "Ghost Tent",
      category: "SHELTER",
      weightGrams: 500,
      quantityOwned: 1,
      essential: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid category & future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_3").set({
      name: "Mystery Item",
      category: "INVALID_CAT",
      weightGrams: 100,
      quantityOwned: 1,
      essential: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_4").set({
      name: "Future Stove",
      category: "COOKING",
      weightGrams: 100,
      quantityOwned: 1,
      essential: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gear").doc("gear_1").delete()
  );
});

test("Authenticated owner: trip and packing items batch creation and updates succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Atomic batch creating trip + starter packing item (mirrors CampRepository starter kit batch)
  const batch = aliceDb.batch();
  const tripRef = aliceDb.collection("users").doc(ALICE_UID).collection("trips").doc("trip_1");
  const itemRef = tripRef.collection("items").doc("item_1");

  batch.set(tripRef, {
    title: "Desolation Wilderness Loop",
    location: "Lake Tahoe, CA",
    tripType: "BACKPACKING",
    nights: 2,
    startDateLabel: "Nov 28 - Nov 30",
    archived: false,
    createdAt: created,
    updatedAt: created,
  });

  batch.set(itemRef, {
    name: "2P Ultralight Tent",
    category: "SHELTER",
    weightGrams: 1190,
    quantity: 1,
    packed: false,
    worn: false,
    gearId: "gear_1",
    createdAt: created,
    updatedAt: created,
  });

  await assertSucceeds(batch.commit());

  // Toggle packed state
  await assertSucceeds(
    itemRef.update({
      packed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Orphaned packing item under non-existent trip must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trips")
      .doc("nonexistent_trip")
      .collection("items")
      .doc("orphan_1")
      .set({
        name: "Orphan Headlamp",
        category: "SAFETY",
        weightGrams: 85,
        quantity: 1,
        packed: false,
        worn: false,
        createdAt: created,
        updatedAt: created,
      })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's trips, gear, or packing items", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const past = new Date(Date.now() - 10000);
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).collection("trips").doc("alice_trip").set({
      title: "Alice's Secret Camp",
      location: "Sierra Nevada",
      tripType: "BACKPACKING",
      nights: 3,
      startDateLabel: "Dec 1 - Dec 4",
      archived: false,
      createdAt: past,
      updatedAt: past,
    });
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trips")
      .doc("alice_trip")
      .collection("items")
      .doc("alice_item")
      .set({
        name: "Down Quilt",
        category: "SLEEP",
        weightGrams: 620,
        quantity: 1,
        packed: true,
        worn: false,
        createdAt: past,
        updatedAt: past,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("trips").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("trips").doc("alice_trip").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trips")
      .doc("alice_trip")
      .update({ title: "Hijacked" })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trips")
      .doc("alice_trip")
      .collection("items")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trips")
      .doc("alice_trip")
      .collection("items")
      .doc("alice_item")
      .delete()
  );
});

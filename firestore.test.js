const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCareerPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    characterName: "Chinedu Okafor",
    gender: "MALE",
    schoolName: "UNILAG (Akoka)",
    archetype: "STRUGGLE",
    stage: "UNIVERSITY",
    levelLabel: "100 Level",
    day: 1,
    totalDays: 1,
    money: 12000,
    energy: 85,
    hunger: 80,
    academicStat: 4.5,
    socialStat: 50,
    riskHeat: 0,
    careerStatus: "ACTIVE",
    carryoverCount: 0,
    strikeDaysLeft: 0,
    accommodation: "HOSTEL",
    affiliation: "NONE",
    itemsOwned: [],
    completedStages: [],
    lastLogSummary: "Arrived at Moremi/Jaja Hall with one Ghana-Must-Go bag.",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validProfilePayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    displayName: "Chinedu",
    bestCareerTitle: "100L Fresher",
    highestNetWorth: 12000,
    highestStage: "UNIVERSITY",
    totalGraduations: 0,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validLeaderboardPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    characterName: "Chinedu Okafor",
    schoolName: "UNILAG (Akoka)",
    stageReached: "UNIVERSITY",
    careerTitle: "First Class Graduate",
    score: 8450,
    netWorth: 245000,
    cgpaDisplay: "4.72 / 5.00",
    visibility: "PUBLIC",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

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

test("Unauthenticated access is denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("careers").get()
  );
  await assertFails(
    unauthDb
      .collection("leaderboard")
      .where("visibility", "==", "PUBLIC")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID))
  );
});

test("User profile: owner create/update succeeds; cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID))
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bestCareerTitle: "Professor Emeritus",
      highestNetWorth: 5000000,
      highestStage: "PROFESSOR",
      totalGraduations: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read or overwrite Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" })
  );
});

test("Careers subcollection: owner CRUD, terminal lock, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceCareers = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("careers");

  // Empty list succeeds without existing() error
  await assertSucceeds(aliceCareers.get());

  // Create valid career (no @DocumentId `id` field)
  await assertSucceeds(
    aliceCareers.doc("run_1").set(validCareerPayload(ALICE_UID))
  );

  // Reject payload containing `id` field
  await assertFails(
    aliceCareers
      .doc("run_bad_id")
      .set(validCareerPayload(ALICE_UID, { id: "run_bad_id" }))
  );

  // Partial update while ACTIVE succeeds
  await assertSucceeds(
    aliceCareers.doc("run_1").update({
      day: 2,
      totalDays: 2,
      money: 15500,
      energy: 70,
      hunger: 65,
      academicStat: 4.65,
      itemsOwned: ["Powerbank"],
      lastLogSummary: "Survived 8am GST 101 lecture.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt cannot be modified
  await assertFails(
    aliceCareers.doc("run_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Transition to terminal state GRADUATED
  await assertSucceeds(
    aliceCareers.doc("run_1").update({
      careerStatus: "GRADUATED",
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Subsequent updates to terminal career are blocked
  await assertFails(
    aliceCareers.doc("run_1").update({
      money: 999999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot list, get, or create inside Alice's careers
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("careers").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("careers")
      .doc("run_1")
      .get()
  );
});

test("Leaderboard: filtered public query succeeds; unfiltered query & spoofing fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates her own entry under doc(ALICE_UID)
  await assertSucceeds(
    aliceDb
      .collection("leaderboard")
      .doc(ALICE_UID)
      .set(validLeaderboardPayload(ALICE_UID))
  );

  // Alice cannot write to Bob's leaderboard slot or spoof userId
  await assertFails(
    aliceDb
      .collection("leaderboard")
      .doc(BOB_UID)
      .set(validLeaderboardPayload(ALICE_UID))
  );

  // Bob CAN read leaderboard using the aligned visibility query
  await assertSucceeds(
    bobDb
      .collection("leaderboard")
      .where("visibility", "==", "PUBLIC")
      .limit(50)
      .get()
  );

  // Unfiltered query on leaderboard is rejected
  await assertFails(bobDb.collection("leaderboard").get());

  // Bob cannot update Alice's leaderboard entry
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      score: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";
const ADMIN_EMAIL = "karimwajid0101@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function aliceContext() {
  return testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  });
}

function bobContext() {
  return testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  });
}

async function submitViralBatch(db, uid, subId, tid, phone = "03001234567") {
  const now = new Date(Date.now() - 1000);
  const batch = db.batch();
  batch.set(db.collection("viral_submissions").doc(subId), {
    userId: uid,
    authorName: "Alice",
    title: "Test Viral Offer",
    referralLink: "https://j10app.win/?dl=abxtwr",
    promoCode: "ABX",
    bonusOffer: "500 PKR Bonus",
    description: "Watch video and join!",
    videoUrl: "https://example.com/promo.mp4",
    amountPkr: 100,
    status: "PENDING_VERIFICATION",
    viralShares: 0,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(db.collection("deposit_proofs").doc(subId), {
    userId: uid,
    submissionId: subId,
    easypaisaSenderPhone: phone,
    easypaisaTid: tid,
    amountPkr: 100,
    status: "PENDING_VERIFICATION",
    createdAt: now,
    updatedAt: now,
  });
  batch.set(db.collection("tid_registry").doc(tid), {
    userId: uid,
    submissionId: subId,
    createdAt: now,
  });
  return batch.commit();
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("app_config").doc("easypaisa").get());
  await assertFails(
    unauthDb.collection("viral_submissions").where("status", "==", "APPROVED").get()
  );
  await assertFails(unauthDb.collection("deposit_proofs").get());
});

test("Only verified admin can configure Easypaisa account; spoofed email is blocked", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = aliceContext().firestore();
  const spoofDb = spoofedAdminContext().firestore();
  const adminDb = adminContext().firestore();

  const payload = {
    accountTitle: "Wajid Karim",
    accountNumber: "03001234567",
    feePkr: 100,
    updatedBy: ADMIN_UID,
    updatedAt: now,
  };

  // Regular user and unverified email spoof must fail
  await assertFails(
    aliceDb.collection("app_config").doc("easypaisa").set({
      ...payload,
      updatedBy: ALICE_UID,
    })
  );
  await assertFails(
    spoofDb.collection("app_config").doc("easypaisa").set({
      ...payload,
      updatedBy: "spoof_uid",
    })
  );

  // Verified admin succeeds, and signed-in users can read config via get
  await assertSucceeds(adminDb.collection("app_config").doc("easypaisa").set(payload));
  await assertSucceeds(aliceDb.collection("app_config").doc("easypaisa").get());
  await assertFails(aliceDb.collection("app_config").get());

  // Verified admin can also update app_share link; non-admin cannot
  const sharePayload = {
    downloadUrl: "https://example.com/refreels.apk",
    shareMessage: "Install RefReels!",
    updatedBy: ADMIN_UID,
    updatedAt: now,
  };
  await assertFails(
    aliceDb.collection("app_config").doc("app_share").set({
      ...sharePayload,
      updatedBy: ALICE_UID,
    })
  );
  await assertSucceeds(adminDb.collection("app_config").doc("app_share").set(sharePayload));
  await assertSucceeds(aliceDb.collection("app_config").doc("app_share").get());
});

test("User atomic batch submission succeeds and enforces TID uniqueness & no self-approval", async () => {
  const aliceDb = aliceContext().firestore();
  const bobDb = bobContext().firestore();
  const now = new Date(Date.now() - 1000);

  // Valid atomic submission succeeds
  await assertSucceeds(submitViralBatch(aliceDb, ALICE_UID, "sub_1", "EP12345678"));

  // Duplicate TID by Bob must fail
  await assertFails(submitViralBatch(bobDb, BOB_UID, "sub_2", "EP12345678"));

  // Self-approval on create must fail
  const selfApproveBatch = aliceDb.batch();
  selfApproveBatch.set(aliceDb.collection("viral_submissions").doc("sub_3"), {
    userId: ALICE_UID,
    authorName: "Alice",
    title: "Self Approved Hack",
    referralLink: "https://j10app.win/?dl=abxtwr",
    bonusOffer: "500 PKR",
    amountPkr: 100,
    status: "APPROVED",
    viralShares: 0,
    createdAt: now,
    updatedAt: now,
  });
  selfApproveBatch.set(aliceDb.collection("deposit_proofs").doc("sub_3"), {
    userId: ALICE_UID,
    submissionId: "sub_3",
    easypaisaSenderPhone: "03001234567",
    easypaisaTid: "EP99999999",
    amountPkr: 100,
    status: "PENDING_VERIFICATION",
    createdAt: now,
    updatedAt: now,
  });
  selfApproveBatch.set(aliceDb.collection("tid_registry").doc("EP99999999"), {
    userId: ALICE_UID,
    submissionId: "sub_3",
    createdAt: now,
  });
  await assertFails(selfApproveBatch.commit());

  // Creating viral_submissions alone without deposit_proofs must fail
  await assertFails(
    aliceDb.collection("viral_submissions").doc("sub_orphan").set({
      userId: ALICE_UID,
      authorName: "Alice",
      title: "No Payment",
      referralLink: "https://j10app.win/?dl=abxtwr",
      bonusOffer: "500 PKR",
      amountPkr: 100,
      status: "PENDING_VERIFICATION",
      viralShares: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("PII isolation: Bob cannot read Alice deposit_proofs or pending submission; unfiltered list fails", async () => {
  const aliceDb = aliceContext().firestore();
  const bobDb = bobContext().firestore();

  await assertSucceeds(submitViralBatch(aliceDb, ALICE_UID, "sub_pii", "EP55667788"));

  // Bob cannot get Alice's deposit proof or pending submission
  await assertFails(bobDb.collection("deposit_proofs").doc("sub_pii").get());
  await assertFails(bobDb.collection("viral_submissions").doc("sub_pii").get());
  await assertFails(bobDb.collection("deposit_proofs").get());
  await assertFails(bobDb.collection("viral_submissions").get());

  // Alice can query her own proofs & submissions
  await assertSucceeds(
    aliceDb.collection("deposit_proofs").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("viral_submissions").where("userId", "==", ALICE_UID).get()
  );
});

test("Admin approves submission atomically; approved viral reel becomes visible and shareable", async () => {
  const aliceDb = aliceContext().firestore();
  const bobDb = bobContext().firestore();
  const adminDb = adminContext().firestore();

  await assertSucceeds(submitViralBatch(aliceDb, ALICE_UID, "sub_live", "EP77889900"));

  // Alice cannot approve her own submission
  await assertFails(
    aliceDb.collection("viral_submissions").doc("sub_live").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin can list pending proofs and approve atomically
  await assertSucceeds(
    adminDb.collection("deposit_proofs").where("status", "==", "PENDING_VERIFICATION").get()
  );
  const approveBatch = adminDb.batch();
  const updateTime = new Date(Date.now() - 500);
  approveBatch.update(adminDb.collection("viral_submissions").doc("sub_live"), {
    status: "APPROVED",
    adminNote: "100 PKR confirmed",
    updatedAt: updateTime,
  });
  approveBatch.update(adminDb.collection("deposit_proofs").doc("sub_live"), {
    status: "APPROVED",
    updatedAt: updateTime,
  });
  await assertSucceeds(approveBatch.commit());

  // Bob can now list APPROVED submissions, increment viralShares by 1, but still cannot read deposit_proofs PII
  await assertSucceeds(
    bobDb.collection("viral_submissions").where("status", "==", "APPROVED").get()
  );
  await assertFails(bobDb.collection("deposit_proofs").doc("sub_live").get());

  await assertSucceeds(
    bobDb.collection("viral_submissions").doc("sub_live").update({
      viralShares: 1,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Bob cannot tamper with title or jump share count by +99
  await assertFails(
    bobDb.collection("viral_submissions").doc("sub_live").update({
      viralShares: 100,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

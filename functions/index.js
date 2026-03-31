// functions/index.js - Firebase Cloud Functions for AI System
// Deploy with: firebase deploy --only functions

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// ============ REQUIREMENT PROCESSING ============

/**
 * HTTP trigger: Process new requirement from admin
 * Endpoint: https://region-supremeai.cloudfunctions.net/processRequirement
 */
exports.processRequirement = functions.https.onRequest(async (req, res) => {
    try {
        const { projectId, description } = req.body;
        
        if (!projectId || !description) {
            return res.status(400).json({ error: "Missing projectId or description" });
        }
        
        // Call Java backend to classify
        const classificationUrl = `http://localhost:8080/classify`;
        const classifyResponse = await axios.post(classificationUrl, { description });
        const size = classifyResponse.data.size; // SMALL, MEDIUM, or BIG
        
        // Save requirement to Firestore
        const reqRef = await db.collection("requirements").add({
            projectId,
            description,
            size,
            status: "pending",
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        // Auto-approve or notify
        if (size === "SMALL") {
            await reqRef.update({ status: "approved" });
            console.log(`✅ Auto-approved SMALL requirement: ${reqRef.id}`);
        } else if (size === "MEDIUM") {
            // Schedule auto-approve after 10 minutes
            db.collection("scheduled_approvals").add({
                requirementId: reqRef.id,
                approvalTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 10 * 60000)),
            });
            
            // Send notification
            await admin.messaging().send({
                notification: {
                    title: "⏳ Approval Needed",
                    body: description,
                },
                data: { requirementId: reqRef.id },
                topic: "admin-notifications",
            });
            console.log(`⏳ MEDIUM requirement pending approval: ${reqRef.id}`);
        } else {
            // Send urgent notification for BIG tasks
            await admin.messaging().send({
                notification: {
                    title: "🛑 URGENT: Manual Approval Required",
                    body: description,
                },
                data: { requirementId: reqRef.id, type: "big_approval" },
                topic: "admin-notifications",
            });
            console.log(`🛑 BIG requirement awaiting manual approval: ${reqRef.id}`);
        }
        
        res.json({
            success: true,
            requirementId: reqRef.id,
            size,
            message: `Requirement processed as ${size}`,
        });
    } catch (error) {
        console.error("Error processing requirement:", error);
        res.status(500).json({ error: error.message });
    }
});

// ============ APPROVAL HANDLING ============

/**
 * HTTP trigger: Admin approves/rejects requirement
 * Endpoint: https://region-supremeai.cloudfunctions.net/approveRequirement
 */
exports.approveRequirement = functions.https.onRequest(async (req, res) => {
    try {
        const { requirementId, approved, notes } = req.body;
        
        if (!requirementId) {
            return res.status(400).json({ error: "Missing requirementId" });
        }
        
        // Update requirement status
        await db.collection("requirements").doc(requirementId).update({
            status: approved ? "approved" : "rejected",
            notes,
            approvedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        // If approved, trigger agent orchestrator
        if (approved) {
            const req_doc = await db.collection("requirements").doc(requirementId).get();
            const { projectId, description } = req_doc.data();
            
            // Call Java backend orchestrator
            const orchestrateUrl = `http://localhost:8080/orchestrate`;
            await axios.post(orchestrateUrl, {
                projectId,
                requirementDescription: description,
            });
            
            // Update project status
            await db.collection("projects").doc(projectId).update({
                status: "building",
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }
        
        res.json({
            success: true,
            status: approved ? "approved" : "rejected",
        });
    } catch (error) {
        console.error("Error approving requirement:", error);
        res.status(500).json({ error: error.message });
    }
});

// ============ AUTO-APPROVAL SCHEDULER ============

/**
 * Scheduled trigger: Auto-approve MEDIUM tasks after 10 minutes
 */
exports.autoApproveScheduled = functions.pubsub.schedule("*/1 * * * *").onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();
    
    const scheduledApprovals = await db.collection("scheduled_approvals")
        .where("approvalTime", "<=", now)
        .get();
    
    for (const doc of scheduledApprovals.docs) {
        const { requirementId } = doc.data();
        
        // Check if still pending
        const req = await db.collection("requirements").doc(requirementId).get();
        if (req.data().status === "pending") {
            await req.ref.update({
                status: "approved",
                autoApprovedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            console.log(`✅ Auto-approved MEDIUM requirement: ${requirementId}`);
        }
        
        // Delete scheduled entry
        await doc.ref.delete();
    }
    
    return null;
});

// ============ AI AGENT ROTATION ============

/**
 * HTTP trigger: Handle quota exceeded / API errors
 * Called by Java backend on 429/403 errors
 */
exports.rotateAgent = functions.https.onRequest(async (req, res) => {
    try {
        const { agentId, reason } = req.body;
        
        // Update agent status
        await db.collection("ai_pool").doc(agentId).update({
            status: "rotated",
            reason,
            rotatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        // Trigger VPN switch (if enabled in config)
        const config = await db.collection("config").doc("system").get();
        if (config.data().vpn_enabled) {
            const vpnResult = await switchVPN(agentId);
            console.log(`🔄 VPN switched for ${agentId}: ${vpnResult}`);
        }
        
        // Notify admin
        await admin.messaging().send({
            notification: {
                title: "⚠️  Agent Rotated",
                body: `${agentId} rotated due to: ${reason}`,
            },
            topic: "admin-notifications",
        });
        
        res.json({ success: true, message: "Agent rotated" });
    } catch (error) {
        console.error("Error rotating agent:", error);
        res.status(500).json({ error: error.message });
    }
});

// ============ CHAT MESSAGE HANDLER ============

/**
 * Firestore trigger: Save AI messages to chat history
 */
exports.onChatMessage = functions.firestore
    .document("projects/{projectId}/chat/{messageId}")
    .onCreate(async (snap, context) => {
        const { projectId } = context.params;
        const message = snap.data();
        
        // Update project's lastMessage timestamp
        await admin.firestore().collection("projects").doc(projectId).update({
            lastMessageAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        // Send real-time notification if from AI
        if (message.sender !== "admin") {
            const project = await admin.firestore().collection("projects").doc(projectId).get();
            const adminUserId = project.data().adminUserId;
            
            if (adminUserId) {
                await admin.messaging().sendToDevice(adminUserId, {
                    notification: {
                        title: `${message.sender} Updated`,
                        body: message.message.substring(0, 50) + "...",
                    },
                    data: {
                        projectId,
                        type: "chat_update",
                    },
                });
            }
        }
    });

// ============ PROGRESS TRACKER ============

/**
 * HTTP trigger: Update project progress
 */
exports.updateProgress = functions.https.onRequest(async (req, res) => {
    try {
        const { projectId, progress, status } = req.body;
        
        await db.collection("projects").doc(projectId).update({
            progress,
            status: status || undefined,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ============ HELPER: VPN SWITCHING ============

async function switchVPN(agentId) {
    // Call Proton/Windscribe API to rotate IP
    // For demo: just log
    console.log(`🔄 Switching VPN for ${agentId}`);
    return "VPN_SWITCHED";
}

// ============ FIRESTORE SECURITY RULES ============
/*
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Admin can read/write all
    match /{document=**} {
      allow read, write: if request.auth.token.admin == true;
    }
    
    // AI system can write to chat/notifications
    match /projects/{projectId}/chat/{document=**} {
      allow write: if request.auth.uid == "ai-system";
    }
    
    match /notifications/{document=**} {
      allow read: if request.auth.uid == resource.data.userId;
    }
    
    // Deny all others
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
*/

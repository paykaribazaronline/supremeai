// functions/lib/index.js - Main Firebase Functions entry point
// This file is the compiled/standalone entry point for Firebase Functions

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

const allowCors = (handler) => async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-api-key');

  if (req.method === 'OPTIONS') {
    return res.status(204).send('');
  }
  return handler(req, res);
};

const authenticate = async (req, res, next) => {
    const apiKey = req.get('x-api-key') || (req.body && req.body.apiKey) || (req.query && req.query.apiKey);
    const systemSecret = functions.config().system && functions.config().system.secret;
    
    if (systemSecret && systemSecret.trim() !== '' && apiKey && apiKey === systemSecret) {
        console.log('Java backend authenticated via system secret');
        return next();
    }
    
    const authHeader = req.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: "Unauthorized: Missing or invalid token" });
    }
    
    try {
        const idToken = authHeader.split('Bearer ')[1];
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        if (decodedToken.admin !== true && decodedToken.admin !== 'true') {
            return res.status(403).json({ error: "Forbidden: Admin access required" });
        }
        req.user = decodedToken;
        return next();
    } catch (error) {
        console.error('Error verifying token:', error);
        return res.status(401).json({ error: "Unauthorized: Invalid token" });
    }
};

const withAuth = (handler) => {
    return async (req, res) => {
        return authenticate(req, res, () => handler(req, res));
    };
};

exports.getSystemHealth = onRequest(allowCors(async (req, res) => {
    res.json({
        success: true,
        status: "healthy",
        timestamp: new Date().toISOString(),
        services: {
            firestore: "connected",
            functions: "healthy"
        }
    });
}));

exports.getConfiguredProviders = onRequest(allowCors(async (req, res) => {
    try {
        const providers = [];
        const snap = await db.collection('ai_providers').get();
        snap.forEach(doc => {
            const data = doc.data();
            if (data.status === 'active') {
                providers.push({
                    id: doc.id,
                    name: data.name || doc.id,
                    type: data.type || 'api',
                    status: data.status,
                    apiKeyConfigured: !!data.apiKey,
                    models: data.models || [],
                });
            }
        });
        res.json({
            success: true,
            data: {
                providers,
                total: providers.length,
                active: providers.length,
            }
        });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
}));

exports.getProviderHealthStats = onRequest(allowCors(async (req, res) => {
    try {
        const providers = [];
        const snap = await db.collection('ai_providers').get();
        snap.forEach(doc => {
            const data = doc.data();
            if (data.status === 'active') {
                providers.push(data);
            }
        });
        res.json({
            success: true,
            data: {
                total: providers.length,
                active: providers.filter(p => p.status === 'active').length,
                error: 0,
            }
        });
    } catch (err) {
        res.json({ success: true, data: { total: 0, active: 0, error: 0 } });
    }
}));

exports.processRequirement = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, description } = req.body;
        
        if (!projectId || !description) {
            return res.status(400).json({ error: "Missing projectId or description" });
        }
        
        const backendUrl = functions.config().backend && functions.config().backend.url || 'https://ide-api.supremeai.google.com';
        const classifyResponse = await axios.post(`${backendUrl}/classify`, { description });
        const size = classifyResponse.data.size || 'SMALL';
        
        const reqRef = await db.collection("requirements").add({
            projectId,
            description,
            size,
            status: "pending",
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        if (size === "SMALL") {
            await reqRef.update({ status: "approved" });
        } else if (size === "MEDIUM") {
            db.collection("scheduled_approvals").add({
                requirementId: reqRef.id,
                approvalTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 10 * 60000)),
            });
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
}));

exports.approveRequirement = onRequest(withAuth(async (req, res) => {
    try {
        const { requirementId, approved, notes } = req.body;
        
        if (!requirementId) {
            return res.status(400).json({ error: "Missing requirementId" });
        }
        
        await db.collection("requirements").doc(requirementId).update({
            status: approved ? "approved" : "rejected",
            notes,
            approvedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        res.json({
            success: true,
            status: approved ? "approved" : "rejected",
        });
    } catch (error) {
        console.error("Error approving requirement:", error);
        res.status(500).json({ error: error.message });
    }
}));

exports.autoApproveScheduled = onSchedule("*/1 * * * *", async (event) => {
    const now = admin.firestore.Timestamp.now();
    
    const scheduledApprovals = await db.collection("scheduled_approvals")
        .where("approvalTime", "<=", now)
        .get();
    
    for (const doc of scheduledApprovals.docs) {
        const { requirementId } = doc.data();
        
        const req = await db.collection("requirements").doc(requirementId).get();
        if (req.data().status === "pending") {
            await req.ref.update({
                status: "approved",
                autoApprovedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }
        
        await doc.ref.delete();
    }
    
    return null;
});

exports.api = functions.https.onRequest(async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    
    if (req.method === 'OPTIONS') {
        return res.status(204).send('');
    }
    
    res.json({
        success: true,
        message: "API endpoint active",
        timestamp: new Date().toISOString()
    });
});
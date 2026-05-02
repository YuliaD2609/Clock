/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { setGlobalOptions } = require("firebase-functions");
const { onRequest } = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * PayPal Webhook Handler
 * Questo endpoint riceve le notifiche da PayPal e sblocca le funzioni nel database.
 */
exports.paypalWebhook = functions.https.onRequest(async (req, res) => {
    if (req.method !== "POST") {
        return res.status(405).send("Method Not Allowed");
    }

    const body = req.body;

    // PayPal invia eventi diversi. Ci interessa PAYMENT.SALE.COMPLETED
    if (body.event_type === "PAYMENT.SALE.COMPLETED") {
        const resource = body.resource;
        const deviceId = resource.custom;
        const amount = resource.amount.total;

        if (!deviceId) {
            return res.status(200).send("No ID");
        }

        const db = admin.database();
        const userRef = db.ref(`users/${deviceId}`);

        let updates = {};
        if (parseFloat(amount) >= 1.00) {
            updates["unlocked_events"] = true;
        }
        if (parseFloat(amount) >= 0.50) {
            updates["unlocked_colors"] = true;
        }

        await userRef.update(updates);
    }

    res.status(200).send("OK");
});


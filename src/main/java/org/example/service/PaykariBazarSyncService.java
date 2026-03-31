package org.example.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaykariBazarSyncService
 *
 * Connects SupremeAI's product data pipeline to the Paykari Bazar platform.
 *
 * SupremeAI scrapes product data from external e-commerce sources
 * (Chaldal, Shwapno, Daily Shopping, …) and this service:
 *
 *  1. Ensures each source is registered as a Shop/Vendor inside Paykari Bazar
 *     via {@link #ensureShopAndCategory}.
 *
 *  2. Applies a wholesale price (10 % below retail) and generates a
 *     canonical "PB-{SHOP_CODE}-{ID}" SKU before writing the product to
 *     the shared Firestore path:
 *         hub → data → products
 *     via {@link #uploadToPaykariBazar}.
 *
 * Firestore layout (Paykari Bazar side):
 *   hub / data / products / {sku}     ← product catalogue
 *   hub / data / shops    / {shopId}  ← vendor registry
 *   hub / data / categories/ {catId}  ← category registry
 */
@Service
public class PaykariBazarSyncService {

    private static final Logger logger = LoggerFactory.getLogger(PaykariBazarSyncService.class);

    // ── Firestore path constants ──────────────────────────────────────────────
    private static final String HUB_COLLECTION  = "hub";
    private static final String HUB_DOCUMENT    = "data";
    private static final String COL_PRODUCTS    = "products";
    private static final String COL_SHOPS       = "shops";
    private static final String COL_CATEGORIES  = "categories";

    // ── Wholesale discount rate ───────────────────────────────────────────────
    /** Retail price is discounted by this factor to derive the wholesale price. */
    private static final double WHOLESALE_DISCOUNT = 0.10;

    // ── Vendor / shop mappings ────────────────────────────────────────────────
    /**
     * Maps well-known external source names to their short codes used inside
     * Paykari Bazar.  "PB" (Paykari Bazar) + shop code forms the SKU prefix.
     *
     * Add new sources here as SupremeAI expands coverage.
     */
    private static final Map<String, String> SHOP_CODES = Map.of(
            "Chaldal",        "CH",
            "Shwapno",        "SW",
            "Daily Shopping", "DS"
    );

    private Firestore db;

    @PostConstruct
    public void init() {
        try {
            this.db = FirestoreClient.getFirestore();
            logger.info("✅ PaykariBazarSyncService initialised — Firestore ready");
        } catch (Exception e) {
            logger.error("⚠️ PaykariBazarSyncService: Firestore init failed: {}", e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Ensures that a Shop entry and a Category entry exist inside
     * the Paykari Bazar Firestore hub for the given {@code source}
     * and {@code category}.
     *
     * If the documents already exist they are left unchanged.
     * If they are missing they are created with sensible defaults.
     *
     * @param source   External site name, e.g. "Chaldal"
     * @param category Product category, e.g. "Vegetables"
     * @return shopId  The Firestore document ID of the shop entry
     */
    public String ensureShopAndCategory(String source, String category) {
        if (db == null) {
            logger.warn("Firestore not available — skipping ensureShopAndCategory");
            return null;
        }

        String shopCode = SHOP_CODES.getOrDefault(source, sanitize(source));
        String shopId   = "SHOP_" + shopCode;
        String catId    = "CAT_"  + sanitize(category);

        // ── Shop / Vendor ────────────────────────────────────────────────────
        try {
            DocumentReference shopRef = hubSubCollection(COL_SHOPS).document(shopId);
            DocumentSnapshot  shopDoc = shopRef.get().get();

            if (!shopDoc.exists()) {
                Map<String, Object> shopData = new HashMap<>();
                shopData.put("shopId",      shopId);
                shopData.put("name",        source);
                shopData.put("code",        shopCode);
                shopData.put("type",        "SUPPLIER");
                shopData.put("platform",    "external");
                shopData.put("isActive",    true);
                shopData.put("createdAt",   System.currentTimeMillis());
                shopData.put("createdBy",   "supremeai");

                shopRef.set(shopData).get();
                logger.info("🏪 Created new shop entry: {} → {}", source, shopId);
            } else {
                logger.debug("🏪 Shop already exists: {}", shopId);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to ensure shop {}: {}", shopId, e.getMessage());
        }

        // ── Category ─────────────────────────────────────────────────────────
        try {
            DocumentReference catRef = hubSubCollection(COL_CATEGORIES).document(catId);
            DocumentSnapshot  catDoc = catRef.get().get();

            if (!catDoc.exists()) {
                Map<String, Object> catData = new HashMap<>();
                catData.put("categoryId", catId);
                catData.put("name",       category);
                catData.put("isActive",   true);
                catData.put("createdAt",  System.currentTimeMillis());
                catData.put("createdBy",  "supremeai");

                catRef.set(catData).get();
                logger.info("🗂️ Created new category entry: {}", category);
            } else {
                logger.debug("🗂️ Category already exists: {}", catId);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to ensure category {}: {}", catId, e.getMessage());
        }

        return shopId;
    }

    /**
     * Uploads a single scraped product to the Paykari Bazar Firestore hub.
     *
     * The product is written to:
     *   {@code hub → data → products → {sku}}
     *
     * A canonical SKU is generated using {@link #generateSku} and the retail
     * price is adjusted to a wholesale price using {@link #applyWholesalePrice}.
     *
     * @param source      External site name ("Chaldal", "Shwapno", …)
     * @param category    Product category string
     * @param productId   External/source product identifier
     * @param name        Product display name
     * @param retailPrice Retail price scraped from the source site
     * @param imageUrl    Product image URL (may be null)
     * @param extra       Any additional key-value data to store alongside
     * @return sku        The generated SKU, or null on failure
     */
    public String uploadToPaykariBazar(
            String source,
            String category,
            String productId,
            String name,
            double retailPrice,
            String imageUrl,
            Map<String, Object> extra) {

        if (db == null) {
            logger.warn("Firestore not available — skipping uploadToPaykariBazar");
            return null;
        }

        // Step 1 – ensure shop & category exist
        String shopId = ensureShopAndCategory(source, category);

        // Step 2 – build SKU and prices
        String sku            = generateSku(source, productId);
        double wholesalePrice = applyWholesalePrice(retailPrice);

        // Step 3 – build product document
        Map<String, Object> product = new HashMap<>();
        product.put("sku",            sku);
        product.put("name",           name);
        product.put("source",         source);
        product.put("shopId",         shopId);
        product.put("category",       category);
        product.put("retailPrice",    round(retailPrice));
        product.put("wholesalePrice", wholesalePrice);
        product.put("currency",       "BDT");
        product.put("imageUrl",       imageUrl != null ? imageUrl : "");
        product.put("isAvailable",    true);
        product.put("syncedAt",       System.currentTimeMillis());
        product.put("syncedBy",       "supremeai");

        if (extra != null) {
            product.putAll(extra);
        }

        // Step 4 – write to Firestore: hub / data / products / {sku}
        try {
            WriteResult result = hubSubCollection(COL_PRODUCTS)
                    .document(sku)
                    .set(product)
                    .get();

            logger.info("📦 Synced product to Paykari Bazar: {} ({})", name, sku);
            return sku;

        } catch (Exception e) {
            logger.error("❌ Failed to upload product {} to Paykari Bazar: {}", sku, e.getMessage());
            return null;
        }
    }

    /**
     * Convenience overload without extra fields.
     */
    public String uploadToPaykariBazar(
            String source,
            String category,
            String productId,
            String name,
            double retailPrice,
            String imageUrl) {
        return uploadToPaykariBazar(source, category, productId, name, retailPrice, imageUrl, null);
    }

    /**
     * Batch-upload a list of products from the same source and category.
     *
     * @param source    External site name
     * @param category  Product category
     * @param products  List of product maps, each must contain keys:
     *                  "id", "name", "price", and optionally "imageUrl" + any extras.
     * @return Number of products successfully uploaded
     */
    public int batchUploadToPaykariBazar(
            String source,
            String category,
            List<Map<String, Object>> products) {

        int uploaded = 0;
        for (Map<String, Object> p : products) {
            try {
                String  id       = String.valueOf(p.get("id"));
                String  name     = String.valueOf(p.get("name"));
                double  price    = toDouble(p.get("price"));
                String  imageUrl = p.containsKey("imageUrl") ? String.valueOf(p.get("imageUrl")) : null;

                // Collect any extra keys not in the standard set
                Map<String, Object> extra = new HashMap<>(p);
                extra.remove("id");
                extra.remove("name");
                extra.remove("price");
                extra.remove("imageUrl");

                String sku = uploadToPaykariBazar(source, category, id, name, price, imageUrl, extra);
                if (sku != null) uploaded++;

            } catch (Exception e) {
                logger.warn("⚠️ Skipping product due to error: {}", e.getMessage());
            }
        }

        logger.info("✅ Batch upload complete: {}/{} products synced from {} / {}",
                uploaded, products.size(), source, category);
        return uploaded;
    }

    // ── Pricing helpers ───────────────────────────────────────────────────────

    /**
     * Applies the standard Paykari Bazar wholesale discount (10%) to a
     * retail price and rounds the result to 2 decimal places.
     *
     * Formula:  wholesalePrice = round(retailPrice × (1 − 0.10), 2)
     *
     * @param retailPrice Retail price in BDT (or any currency)
     * @return Wholesale price
     */
    public double applyWholesalePrice(double retailPrice) {
        return round(retailPrice * (1.0 - WHOLESALE_DISCOUNT));
    }

    // ── SKU helpers ───────────────────────────────────────────────────────────

    /**
     * Generates a canonical Paykari Bazar SKU.
     *
     * Format: {@code PB-{SHOP_CODE}-{PRODUCT_ID}}
     * Example: {@code PB-CH-12345}  (Chaldal product 12345)
     *
     * @param source    External site name
     * @param productId Source-side product identifier
     * @return SKU string
     */
    public String generateSku(String source, String productId) {
        String shopCode = SHOP_CODES.getOrDefault(source, sanitize(source));
        return "PB-" + shopCode + "-" + sanitize(productId);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns a CollectionReference for a sub-collection inside the
     * Paykari Bazar hub document: {@code hub / data / {subCollection}}.
     */
    private CollectionReference hubSubCollection(String subCollection) {
        return db.collection(HUB_COLLECTION)
                 .document(HUB_DOCUMENT)
                 .collection(subCollection);
    }

    /** Rounds a double to 2 decimal places (HALF_UP). */
    private double round(double value) {
        return BigDecimal.valueOf(value)
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    /** Converts an Object to double safely. */
    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException e) { return 0.0; }
    }

    /**
     * Sanitises a string for use as a Firestore document ID or SKU segment:
     * upper-cases, replaces whitespace and special chars with underscores.
     */
    private String sanitize(String value) {
        if (value == null) return "UNKNOWN";
        return value.toUpperCase()
                    .replaceAll("[^A-Z0-9_\\-]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
    }
}

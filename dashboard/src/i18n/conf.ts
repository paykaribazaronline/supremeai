import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";

// Import translation files
import bnTranslation from "./bn.json";
import enTranslation from "./en.json";

const resources = {
  en: {
    translation: enTranslation,
  },
  bn: {
    translation: bnTranslation,
  },
};

// Only initialize i18next if it hasn't been initialized already
// This prevents "i18next is already initialized" warnings
if (!i18n.isInitialized) {
  i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      resources,
      fallbackLng: "en",
      debug: process.env.NODE_ENV === "development",

      interpolation: {
        escapeValue: false, // React already escapes values
      },

      detection: {
        order: ["localStorage", "navigator"],
        caches: ["localStorage"],
      },
    });
}

export default i18n;

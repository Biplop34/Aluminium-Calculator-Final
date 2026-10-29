var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));

// server.ts
var import_express = __toESM(require("express"), 1);
var import_path = __toESM(require("path"), 1);
var import_vite = require("vite");
var import_genai = require("@google/genai");
var import_dotenv = __toESM(require("dotenv"), 1);
import_dotenv.default.config();
async function startServer() {
  const app = (0, import_express.default)();
  const PORT = 3e3;
  app.use((req, res, next) => {
    const origin = req.headers.origin;
    if (origin) {
      res.setHeader("Access-Control-Allow-Origin", origin);
    } else {
      res.setHeader("Access-Control-Allow-Origin", "*");
    }
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, PATCH, DELETE");
    res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,Content-Type,Authorization");
    res.setHeader("Access-Control-Allow-Credentials", "true");
    if (req.method === "OPTIONS") {
      return res.sendStatus(200);
    }
    next();
  });
  app.use(import_express.default.json({ limit: "50mb" }));
  app.use(import_express.default.urlencoded({ limit: "50mb", extended: true }));
  let ai = null;
  const getAiClient = () => {
    if (!ai) {
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        throw new Error("GEMINI_API_KEY environment variable is not defined.");
      }
      ai = new import_genai.GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            "User-Agent": "aistudio-build"
          }
        }
      });
    }
    return ai;
  };
  app.post("/api/scan", async (req, res) => {
    try {
      const { image } = req.body;
      if (!image) {
        return res.status(400).json({ error: "No image data provided" });
      }
      const match = image.match(/^data:(image\/\w+);base64,(.+)$/);
      if (!match) {
        return res.status(400).json({ error: "Invalid image format. Expected a base64 Data URL." });
      }
      const mimeType = match[1];
      const base64Data = match[2];
      const client = getAiClient();
      const prompt = "You are a high-precision handwritten note reader and calculator dimensions extractor for Thai aluminium window and glass fabrication workshops.\nLook at the handwritten image, parse each listed item, and extract:\n1. \"height\" - The value to the left of the 'x' or 'X' or '\u00d7' multiplier symbol.\n2. \"length1\" - The first length value to the right of the 'x' symbol (Top length).\n   CRITICAL UNDERLINE / FRACTION RULE:\n   If after 'x' there is an underline or horizontal fraction bar with numbers written above and below it:\n   - The number written ABOVE the underline is \"length1\" (Top length).\n   - If written side-by-side (e.g. 149.7/150 or 46-45/5), the first part is \"length1\".\n3. \"length2\" - The second length value (Bottom length):\n   - The number written BELOW the underline is \"length2\" (Bottom length).\n   - If written side-by-side (e.g. 149.7/150, 46-45/5, 119/7 - 120/1), the second part is \"length2\".\n   - If only a single length is present without an underline or bottom value, leave \"length2\" empty or null.\n\nIMPORTANT REGIONAL NUMBERING RULES FOR THAI FABRICATION CALCULATION:\n- A slash \"/\", dash \"-\", star \"*\", or dot \".\" within any number represents a DECIMAL POINT \".\"!\n  For example:\n  \"206.1\", \"206/1\", \"206-1\", \"202*1\" must ALL be converted to \"206.1\" (or \"202.1\").\n  \"21/2\" or \"21-2\" must be converted to \"21.2\".\n  \"149/7\" or \"149-7\" or \"149*7\" must be converted to \"149.7\".\n  \"45/5\" must be converted to \"45.5\".\n  \"181/9\" or \"181-9\" or \"181*9\" must be converted to \"181.9\".\n- If a number has no fraction/separator, keep it as integer (e.g. \"150\", \"46\").\n- Extract all valid measurement rows. Keep them in the exact order found.\n- CRITICAL: Extract EVERY item on the page without skipping.";
const modelCandidates = ["gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash"];
      let lastError = null;
      let responseText = "";
      for (const modelName of modelCandidates) {
        try {
          console.log(`Attempting image scan with model: ${modelName}`);
          const response = await client.models.generateContent({
            model: modelName,
            contents: [
              {
                role: "user",
                parts: [
                  { text: prompt },
                  { inlineData: { mimeType, data: base64Data } }
                ]
              }
            ],
            config: {
              responseMimeType: "application/json",
              maxOutputTokens: 8192,
              responseSchema: {
                type: import_genai.Type.OBJECT,
                properties: {
                  items: {
                    type: import_genai.Type.ARRAY,
                    items: {
                      type: import_genai.Type.OBJECT,
                      properties: {
                        id: { type: import_genai.Type.NUMBER, description: "Item serial number index" },
                        height: { type: import_genai.Type.STRING, description: "Parsed height, e.g. '21.2'" },
                        length1: { type: import_genai.Type.STRING, description: "Parsed length 1, e.g. '46.0'" },
                        length2: { type: import_genai.Type.STRING, description: "Parsed length 2 (optional), e.g. '45.5' or empty/null" }
                      },
                      required: ["id", "height", "length1"]
                    }
                  }
                },
                required: ["items"]
              }
            }
          });
          if (response && response.text) {
            responseText = response.text;
            console.log(`Successfully completed scan using model: ${modelName}`);
            break;
          }
        } catch (err) {
          console.warn(`Model ${modelName} failed or busy:`, err.message || err);
          lastError = err;
        }
      }
      if (!responseText) {
        throw new Error(lastError ? lastError.message || JSON.stringify(lastError) : "All candidate models failed to respond.");
      }
      const result = JSON.parse(responseText);
      res.json(result);
    } catch (err) {
      console.error("Scanning Error:", err);
      res.status(500).json({ error: err.message || "Failed to parse handwriting image." });
    }
  });
  if (process.env.NODE_ENV !== "production") {
    const vite = await (0, import_vite.createServer)({
      server: { middlewareMode: true },
      appType: "spa"
    });
    app.use(vite.middlewares);
    console.log("Vite middleware mounted for development");
  } else {
    const distPath = import_path.default.join(process.cwd(), "dist");
    app.use(import_express.default.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(import_path.default.join(distPath, "index.html"));
    });
    console.log("Serving static files from dist in production");
  }
  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}
startServer();
//# sourceMappingURL=server.cjs.map

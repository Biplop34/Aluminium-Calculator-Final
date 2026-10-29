const fs = require("fs");
const content = fs.readFileSync("app/src/main/assets/assets/index-C_ZGULEN.js", "utf8");
const lines = content.split("\n");
const line = lines[2937];

let stack = [];
let inStr = false;
let quoteChar = "";

for (let i = 0; i < line.length; i++) {
  const c = line[i];
  const prev = i > 0 ? line[i - 1] : "";

  if (inStr) {
    if (c === quoteChar && prev !== "\\") {
      inStr = false;
    }
    continue;
  }

  if (c === '"' || c === "'" || c === "`") {
    inStr = true;
    quoteChar = c;
    continue;
  }

  if (c === "(" || c === "[" || c === "{") {
    stack.push({ char: c, pos: i });
  } else if (c === ")" || c === "]" || c === "}") {
    if (stack.length === 0) {
      console.log(`Unmatched closing ${c} at pos ${i}`);
      console.log("Snippet:", line.slice(Math.max(0, i - 40), i + 40));
      break;
    }
    const last = stack.pop();
    const match = (last.char === "(" && c === ")") ||
                  (last.char === "[" && c === "]") ||
                  (last.char === "{" && c === "}");
    if (!match) {
      console.log(`Mismatched ${last.char} (from pos ${last.pos}) with ${c} at pos ${i}`);
      console.log("Opening context:", line.slice(Math.max(0, last.pos - 20), last.pos + 30));
      console.log("Closing context:", line.slice(Math.max(0, i - 30), i + 30));
      break;
    }
  }
}

if (stack.length > 0) {
  console.log("Unclosed opening brackets count:", stack.length);
  for (let s of stack.slice(-5)) {
    console.log(`Unclosed ${s.char} at pos ${s.pos}: ${line.slice(s.pos, s.pos + 40)}`);
  }
} else {
  console.log("All brackets balanced in line 2938!");
}

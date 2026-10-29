const fs = require('fs');
const vm = require('vm');

const newHjComponent = `function hj(){
  const [display, setDisplay] = H.useState("0");
  const [prevValue, setPrevValue] = H.useState(null);
  const [operator, setOperator] = H.useState(null);
  const [waitingForOperand, setWaitingForOperand] = H.useState(false);
  const [expression, setExpression] = H.useState("");
  const [history, setHistory] = H.useState(() => {
    if (typeof window !== "undefined") {
      try {
        const saved = localStorage.getItem("app2_history");
        return saved ? JSON.parse(saved) : [];
      } catch(e) { return []; }
    }
    return [];
  });
  const [isHistoryOpen, setIsHistoryOpen] = H.useState(false);
  const [isGuideOpen, setIsGuideOpen] = H.useState(false);
  const [toast, setToast] = H.useState(null);

  H.useEffect(() => {
    try {
      localStorage.setItem("app2_history", JSON.stringify(history));
    } catch(e) {}
  }, [history]);

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2000);
  };

  // Base-8 logic helpers
  const toParts = (val) => {
    if (!val || val === "." || val === "-") return 0;
    const isNeg = val.startsWith("-");
    const clean = isNeg ? val.slice(1) : val;
    const parts = clean.split(".");
    const wholes = parseInt(parts[0] || "0", 10) || 0;
    const sutas = parseInt(parts[1] || "0", 10) || 0;
    const total = wholes * 8 + sutas;
    return isNeg ? -total : total;
  };

  const fromParts = (total) => {
    const isNeg = total < 0;
    const abs = Math.abs(total);
    const wholes = Math.floor(abs / 8);
    const sutas = abs % 8;
    return (isNeg ? "-" : "") + wholes + "." + sutas;
  };

  const calc = (op, a, b) => {
    switch(op) {
      case "+": return a + b;
      case "-": return a - b;
      case "*": return Math.round((a * b) / 8);
      case "/": return b === 0 ? 0 : Math.round((a * 8) / b);
      case "%": return Math.round((a * b) / 800);
      default: return b;
    }
  };

  // Normalization: if user types .8 or above, auto-normalize
  H.useEffect(() => {
    const clean = display.startsWith("-") ? display.slice(1) : display;
    const [whole, part] = clean.split(".");
    if (part && parseInt(part, 10) >= 8) {
      const tot = toParts(display);
      setDisplay(fromParts(tot));
    }
  }, [display]);

  // Suta breakdown conversion
  const currentParts = toParts(display);
  const absParts = Math.abs(currentParts);
  const isNegative = currentParts < 0;
  const currentWholes = Math.floor(absParts / 8);
  const currentSutas = absParts % 8;
  const fracMap = ["", "⅛", "¼", "⅜", "½", "⅝", "¾", "⅞"];
  const currentFraction = fracMap[currentSutas] || "";
  const inchFraction = currentWholes + (currentFraction ? " " + currentFraction : "") + '"';
  const metricMm = ((absParts / 8) * 25.4).toFixed(1);
  const metricCm = ((absParts / 8) * 2.54).toFixed(2);
  const decimalInches = (absParts / 8).toFixed(3);

  // Live preview
  const liveResult = (() => {
    if (prevValue === null || !operator || waitingForOperand) return null;
    const res = calc(operator, prevValue, toParts(display));
    return fromParts(res);
  })();

  const handleDigit = (d) => {
    if (waitingForOperand) {
      if (!operator) setExpression("");
      setDisplay(d);
      setWaitingForOperand(false);
    } else {
      if (display === "0") {
        setDisplay(d);
      } else if (display === "-0") {
        setDisplay("-" + d);
      } else {
        setDisplay(display + d);
      }
    }
  };

  const handleDot = () => {
    if (waitingForOperand) {
      setDisplay("0.");
      setWaitingForOperand(false);
      return;
    }
    if (!display.includes(".")) {
      setDisplay(display + ".");
    }
  };

  const handleBackspace = () => {
    if (waitingForOperand) return;
    if (display.length > 1 && !(display.length === 2 && display.startsWith("-"))) {
      setDisplay(display.slice(0, -1));
    } else {
      setDisplay("0");
    }
  };

  const handleClear = () => {
    setDisplay("0");
    setWaitingForOperand(false);
  };

  const handleAllClear = () => {
    setDisplay("0");
    setPrevValue(null);
    setOperator(null);
    setWaitingForOperand(false);
    setExpression("");
  };

  const handlePlusMinus = () => {
    if (display === "0") return;
    if (display.startsWith("-")) {
      setDisplay(display.slice(1));
    } else {
      setDisplay("-" + display);
    }
  };

  const handleSutaQuick = (sutaVal) => {
    if (waitingForOperand) {
      setDisplay("0." + sutaVal);
      setWaitingForOperand(false);
      return;
    }
    const isNeg = display.startsWith("-");
    const clean = isNeg ? display.slice(1) : display;
    const parts = clean.split(".");
    const whole = parts[0] || "0";
    setDisplay((isNeg ? "-" : "") + whole + "." + sutaVal);
  };

  const handleOperator = (nextOp) => {
    const inputVal = toParts(display);
    if (waitingForOperand && operator) {
      setOperator(nextOp);
      return;
    }
    const opSymbol = nextOp === "*" ? "×" : nextOp === "/" ? "÷" : nextOp;
    if (prevValue === null) {
      setPrevValue(inputVal);
      setExpression(display + " " + opSymbol);
    } else if (operator) {
      const res = calc(operator, prevValue, inputVal);
      setPrevValue(res);
      const resStr = fromParts(res);
      setDisplay(resStr);
      setExpression(resStr + " " + opSymbol);
    }
    setWaitingForOperand(true);
    setOperator(nextOp);
  };

  const handleEquals = () => {
    if (operator && prevValue !== null) {
      const inputVal = toParts(display);
      const res = calc(operator, prevValue, inputVal);
      const resStr = fromParts(res);
      const opSymbol = operator === "*" ? "×" : operator === "/" ? "÷" : operator;
      const fullExpr = expression + " " + display;
      const historyItem = {
        id: Date.now().toString(),
        expression: fullExpr,
        result: resStr,
        resultValue: res,
        desc: fromParts(res) + ' (' + (Math.floor(Math.abs(res)/8)) + '" ' + (Math.abs(res)%8) + 's)',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      setHistory(prev => [historyItem, ...prev].slice(0, 30));
      setDisplay(resStr);
      setPrevValue(null);
      setOperator(null);
      setExpression(fullExpr + " =");
      setWaitingForOperand(true);
    }
  };

  const handleCopy = () => {
    const textToCopy = display + ' ( ' + (isNegative ? '-' : '') + currentWholes + '" ' + currentSutas + ' Suta = ' + (isNegative ? '-' : '') + metricMm + ' mm )';
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(textToCopy);
      showToast("Copied: " + display);
    } else {
      showToast("Value: " + display);
    }
  };

  const handleRestore = (item) => {
    setDisplay(item.result);
    setPrevValue(null);
    setOperator(null);
    setExpression(item.expression + " =");
    setWaitingForOperand(true);
    setIsHistoryOpen(false);
  };

  const sutaReference = [
    { suta: "1 Suta", pt: ".1", frac: "⅛ inch", mm: "3.18 mm" },
    { suta: "2 Suta", pt: ".2", frac: "¼ inch", mm: "6.35 mm" },
    { suta: "3 Suta", pt: ".3", frac: "⅜ inch", mm: "9.53 mm" },
    { suta: "4 Suta", pt: ".4", frac: "½ inch", mm: "12.70 mm" },
    { suta: "5 Suta", pt: ".5", frac: "⅝ inch", mm: "15.88 mm" },
    { suta: "6 Suta", pt: ".6", frac: "¾ inch", mm: "19.05 mm" },
    { suta: "7 Suta", pt: ".7", frac: "⅞ inch", mm: "22.23 mm" },
    { suta: "8 Suta", pt: "1.0", frac: "1 inch", mm: "25.40 mm" }
  ];

  return p.jsxs("div", {
    className: "h-full w-full bg-slate-100/70 text-slate-800 flex flex-col justify-between max-w-md mx-auto relative select-none",
    children: [
      // Top Navigation / Header
      p.jsxs("div", {
        className: "bg-white px-4 py-3 border-b border-slate-200 flex items-center justify-between shadow-sm",
        children: [
          p.jsxs("div", {
            className: "flex items-center gap-2",
            children: [
              p.jsx("div", { className: "w-2.5 h-6 bg-teal-600 rounded-full" }),
              p.jsxs("div", {
                children: [
                  p.jsx("h2", { className: "text-sm font-black text-slate-900 tracking-tight uppercase leading-none", children: "Base-8 Suta Math" }),
                  p.jsx("p", { className: "text-[10px] font-bold text-teal-700 tracking-wider mt-0.5", children: "1 Inch = 8 Suta Point Math" })
                ]
              })
            ]
          }),
          p.jsxs("div", {
            className: "flex items-center gap-1.5",
            children: [
              p.jsx("button", {
                type: "button",
                onClick: () => setIsGuideOpen(true),
                className: "px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-[11px] flex items-center gap-1 transition-colors active:scale-95",
                children: "📐 Guide"
              }),
              p.jsxs("button", {
                type: "button",
                onClick: () => setIsHistoryOpen(true),
                className: "px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-[11px] flex items-center gap-1 transition-colors active:scale-95",
                children: [
                  "📜 Tape",
                  history.length > 0 && p.jsx("span", { className: "w-4 h-4 rounded-full bg-teal-600 text-white text-[9px] flex items-center justify-center font-black", children: history.length })
                ]
              })
            ]
          })
        ]
      }),

      // Display Screen Container
      p.jsxs("div", {
        className: "flex-1 px-4 py-3 flex flex-col justify-end overflow-hidden",
        children: [
          // Toast alert
          toast && p.jsx("div", {
            className: "self-center mb-2 px-3 py-1 bg-slate-900/90 text-white text-xs font-bold rounded-full shadow-lg animate-fadeIn",
            children: toast
          }),

          // Main Display Card
          p.jsxs("div", {
            className: "bg-white rounded-3xl p-4 md:p-5 shadow-sm border border-slate-200 flex flex-col justify-between",
            children: [
              // Expression & Action Bar
              p.jsxs("div", {
                className: "flex items-center justify-between min-h-[24px] mb-1 text-xs",
                children: [
                  p.jsx("span", {
                    className: "font-mono font-medium text-slate-400 truncate pr-2 tracking-wide",
                    children: expression || (operator ? (fromParts(prevValue) + " " + (operator === "*" ? "×" : operator === "/" ? "÷" : operator)) : "")
                  }),
                  p.jsxs("div", {
                    className: "flex items-center gap-2",
                    children: [
                      liveResult && p.jsxs("span", {
                        className: "font-black text-teal-600 text-xs bg-teal-50 px-2 py-0.5 rounded-md border border-teal-100",
                        children: ["= ", liveResult]
                      }),
                      p.jsx("button", {
                        type: "button",
                        onClick: handleCopy,
                        className: "text-slate-400 hover:text-teal-600 font-bold text-[11px] p-1 rounded hover:bg-slate-50 transition-colors",
                        title: "Copy Result",
                        children: "Copy"
                      })
                    ]
                  })
                ]
              }),

              // Big Numeric Display
              p.jsx("div", {
                className: "text-right font-black tracking-tighter text-slate-900 leading-none py-1 truncate",
                style: { fontSize: display.length > 10 ? "2.2rem" : display.length > 7 ? "3rem" : "3.75rem" },
                children: display
              }),

              // Suta & Metric Conversions Card (The Game Changer!)
              p.jsxs("div", {
                className: "mt-3 pt-3 border-t border-slate-100 grid grid-cols-3 gap-2 bg-slate-50/80 p-2.5 rounded-2xl text-center",
                children: [
                  p.jsxs("div", {
                    className: "border-r border-slate-200/80 pr-1",
                    children: [
                      p.jsx("p", { className: "text-[9px] uppercase font-bold text-slate-400 tracking-wider", children: "Inches & Suta" }),
                      p.jsx("p", { className: "text-xs font-black text-slate-800 truncate", children: (isNegative ? "-" : "") + currentWholes + '" ' + currentSutas + "s" }),
                      p.jsx("p", { className: "text-[10px] font-bold text-teal-600 truncate", children: (isNegative ? "-" : "") + inchFraction })
                    ]
                  }),
                  p.jsxs("div", {
                    className: "border-r border-slate-200/80 pr-1",
                    children: [
                      p.jsx("p", { className: "text-[9px] uppercase font-bold text-slate-400 tracking-wider", children: "Total Sutas" }),
                      p.jsx("p", { className: "text-xs font-black text-slate-800 truncate", children: (isNegative ? "-" : "") + absParts + " Suta" }),
                      p.jsx("p", { className: "text-[10px] font-bold text-slate-500 truncate", children: (isNegative ? "-" : "") + decimalInches + '"' })
                    ]
                  }),
                  p.jsxs("div", {
                    children: [
                      p.jsx("p", { className: "text-[9px] uppercase font-bold text-slate-400 tracking-wider", children: "Millimeters" }),
                      p.jsx("p", { className: "text-xs font-black text-teal-700 truncate", children: (isNegative ? "-" : "") + metricMm + " mm" }),
                      p.jsx("p", { className: "text-[10px] font-bold text-slate-500 truncate", children: (isNegative ? "-" : "") + metricCm + " cm" })
                    ]
                  })
                ]
              })
            ]
          })
        ]
      }),

      // Quick Suta Fractional Shortcut Chips Bar
      p.jsxs("div", {
        className: "px-4 py-1.5 flex items-center gap-1.5 overflow-x-auto no-scrollbar",
        children: [
          p.jsx("span", { className: "text-[10px] font-black uppercase text-slate-400 tracking-wider flex-shrink-0 mr-0.5", children: "Quick .Suta:" }),
          [
            { s: "1", label: ".1 (⅛\")" },
            { s: "2", label: ".2 (¼\")" },
            { s: "3", label: ".3 (⅜\")" },
            { s: "4", label: ".4 (½\")" },
            { s: "5", label: ".5 (⅝\")" },
            { s: "6", label: ".6 (¾\")" },
            { s: "7", label: ".7 (⅞\")" }
          ].map(chip => p.jsx("button", {
            key: chip.s,
            type: "button",
            onClick: () => handleSutaQuick(chip.s),
            className: "flex-shrink-0 px-2.5 py-1 rounded-full bg-white hover:bg-teal-50 border border-slate-200 hover:border-teal-400 text-slate-700 font-bold text-[11px] shadow-sm transition-all active:scale-95",
            children: chip.label
          }))
        ]
      }),

      // Professional Calculator Keypad (4 columns x 5 rows)
      p.jsxs("div", {
        className: "p-4 grid grid-cols-4 gap-2.5 bg-white border-t border-slate-200/80 shadow-[0_-4px_20px_rgba(0,0,0,0.03)]",
        children: [
          // Row 1: Clear, PlusMinus, Backspace, Divide
          p.jsx("button", {
            type: "button",
            onClick: handleAllClear,
            className: "h-14 rounded-2xl bg-red-50 hover:bg-red-100 text-red-600 font-black text-base border border-red-100 transition-all active:scale-95 shadow-sm",
            children: "AC"
          }),
          p.jsx("button", {
            type: "button",
            onClick: handleClear,
            className: "h-14 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-base border border-slate-200/60 transition-all active:scale-95 shadow-sm",
            children: "C"
          }),
          p.jsx("button", {
            type: "button",
            onClick: handlePlusMinus,
            className: "h-14 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-lg border border-slate-200/60 transition-all active:scale-95 shadow-sm",
            children: "±"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleOperator("/"),
            className: "h-14 rounded-2xl font-black text-2xl transition-all active:scale-95 shadow-sm " + (operator === "/" ? "bg-teal-600 text-white shadow-teal-600/30" : "bg-teal-50 hover:bg-teal-100 text-teal-700 border border-teal-200"),
            children: "÷"
          }),

          // Row 2: 7, 8, 9, Multiply
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("7"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "7"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("8"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "8"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("9"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "9"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleOperator("*"),
            className: "h-14 rounded-2xl font-black text-2xl transition-all active:scale-95 shadow-sm " + (operator === "*" ? "bg-teal-600 text-white shadow-teal-600/30" : "bg-teal-50 hover:bg-teal-100 text-teal-700 border border-teal-200"),
            children: "×"
          }),

          // Row 3: 4, 5, 6, Subtract
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("4"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "4"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("5"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "5"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("6"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "6"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleOperator("-"),
            className: "h-14 rounded-2xl font-black text-2xl transition-all active:scale-95 shadow-sm " + (operator === "-" ? "bg-teal-600 text-white shadow-teal-600/30" : "bg-teal-50 hover:bg-teal-100 text-teal-700 border border-teal-200"),
            children: "−"
          }),

          // Row 4: 1, 2, 3, Add
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("1"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "1"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("2"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "2"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("3"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "3"
          }),
          p.jsx("button", {
            type: "button",
            onClick: () => handleOperator("+"),
            className: "h-14 rounded-2xl font-black text-2xl transition-all active:scale-95 shadow-sm " + (operator === "+" ? "bg-teal-600 text-white shadow-teal-600/30" : "bg-teal-50 hover:bg-teal-100 text-teal-700 border border-teal-200"),
            children: "+"
          }),

          // Row 5: 0, Suta Dot, Backspace, Equals
          p.jsx("button", {
            type: "button",
            onClick: () => handleDigit("0"),
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-2xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "0"
          }),
          p.jsx("button", {
            type: "button",
            onClick: handleDot,
            className: "h-14 rounded-2xl bg-slate-50 hover:bg-white text-slate-900 font-black text-3xl border border-slate-200/80 transition-all active:scale-95 shadow-sm hover:shadow",
            children: "."
          }),
          p.jsx("button", {
            type: "button",
            onClick: handleBackspace,
            className: "h-14 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-lg border border-slate-200/60 transition-all active:scale-95 shadow-sm",
            title: "Backspace",
            children: "⌫"
          }),
          p.jsx("button", {
            type: "button",
            onClick: handleEquals,
            className: "h-14 rounded-2xl bg-teal-600 hover:bg-teal-700 active:bg-teal-800 text-white font-black text-3xl transition-all active:scale-95 shadow-lg shadow-teal-600/30",
            children: "="
          })
        ]
      }),

      // History Tape Drawer / Modal
      isHistoryOpen && p.jsx("div", {
        className: "fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fadeIn",
        onClick: () => setIsHistoryOpen(false),
        children: p.jsxs("div", {
          className: "w-full max-w-sm bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[80vh]",
          onClick: e => e.stopPropagation(),
          children: [
            p.jsxs("div", {
              className: "p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50",
              children: [
                p.jsxs("div", {
                  className: "flex items-center gap-2",
                  children: [
                    p.jsx("div", { className: "w-2 h-4 bg-teal-600 rounded-full" }),
                    p.jsx("h3", { className: "font-black text-sm text-slate-900 uppercase", children: "Calculation Tape" })
                  ]
                }),
                p.jsxs("div", {
                  className: "flex items-center gap-2",
                  children: [
                    history.length > 0 && p.jsx("button", {
                      type: "button",
                      onClick: () => { setHistory([]); showToast("History cleared"); },
                      className: "text-[11px] font-bold text-red-500 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50 transition-colors",
                      children: "Clear All"
                    }),
                    p.jsx("button", {
                      type: "button",
                      onClick: () => setIsHistoryOpen(false),
                      className: "w-7 h-7 rounded-full hover:bg-slate-200 flex items-center justify-center text-slate-400 font-bold text-xs",
                      children: "✕"
                    })
                  ]
                })
              ]
            }),
            p.jsx("div", {
              className: "p-4 flex-1 overflow-y-auto space-y-2.5",
              children: history.length === 0 ? p.jsx("div", {
                className: "text-center py-10 text-slate-400 text-xs font-bold",
                children: "No calculations yet. Perform math to see tape here."
              }) : history.map(item => p.jsxs("div", {
                key: item.id,
                onClick: () => handleRestore(item),
                className: "p-3 rounded-2xl border border-slate-100 bg-slate-50 hover:bg-teal-50/60 hover:border-teal-200 transition-all cursor-pointer group active:scale-[0.99]",
                children: [
                  p.jsxs("div", {
                    className: "flex items-center justify-between text-xs text-slate-400 font-medium mb-1",
                    children: [
                      p.jsx("span", { className: "truncate max-w-[200px]", children: item.expression }),
                      p.jsx("span", { className: "text-[10px]", children: item.timestamp })
                    ]
                  }),
                  p.jsxs("div", {
                    className: "flex items-baseline justify-between",
                    children: [
                      p.jsxs("span", { className: "text-xl font-black text-slate-900 group-hover:text-teal-700", children: ["= ", item.result] }),
                      item.desc && p.jsx("span", { className: "text-[11px] font-bold text-teal-600", children: item.desc })
                    ]
                  })
                ]
              }))
            }),
            p.jsx("div", {
              className: "p-3 border-t border-slate-100 text-center bg-slate-50",
              children: p.jsx("button", {
                type: "button",
                onClick: () => setIsHistoryOpen(false),
                className: "w-full py-2 bg-white hover:bg-slate-100 border border-slate-200 text-slate-700 font-bold text-xs rounded-xl transition-colors",
                children: "Close"
              })
            })
          ]
        })
      }),

      // Suta Reference Guide Modal
      isGuideOpen && p.jsx("div", {
        className: "fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fadeIn",
        onClick: () => setIsGuideOpen(false),
        children: p.jsxs("div", {
          className: "w-full max-w-sm bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[85vh]",
          onClick: e => e.stopPropagation(),
          children: [
            p.jsxs("div", {
              className: "p-4 border-b border-slate-100 flex items-center justify-between bg-teal-600 text-white",
              children: [
                p.jsxs("div", {
                  children: [
                    p.jsx("h3", { className: "font-black text-sm tracking-tight", children: "Base-8 Suta Point Math Guide" }),
                    p.jsx("p", { className: "text-[10px] text-teal-100 font-medium", children: "Standard Bangladesh & Indian Aluminium Suta Rules" })
                  ]
                }),
                p.jsx("button", {
                  type: "button",
                  onClick: () => setIsGuideOpen(false),
                  className: "w-7 h-7 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center text-white font-bold text-xs",
                  children: "✕"
                })
              ]
            }),
            p.jsxs("div", {
              className: "p-4 overflow-y-auto space-y-3",
              children: [
                p.jsx("div", {
                  className: "p-3 bg-teal-50 rounded-2xl border border-teal-100 text-xs text-teal-900 leading-relaxed",
                  children: "In Thai & Aluminium fabrication, 1 Inch is divided into 8 Sutas (সুতা). In this calculator, the number after the point represents sutas (e.g. 10.4 = 10 inches and 4 sutas = 10 ½\")."
                }),
                p.jsx("div", {
                  className: "overflow-x-auto",
                  children: p.jsxs("table", {
                    className: "w-full text-center text-xs border-collapse",
                    children: [
                      p.jsx("thead", {
                        children: p.jsxs("tr", {
                          className: "bg-slate-100 text-slate-700 font-bold",
                          children: [
                            p.jsx("th", { className: "p-2 border border-slate-200", children: "Suta" }),
                            p.jsx("th", { className: "p-2 border border-slate-200", children: "Point" }),
                            p.jsx("th", { className: "p-2 border border-slate-200", children: "Inch" }),
                            p.jsx("th", { className: "p-2 border border-slate-200", children: "MM" })
                          ]
                        })
                      }),
                      p.jsx("tbody", {
                        children: sutaReference.map(row => p.jsxs("tr", {
                          key: row.suta,
                          className: "border-b border-slate-100 hover:bg-slate-50",
                          children: [
                            p.jsx("td", { className: "p-2 border border-slate-200 font-black text-slate-800", children: row.suta }),
                            p.jsx("td", { className: "p-2 border border-slate-200 font-mono font-bold text-teal-700", children: row.pt }),
                            p.jsx("td", { className: "p-2 border border-slate-200 font-medium", children: row.frac }),
                            p.jsx("td", { className: "p-2 border border-slate-200 font-mono text-slate-500", children: row.mm })
                          ]
                        }))
                      })
                    ]
                  })
                })
              ]
            }),
            p.jsx("div", {
              className: "p-3 border-t border-slate-100 text-center bg-slate-50",
              children: p.jsx("button", {
                type: "button",
                onClick: () => setIsGuideOpen(false),
                className: "w-full py-2 bg-teal-600 hover:bg-teal-700 text-white font-bold text-xs rounded-xl transition-colors shadow-sm",
                children: "Got it"
              })
            })
          ]
        })
      })
    ]
  });
}`;

// Read the current index JS
const bundlePath = 'app/src/main/assets/assets/index-C_ZGULEN.js';
let c = fs.readFileSync(bundlePath, 'utf8');

const startHj = c.indexOf('function hj()');
const endHj = c.indexOf('function mj()');
if (startHj === -1 || endHj === -1) {
  throw new Error('Could not find hj boundary in ' + bundlePath);
}

// Replace the old hj implementation with our new world-class hj
c = c.slice(0, startHj) + newHjComponent + c.slice(endHj);

// Validate with SourceTextModule
try {
  new vm.SourceTextModule(c);
  console.log('VALIDATION SUCCESSFUL: 100% syntactically valid ES module!');
} catch (e) {
  console.error('Validation failed:', e);
  process.exit(1);
}

// Write to both assets
fs.writeFileSync('app/src/main/assets/assets/index-C_ZGULEN.js', c);
fs.writeFileSync('app/src/main/assets/public/assets/index-C_ZGULEN.js', c);
console.log('Successfully written new Base-8 calculator to both bundle files!');

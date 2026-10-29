import os
import subprocess
import tempfile

TARGET_OLD = 'p.jsxs("header",{className:"sticky top-0 z-50 flex items-center justify-between px-4 md:px-8 py-3.5 bg-white/95 backdrop-blur-md border-b border-slate-100 shrink-0",children:[p.jsxs("div",{className:"flex items-center gap-2",children:[p.jsx("img",{src:"/logo.png?v=2",alt:"Logo",className:"w-7 h-7 md:w-8 md:h-8 rounded-lg shadow-sm border border-slate-100",referrerPolicy:"no-referrer"}),p.jsx("span",{className:"font-black text-slate-800 text-[11px] md:text-sm tracking-wider uppercase",children:"Thai Fabricator"})]}),p.jsxs("div",{className:"flex items-center gap-6 md:gap-8",children:[p.jsx("button",{onClick:()=>l("app1"),className:_f("text-sm md:text-base font-bold uppercase tracking-wider transition-all duration-200 active:scale-95",i==="app1"?"text-slate-900 scale-105 border-b-2 border-slate-900 pb-0.5":"text-slate-400 hover:text-slate-600"),children:"Thai"}),p.jsx("button",{onClick:()=>l("app2"),className:_f("text-sm md:text-base font-bold uppercase tracking-wider transition-all duration-200 active:scale-95",i==="app2"?"text-slate-900 scale-105 border-b-2 border-slate-900 pb-0.5":"text-slate-400 hover:text-slate-600"),children:"Base-8"})]})]}),'

REPLACEMENT_NEW = 'p.jsxs("header",{className:"sticky top-0 z-50 flex items-center justify-between px-3 md:px-6 py-2 bg-white/95 backdrop-blur-md border-b border-slate-100 shrink-0",children:[p.jsxs("div",{className:"flex items-center gap-1.5",children:[p.jsx("img",{src:"/logo.png?v=2",alt:"Logo",className:"w-6 h-6 md:w-7 md:h-7 rounded-md shadow-xs border border-slate-100",referrerPolicy:"no-referrer"}),p.jsx("span",{className:"font-black text-slate-800 text-xs md:text-sm tracking-wider uppercase",children:"Thai Fabricator"})]}),p.jsxs("div",{className:"flex items-center p-1 bg-slate-100/90 rounded-xl border border-slate-200/60 shadow-xs",children:[p.jsx("button",{type:"button",onClick:()=>l("app1"),className:"px-3.5 py-1 rounded-lg text-xs font-bold transition-all duration-150 "+(i==="app1"?"bg-teal-600 text-white shadow-xs":"text-slate-600 hover:text-slate-900"),children:"Thai"}),p.jsx("button",{type:"button",onClick:()=>l("app2"),className:"px-3.5 py-1 rounded-lg text-xs font-bold transition-all duration-150 "+(i==="app2"?"bg-teal-600 text-white shadow-xs":"text-slate-600 hover:text-slate-900"),children:"Base-8"})]})]}),'

def main():
    target_files = [
        "app/src/main/assets/assets/index-C_ZGULEN.js",
        "app/src/main/assets/public/assets/index-C_ZGULEN.js"
    ]

    for tf in target_files:
        if not os.path.exists(tf):
            print(f"Skipping {tf}: not found")
            continue

        with open(tf, "r", encoding="utf-8") as f:
            content = f.read()

        if TARGET_OLD not in content:
            raise RuntimeError(f"Could not find TARGET_OLD in {tf}")

        new_content = content.replace(TARGET_OLD, REPLACEMENT_NEW, 1)

        # Validate with node
        with tempfile.NamedTemporaryFile("w", suffix=".mjs", delete=False) as tmp:
            tmp.write(new_content)
            tmp_path = tmp.name

        try:
            val_res = subprocess.run(
                ["node", "--check", tmp_path],
                capture_output=True,
                text=True
            )
            if val_res.returncode != 0:
                print("Node syntax validation FAILED:")
                print(val_res.stderr)
                os.remove(tmp_path)
                return 1
            print(f"Validation PASSED for {tf}!")
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

        with open(tf, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Successfully updated {tf}")

    print("All files updated successfully with new compact header and switching dongle!")
    return 0

if __name__ == "__main__":
    exit(main())

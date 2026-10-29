import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '@/src/lib/utils';

interface LayoutProps {
  children: React.ReactNode;
  activeService: 'app1' | 'app2';
  onSwitch: (service: 'app1' | 'app2') => void;
  onExit: () => void;
}

export default function Layout({ children, activeService, onSwitch, onExit }: LayoutProps) {
  return (
    <div className="flex flex-col h-screen bg-slate-50 overflow-hidden pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)] pl-[env(safe-area-inset-left)] pr-[env(safe-area-inset-right)] select-none">
      {/* Optimized Top Header: Brand on Left, Stylish Thai & Base-8 Switcher Dongle on Right */}
      <header className="sticky top-0 z-50 flex items-center justify-between px-4 md:px-8 py-3 bg-white/95 backdrop-blur-md border-b border-slate-100 shrink-0">
        {/* Left: Brand Icon & Title */}
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-center text-teal-600 shadow-sm">
            <img 
              src="/logo.png?v=2" 
              alt="Logo" 
              className="w-5 h-5 rounded-md object-contain" 
              referrerPolicy="no-referrer" 
            />
          </div>
          <span className="font-black text-slate-800 text-xs md:text-sm tracking-wider uppercase">Thai Fabricator</span>
        </div>

        {/* Right: Stylish Thai & Base-8 Switcher Dongle in Light Mode */}
        <div className="flex items-center bg-slate-100 p-1 rounded-full border border-slate-200/90 shadow-inner gap-1">
          <button
            onClick={() => onSwitch('app1')}
            className={cn(
              "relative flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95",
              activeService === 'app1'
                ? "bg-white text-slate-900 border border-slate-200 shadow-sm"
                : "text-slate-400 hover:text-slate-700"
            )}
          >
            <span className={cn("w-1.5 h-1.5 rounded-full transition-all duration-200", activeService === 'app1' ? "bg-teal-500 shadow-sm shadow-teal-500/50" : "bg-transparent")} />
            <span>Thai</span>
          </button>
          <button
            onClick={() => onSwitch('app2')}
            className={cn(
              "relative flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider transition-all duration-200 active:scale-95",
              activeService === 'app2'
                ? "bg-white text-slate-900 border border-slate-200 shadow-sm"
                : "text-slate-400 hover:text-slate-700"
            )}
          >
            <span className={cn("w-1.5 h-1.5 rounded-full transition-all duration-200", activeService === 'app2' ? "bg-teal-500 shadow-sm shadow-teal-500/50" : "bg-transparent")} />
            <span>Base-8</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 relative overflow-auto">
        <AnimatePresence mode="wait">
          <motion.div
            key={activeService}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="h-full"
          >
            {children}
          </motion.div>
        </AnimatePresence>
      </main>
    </div>
  );
}

import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Video, Bot, Disc, FileText, Settings, PlusCircle } from 'lucide-react';

export default function Sidebar({ onOpenNewMeeting }) {
  const navItems = [
    { to: '/dashboard', label: 'Overview', icon: LayoutDashboard },
    { to: '/meetings', label: 'Meetings Workspace', icon: Video },
    { to: '/ai-rep', label: 'AI Rep Studio', icon: Bot },
    { to: '/recordings', label: 'Recordings Vault', icon: Disc },
  ];

  return (
    <aside className="w-64 glass-panel border-r border-white/10 flex flex-col justify-between hidden md:flex shrink-0 min-h-[calc(100vh-65px)] p-4">
      <div className="space-y-6">
        <button
          onClick={onOpenNewMeeting}
          className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-indigo-600 to-cyan-500 hover:from-indigo-500 hover:to-cyan-400 text-white font-semibold flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/25 transition-all transform active:scale-95 cursor-pointer"
        >
          <PlusCircle className="w-5 h-5" />
          <span>New Meeting</span>
        </button>

        <nav className="space-y-1">
          <p className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Main Menu</p>
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3.5 py-2.5 rounded-xl font-medium text-sm transition-all ${
                    isActive
                      ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 shadow-inner'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                  }`
                }
              >
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </div>

      <div className="pt-4 border-t border-white/5">
        <div className="p-3.5 rounded-xl bg-slate-900/60 border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Engine</span>
            <span className="text-indigo-400 font-mono">v1.0.0-PRO</span>
          </div>
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Kafka Events</span>
            <span className="text-emerald-400 font-mono">Online</span>
          </div>
        </div>
      </div>
    </aside>
  );
}

import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { useNavigate } from 'react-router-dom';
import { 
  Video, Clock, FileText, Bot, Plus, Play, CheckCircle2, 
  Calendar, Users, ArrowUpRight, Sparkles, Loader2, RefreshCw
} from 'lucide-react';

export default function Dashboard({ onOpenNewMeeting }) {
  const [meetings, setMeetings] = useState([]);
  const [aiRep, setAiRep] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const navigate = useNavigate();

  const fetchDashboardData = async () => {
    try {
      setRefreshing(true);
      const meetingsRes = await api.get('/meetings');
      setMeetings(meetingsRes.data || []);

      try {
        const aiRepRes = await api.get('/ai-representatives/me');
        setAiRep(aiRepRes.data);
      } catch (err) {
        // AI rep might not be initialized yet
      }
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const totalMeetings = meetings.length;
  const activeMeetings = meetings.filter((m) => m.status === 'IN_PROGRESS' || m.status === 'LIVE');
  const endedMeetings = meetings.filter((m) => m.status === 'ENDED' || m.status === 'COMPLETED');

  return (
    <div className="space-y-8 pb-12">
      {/* Top Banner */}
      <div className="glass-panel rounded-2xl p-8 border border-white/10 relative overflow-hidden bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900">
        <div className="absolute right-0 top-0 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5" />
              <span>AI Intelligence Engine Active</span>
            </div>
            <h1 className="text-3xl font-extrabold text-white tracking-tight">Meeting Control Center</h1>
            <p className="text-slate-400 text-sm max-w-xl">
              Monitor active sessions, review AI-generated transcripts, inspect meeting sentiment, and manage your autonomous AI representative.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchDashboardData}
              disabled={refreshing}
              className="p-3 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-300 border border-white/10 transition-all cursor-pointer"
              title="Refresh Dashboard"
            >
              <RefreshCw className={`w-5 h-5 ${refreshing ? 'animate-spin' : ''}`} />
            </button>

            <button
              onClick={onOpenNewMeeting}
              className="py-3 px-5 rounded-xl bg-gradient-to-r from-indigo-600 to-cyan-500 hover:from-indigo-500 hover:to-cyan-400 text-white font-bold flex items-center gap-2 shadow-lg shadow-indigo-500/30 transition-all transform active:scale-95 cursor-pointer"
            >
              <Plus className="w-5 h-5" />
              <span>Start Instant Meeting</span>
            </button>
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="glass-card rounded-2xl p-6 border border-white/10 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Meetings</span>
            <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <Video className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white mt-4">{loading ? '...' : totalMeetings}</p>
          <p className="text-xs text-emerald-400 mt-2 flex items-center gap-1 font-medium">
            <CheckCircle2 className="w-3.5 h-3.5" /> All synced with Spring Boot
          </p>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-white/10 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Live & Active</span>
            <div className="p-2.5 rounded-xl bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white mt-4">{loading ? '...' : activeMeetings.length}</p>
          <p className="text-xs text-cyan-400 mt-2 font-medium">
            {activeMeetings.length > 0 ? '🟢 Session currently live' : 'No active sessions'}
          </p>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-white/10 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Transcripts & AI</span>
            <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <FileText className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white mt-4">{loading ? '...' : endedMeetings.length}</p>
          <p className="text-xs text-slate-400 mt-2">Kafka event pipeline active</p>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-white/10 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">AI Representative</span>
            <div className="p-2.5 rounded-xl bg-violet-500/10 text-violet-400 border border-violet-500/20">
              <Bot className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white mt-4">
            {aiRep?.proxyModeEnabled ? 'ACTIVE' : 'READY'}
          </p>
          <p className="text-xs text-violet-400 mt-2 font-medium">
            {aiRep?.personaName || 'Persona Configured'}
          </p>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Meetings List */}
        <div className="lg:col-span-2 space-y-6">
          <div className="glass-panel rounded-2xl p-6 border border-white/10">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-xl font-bold text-white tracking-tight">Recent & Scheduled Meetings</h2>
                <p className="text-xs text-slate-400 mt-1">Manage, join, or view AI analytics for your sessions</p>
              </div>
              <span className="text-xs px-3 py-1 rounded-full bg-slate-800 text-slate-300 border border-white/5 font-semibold">
                {meetings.length} Total
              </span>
            </div>

            {loading ? (
              <div className="py-16 text-center text-slate-400 space-y-3">
                <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-400" />
                <p className="text-sm">Fetching meetings from Spring Boot backend...</p>
              </div>
            ) : meetings.length === 0 ? (
              <div className="py-16 text-center text-slate-400 space-y-4 bg-slate-900/40 rounded-xl border border-dashed border-white/10">
                <Video className="w-12 h-12 text-slate-600 mx-auto" />
                <div>
                  <p className="text-base font-semibold text-white">No meetings created yet</p>
                  <p className="text-xs text-slate-400 mt-1">Click "Start Instant Meeting" to create your first session.</p>
                </div>
                <button
                  onClick={onOpenNewMeeting}
                  className="py-2.5 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm inline-flex items-center gap-2"
                >
                  <Plus className="w-4 h-4" />
                  <span>Create Meeting</span>
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {meetings.map((meeting) => (
                  <div
                    key={meeting.id}
                    className="p-4 rounded-xl bg-slate-900/60 hover:bg-slate-800/80 border border-white/5 hover:border-indigo-500/30 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 group"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2.5">
                        <h3 className="font-semibold text-white text-base group-hover:text-indigo-300 transition-colors">
                          {meeting.title}
                        </h3>
                        <span
                          className={`text-[10px] uppercase font-extrabold px-2 py-0.5 rounded-md border ${
                            meeting.status === 'IN_PROGRESS' || meeting.status === 'LIVE'
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30 animate-pulse'
                              : 'bg-slate-800 text-slate-400 border-slate-700'
                          }`}
                        >
                          {meeting.status}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 line-clamp-1">{meeting.description || 'No description provided.'}</p>
                      <div className="flex items-center gap-4 text-[11px] text-slate-500 pt-1">
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5" />
                          {meeting.scheduledStartTime ? new Date(meeting.scheduledStartTime).toLocaleString() : 'Now'}
                        </span>
                        <span className="flex items-center gap-1">
                          <Users className="w-3.5 h-3.5" />
                          ID: #{meeting.id}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => navigate(`/room/${meeting.id}`)}
                        className="py-2 px-3.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600 text-indigo-300 hover:text-white border border-indigo-500/30 font-medium text-xs flex items-center gap-1.5 transition-all cursor-pointer"
                      >
                        <Play className="w-3.5 h-3.5" />
                        <span>Join Room</span>
                      </button>

                      <button
                        onClick={() => navigate(`/meetings/${meeting.id}`)}
                        className="py-2 px-3.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-white/10 font-medium text-xs flex items-center gap-1.5 transition-all cursor-pointer"
                      >
                        <FileText className="w-3.5 h-3.5 text-cyan-400" />
                        <span>AI Intelligence</span>
                        <ArrowUpRight className="w-3.5 h-3.5 text-slate-400" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Col: AI Rep Card & Quick Tips */}
        <div className="space-y-6">
          <div className="glass-panel rounded-2xl p-6 border border-white/10 bg-gradient-to-b from-slate-900 to-slate-950 relative overflow-hidden">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 rounded-xl bg-violet-500/10 border border-violet-500/20 text-violet-400 glow-indigo">
                <Bot className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-white text-lg">AI Representative</h3>
                <p className="text-xs text-slate-400">Autonomous meeting proxy</p>
              </div>
            </div>

            <p className="text-xs text-slate-300 leading-relaxed mb-4">
              Configure your personal AI agent to attend meetings, represent your interests, take live notes, and respond to key topics on your behalf.
            </p>

            <div className="p-3.5 rounded-xl bg-slate-900 border border-white/5 mb-5 space-y-2">
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-400">Persona Name</span>
                <span className="text-white font-semibold">{aiRep?.personaName || 'Rahul\'s AI Proxy'}</span>
              </div>
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-400">Proxy Mode</span>
                <span className={`font-semibold ${aiRep?.proxyModeEnabled ? 'text-emerald-400' : 'text-slate-400'}`}>
                  {aiRep?.proxyModeEnabled ? 'ENABLED' : 'DISABLED'}
                </span>
              </div>
            </div>

            <button
              onClick={() => navigate('/ai-rep')}
              className="w-full py-2.5 px-4 rounded-xl bg-violet-600/20 hover:bg-violet-600 text-violet-300 hover:text-white border border-violet-500/30 font-semibold text-xs flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <span>Configure AI Representative Studio</span>
              <ArrowUpRight className="w-4 h-4" />
            </button>
          </div>

          <div className="glass-card rounded-2xl p-6 border border-white/10 space-y-3">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-cyan-400" />
              <span>Supported Capabilities</span>
            </h4>
            <ul className="text-xs text-slate-400 space-y-2">
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-400"></span>
                Instant AI Transcription & Speaker Diarization
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-cyan-400"></span>
                Action Item Extraction & Key Decision Summaries
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                Multilingual Real-time Live Translation
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

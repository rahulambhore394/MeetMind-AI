import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { Disc, Play, Download, Sparkles, FileText, Search, Loader2 } from 'lucide-react';

export default function Recordings() {
  const [recordings, setRecordings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchRecordings = async () => {
    try {
      setLoading(true);
      const res = await api.get('/meetings');
      const meetingsList = res.data || [];
      
      const allRecs = [];
      for (const m of meetingsList) {
        try {
          const recRes = await api.get(`/meetings/${m.id}/recordings`);
          if (recRes.data && recRes.data.length > 0) {
            recRes.data.forEach((r) => {
              allRecs.push({ ...r, meetingTitle: m.title, meetingId: m.id });
            });
          }
        } catch (e) {
          // No recs for this meeting
        }
      }

      setRecordings(allRecs);
    } catch (err) {
      console.error('Failed to fetch recordings vault:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecordings();
  }, []);

  return (
    <div className="space-y-6 pb-16">
      {/* Header */}
      <div className="glass-panel rounded-2xl p-8 border border-white/10 relative overflow-hidden bg-gradient-to-r from-slate-900 via-cyan-950/30 to-slate-900">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold">
            <Disc className="w-3.5 h-3.5" />
            <span>Media Vault & Stream Archive</span>
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Recordings Vault</h1>
          <p className="text-slate-400 text-sm max-w-xl">
            Access and replay all recorded audio and video streams across your meeting history.
          </p>
        </div>
      </div>

      {/* Main Grid */}
      <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <h2 className="text-lg font-bold text-white">Stored Session Files</h2>
          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search recordings..."
              className="w-full pl-9 pr-4 py-2 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-cyan-500"
            />
          </div>
        </div>

        {loading ? (
          <div className="py-16 text-center text-slate-400 space-y-3">
            <Loader2 className="w-8 h-8 animate-spin mx-auto text-cyan-400" />
            <p className="text-sm">Fetching recorded sessions from storage...</p>
          </div>
        ) : recordings.length === 0 ? (
          <div className="py-16 text-center text-slate-400 space-y-3 bg-slate-900/40 rounded-xl border border-dashed border-white/10">
            <Disc className="w-12 h-12 text-slate-600 mx-auto" />
            <div>
              <p className="text-base font-semibold text-white">No recordings archived yet</p>
              <p className="text-xs text-slate-400 mt-1">Start a meeting session and trigger recording to archive stream files here.</p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {recordings.map((rec) => (
              <div key={rec.id} className="p-5 rounded-2xl bg-slate-900/80 border border-white/10 space-y-4 hover:border-cyan-500/30 transition-all">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="font-bold text-white text-base">{rec.meetingTitle}</h3>
                    <p className="text-xs text-slate-400">Recording ID: #{rec.id} • Meeting #{rec.meetingId}</p>
                  </div>
                  <span className="text-[10px] px-2.5 py-1 rounded-md bg-cyan-500/10 text-cyan-300 font-mono border border-cyan-500/20">
                    {rec.format || 'MP4 / WAV'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/80 border border-white/5 space-y-2">
                  <div className="flex justify-between text-xs text-slate-400">
                    <span>Playback Stream</span>
                    <span>Duration: {rec.durationSeconds || 0}s</span>
                  </div>
                  {rec.fileUrl ? (
                    <audio controls className="w-full">
                      <source src={rec.fileUrl} type="audio/wav" />
                    </audio>
                  ) : (
                    <div className="py-4 text-center text-xs text-slate-500 italic">
                      Audio file stream active in Spring Boot media storage
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

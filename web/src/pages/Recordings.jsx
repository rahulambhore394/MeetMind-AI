import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { Disc, Play, Download, Sparkles, FileText, Search, Loader2 } from 'lucide-react';

export default function Recordings() {
  const [recordings, setRecordings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  const [activePlaybackUrl, setActivePlaybackUrl] = useState(null);
  const [playingRec, setPlayingRec] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);

  const handlePlay = async (rec) => {
    try {
      setDownloadingId(rec.id);
      const response = await api.get(`/meetings/${rec.meetingId}/recordings/${rec.id}/download`, {
        responseType: 'blob'
      });
      const blob = new Blob([response.data], { type: 'video/mp4' });
      const url = window.URL.createObjectURL(blob);
      setActivePlaybackUrl(url);
      setPlayingRec(rec);
    } catch (err) {
      alert('Playback load error: ' + (err.response?.data?.message || err.message));
    } finally {
      setDownloadingId(null);
    }
  };

  const handleDownload = async (rec) => {
    try {
      setDownloadingId(rec.id);
      const response = await api.get(`/meetings/${rec.meetingId}/recordings/${rec.id}/download`, {
        responseType: 'blob'
      });
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `recording_${rec.meetingId}_${rec.id}.mp4`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Download failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setDownloadingId(null);
    }
  };

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
                  <span className={`text-[10px] px-2.5 py-1 rounded-md font-mono border ${
                    rec.status === 'COMPLETED' 
                      ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20' 
                      : rec.status === 'PROCESSING' 
                      ? 'bg-cyan-500/10 text-cyan-300 border-cyan-500/20 animate-pulse'
                      : 'bg-rose-500/10 text-rose-300 border-rose-500/20'
                  }`}>
                    {rec.status || 'SAVED'}
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-white/5 space-y-3">
                  <div className="flex justify-between text-xs text-slate-400">
                    <span>Started: {rec.startedAt ? new Date(rec.startedAt).toLocaleTimeString() : 'Recently'}</span>
                    <span>Duration: {rec.duration ? `${rec.duration}s` : 'Recorded'}</span>
                  </div>

                  <div className="flex items-center gap-2 pt-1">
                    <button
                      onClick={() => handlePlay(rec)}
                      disabled={downloadingId === rec.id}
                      className="flex-1 py-2 px-3 rounded-xl bg-cyan-600/20 hover:bg-cyan-600/30 text-cyan-300 border border-cyan-500/30 text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50"
                    >
                      {downloadingId === rec.id ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Play className="w-3.5 h-3.5" />
                      )}
                      <span>Play Media</span>
                    </button>

                    <button
                      onClick={() => handleDownload(rec)}
                      disabled={downloadingId === rec.id}
                      className="py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer disabled:opacity-50"
                      title="Download Recording File"
                    >
                      <Download className="w-3.5 h-3.5" />
                      <span className="hidden sm:inline">Download</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Playback Modal */}
      {activePlaybackUrl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-fade-in">
          <div className="w-full max-w-xl bg-slate-900 border border-white/10 rounded-2xl p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <div>
                <h3 className="font-bold text-white text-base">Replaying Recording #{playingRec?.id}</h3>
                <p className="text-xs text-slate-400">{playingRec?.meetingTitle}</p>
              </div>
              <button
                onClick={() => {
                  setActivePlaybackUrl(null);
                  setPlayingRec(null);
                }}
                className="text-slate-400 hover:text-white text-sm font-bold px-2 py-1 cursor-pointer"
              >
                ✕ Close
              </button>
            </div>

            <div className="bg-black rounded-xl overflow-hidden flex items-center justify-center min-h-[200px]">
              <video
                controls
                autoPlay
                src={activePlaybackUrl}
                className="w-full max-h-[360px] object-contain"
              >
                Your browser does not support video playback.
              </video>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

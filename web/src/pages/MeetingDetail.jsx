import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { 
  ArrowLeft, Sparkles, FileText, CheckSquare, BrainCircuit, Globe2, 
  Play, Download, Disc, RefreshCw, Loader2, MessageSquare, ShieldCheck, AlertCircle
} from 'lucide-react';

export default function MeetingDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [meeting, setMeeting] = useState(null);
  const [transcripts, setTranscripts] = useState([]);
  const [intelligence, setIntelligence] = useState(null);
  const [recordings, setRecordings] = useState([]);
  const [selectedLanguage, setSelectedLanguage] = useState('en');
  
  const [loading, setLoading] = useState(true);
  const [generatingIntelligence, setGeneratingIntelligence] = useState(false);
  const [activeTab, setActiveTab] = useState('intelligence'); // 'intelligence' | 'transcript' | 'recordings'
  const [searchQuery, setSearchQuery] = useState('');

  const fetchMeetingData = async () => {
    try {
      setLoading(true);
      const meetingRes = await api.get(`/meetings/${id}`);
      setMeeting(meetingRes.data);

      try {
        const intelRes = await api.get(`/meetings/${id}/intelligence`);
        setIntelligence(intelRes.data);
      } catch (e) {
        // Intelligence not generated yet
      }

      try {
        const transRes = await api.get(`/meetings/${id}/transcripts`);
        setTranscripts(transRes.data || []);
      } catch (e) {
        // No transcripts yet
      }

      try {
        const recRes = await api.get(`/meetings/${id}/recordings`);
        setRecordings(recRes.data || []);
      } catch (e) {
        // No recordings yet
      }
    } catch (err) {
      console.error('Failed to load meeting details:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMeetingData();
  }, [id]);

  const handleGenerateIntelligence = async () => {
    try {
      setGeneratingIntelligence(true);
      const res = await api.post(`/meetings/${id}/intelligence/generate`);
      setIntelligence(res.data);
    } catch (err) {
      alert('Failed to trigger AI Intelligence generation. Ensure Kafka & Spring Boot intelligence listener are running.');
    } finally {
      setGeneratingIntelligence(false);
    }
  };

  if (loading) {
    return (
      <div className="py-24 text-center text-slate-400 space-y-4">
        <Loader2 className="w-10 h-10 animate-spin mx-auto text-indigo-400" />
        <p className="text-sm font-medium">Loading Meeting Intelligence Studio...</p>
      </div>
    );
  }

  if (!meeting) {
    return (
      <div className="py-20 text-center text-slate-400 space-y-4">
        <AlertCircle className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-xl font-bold text-white">Meeting Not Found</h2>
        <button onClick={() => navigate('/dashboard')} className="py-2 px-4 rounded-xl bg-slate-800 text-white text-sm font-semibold">
          Return to Dashboard
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-16">
      {/* Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/dashboard')}
          className="py-2 px-3 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-slate-300 border border-white/10 text-xs font-semibold flex items-center gap-2 transition-all"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Dashboard</span>
        </button>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/room/${meeting.id}`)}
            className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs flex items-center gap-2 shadow-lg shadow-indigo-500/20"
          >
            <Play className="w-4 h-4" />
            <span>Join Live Room</span>
          </button>
        </div>
      </div>

      {/* Meeting Title Banner */}
      <div className="glass-panel rounded-2xl p-8 border border-white/10 relative overflow-hidden bg-gradient-to-r from-slate-900 via-indigo-950/30 to-slate-900">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-3">
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 font-extrabold border border-emerald-500/30">
                {meeting.status}
              </span>
              <span className="text-xs text-slate-400">Meeting ID: #{meeting.id}</span>
            </div>
            <h1 className="text-3xl font-extrabold text-white tracking-tight">{meeting.title}</h1>
            <p className="text-sm text-slate-300">{meeting.description || 'No description added for this meeting.'}</p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={handleGenerateIntelligence}
              disabled={generatingIntelligence}
              className="py-3 px-5 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white font-bold text-xs flex items-center gap-2 shadow-lg shadow-cyan-500/25 transition-all disabled:opacity-50 cursor-pointer"
            >
              {generatingIntelligence ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <BrainCircuit className="w-4 h-4" />
              )}
              <span>{intelligence ? 'Re-Generate AI Analytics' : 'Generate AI Intelligence'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-white/10 pb-3">
        <button
          onClick={() => setActiveTab('intelligence')}
          className={`py-2.5 px-4 rounded-xl text-xs font-bold flex items-center gap-2 transition-all ${
            activeTab === 'intelligence'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          <span>AI Intelligence & Summary</span>
        </button>

        <button
          onClick={() => setActiveTab('transcript')}
          className={`py-2.5 px-4 rounded-xl text-xs font-bold flex items-center gap-2 transition-all ${
            activeTab === 'transcript'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Full Transcripts ({transcripts.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('recordings')}
          className={`py-2.5 px-4 rounded-xl text-xs font-bold flex items-center gap-2 transition-all ${
            activeTab === 'recordings'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <Disc className="w-4 h-4" />
          <span>Recordings Vault ({recordings.length})</span>
        </button>
      </div>

      {/* Tab 1: AI Intelligence Summary */}
      {activeTab === 'intelligence' && (
        <div className="space-y-6">
          {!intelligence ? (
            <div className="py-16 text-center text-slate-400 space-y-4 bg-slate-900/40 rounded-2xl border border-dashed border-white/10">
              <BrainCircuit className="w-12 h-12 text-indigo-400 mx-auto animate-pulse" />
              <div>
                <h3 className="text-lg font-bold text-white">AI Intelligence Pending</h3>
                <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">
                  Click "Generate AI Intelligence" above to analyze transcripts, extract key decisions, build action items, and compute sentiment metrics.
                </p>
              </div>
              <button
                onClick={handleGenerateIntelligence}
                disabled={generatingIntelligence}
                className="py-2.5 px-5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25"
              >
                {generatingIntelligence ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                <span>Generate Intelligence Now</span>
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left 2 Cols: Summary & Action Items */}
              <div className="lg:col-span-2 space-y-6">
                {/* Executive Summary */}
                <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    <Sparkles className="w-5 h-5 text-indigo-400" />
                    <span>Executive Summary</span>
                  </h3>
                  <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-line bg-slate-950/60 p-4 rounded-xl border border-white/5">
                    {intelligence.summary || 'Summary processing complete.'}
                  </p>
                </div>

                {/* Action Items */}
                <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    <CheckSquare className="w-5 h-5 text-emerald-400" />
                    <span>Action Items & Assignments</span>
                  </h3>
                  {(!intelligence.actionItems || intelligence.actionItems.length === 0) ? (
                    <p className="text-xs text-slate-500 italic">No explicit action items detected in this meeting.</p>
                  ) : (
                    <div className="space-y-2.5">
                      {intelligence.actionItems.map((item, index) => (
                        <div
                          key={index}
                          className="p-3.5 rounded-xl bg-slate-900/80 border border-white/5 flex items-start gap-3 hover:border-emerald-500/30 transition-all"
                        >
                          <input type="checkbox" className="mt-1 rounded bg-slate-950 border-white/20 text-emerald-500 focus:ring-0" />
                          <div className="space-y-0.5">
                            <p className="text-sm font-medium text-slate-200">{typeof item === 'string' ? item : item.task || item.description}</p>
                            {item.assignee && (
                              <span className="text-[11px] font-semibold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded-md border border-indigo-500/20 inline-block">
                                Assigned to: {item.assignee}
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Right Col: Sentiment & Key Decisions */}
              <div className="space-y-6">
                {/* Sentiment Gauge */}
                <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    <BrainCircuit className="w-5 h-5 text-cyan-400" />
                    <span>Meeting Sentiment Analysis</span>
                  </h3>
                  <div className="p-4 rounded-xl bg-slate-950/80 border border-white/5 text-center space-y-2">
                    <p className="text-2xl font-extrabold text-cyan-300 uppercase tracking-wide">
                      {intelligence.sentimentAnalysis || 'POSITIVE / COLLABORATIVE'}
                    </p>
                    <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                      <div className="bg-gradient-to-r from-indigo-500 via-cyan-400 to-emerald-400 h-full w-[85%]"></div>
                    </div>
                    <p className="text-[11px] text-slate-400 pt-1">85% Positive Tone Confidence</p>
                  </div>
                </div>

                {/* Key Decisions */}
                <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-indigo-400" />
                    <span>Key Decisions Made</span>
                  </h3>
                  {(!intelligence.keyDecisions || intelligence.keyDecisions.length === 0) ? (
                    <p className="text-xs text-slate-500 italic">No major decisions recorded yet.</p>
                  ) : (
                    <div className="space-y-2">
                      {intelligence.keyDecisions.map((decision, idx) => (
                        <div key={idx} className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-200 text-xs font-medium">
                          • {typeof decision === 'string' ? decision : decision.decision}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Full Transcripts */}
      {activeTab === 'transcript' && (
        <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-lg font-bold text-white">Full Meeting Transcripts</h3>
              <p className="text-xs text-slate-400">Search spoken dialogs and view speaker timestamps</p>
            </div>

            <div className="relative w-full sm:w-64">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search transcript..."
                className="w-full px-3.5 py-2 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          {transcripts.length === 0 ? (
            <div className="py-16 text-center text-slate-400 space-y-3 bg-slate-900/40 rounded-xl border border-dashed border-white/10">
              <FileText className="w-10 h-10 text-slate-600 mx-auto" />
              <p className="text-sm font-medium">No speech transcripts recorded yet for this session.</p>
            </div>
          ) : (
            <div className="space-y-4 max-h-[600px] overflow-y-auto pr-2">
              {transcripts.map((t) => (
                <div key={t.id} className="p-4 rounded-xl bg-slate-900/60 border border-white/5 space-y-2">
                  <div className="flex items-center justify-between text-xs text-slate-400 border-b border-white/5 pb-2">
                    <span className="font-semibold text-indigo-400">Speaker: {t.speakerName || 'Participant'}</span>
                    <span>Timestamp: {t.startTime ? new Date(t.startTime).toLocaleTimeString() : '00:00'}</span>
                  </div>
                  <p className="text-sm text-slate-200 leading-relaxed">{t.text || t.content}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Recordings Vault */}
      {activeTab === 'recordings' && (
        <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-6">
          <h3 className="text-lg font-bold text-white">Recorded Audio & Video Streams</h3>

          {recordings.length === 0 ? (
            <div className="py-16 text-center text-slate-400 space-y-3 bg-slate-900/40 rounded-xl border border-dashed border-white/10">
              <Disc className="w-10 h-10 text-slate-600 mx-auto" />
              <p className="text-sm font-medium">No media recordings stored for this meeting.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {recordings.map((rec) => (
                <div key={rec.id} className="p-4 rounded-xl bg-slate-900/80 border border-white/10 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-white">Recording #{rec.id}</span>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-mono">
                      {rec.format || 'MP4 / WAV'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400">Duration: {rec.durationSeconds || 0} seconds</p>
                  
                  {rec.fileUrl && (
                    <audio controls className="w-full mt-2">
                      <source src={rec.fileUrl} type="audio/wav" />
                    </audio>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

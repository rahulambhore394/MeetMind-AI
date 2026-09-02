import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { 
  Bot, Sparkles, ToggleLeft, ToggleRight, Save, Send, 
  Cpu, MessageSquare, Check, Shield, AlertCircle, Loader2 
} from 'lucide-react';

export default function AiRepStudio() {
  const [personaName, setPersonaName] = useState("Rahul's Autonomous AI Proxy");
  const [instructions, setInstructions] = useState(
    "Act as Rahul's intelligent proxy. Answer team queries regarding project status, technical architecture, and next steps with precision."
  );
  const [autoRespondKeywords, setAutoRespondKeywords] = useState("status, architecture, database, deployment, timeline");
  const [proxyModeEnabled, setProxyModeEnabled] = useState(false);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  // Live simulation chat state
  const [simMessages, setSimMessages] = useState([
    { sender: 'System', text: 'AI Representative Proxy Engine Initialized in Sandbox mode.' }
  ]);
  const [simInput, setSimInput] = useState('');
  const [simulating, setSimulating] = useState(false);

  const fetchAiRepConfig = async () => {
    try {
      setLoading(true);
      const res = await api.get('/ai-representatives/me');
      if (res.data) {
        setPersonaName(res.data.personaName || personaName);
        setInstructions(res.data.instructions || instructions);
        setAutoRespondKeywords(res.data.autoRespondKeywords || autoRespondKeywords);
        setProxyModeEnabled(res.data.proxyModeEnabled ?? false);
      }
    } catch (err) {
      console.log('No existing AI Rep configuration found, using defaults.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAiRepConfig();
  }, []);

  const handleSaveConfig = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);
      setSuccessMessage(null);
      await api.post('/ai-representatives', {
        personaName,
        instructions,
        autoRespondKeywords,
        proxyModeEnabled
      });
      setSuccessMessage('AI Representative Configuration Saved & Deployed!');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err) {
      alert('Failed to save AI Representative config.');
    } finally {
      setSaving(false);
    }
  };

  const handleSimulateChat = (e) => {
    e.preventDefault();
    if (!simInput.trim()) return;

    const userMsg = simInput;
    setSimMessages((prev) => [...prev, { sender: 'User', text: userMsg }]);
    setSimInput('');
    setSimulating(true);

    setTimeout(() => {
      setSimMessages((prev) => [
        ...prev,
        {
          sender: personaName,
          text: `[AI Proxy Reply]: Regarding "${userMsg}", based on my instructions: I have checked the system status. Backend Spring Boot and Kafka event pipeline are running smoothly.`
        }
      ]);
      setSimulating(false);
    }, 1000);
  };

  if (loading) {
    return (
      <div className="py-24 text-center text-slate-400 space-y-4">
        <Loader2 className="w-10 h-10 animate-spin mx-auto text-violet-400" />
        <p className="text-sm font-medium">Loading AI Representative Studio...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-8 border border-white/10 relative overflow-hidden bg-gradient-to-r from-slate-900 via-violet-950/40 to-slate-900">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-violet-500/10 border border-violet-500/20 text-violet-300 text-xs font-semibold">
              <Bot className="w-3.5 h-3.5" />
              <span>Autonomous Proxy Mode</span>
            </div>
            <h1 className="text-3xl font-extrabold text-white tracking-tight">AI Representative Studio</h1>
            <p className="text-slate-400 text-sm max-w-xl">
              Build and customize an autonomous AI delegate that represents you in meetings, answers questions in real-time, and takes structured notes.
            </p>
          </div>

          <div className="flex items-center gap-4 bg-slate-900/80 p-4 rounded-xl border border-white/10 shrink-0">
            <div className="text-right">
              <p className="text-xs text-slate-400 font-medium">Proxy Mode</p>
              <p className={`text-sm font-extrabold ${proxyModeEnabled ? 'text-emerald-400' : 'text-slate-500'}`}>
                {proxyModeEnabled ? 'ACTIVE & RESPONDING' : 'OFF / STANDBY'}
              </p>
            </div>

            <button
              type="button"
              onClick={() => setProxyModeEnabled(!proxyModeEnabled)}
              className="text-white hover:opacity-90 transition-opacity cursor-pointer"
            >
              {proxyModeEnabled ? (
                <ToggleRight className="w-10 h-10 text-emerald-400" />
              ) : (
                <ToggleLeft className="w-10 h-10 text-slate-600" />
              )}
            </button>
          </div>
        </div>
      </div>

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm flex items-center gap-3">
          <Check className="w-5 h-5" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Main Grid: Config Form & Simulation */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Config Form */}
        <form onSubmit={handleSaveConfig} className="glass-panel rounded-2xl p-6 border border-white/10 space-y-6">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Cpu className="w-5 h-5 text-violet-400" />
            <span>Persona & Behavioral Rules</span>
          </h2>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Persona Display Name
            </label>
            <input
              type="text"
              value={personaName}
              onChange={(e) => setPersonaName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900/90 border border-white/10 text-white text-sm focus:outline-none focus:border-violet-500"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              System Prompt & Instructions
            </label>
            <textarea
              rows={4}
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900/90 border border-white/10 text-white text-sm focus:outline-none focus:border-violet-500 resize-none"
              required
            />
            <p className="text-[11px] text-slate-400 mt-1">Specify how the AI proxy should answer questions when you are away.</p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Auto-Response Keywords (Comma Separated)
            </label>
            <input
              type="text"
              value={autoRespondKeywords}
              onChange={(e) => setAutoRespondKeywords(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900/90 border border-white/10 text-white text-sm focus:outline-none focus:border-violet-500"
            />
          </div>

          <button
            type="submit"
            disabled={saving}
            className="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-lg shadow-violet-500/30 transition-all cursor-pointer"
          >
            {saving ? <Loader2 className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
            <span>Save & Deploy Persona</span>
          </button>
        </form>

        {/* Live Proxy Simulation Sandbox */}
        <div className="glass-panel rounded-2xl p-6 border border-white/10 flex flex-col justify-between space-y-4">
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <MessageSquare className="w-5 h-5 text-cyan-400" />
                <span>AI Rep Sandbox Tester</span>
              </h2>
              <span className="text-[10px] px-2.5 py-1 rounded-full bg-cyan-500/10 text-cyan-300 font-mono border border-cyan-500/20">
                Sandbox Mode
              </span>
            </div>

            {/* Chat Box */}
            <div className="h-[320px] overflow-y-auto space-y-3 p-4 rounded-xl bg-slate-950/80 border border-white/5">
              {simMessages.map((msg, index) => (
                <div
                  key={index}
                  className={`p-3 rounded-xl text-xs max-w-[85%] ${
                    msg.sender === 'User'
                      ? 'bg-indigo-600/30 text-indigo-100 border border-indigo-500/30 ml-auto'
                      : msg.sender === 'System'
                      ? 'bg-slate-900 text-slate-400 border border-white/5 mx-auto text-center'
                      : 'bg-violet-900/30 text-violet-100 border border-violet-500/30 mr-auto'
                  }`}
                >
                  <p className="font-semibold text-[10px] text-slate-400 mb-0.5">{msg.sender}</p>
                  <p className="leading-relaxed">{msg.text}</p>
                </div>
              ))}
              {simulating && (
                <div className="p-3 rounded-xl bg-violet-900/20 text-violet-300 text-xs border border-violet-500/20 mr-auto flex items-center gap-2">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>{personaName} is thinking...</span>
                </div>
              )}
            </div>
          </div>

          {/* Chat Input */}
          <form onSubmit={handleSimulateChat} className="flex gap-2 pt-2">
            <input
              type="text"
              value={simInput}
              onChange={(e) => setSimInput(e.target.value)}
              placeholder="Ask a question to test your AI proxy..."
              className="flex-1 px-4 py-3 rounded-xl bg-slate-900 border border-white/10 text-white text-xs focus:outline-none focus:border-cyan-500"
            />
            <button
              type="submit"
              disabled={simulating}
              className="px-4 py-3 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs flex items-center justify-center transition-all cursor-pointer"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

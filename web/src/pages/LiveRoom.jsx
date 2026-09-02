import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { 
  Video, Mic, MicOff, VideoOff, PhoneOff, MessageSquare, 
  Users, Sparkles, Send, Globe, Radio, Shield, Subtitles
} from 'lucide-react';

export default function LiveRoom() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [micOn, setMicOn] = useState(true);
  const [cameraOn, setCameraOn] = useState(true);
  const [subtitlesOn, setSubtitlesOn] = useState(true);
  const [chatOpen, setChatOpen] = useState(true);
  const [participantsOpen, setParticipantsOpen] = useState(false);

  const [chatMessages, setChatMessages] = useState([
    { sender: 'AI Meeting Assistant', text: 'Welcome to the live meeting session! Kafka live events and AI transcription active.', time: '12:00 PM' }
  ]);
  const [inputMsg, setInputMsg] = useState('');
  
  const [liveCaptions, setLiveCaptions] = useState([
    { speaker: 'Host', text: 'Welcome everyone! Today we are testing MeetMind AI Spring Boot + Web application integration.' }
  ]);

  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!inputMsg.trim()) return;

    const newMsg = {
      sender: 'You',
      text: inputMsg,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setChatMessages((prev) => [...prev, newMsg]);
    setInputMsg('');
  };

  return (
    <div className="h-[calc(100vh-100px)] flex flex-col space-y-4">
      {/* Top Bar */}
      <div className="glass-panel rounded-2xl p-4 border border-white/10 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-bold animate-pulse">
            <Radio className="w-3.5 h-3.5" />
            <span>LIVE SESSION #{id}</span>
          </div>
          <h2 className="text-base font-bold text-white hidden sm:block">Intelligent Meeting Room</h2>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setSubtitlesOn(!subtitlesOn)}
            className={`py-2 px-3.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
              subtitlesOn ? 'bg-indigo-600/30 text-indigo-300 border border-indigo-500/30' : 'bg-slate-800 text-slate-400'
            }`}
          >
            <Subtitles className="w-4 h-4" />
            <span className="hidden sm:inline">AI Captions</span>
          </button>

          <button
            onClick={() => { setChatOpen(!chatOpen); setParticipantsOpen(false); }}
            className={`py-2 px-3.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
              chatOpen ? 'bg-cyan-600/30 text-cyan-300 border border-cyan-500/30' : 'bg-slate-800 text-slate-400'
            }`}
          >
            <MessageSquare className="w-4 h-4" />
            <span className="hidden sm:inline">Chat</span>
          </button>
        </div>
      </div>

      {/* Main Grid: Video Stream + Side Drawer */}
      <div className="flex-1 flex gap-4 overflow-hidden">
        {/* Main Video Viewport */}
        <div className="flex-1 glass-panel rounded-2xl border border-white/10 p-4 flex flex-col justify-between relative overflow-hidden bg-slate-950">
          {/* Participant Tile Grid */}
          <div className="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-4 items-center justify-center p-4">
            {/* Local Stream Tile */}
            <div className="relative w-full h-full min-h-[220px] rounded-2xl bg-slate-900 border border-white/10 flex items-center justify-center overflow-hidden group shadow-2xl">
              {cameraOn ? (
                <div className="absolute inset-0 bg-gradient-to-tr from-slate-900 via-indigo-950 to-slate-900 flex flex-col items-center justify-center text-center p-6">
                  <div className="w-20 h-20 rounded-full bg-indigo-500/20 border-2 border-indigo-400 flex items-center justify-center font-bold text-2xl text-white shadow-xl glow-indigo mb-3">
                    YOU
                  </div>
                  <p className="text-sm font-semibold text-white">Rahul (You)</p>
                  <p className="text-xs text-indigo-400">Host & Presenter</p>
                </div>
              ) : (
                <div className="text-center text-slate-500 space-y-2">
                  <VideoOff className="w-12 h-12 mx-auto" />
                  <p className="text-xs font-semibold">Camera Turned Off</p>
                </div>
              )}

              <div className="absolute bottom-3 left-3 bg-slate-950/80 px-3 py-1 rounded-lg border border-white/10 text-xs text-white font-medium flex items-center gap-2">
                <span>You</span>
                {!micOn && <MicOff className="w-3.5 h-3.5 text-rose-400" />}
              </div>
            </div>

            {/* AI Representative Proxy Tile */}
            <div className="relative w-full h-full min-h-[220px] rounded-2xl bg-slate-900 border border-violet-500/30 flex items-center justify-center overflow-hidden shadow-2xl">
              <div className="absolute inset-0 bg-gradient-to-tr from-slate-950 via-violet-950/40 to-slate-950 flex flex-col items-center justify-center text-center p-6">
                <div className="w-20 h-20 rounded-full bg-violet-500/20 border-2 border-violet-400 flex items-center justify-center text-violet-300 shadow-xl glow-indigo mb-3 animate-pulse">
                  <Sparkles className="w-10 h-10" />
                </div>
                <p className="text-sm font-semibold text-white">Autonomous AI Proxy</p>
                <p className="text-xs text-violet-400">Active Delegate</p>
              </div>

              <div className="absolute bottom-3 left-3 bg-violet-950/90 px-3 py-1 rounded-lg border border-violet-500/30 text-xs text-violet-200 font-medium flex items-center gap-2">
                <Radio className="w-3 h-3 text-emerald-400 animate-ping" />
                <span>AI Agent Ready</span>
              </div>
            </div>
          </div>

          {/* Subtitles Overlay */}
          {subtitlesOn && (
            <div className="w-full bg-slate-900/90 border border-white/10 backdrop-blur-md p-3.5 rounded-xl mb-4 space-y-1 text-center shadow-lg">
              <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-widest block">Live AI Subtitle Subsystem</span>
              <p className="text-sm text-slate-100 font-medium">
                "{liveCaptions[liveCaptions.length - 1]?.text}"
              </p>
            </div>
          )}

          {/* Room Controls Bar */}
          <div className="flex items-center justify-center gap-4 py-3 bg-slate-900/90 border border-white/10 rounded-2xl px-6">
            <button
              onClick={() => setMicOn(!micOn)}
              className={`p-3.5 rounded-xl transition-all cursor-pointer ${
                micOn ? 'bg-slate-800 text-white hover:bg-slate-700' : 'bg-rose-600 text-white shadow-lg shadow-rose-600/30'
              }`}
            >
              {micOn ? <Mic className="w-5 h-5" /> : <MicOff className="w-5 h-5" />}
            </button>

            <button
              onClick={() => setCameraOn(!cameraOn)}
              className={`p-3.5 rounded-xl transition-all cursor-pointer ${
                cameraOn ? 'bg-slate-800 text-white hover:bg-slate-700' : 'bg-rose-600 text-white shadow-lg shadow-rose-600/30'
              }`}
            >
              {cameraOn ? <Video className="w-5 h-5" /> : <VideoOff className="w-5 h-5" />}
            </button>

            <button
              onClick={() => navigate('/dashboard')}
              className="py-3 px-6 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm flex items-center gap-2 shadow-lg shadow-rose-600/30 transition-all cursor-pointer"
            >
              <PhoneOff className="w-5 h-5" />
              <span>Leave Room</span>
            </button>
          </div>
        </div>

        {/* Side Drawer: Chat & Participants */}
        {chatOpen && (
          <div className="w-80 glass-panel rounded-2xl border border-white/10 p-4 flex flex-col justify-between hidden md:flex shrink-0">
            <div className="space-y-4 flex-1 flex flex-col">
              <div className="flex items-center justify-between border-b border-white/10 pb-3">
                <h3 className="font-bold text-white text-sm flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-cyan-400" />
                  <span>In-Meeting Live Chat</span>
                </h3>
              </div>

              {/* Messages list */}
              <div className="flex-1 overflow-y-auto space-y-3 pr-1 max-h-[420px]">
                {chatMessages.map((msg, index) => (
                  <div key={index} className="p-3 rounded-xl bg-slate-900/90 border border-white/5 space-y-1">
                    <div className="flex items-center justify-between text-[11px] text-slate-400">
                      <span className="font-semibold text-indigo-400">{msg.sender}</span>
                      <span>{msg.time}</span>
                    </div>
                    <p className="text-xs text-slate-200 leading-relaxed">{msg.text}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* Chat Input */}
            <form onSubmit={handleSendMessage} className="flex gap-2 pt-3 border-t border-white/10">
              <input
                type="text"
                value={inputMsg}
                onChange={(e) => setInputMsg(e.target.value)}
                placeholder="Type a message..."
                className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-900 border border-white/10 text-white text-xs focus:outline-none focus:border-cyan-500"
              />
              <button
                type="submit"
                className="p-2.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white transition-all cursor-pointer"
              >
                <Send className="w-4 h-4" />
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}

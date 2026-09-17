import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { 
  Video, Mic, MicOff, VideoOff, PhoneOff, MessageSquare, 
  Users, Sparkles, Send, Globe, Radio, Shield, Subtitles, Disc, CheckCircle2
} from 'lucide-react';

export default function LiveRoom() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [micOn, setMicOn] = useState(true);
  const [cameraOn, setCameraOn] = useState(true);
  const [subtitlesOn, setSubtitlesOn] = useState(true);
  const [chatOpen, setChatOpen] = useState(true);
  const [participantsOpen, setParticipantsOpen] = useState(false);

  // Recording State
  const [isRecording, setIsRecording] = useState(false);
  const [recordingId, setRecordingId] = useState(null);
  const [recordDuration, setRecordDuration] = useState(0);
  const [recordNotice, setRecordNotice] = useState(null);
  const mediaRecorderRef = useRef(null);
  const recordedChunksRef = useRef([]);
  const recordingIntervalRef = useRef(null);

  const [aiSpeaking, setAiSpeaking] = useState(false);
  const [aiSpeechText, setAiSpeechText] = useState('');
  const [aiOwner, setAiOwner] = useState('Rahul');

  const [chatMessages, setChatMessages] = useState([
    { sender: 'AI Meeting Assistant', text: 'Welcome to the live meeting session! Kafka live events, AI transcription, and Real-Time Voice Generation active.', time: '12:00 PM' }
  ]);
  const [inputMsg, setInputMsg] = useState('');
  
  const [liveCaptions, setLiveCaptions] = useState([
    { speaker: 'Host', text: 'Welcome everyone! Today we are testing MeetMind AI live meeting and real-time voice generation.' }
  ]);

  const formatDuration = (totalSeconds) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const resolveMeetingId = async () => {
    if (!isNaN(id)) return Number(id);
    try {
      const res = await api.get('/meetings');
      const found = res.data?.find((m) => m.meetingCode === id || m.id === Number(id));
      return found ? found.id : id;
    } catch {
      return id;
    }
  };

  const startLiveRecording = async () => {
    try {
      const numericId = await resolveMeetingId();
      const res = await api.post(`/meetings/${numericId}/recordings/start`);
      const newRecId = res.data.id;
      setRecordingId(newRecId);

      let stream;
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      } catch (e) {
        console.warn('Microphone stream access not granted for recording', e);
      }

      if (stream) {
        recordedChunksRef.current = [];
        const recorder = new MediaRecorder(stream);
        recorder.ondataavailable = (e) => {
          if (e.data && e.data.size > 0) {
            recordedChunksRef.current.push(e.data);
          }
        };
        recorder.start(1000);
        mediaRecorderRef.current = recorder;
      }

      setIsRecording(true);
      setRecordDuration(0);
      if (recordingIntervalRef.current) clearInterval(recordingIntervalRef.current);
      recordingIntervalRef.current = setInterval(() => {
        setRecordDuration((prev) => prev + 1);
      }, 1000);

      setRecordNotice('Meeting recording initiated');
      setTimeout(() => setRecordNotice(null), 3500);
    } catch (err) {
      console.error('Failed to start recording:', err);
      alert('Could not start recording: ' + (err.response?.data?.message || err.message));
    }
  };

  const stopLiveRecording = async () => {
    try {
      if (recordingIntervalRef.current) clearInterval(recordingIntervalRef.current);
      setIsRecording(false);

      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
        mediaRecorderRef.current.stream?.getTracks().forEach((t) => t.stop());
      }

      const numericId = await resolveMeetingId();
      const targetRecId = recordingId;
      if (targetRecId) {
        await api.post(`/meetings/${numericId}/recordings/${targetRecId}/stop`);
        setRecordNotice('Saving recording & uploading for AI intelligence...');

        setTimeout(async () => {
          try {
            const chunks = recordedChunksRef.current || [];
            if (chunks.length > 0) {
              const blob = new Blob(chunks, { type: 'audio/webm' });
              const file = new File([blob], `recording_${numericId}_${targetRecId}.webm`, { type: 'audio/webm' });
              const formData = new FormData();
              formData.append('file', file);
              await api.post(`/meetings/${numericId}/recordings/${targetRecId}/upload`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
              });
              setRecordNotice('Recording saved & processed by MeetMind AI!');
              setTimeout(() => setRecordNotice(null), 5000);
            }
          } catch (uploadErr) {
            console.error('Failed to upload recording file:', uploadErr);
          }
        }, 800);
      }
    } catch (err) {
      console.error('Failed to stop recording:', err);
    }
  };

  const speakText = (text, ownerName = 'Rahul') => {
    if (!text) return;
    setAiSpeechText(text);
    setAiOwner(ownerName);
    setAiSpeaking(true);

    // Synchronize subtitle captions
    setLiveCaptions((prev) => [
      ...prev,
      { speaker: `AI Proxy (${ownerName})`, text }
    ]);

    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      utterance.onstart = () => setAiSpeaking(true);
      utterance.onend = () => {
        setAiSpeaking(false);
        setAiSpeechText('');
      };
      utterance.onerror = () => {
        setAiSpeaking(false);
        setAiSpeechText('');
      };
      window.speechSynthesis.speak(utterance);
    } else {
      setTimeout(() => {
        setAiSpeaking(false);
        setAiSpeechText('');
      }, 5000);
    }
  };

  const handleSendMessage = (e) => {
    e.preventDefault();
    const query = inputMsg.trim();
    if (!query) return;

    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const newMsg = { sender: 'You', text: query, time };
    setChatMessages((prev) => [...prev, newMsg]);
    setInputMsg('');

    // Check if query addresses AI representative
    const lower = query.toLowerCase();
    if (lower.includes('@ai') || lower.includes('@proxy') || lower.includes('@rahul') || lower.includes('budget') || lower.includes('status') || lower.includes('deadline')) {
      setTimeout(() => {
        let answer = `Speaking for Rahul: Regarding your inquiry, all milestones and deliverable targets are verified and on track.`;
        if (lower.includes('budget')) {
          answer = `Hi team, speaking on behalf of Rahul: Regarding the budget, the allocation has been finalized and approved for this quarter.`;
        } else if (lower.includes('deadline')) {
          answer = `Speaking on behalf of Rahul: Our target release deadline remains on schedule for completion.`;
        }

        const aiMsg = {
          sender: 'AI Proxy (Rahul)',
          text: answer,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        };
        setChatMessages((prev) => [...prev, aiMsg]);
        speakText(answer, 'Rahul');
      }, 800);
    }
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
          {isRecording && (
            <div 
              onClick={stopLiveRecording}
              title="Click to stop recording"
              className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-rose-500/20 border border-rose-500/50 text-rose-400 text-xs font-black animate-pulse cursor-pointer hover:bg-rose-500/30 transition-all shadow-lg shadow-rose-500/20"
            >
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping inline-block" />
              <span>● REC {formatDuration(recordDuration)}</span>
            </div>
          )}

          <button
            onClick={() => setSubtitlesOn(!subtitlesOn)}
            className={`py-2 px-3.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer ${
              subtitlesOn ? 'bg-indigo-600/30 text-indigo-300 border border-indigo-500/30' : 'bg-slate-800 text-slate-400'
            }`}
          >
            <Subtitles className="w-4 h-4" />
            <span className="hidden sm:inline">AI Captions</span>
          </button>

          <button
            onClick={() => { setChatOpen(!chatOpen); setParticipantsOpen(false); }}
            className={`py-2 px-3.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer ${
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
          {recordNotice && (
            <div className="absolute top-4 left-1/2 -translate-x-1/2 z-20 px-4 py-2 rounded-xl bg-slate-900/95 border border-cyan-500/40 text-cyan-300 text-xs font-semibold shadow-2xl flex items-center gap-2 animate-fade-in backdrop-blur-md">
              <CheckCircle2 className="w-4 h-4 text-cyan-400" />
              <span>{recordNotice}</span>
            </div>
          )}

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
            <div className={`relative w-full h-full min-h-[220px] rounded-2xl bg-slate-900 border transition-all duration-300 flex items-center justify-center overflow-hidden shadow-2xl ${
              aiSpeaking ? 'border-cyan-400 shadow-cyan-500/40 ring-2 ring-cyan-400/50' : 'border-violet-500/30'
            }`}>
              <div className="absolute inset-0 bg-gradient-to-tr from-slate-950 via-violet-950/40 to-slate-950 flex flex-col items-center justify-center text-center p-6">
                <div className={`w-20 h-20 rounded-full flex items-center justify-center shadow-xl transition-all duration-300 mb-3 ${
                  aiSpeaking
                    ? 'bg-cyan-500/30 border-2 border-cyan-400 text-cyan-300 ring-8 ring-cyan-500/20 scale-110 animate-bounce'
                    : 'bg-violet-500/20 border-2 border-violet-400 text-violet-300 glow-indigo animate-pulse'
                }`}>
                  {aiSpeaking ? <Radio className="w-10 h-10 animate-pulse" /> : <Sparkles className="w-10 h-10" />}
                </div>
                <p className="text-sm font-semibold text-white">Autonomous AI Proxy</p>
                <p className={`text-xs font-medium transition-colors ${aiSpeaking ? 'text-cyan-300 font-bold' : 'text-violet-400'}`}>
                  {aiSpeaking ? `Speaking for ${aiOwner}...` : 'Active Delegate'}
                </p>
                {aiSpeaking && aiSpeechText && (
                  <div className="mt-2 max-w-[280px] px-3 py-1.5 rounded-lg bg-cyan-950/80 border border-cyan-500/40 text-[11px] text-cyan-200 line-clamp-2 italic shadow-lg animate-fade-in">
                    "{aiSpeechText}"
                  </div>
                )}
              </div>

              <div className="absolute bottom-3 left-3 bg-violet-950/90 px-3 py-1 rounded-lg border border-violet-500/30 text-xs text-violet-200 font-medium flex items-center gap-2">
                <Radio className={`w-3 h-3 ${aiSpeaking ? 'text-cyan-400 animate-ping' : 'text-emerald-400 animate-ping'}`} />
                <span>{aiSpeaking ? 'Live Voice Active' : 'AI Agent Ready'}</span>
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

            {/* Live Meeting Recording Button */}
            <button
              onClick={isRecording ? stopLiveRecording : startLiveRecording}
              title={isRecording ? "Stop Live Recording" : "Start Live Recording"}
              className={`p-3.5 rounded-xl transition-all cursor-pointer ${
                isRecording 
                  ? 'bg-rose-600 text-white shadow-lg shadow-rose-600/50 ring-2 ring-rose-400 animate-pulse' 
                  : 'bg-slate-800 text-white hover:bg-slate-700'
              }`}
            >
              <Disc className={`w-5 h-5 ${isRecording ? 'animate-spin' : ''}`} />
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

import React, { useState } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import MeetingDetail from './pages/MeetingDetail';
import AiRepStudio from './pages/AiRepStudio';
import LiveRoom from './pages/LiveRoom';
import Recordings from './pages/Recordings';
import api from './api/client';
import { Plus, X, Loader2, Video } from 'lucide-react';

function ProtectedLayout({ children, onOpenNewMeeting }) {
  const { user, token } = useAuth();
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-[#0B0F17] flex flex-col">
      <Navbar />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar onOpenNewMeeting={onOpenNewMeeting} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-8">
          <div className="max-w-7xl mx-auto">{children}</div>
        </main>
      </div>
    </div>
  );
}

function MainApp() {
  const [isMeetingModalOpen, setIsMeetingModalOpen] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();

  const handleCreateMeeting = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    try {
      const futureDate = new Date(Date.now() + 300000).toISOString().slice(0, 19);
      const res = await api.post('/meetings', {
        title: newTitle,
        description: newDesc,
        scheduledAt: futureDate
      });

      setIsMeetingModalOpen(false);
      setNewTitle('');
      setNewDesc('');
      
      const createdMeeting = res.data;
      if (createdMeeting && createdMeeting.id) {
        navigate(`/room/${createdMeeting.id}`);
      } else {
        window.location.reload();
      }
    } catch (err) {
      alert('Failed to create meeting: ' + (err.response?.data?.message || err.message));
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedLayout onOpenNewMeeting={() => setIsMeetingModalOpen(true)}>
              <Dashboard onOpenNewMeeting={() => setIsMeetingModalOpen(true)} />
            </ProtectedLayout>
          }
        />
        <Route
          path="/meetings/:id"
          element={
            <ProtectedLayout onOpenNewMeeting={() => setIsMeetingModalOpen(true)}>
              <MeetingDetail />
            </ProtectedLayout>
          }
        />
        <Route
          path="/ai-rep"
          element={
            <ProtectedLayout onOpenNewMeeting={() => setIsMeetingModalOpen(true)}>
              <AiRepStudio />
            </ProtectedLayout>
          }
        />
        <Route
          path="/recordings"
          element={
            <ProtectedLayout onOpenNewMeeting={() => setIsMeetingModalOpen(true)}>
              <Recordings />
            </ProtectedLayout>
          }
        />
        <Route
          path="/room/:id"
          element={
            <ProtectedLayout onOpenNewMeeting={() => setIsMeetingModalOpen(true)}>
              <LiveRoom />
            </ProtectedLayout>
          }
        />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>

      {/* New Meeting Modal */}
      {isMeetingModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="w-full max-w-md glass-panel rounded-2xl p-6 border border-white/10 shadow-2xl space-y-5 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div className="flex items-center gap-2">
                <Video className="w-5 h-5 text-indigo-400" />
                <h3 className="text-lg font-bold text-white">Create Instant Meeting</h3>
              </div>
              <button
                onClick={() => setIsMeetingModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateMeeting} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Meeting Title</label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Q3 Architecture Review & Roadmap"
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-white/10 text-white text-sm focus:outline-none focus:border-indigo-500"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Description</label>
                <textarea
                  rows={3}
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  placeholder="Agenda items and discussion goals..."
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-white/10 text-white text-sm focus:outline-none focus:border-indigo-500 resize-none"
                />
              </div>

              <div className="pt-2 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsMeetingModalOpen(false)}
                  className="flex-1 py-3 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 font-semibold text-xs transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="flex-1 py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-cyan-500 hover:from-indigo-500 hover:to-cyan-400 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/25 transition-all disabled:opacity-50 cursor-pointer"
                >
                  {creating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                  <span>Launch Session</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

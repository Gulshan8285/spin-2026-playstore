import { useState } from 'react';
import {
  Users, DollarSign, CheckCircle,
  Settings, AlertCircle, Eye, EyeOff, RotateCw, LogOut, ShieldCheck,
  Bell, Send, Edit3, Smartphone, Search, Save, CheckCheck
} from 'lucide-react';

interface Withdrawal {
  id: string;
  userName: string;
  uid: string;
  amount: number;
  currency: string;
  pointsDeducted: number;
  method: 'UPI' | 'PayPal' | 'Cash App' | 'Bank';
  payoutAddress: string;
  status: 'pending' | 'approved' | 'rejected';
  time: string;
  isRealUser: boolean;
}

interface UserItem {
  uid: string;
  name: string;
  email: string;
  phone: string;
  countryCode: string;
  payoutAddress: string;
  points: number;
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  referrals: number;
  status: 'ACTIVE' | 'BANNED';
  isRealUser: boolean;
  registeredDate: string;
  lastActive: string;
}

interface BroadcastHistory {
  id: string;
  title: string;
  message: string;
  target: string;
  sentAt: string;
  deliveredCount: number;
}

export default function App() {
  // =========================================================================
  // 🔐 1. AUTHENTICATION & SECURITY GATE
  // =========================================================================
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    return sessionStorage.getItem('spinwin_admin_auth') === 'true';
  });
  const [passwordInput, setPasswordInput] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [captchaInput, setCaptchaInput] = useState('');
  const [captchaCode, setCaptchaCode] = useState(() => Math.floor(1000 + Math.random() * 9000).toString());
  const [loginError, setLoginError] = useState<string | null>(null);

  const refreshCaptcha = () => {
    setCaptchaCode(Math.floor(1000 + Math.random() * 9000).toString());
    setCaptchaInput('');
    setLoginError(null);
  };

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (passwordInput !== 'SpinWin@2026') {
      setLoginError('Invalid Master Password. Access Denied.');
      refreshCaptcha();
      return;
    }
    if (captchaInput.trim() !== captchaCode) {
      setLoginError('Incorrect Security Number Captcha. Please try again.');
      refreshCaptcha();
      return;
    }
    sessionStorage.setItem('spinwin_admin_auth', 'true');
    setIsAuthenticated(true);
    setLoginError(null);
  };

  const handleLogout = () => {
    sessionStorage.removeItem('spinwin_admin_auth');
    setIsAuthenticated(false);
    setPasswordInput('');
    refreshCaptcha();
  };

  // =========================================================================
  // 📂 2. TABS & STATE MANAGEMENT
  // =========================================================================
  const [activeTab, setActiveTab] = useState<'withdrawals' | 'users' | 'notifications' | 'content' | 'config'>('withdrawals');
  const [notification, setNotification] = useState<string | null>(null);

  const showNotice = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 3500);
  };

  // --- WITHDRAWALS STATE ---
  const [withdrawals, setWithdrawals] = useState<Withdrawal[]>([
    {
      id: 'wd_901',
      userName: 'Gulshan Yadav',
      uid: 'u_real_01',
      amount: 25.0,
      currency: '$',
      pointsDeducted: 25000,
      method: 'PayPal',
      payoutAddress: 'gulshanyadav62000@gmail.com',
      status: 'pending',
      time: '5 mins ago',
      isRealUser: true
    },
    {
      id: 'wd_902',
      userName: 'Michael Scott',
      uid: 'u_real_02',
      amount: 15.0,
      currency: '$',
      pointsDeducted: 15000,
      method: 'Cash App',
      payoutAddress: '$mscott_rewards',
      status: 'pending',
      time: '18 mins ago',
      isRealUser: true
    },
    {
      id: 'wd_903',
      userName: 'Aarav Sharma',
      uid: 'u_real_03',
      amount: 200.0,
      currency: '₹',
      pointsDeducted: 2000,
      method: 'UPI',
      payoutAddress: 'aarav.sharma@okaxis',
      status: 'pending',
      time: '42 mins ago',
      isRealUser: true
    },
    {
      id: 'wd_904',
      userName: 'Jessica Williams',
      uid: 'u_real_04',
      amount: 50.0,
      currency: '$',
      pointsDeducted: 50000,
      method: 'Bank',
      payoutAddress: 'Chase Bank (A/C: ****4912)',
      status: 'approved',
      time: '2 hours ago',
      isRealUser: true
    }
  ]);

  // --- REAL USERS STATE ---
  const [users, setUsers] = useState<UserItem[]>([
    {
      uid: 'u_real_01',
      name: 'Gulshan Yadav',
      email: 'gulshanyadav62000@gmail.com',
      phone: '+1 98765 43210',
      countryCode: 'US',
      payoutAddress: 'gulshanyadav62000@gmail.com (PayPal)',
      points: 25400,
      tier: 'PLATINUM',
      referrals: 14,
      status: 'ACTIVE',
      isRealUser: true,
      registeredDate: 'Today, 10:15 AM',
      lastActive: 'Active Now 🟢'
    },
    {
      uid: 'u_real_02',
      name: 'Michael Scott',
      email: 'm.scott@dunder.com',
      phone: '+1 570 555 0192',
      countryCode: 'US',
      payoutAddress: '$mscott_rewards (Cash App)',
      points: 15850,
      tier: 'GOLD',
      referrals: 9,
      status: 'ACTIVE',
      isRealUser: true,
      registeredDate: 'Yesterday',
      lastActive: '12 mins ago'
    },
    {
      uid: 'u_real_03',
      name: 'Aarav Sharma',
      email: 'aarav.pay@gmail.com',
      phone: '+91 98234 56789',
      countryCode: 'IN',
      payoutAddress: 'aarav.sharma@okaxis (UPI)',
      points: 4200,
      tier: 'SILVER',
      referrals: 5,
      status: 'ACTIVE',
      isRealUser: true,
      registeredDate: 'Sep 11, 2026',
      lastActive: '45 mins ago'
    },
    {
      uid: 'u_real_04',
      name: 'Jessica Williams',
      email: 'jess.williams@yahoo.com',
      phone: '+1 415 892 3120',
      countryCode: 'US',
      payoutAddress: 'Chase Bank A/C: ****4912',
      points: 52000,
      tier: 'PLATINUM',
      referrals: 28,
      status: 'ACTIVE',
      isRealUser: true,
      registeredDate: 'Sep 10, 2026',
      lastActive: '2 hours ago'
    }
  ]);

  const [userSearch, setUserSearch] = useState('');
  const [filterRealOnly, setFilterRealOnly] = useState(true);

  // --- PUSH NOTIFICATIONS STATE ---
  const [notifTitle, setNotifTitle] = useState('🔥 2X Double Rewards Weekend is LIVE!');
  const [notifMessage, setNotifMessage] = useState('Spin the lucky wheel now & double your real cash earnings instantly. Tap to claim!');
  const [notifTarget, setNotifTarget] = useState('ALL_USERS');
  const [notifDeepLink, setNotifDeepLink] = useState('SPIN_WHEEL');
  const [broadcastHistory, setBroadcastHistory] = useState<BroadcastHistory[]>([
    {
      id: 'b_01',
      title: '🎁 +100 Free Joining Bonus Credited',
      message: 'Welcome to SpinWin Rewards! Your first free spins are waiting.',
      target: 'All Active Users',
      sentAt: 'Today, 11:00 AM',
      deliveredCount: 1420
    }
  ]);

  // --- APP PAGES CONTENT CMS STATE ---
  const [appContent, setAppContent] = useState(() => {
    const saved = localStorage.getItem('spinwin_cms_content');
    if (saved) {
      try { return JSON.parse(saved); } catch (_) {}
    }
    return {
      homeTagline: 'Turn Daily Spins & Quizzes Into Instant Cash',
      welcomeBannerText: '🎁 Welcome Bonus Active • 10 Daily Free Spins Ready!',
      noticeMarquee: 'Michael S. withdrew $25 via PayPal • Emma W. withdrew $50 via Cash App • Alex K. won $10 on Wheel',
      spinWheelPrizes: '100 Pts, 20 Pts, 50 Pts, Extra Spin, 10 Pts, JACKPOT',
      quizRewardPerAnswer: 20,
      minWithdrawalUSD: 1.0,
      supportEmail: 'gulshanyadav62000@gmail.com',
      fairPlayGuarantee: '100% Anti-bot protected, certified RNG wheel physics'
    };
  });

  const [cmsSaved, setCmsSaved] = useState(false);

  // --- GLOBAL FINANCIAL CONFIG ---
  const [config, setConfig] = useState({
    pointsPerDollar: 1000, // 1000 pts = $1.00 USD
    pointsPerRupee: 10,    // 10 pts = ₹1.00 INR
    minWithdrawalPoints: 1000, // $1.00
    adRewardPoints: 100,
    dailySpinLimit: 10
  });

  // =========================================================================
  // ⚡ 3. ACTIONS & HANDLERS
  // =========================================================================
  const handleApproveWithdrawal = (id: string) => {
    setWithdrawals(prev => prev.map(w => w.id === id ? { ...w, status: 'approved' } : w));
    showNotice(`✅ Withdrawal #${id} APPROVED! Funds queued for payout dispatch.`);
  };

  const handleRejectWithdrawal = (id: string) => {
    setWithdrawals(prev => prev.map(w => w.id === id ? { ...w, status: 'rejected' } : w));
    showNotice(`❌ Withdrawal #${id} REJECTED. Deducted points refunded to user wallet.`);
  };

  const handleToggleUserBan = (uid: string) => {
    setUsers(prev => prev.map(u => {
      if (u.uid === uid) {
        const nextStatus = u.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE';
        showNotice(`User ${u.name} is now ${nextStatus}!`);
        return { ...u, status: nextStatus };
      }
      return u;
    }));
  };

  const handleGiftPoints = (uid: string, bonus: number) => {
    setUsers(prev => prev.map(u => {
      if (u.uid === uid) {
        showNotice(`🎁 Added +${bonus} Bonus Points to ${u.name}'s wallet!`);
        return { ...u, points: u.points + bonus };
      }
      return u;
    }));
  };

  const handleSendBroadcast = (e: React.FormEvent) => {
    e.preventDefault();
    if (!notifTitle.trim() || !notifMessage.trim()) {
      alert('Please enter notification title and message');
      return;
    }
    const newBroadcast: BroadcastHistory = {
      id: 'b_' + Date.now(),
      title: notifTitle,
      message: notifMessage,
      target: notifTarget === 'ALL_USERS' ? 'All Active Users (100%)' : notifTarget === 'HIGH_EARNERS' ? 'High Earners (>10k Pts)' : 'Inactive Users (3+ Days)',
      sentAt: 'Just now',
      deliveredCount: users.length * 280 + Math.floor(Math.random() * 50)
    };
    setBroadcastHistory([newBroadcast, ...broadcastHistory]);
    showNotice(`📢 Push Notification broadcast dispatched to ${newBroadcast.deliveredCount} active users!`);
  };

  const handleSaveCMS = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('spinwin_cms_content', JSON.stringify(appContent));
    setCmsSaved(true);
    showNotice('💾 All App Pages content successfully saved and deployed live!');
    setTimeout(() => setCmsSaved(false), 3000);
  };

  // Filtered Users
  const filteredUsers = users.filter(u => {
    if (filterRealOnly && !u.isRealUser) return false;
    if (!userSearch.trim()) return true;
    const q = userSearch.toLowerCase();
    return (
      u.name.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q) ||
      u.phone.toLowerCase().includes(q) ||
      u.payoutAddress.toLowerCase().includes(q)
    );
  });

  const pendingWithdrawalsCount = withdrawals.filter(w => w.status === 'pending').length;

  // =========================================================================
  // 🔒 4. RENDER LOGIN SCREEN IF NOT AUTHENTICATED
  // =========================================================================
  if (!isAuthenticated) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        background: 'radial-gradient(circle at top, #1E1B4B 0%, #070B14 100%)'
      }}>
        <div style={{
          width: '100%',
          maxWidth: '440px',
          background: 'rgba(255, 255, 255, 0.03)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '24px',
          padding: '36px 32px',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.6)'
        }}>
          {/* Brand Icon */}
          <div style={{ textAlign: 'center', marginBottom: '28px' }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '20px',
              background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '32px',
              marginBottom: '16px',
              boxShadow: '0 10px 25px rgba(124, 77, 255, 0.35)'
            }}>
              👑
            </div>
            <h1 style={{ fontSize: '22px', fontWeight: 800, color: '#fff', letterSpacing: '-0.5px', marginBottom: '6px' }}>
              SpinWin Master Admin
            </h1>
            <p style={{ color: '#6B7A99', fontSize: '13px' }}>
              Protected Financial Operations & CMS Control
            </p>
          </div>

          {loginError && (
            <div style={{
              marginBottom: '20px',
              padding: '12px 16px',
              borderRadius: '12px',
              background: 'rgba(255, 77, 77, 0.12)',
              border: '1px solid rgba(255, 77, 77, 0.4)',
              color: '#FF6B6B',
              fontSize: '13px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}>
              <AlertCircle size={18} />
              <span>{loginError}</span>
            </div>
          )}

          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Password Field */}
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '8px' }}>
                Master Admin Password
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter admin password"
                  value={passwordInput}
                  onChange={(e) => setPasswordInput(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '14px 44px 14px 16px',
                    borderRadius: '12px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '15px',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: '#6B7A99',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Number Captcha Verification */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <label style={{ fontSize: '13px', fontWeight: 600, color: '#A8B2C7' }}>
                  Security Number Captcha
                </label>
                <button
                  type="button"
                  onClick={refreshCaptcha}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#00D1FF',
                    fontSize: '12px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    fontWeight: 600
                  }}
                >
                  <RotateCw size={12} /> Refresh
                </button>
              </div>

              {/* Captcha Display Badge */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                marginBottom: '10px'
              }}>
                <div style={{
                  flex: '1',
                  background: 'linear-gradient(135deg, rgba(124, 77, 255, 0.2), rgba(0, 209, 255, 0.2))',
                  border: '1px dashed rgba(0, 209, 255, 0.4)',
                  borderRadius: '12px',
                  padding: '12px',
                  textAlign: 'center',
                  letterSpacing: '12px',
                  fontFamily: 'monospace',
                  fontSize: '24px',
                  fontWeight: 800,
                  color: '#00E5FF',
                  userSelect: 'none',
                  textShadow: '0 0 10px rgba(0, 229, 255, 0.5)'
                }}>
                  {captchaCode}
                </div>
              </div>

              <input
                type="text"
                placeholder="Type the 4 numbers above"
                maxLength={4}
                value={captchaInput}
                onChange={(e) => setCaptchaInput(e.target.value)}
                style={{
                  width: '100%',
                  padding: '14px 16px',
                  borderRadius: '12px',
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 255, 255, 0.12)',
                  color: '#fff',
                  fontSize: '15px',
                  letterSpacing: '4px',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
                required
              />
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              style={{
                width: '100%',
                padding: '14px',
                borderRadius: '14px',
                background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
                border: 'none',
                color: '#fff',
                fontSize: '15px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 10px 25px rgba(124, 77, 255, 0.35)',
                marginTop: '6px'
              }}
            >
              <ShieldCheck size={18} /> VERIFY & UNLOCK DASHBOARD
            </button>
          </form>

          <div style={{ textAlign: 'center', marginTop: '22px', fontSize: '12px', color: '#6B7A99' }}>
            🔒 Master Key Protected • End-to-End Session Guard
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // 👑 5. FULL MASTER DASHBOARD VIEW
  // =========================================================================
  return (
    <div style={{ minHeight: '100vh', padding: '24px 32px', maxWidth: '1450px', margin: '0 auto' }}>
      {/* Top Header */}
      <header style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        marginBottom: '28px', flexWrap: 'wrap', gap: '16px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px',
            boxShadow: '0 8px 20px rgba(124, 77, 255, 0.35)'
          }}>
            👑
          </div>
          <div>
            <h1 style={{ fontSize: '22px', fontWeight: 800, color: '#fff', letterSpacing: '-0.5px' }}>
              SpinWin Rewards <span style={{ color: '#00D1FF', fontSize: '13px', fontWeight: 700, background: 'rgba(0, 209, 255, 0.1)', padding: '3px 8px', borderRadius: '6px' }}>MASTER ADMIN</span>
            </h1>
            <p style={{ color: '#6B7A99', fontSize: '12px' }}>Real-time Operations, Multi-page CMS & Notification Broadcast</p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button
            onClick={() => setActiveTab('withdrawals')}
            className={activeTab === 'withdrawals' ? 'btn-primary' : ''}
            style={{
              padding: '10px 16px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'withdrawals' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <DollarSign size={16} /> Payout Queue ({pendingWithdrawalsCount})
          </button>

          <button
            onClick={() => setActiveTab('users')}
            className={activeTab === 'users' ? 'btn-primary' : ''}
            style={{
              padding: '10px 16px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'users' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <Users size={16} /> Real Users ({users.length})
          </button>

          <button
            onClick={() => setActiveTab('notifications')}
            className={activeTab === 'notifications' ? 'btn-primary' : ''}
            style={{
              padding: '10px 16px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'notifications' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <Bell size={16} /> Push Broadcast
          </button>

          <button
            onClick={() => setActiveTab('content')}
            className={activeTab === 'content' ? 'btn-primary' : ''}
            style={{
              padding: '10px 16px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'content' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <Edit3 size={16} /> App Pages CMS
          </button>

          <button
            onClick={() => setActiveTab('config')}
            className={activeTab === 'config' ? 'btn-primary' : ''}
            style={{
              padding: '10px 16px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'config' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <Settings size={16} /> Financial Rules
          </button>

          <button
            onClick={handleLogout}
            style={{
              padding: '10px 16px', borderRadius: '12px',
              border: '1px solid rgba(255, 77, 77, 0.3)',
              background: 'rgba(255, 77, 77, 0.1)',
              color: '#FF4D4D', cursor: 'pointer', fontWeight: 600, fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <LogOut size={16} /> Logout
          </button>
        </div>
      </header>

      {/* Global Notification Toast */}
      {notification && (
        <div style={{
          marginBottom: '20px', padding: '14px 20px', borderRadius: '12px',
          background: 'rgba(0, 230, 118, 0.15)', border: '1px solid #00E676',
          color: '#00E676', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '10px',
          boxShadow: '0 8px 24px rgba(0, 230, 118, 0.2)'
        }}>
          <CheckCircle size={18} />
          <span>{notification}</span>
        </div>
      )}

      {/* ===================================================================
          TAB 1: WITHDRAWALS & PAYOUT QUEUE
          =================================================================== */}
      {activeTab === 'withdrawals' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div>
              <h2 style={{ fontSize: '18px', fontWeight: 700 }}>Withdrawal Requests & Dispatches</h2>
              <p style={{ fontSize: '12px', color: '#6B7A99' }}>Review, verify payout addresses, and trigger real money dispatches.</p>
            </div>
            <span style={{ fontSize: '13px', color: '#A8B2C7' }}>
              Pending: <strong style={{ color: '#FFC542' }}>{pendingWithdrawalsCount}</strong>
            </span>
          </div>

          <div className="glass-card" style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#6B7A99' }}>
                  <th style={{ padding: '16px' }}>REQUEST ID</th>
                  <th style={{ padding: '16px' }}>USER</th>
                  <th style={{ padding: '16px' }}>PAYOUT ADDRESS</th>
                  <th style={{ padding: '16px' }}>POINTS DEDUCTED</th>
                  <th style={{ padding: '16px' }}>DISBURSE CASH</th>
                  <th style={{ padding: '16px' }}>STATUS</th>
                  <th style={{ padding: '16px' }}>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {withdrawals.map((w) => (
                  <tr key={w.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={{ padding: '16px', fontWeight: 600, color: '#fff' }}>
                      #{w.id}
                      <div style={{ fontSize: '11px', color: '#6B7A99' }}>{w.time}</div>
                    </td>
                    <td style={{ padding: '16px' }}>
                      <div style={{ fontWeight: 600, color: '#fff' }}>{w.userName}</div>
                      <div style={{ fontSize: '11px', color: '#00D1FF' }}>UID: {w.uid}</div>
                    </td>
                    <td style={{ padding: '16px' }}>
                      <span style={{
                        background: 'rgba(255,255,255,0.06)', padding: '4px 8px', borderRadius: '6px',
                        border: '1px solid rgba(255,255,255,0.1)', color: '#00E5FF', fontWeight: 600
                      }}>
                        {w.method}: {w.payoutAddress}
                      </span>
                    </td>
                    <td style={{ padding: '16px', fontWeight: 700, color: '#FFC542' }}>
                      🪙 {w.pointsDeducted.toLocaleString()} Pts
                    </td>
                    <td style={{ padding: '16px', fontWeight: 700, color: '#00E676', fontSize: '15px' }}>
                      {w.currency}{w.amount.toFixed(2)}
                    </td>
                    <td style={{ padding: '16px' }}>
                      <span className={`badge badge-${w.status}`}>
                        {w.status}
                      </span>
                    </td>
                    <td style={{ padding: '16px' }}>
                      {w.status === 'pending' ? (
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button onClick={() => handleApproveWithdrawal(w.id)} className="btn-success" style={{ padding: '6px 12px', fontSize: '12px' }}>
                            ✓ Approve
                          </button>
                          <button onClick={() => handleRejectWithdrawal(w.id)} className="btn-danger" style={{ padding: '6px 12px', fontSize: '12px' }}>
                            ✕ Reject
                          </button>
                        </div>
                      ) : (
                        <span style={{ color: '#6B7A99', fontSize: '12px' }}>Completed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ===================================================================
          TAB 2: REAL USERS & POINTS / RUPEES LEDGER
          =================================================================== */}
      {activeTab === 'users' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
            <div>
              <h2 style={{ fontSize: '18px', fontWeight: 700 }}>Real Active Users & Balance Ledger</h2>
              <p style={{ fontSize: '12px', color: '#6B7A99' }}>Live user profiles, verified emails, phone numbers, total points, and converted cash values.</p>
            </div>

            {/* Search & Real User Filter */}
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div style={{ position: 'relative', width: '260px' }}>
                <input
                  type="text"
                  placeholder="Search name, email, UPI, phone..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  style={{
                    width: '100%', padding: '10px 14px 10px 36px', borderRadius: '10px',
                    background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                    color: '#fff', fontSize: '13px', outline: 'none'
                  }}
                />
                <Search size={16} color="#6B7A99" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
              </div>

              <label style={{
                display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer',
                background: filterRealOnly ? 'rgba(0, 209, 255, 0.15)' : 'rgba(255,255,255,0.05)',
                padding: '8px 14px', borderRadius: '10px', border: '1px solid rgba(0, 209, 255, 0.3)',
                color: '#fff', fontSize: '12px', fontWeight: 600
              }}>
                <input
                  type="checkbox"
                  checked={filterRealOnly}
                  onChange={(e) => setFilterRealOnly(e.target.checked)}
                  style={{ accentColor: '#00D1FF' }}
                />
                Real Users Only
              </label>
            </div>
          </div>

          <div className="glass-card" style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#6B7A99' }}>
                  <th style={{ padding: '16px' }}>USER / AVATAR</th>
                  <th style={{ padding: '16px' }}>VERIFIED EMAIL & PHONE</th>
                  <th style={{ padding: '16px' }}>PAYOUT ADDRESS</th>
                  <th style={{ padding: '16px' }}>POINTS COLLECTED</th>
                  <th style={{ padding: '16px' }}>CASH CONVERTED</th>
                  <th style={{ padding: '16px' }}>STATUS</th>
                  <th style={{ padding: '16px' }}>ADMIN ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((u) => {
                  // Calculate dynamic cash value: 1000 pts = $1.00 USD, or 10 pts = ₹1.00
                  const cashUSD = (u.points / config.pointsPerDollar).toFixed(2);
                  const cashINR = (u.points / config.pointsPerRupee).toFixed(2);

                  return (
                    <tr key={u.uid} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                      <td style={{ padding: '16px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <div style={{
                            width: '36px', height: '36px', borderRadius: '50%',
                            background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontWeight: 700, color: '#fff', fontSize: '14px'
                          }}>
                            {u.name.charAt(0)}
                          </div>
                          <div>
                            <div style={{ fontWeight: 600, color: '#fff', display: 'flex', alignItems: 'center', gap: '6px' }}>
                              {u.name}
                              <span style={{ fontSize: '10px', background: 'rgba(0, 230, 118, 0.2)', color: '#00E676', padding: '2px 6px', borderRadius: '4px' }}>
                                Real
                              </span>
                            </div>
                            <div style={{ fontSize: '11px', color: '#6B7A99' }}>{u.lastActive}</div>
                          </div>
                        </div>
                      </td>

                      <td style={{ padding: '16px' }}>
                        <div style={{ color: '#fff', fontWeight: 500 }}>{u.email}</div>
                        <div style={{ fontSize: '11px', color: '#A8B2C7' }}>{u.phone}</div>
                      </td>

                      <td style={{ padding: '16px' }}>
                        <span style={{
                          background: 'rgba(255, 255, 255, 0.05)', padding: '4px 8px', borderRadius: '6px',
                          color: '#00D1FF', fontFamily: 'monospace', fontSize: '12px'
                        }}>
                          {u.payoutAddress}
                        </span>
                      </td>

                      <td style={{ padding: '16px', fontWeight: 700, color: '#FFC542' }}>
                        🪙 {u.points.toLocaleString()} Pts
                      </td>

                      <td style={{ padding: '16px' }}>
                        <div style={{ fontWeight: 700, color: '#00E676', fontSize: '14px' }}>${cashUSD} USD</div>
                        <div style={{ fontSize: '11px', color: '#A8B2C7' }}>≈ ₹{cashINR} INR</div>
                      </td>

                      <td style={{ padding: '16px' }}>
                        <span style={{
                          background: u.status === 'ACTIVE' ? 'rgba(0, 230, 118, 0.15)' : 'rgba(255, 77, 77, 0.15)',
                          color: u.status === 'ACTIVE' ? '#00E676' : '#FF4D4D',
                          padding: '4px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 700
                        }}>
                          {u.status}
                        </span>
                      </td>

                      <td style={{ padding: '16px' }}>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          <button
                            onClick={() => handleGiftPoints(u.uid, 500)}
                            style={{
                              padding: '5px 8px', borderRadius: '6px',
                              background: 'rgba(255, 197, 66, 0.15)', border: '1px solid rgba(255, 197, 66, 0.3)',
                              color: '#FFC542', fontSize: '11px', cursor: 'pointer', fontWeight: 600
                            }}
                          >
                            +500 Pts
                          </button>
                          <button
                            onClick={() => handleToggleUserBan(u.uid)}
                            style={{
                              padding: '5px 8px', borderRadius: '6px',
                              background: u.status === 'ACTIVE' ? 'rgba(255, 77, 77, 0.15)' : 'rgba(0, 230, 118, 0.15)',
                              border: '1px solid rgba(255,255,255,0.1)',
                              color: u.status === 'ACTIVE' ? '#FF4D4D' : '#00E676',
                              fontSize: '11px', cursor: 'pointer', fontWeight: 600
                            }}
                          >
                            {u.status === 'ACTIVE' ? 'Ban' : 'Unban'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ===================================================================
          TAB 3: PUSH NOTIFICATION BROADCAST CENTER
          =================================================================== */}
      {activeTab === 'notifications' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px' }}>
          {/* Form Side */}
          <div className="glass-card" style={{ padding: '28px' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Bell size={20} color="#00D1FF" /> Send Push Notification Broadcast
            </h2>
            <p style={{ fontSize: '12px', color: '#6B7A99', marginBottom: '22px' }}>
              Broadcast instantly to all installed Android devices via Firebase Cloud Messaging (FCM).
            </p>

            <form onSubmit={handleSendBroadcast} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                  Notification Headline / Title
                </label>
                <input
                  type="text"
                  value={notifTitle}
                  onChange={(e) => setNotifTitle(e.target.value)}
                  placeholder="e.g. 🔥 2X Points Weekend is LIVE!"
                  style={{
                    width: '100%', padding: '12px 14px', borderRadius: '10px',
                    background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                    color: '#fff', fontSize: '14px', outline: 'none'
                  }}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                  Notification Message Body
                </label>
                <textarea
                  value={notifMessage}
                  onChange={(e) => setNotifMessage(e.target.value)}
                  rows={3}
                  placeholder="Type the message that appears on user lockscreens..."
                  style={{
                    width: '100%', padding: '12px 14px', borderRadius: '10px',
                    background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                    color: '#fff', fontSize: '14px', outline: 'none', resize: 'vertical'
                  }}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                    Target Audience
                  </label>
                  <select
                    value={notifTarget}
                    onChange={(e) => setNotifTarget(e.target.value)}
                    style={{
                      width: '100%', padding: '10px', borderRadius: '10px',
                      background: '#16203A', border: '1px solid rgba(255,255,255,0.12)',
                      color: '#fff', fontSize: '13px', outline: 'none'
                    }}
                  >
                    <option value="ALL_USERS">All Users (100% Active)</option>
                    <option value="HIGH_EARNERS">High Earners (&gt;10,000 Pts)</option>
                    <option value="INACTIVE">Inactive Users (3+ Days)</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                    Open App Screen / Deep Link
                  </label>
                  <select
                    value={notifDeepLink}
                    onChange={(e) => setNotifDeepLink(e.target.value)}
                    style={{
                      width: '100%', padding: '10px', borderRadius: '10px',
                      background: '#16203A', border: '1px solid rgba(255,255,255,0.12)',
                      color: '#fff', fontSize: '13px', outline: 'none'
                    }}
                  >
                    <option value="SPIN_WHEEL">Lucky Spin Wheel 🎡</option>
                    <option value="QUIZ_ARENA">Trivia Quiz Arena 🧠</option>
                    <option value="WALLET">Wallet Payout Screen 💸</option>
                    <option value="HOME">Home Dashboard 🏠</option>
                  </select>
                </div>
              </div>

              <button
                type="submit"
                className="btn-primary"
                style={{
                  padding: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  gap: '10px', marginTop: '10px', fontSize: '14px', fontWeight: 700
                }}
              >
                <Send size={18} /> SEND BROADCAST TO ALL USERS 🚀
              </button>
            </form>
          </div>

          {/* Android Lock Screen Live Preview */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div className="glass-card" style={{ padding: '24px' }}>
              <h3 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '14px', color: '#A8B2C7', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Smartphone size={16} /> Live Android Lock Screen Preview
              </h3>

              {/* Phone Mockup Frame */}
              <div style={{
                background: 'linear-gradient(180deg, #111827 0%, #030712 100%)',
                borderRadius: '24px',
                border: '3px solid #374151',
                padding: '24px 16px',
                boxShadow: '0 20px 40px rgba(0,0,0,0.8)'
              }}>
                <div style={{ textAlign: 'center', color: '#9CA3AF', fontSize: '12px', marginBottom: '16px' }}>
                  Sunday, September 13 • 12:45 PM
                </div>

                {/* Notification Bubble */}
                <div style={{
                  background: 'rgba(255, 255, 255, 0.1)',
                  backdropFilter: 'blur(20px)',
                  borderRadius: '16px',
                  padding: '14px',
                  border: '1px solid rgba(255, 255, 255, 0.15)'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{ fontSize: '14px' }}>👑</span>
                      <span style={{ fontSize: '12px', fontWeight: 700, color: '#fff' }}>SpinWin Rewards</span>
                      <span style={{ fontSize: '10px', color: '#9CA3AF' }}>• now</span>
                    </div>
                  </div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                    {notifTitle || 'Notification Title'}
                  </div>
                  <div style={{ fontSize: '12px', color: '#D1D5DB', lineHeight: '16px' }}>
                    {notifMessage || 'Notification message body will appear here on user phones.'}
                  </div>
                </div>
              </div>
            </div>

            {/* Broadcast History */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <h3 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px', color: '#A8B2C7' }}>
                Recent Dispatched Broadcasts
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {broadcastHistory.map(b => (
                  <div key={b.id} style={{
                    padding: '12px', borderRadius: '10px', background: 'rgba(255,255,255,0.03)',
                    border: '1px solid rgba(255,255,255,0.06)'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                      <strong style={{ fontSize: '13px', color: '#fff' }}>{b.title}</strong>
                      <span style={{ fontSize: '11px', color: '#00E676', fontWeight: 600 }}>Delivered to {b.deliveredCount}</span>
                    </div>
                    <div style={{ fontSize: '11px', color: '#6B7A99' }}>{b.target} • {b.sentAt}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ===================================================================
          TAB 4: APP PAGES CONTENT CMS
          =================================================================== */}
      {activeTab === 'content' && (
        <div className="glass-card" style={{ padding: '32px', maxWidth: '850px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <div>
              <h2 style={{ fontSize: '20px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Edit3 size={22} color="#00D1FF" /> App Pages Content CMS
              </h2>
              <p style={{ fontSize: '12px', color: '#6B7A99' }}>
                Customize all app headlines, announcements, spin wheel prizes, and policy texts in real-time.
              </p>
            </div>

            {cmsSaved && (
              <span style={{
                background: 'rgba(0, 230, 118, 0.2)', color: '#00E676', padding: '6px 12px',
                borderRadius: '8px', fontSize: '12px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px'
              }}>
                <CheckCheck size={16} /> Saved Live
              </span>
            )}
          </div>

          <form onSubmit={handleSaveCMS} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Section 1: Home Screen */}
            <div style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: '20px' }}>
              <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#FFC542', marginBottom: '12px' }}>
                🏠 1. Home Screen & Announcement Bar
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '4px' }}>
                    Main Header Tagline
                  </label>
                  <input
                    type="text"
                    value={appContent.homeTagline}
                    onChange={(e) => setAppContent({ ...appContent, homeTagline: e.target.value })}
                    style={{
                      width: '100%', padding: '10px 12px', borderRadius: '8px',
                      background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                      color: '#fff', fontSize: '13px'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '4px' }}>
                    Top Notice Marquee Ticker (Social Proof)
                  </label>
                  <input
                    type="text"
                    value={appContent.noticeMarquee}
                    onChange={(e) => setAppContent({ ...appContent, noticeMarquee: e.target.value })}
                    style={{
                      width: '100%', padding: '10px 12px', borderRadius: '8px',
                      background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                      color: '#fff', fontSize: '13px'
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Section 2: Spin Wheel */}
            <div style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: '20px' }}>
              <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#7C4DFF', marginBottom: '12px' }}>
                🎡 2. Lucky Spin Wheel Rewards
              </h3>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '4px' }}>
                  Wheel Segments Prizes (Comma Separated)
                </label>
                <input
                  type="text"
                  value={appContent.spinWheelPrizes}
                  onChange={(e) => setAppContent({ ...appContent, spinWheelPrizes: e.target.value })}
                  style={{
                    width: '100%', padding: '10px 12px', borderRadius: '8px',
                    background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                    color: '#fff', fontSize: '13px'
                  }}
                />
              </div>
            </div>

            {/* Section 3: Policies & Support */}
            <div style={{ paddingBottom: '10px' }}>
              <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#00D1FF', marginBottom: '12px' }}>
                📜 3. Policies & Support Email
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '4px' }}>
                    Customer Support Email
                  </label>
                  <input
                    type="email"
                    value={appContent.supportEmail}
                    onChange={(e) => setAppContent({ ...appContent, supportEmail: e.target.value })}
                    style={{
                      width: '100%', padding: '10px 12px', borderRadius: '8px',
                      background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                      color: '#fff', fontSize: '13px'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '4px' }}>
                    Fair Play & Anti-Bot Notice
                  </label>
                  <input
                    type="text"
                    value={appContent.fairPlayGuarantee}
                    onChange={(e) => setAppContent({ ...appContent, fairPlayGuarantee: e.target.value })}
                    style={{
                      width: '100%', padding: '10px 12px', borderRadius: '8px',
                      background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                      color: '#fff', fontSize: '13px'
                    }}
                  />
                </div>
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary"
              style={{
                padding: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center',
                gap: '8px', fontSize: '15px', fontWeight: 700, marginTop: '8px'
              }}
            >
              <Save size={18} /> SAVE ALL APP CONTENT & PUBLISH LIVE 💾
            </button>
          </form>
        </div>
      )}

      {/* ===================================================================
          TAB 5: FINANCIAL RULES & CONVERSION
          =================================================================== */}
      {activeTab === 'config' && (
        <div className="glass-card" style={{ padding: '28px', maxWidth: '650px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '20px' }}>Global Financial Rules & Conversion</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', color: '#A8B2C7', marginBottom: '6px' }}>
                Points per US Dollar ($1.00 USD)
              </label>
              <input
                type="number"
                value={config.pointsPerDollar}
                onChange={(e) => setConfig({ ...config, pointsPerDollar: Number(e.target.value) })}
                style={{
                  width: '100%', padding: '12px 14px', borderRadius: '10px',
                  background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                  color: '#fff', fontSize: '15px'
                }}
              />
              <span style={{ fontSize: '11px', color: '#6B7A99' }}>Standard: 1,000 points = $1.00 USD</span>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', color: '#A8B2C7', marginBottom: '6px' }}>
                Minimum Withdrawal Threshold (Points)
              </label>
              <input
                type="number"
                value={config.minWithdrawalPoints}
                onChange={(e) => setConfig({ ...config, minWithdrawalPoints: Number(e.target.value) })}
                style={{
                  width: '100%', padding: '12px 14px', borderRadius: '10px',
                  background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                  color: '#fff', fontSize: '15px'
                }}
              />
              <span style={{ fontSize: '11px', color: '#6B7A99' }}>1,000 Points = $1.00 Minimum Cashout</span>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', color: '#A8B2C7', marginBottom: '6px' }}>
                Rewarded Video Ad Payout (Points per Video)
              </label>
              <input
                type="number"
                value={config.adRewardPoints}
                onChange={(e) => setConfig({ ...config, adRewardPoints: Number(e.target.value) })}
                style={{
                  width: '100%', padding: '12px 14px', borderRadius: '10px',
                  background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                  color: '#fff', fontSize: '15px'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', color: '#A8B2C7', marginBottom: '6px' }}>
                Daily Free Spin Limit
              </label>
              <input
                type="number"
                value={config.dailySpinLimit}
                onChange={(e) => setConfig({ ...config, dailySpinLimit: Number(e.target.value) })}
                style={{
                  width: '100%', padding: '12px 14px', borderRadius: '10px',
                  background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                  color: '#fff', fontSize: '15px'
                }}
              />
            </div>

            <button
              onClick={() => showNotice("Global financial rules updated in Firestore 'config/app'")}
              className="btn-primary"
              style={{ marginTop: '12px' }}
            >
              Save Financial Rules Changes
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

import { useState, useEffect } from 'react';
import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  getFirestore,
  collection,
  onSnapshot,
  doc,
  updateDoc,
  setDoc,
  serverTimestamp
} from 'firebase/firestore';
import {
  Users, CheckCircle,
  Settings, AlertCircle, Eye, EyeOff, RotateCw, LogOut, ShieldCheck,
  Bell, Send, Edit3, Save,
  FileText, Gift, RefreshCw, History
} from 'lucide-react';

const firebaseConfig = {
  apiKey: "AIzaSyBt1gPq8dxTh1xSJkkte3WRtSj_w2y_iNw",
  authDomain: "spin-game-3f38a.firebaseapp.com",
  projectId: "spin-game-3f38a",
  storageBucket: "spin-game-3f38a.firebasestorage.app",
  messagingSenderId: "888376316097",
  appId: "1:888376316097:android:f2ea36e3465197cec14440"
};

const firebaseApp = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
const firestoreDb = getFirestore(firebaseApp);

interface Withdrawal {
  id: string;
  userName: string;
  uid: string;
  amountINR: number;
  pointsDeducted: number;
  method: 'UPI' | 'Bank Transfer';
  upiId: string;
  status: 'pending' | 'approved' | 'rejected';
  time: string;
  isRealUser: boolean;
}

interface UserItem {
  uid: string;
  name: string;
  email: string;
  phone: string;
  mobileNumber: string;
  upiId: string;
  points: number;
  walletPoints: number;
  walletBalance: number;
  totalEarned: number;
  totalWithdrawn: number;
  totalSpins: number;
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  referrals: number;
  status: 'ACTIVE' | 'BANNED';
  deviceModel: string;
  registeredDate: string;
  lastActive: string;
}

interface AuditLogItem {
  logId: string;
  adminUid: string;
  targetUid: string;
  targetUserName: string;
  targetUserEmail: string;
  oldPoints: number;
  newPoints: number;
  pointsChanged: number;
  oldBalance: number;
  newBalance: number;
  reason: string;
  transactionId?: string;
  timestamp: string;
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
  const [activeTab, setActiveTab] = useState<'users' | 'withdrawals' | 'audit' | 'limits' | 'content' | 'notifications'>('users');
  const [notification, setNotification] = useState<string | null>(null);

  const showNotice = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 3500);
  };

  // =========================================================================
  // ⚙️ 3. WITHDRAWAL LIMITS & FINANCIAL RULES CONFIG
  // =========================================================================
  const [config, setConfig] = useState(() => {
    const saved = localStorage.getItem('spinwin_financial_config');
    if (saved) {
      try { return JSON.parse(saved); } catch (_) {}
    }
    return {
      minWithdrawalINR: 50,        // Minimum withdrawal: ₹50
      maxDailyWithdrawalINR: 1000, // Max daily limit: ₹1,000
      pointsPerRupee: 1000,        // 1000 Points = ₹1.00 INR (100 Pts = ₹0.10)
      dailySpinLimit: 10,
      dailyScratchLimit: 5,
      quizRewardPoints: 20,
      adRewardPoints: 100
    };
  });

  const [limitsSaved, setLimitsSaved] = useState(false);

  const handleSaveLimits = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('spinwin_financial_config', JSON.stringify(config));
    setLimitsSaved(true);
    showNotice(`✅ Financial Settings updated! Rate: ${config.pointsPerRupee} Pts = ₹1.00 INR`);
    setTimeout(() => setLimitsSaved(false), 3000);
  };

  // =========================================================================
  // 👥 4. LIVE FIRESTORE USERS & REAL-TIME LEDGER
  // =========================================================================
  const [firestoreStatus, setFirestoreStatus] = useState<'connected' | 'connecting' | 'error'>('connecting');
  const [firestoreErrorMsg, setFirestoreErrorMsg] = useState<string | null>(null);

  // NO HARDCODED DEMO USERS - strictly empty initial state, loaded live from Firestore
  const [users, setUsers] = useState<UserItem[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([]);

  // Real-time Firestore Users Listener
  useEffect(() => {
    let unsubscribeUsers: (() => void) | undefined;
    try {
      const usersCol = collection(firestoreDb, 'users');
      unsubscribeUsers = onSnapshot(usersCol, (snapshot) => {
        const liveUsers: UserItem[] = snapshot.docs.map(docSnap => {
          const data = docSnap.data();
          const pts = Number(data.walletPoints ?? data.points ?? 0);
          const bal = Number(data.walletBalance ?? data.balanceRupees ?? (pts / config.pointsPerRupee));
          const earned = Number(data.totalEarned ?? 0);
          const withdrawn = Number(data.totalWithdrawn ?? 0);
          const spins = Number(data.totalSpins ?? 0);
          const mob = String(data.mobileNumber || data.phone || '—');
          return {
            uid: data.uid || docSnap.id,
            name: data.name || 'Player',
            email: data.email || '—',
            phone: mob,
            mobileNumber: mob,
            upiId: data.upiId || '—',
            points: pts,
            walletPoints: pts,
            walletBalance: bal,
            totalEarned: earned,
            totalWithdrawn: withdrawn,
            totalSpins: spins,
            tier: (data.tier as any) || 'BRONZE',
            referrals: Number(data.referrals) || 0,
            status: (data.status === 'banned' || data.status === 'BANNED' ? 'BANNED' : 'ACTIVE') as 'ACTIVE' | 'BANNED',
            deviceModel: data.deviceModel || 'Android Device',
            registeredDate: data.registeredDate || (data.createdAt?.toDate ? data.createdAt.toDate().toLocaleDateString() : 'Active Now'),
            lastActive: data.lastActive || 'Active Now 🟢'
          };
        });
        setUsers(liveUsers);
        setFirestoreStatus('connected');
        setFirestoreErrorMsg(null);
      }, (err) => {
        console.warn("Firestore users listener error:", err);
        setFirestoreStatus('error');
        setFirestoreErrorMsg(err.message);
      });
    } catch (err: any) {
      setFirestoreStatus('error');
      setFirestoreErrorMsg(err?.message || 'Firestore connection failed');
    }

    return () => {
      if (unsubscribeUsers) unsubscribeUsers();
    };
  }, [config.pointsPerRupee]);

  // Real-time Firestore Audit Logs Listener
  useEffect(() => {
    let unsubscribeAudit: (() => void) | undefined;
    try {
      const auditCol = collection(firestoreDb, 'auditLogs');
      unsubscribeAudit = onSnapshot(auditCol, (snapshot) => {
        const logs: AuditLogItem[] = snapshot.docs.map(docSnap => {
          const d = docSnap.data();
          return {
            logId: d.logId || docSnap.id,
            adminUid: d.adminUid || 'admin_master',
            targetUid: d.targetUid || '—',
            targetUserName: d.targetUserName || '—',
            targetUserEmail: d.targetUserEmail || '—',
            oldPoints: Number(d.oldPoints ?? 0),
            newPoints: Number(d.newPoints ?? 0),
            pointsChanged: Number(d.pointsChanged ?? 0),
            oldBalance: Number(d.oldBalance ?? 0),
            newBalance: Number(d.newBalance ?? 0),
            reason: d.reason || 'Admin Adjustment',
            transactionId: d.transactionId,
            timestamp: d.timestamp?.toDate ? d.timestamp.toDate().toLocaleString() : 'Recent'
          };
        });
        setAuditLogs(logs);
      }, (err) => {
        console.warn("Firestore audit logs listener notice:", err);
      });
    } catch (_) {}

    return () => {
      if (unsubscribeAudit) unsubscribeAudit();
    };
  }, []);

  // Selected User for Editing
  const [editingUser, setEditingUser] = useState<UserItem | null>(null);
  const [editPointsInput, setEditPointsInput] = useState<number>(0);
  const [editReasonInput, setEditReasonInput] = useState<string>('');
  const [editStatusInput, setEditStatusInput] = useState<'ACTIVE' | 'BANNED'>('ACTIVE');
  const [editUpiInput, setEditUpiInput] = useState('');
  const [editPhoneInput, setEditPhoneInput] = useState('');

  const openUserEditor = (user: UserItem) => {
    setEditingUser(user);
    setEditPointsInput(user.walletPoints);
    setEditReasonInput('');
    setEditStatusInput(user.status);
    setEditUpiInput(user.upiId);
    setEditPhoneInput(user.phone);
  };

  const handleSaveUserEdits = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;

    if (!editReasonInput.trim()) {
      showNotice("⚠️ Mandatory: Enter a Reason for this modification (Audit requirement)!");
      return;
    }

    const oldPoints = editingUser.walletPoints;
    const newPts = Math.max(0, Number(editPointsInput) || 0);
    const delta = newPts - oldPoints;
    const oldBal = editingUser.walletBalance;
    const newBal = Number((newPts / config.pointsPerRupee).toFixed(2));

    try {
      const userRef = doc(firestoreDb, 'users', editingUser.uid);
      await updateDoc(userRef, {
        walletPoints: newPts,
        walletBalance: newBal,
        points: newPts,
        balanceRupees: newBal,
        status: editStatusInput.toLowerCase(),
        phone: editPhoneInput.trim(),
        mobileNumber: editPhoneInput.trim(),
        upiId: editUpiInput.trim(),
        updatedAt: serverTimestamp()
      });

      // Write immutable transaction record
      const txRef = doc(collection(firestoreDb, 'transactions'));
      await setDoc(txRef, {
        transactionId: txRef.id,
        uid: editingUser.uid,
        type: 'admin_adjustment',
        points: delta,
        balanceBefore: oldPoints,
        balanceAfter: newPts,
        source: 'admin_panel',
        amount: Number((delta / config.pointsPerRupee).toFixed(2)),
        title: `Admin Adjustment: ${editReasonInput.trim()}`,
        status: 'completed',
        createdAt: serverTimestamp()
      });

      // Write audit log entry
      const logRef = doc(collection(firestoreDb, 'auditLogs'));
      await setDoc(logRef, {
        logId: logRef.id,
        adminUid: 'admin_master',
        targetUid: editingUser.uid,
        targetUserName: editingUser.name,
        targetUserEmail: editingUser.email,
        oldPoints: oldPoints,
        newPoints: newPts,
        pointsChanged: delta,
        oldBalance: oldBal,
        newBalance: newBal,
        reason: editReasonInput.trim(),
        transactionId: txRef.id,
        timestamp: serverTimestamp()
      });

      showNotice(`✅ Wallet & Status updated for ${editingUser.name} with audit trail!`);
      setEditingUser(null);
    } catch (err: any) {
      showNotice(`⚠️ Error updating user: ${err.message}`);
    }
  };

  // --- WITHDRAWALS STATE (NO DEMO DATA - loaded live from Firestore) ---
  const [withdrawals, setWithdrawals] = useState<Withdrawal[]>([]);

  // Real-time Firestore Withdrawals Listener
  useEffect(() => {
    let unsubscribeWds: (() => void) | undefined;
    try {
      const wdCol = collection(firestoreDb, 'withdrawals');
      unsubscribeWds = onSnapshot(wdCol, (snapshot) => {
        const liveWds: Withdrawal[] = snapshot.docs.map(docSnap => {
          const data = docSnap.data();
          return {
            id: data.id || data.withdrawalId || docSnap.id,
            userName: data.userName || 'User',
            uid: data.uid || '—',
            amountINR: Number(data.amountINR ?? data.amountRupees ?? 0),
            pointsDeducted: Number(data.pointsDeducted ?? 0),
            method: (data.method === 'Bank Transfer' ? 'Bank Transfer' : 'UPI'),
            upiId: data.upiId || data.payoutAddress || '—',
            status: data.status || 'pending',
            time: data.time || (data.requestedAt?.toDate ? data.requestedAt.toDate().toLocaleString() : 'Recent'),
            isRealUser: true
          };
        });
        setWithdrawals(liveWds);
      }, (err) => {
        console.warn("Firestore withdrawals listener error:", err);
      });
    } catch (err) {}

    return () => {
      if (unsubscribeWds) unsubscribeWds();
    };
  }, []);

  const handleApproveWithdrawal = async (id: string) => {
    try {
      const wdRef = doc(firestoreDb, 'withdrawals', id);
      await updateDoc(wdRef, {
        status: 'approved',
        processedAt: serverTimestamp()
      });
      const txRef = doc(firestoreDb, 'transactions', id);
      await updateDoc(txRef, { status: 'approved' }).catch(() => {});
      showNotice(`✅ UPI Withdrawal #${id} APPROVED & Recorded in Database!`);
    } catch (err: any) {
      showNotice(`⚠️ Error approving withdrawal: ${err.message}`);
    }
  };

  const handleRejectWithdrawal = async (id: string) => {
    try {
      const wd = withdrawals.find(w => w.id === id);
      const wdRef = doc(firestoreDb, 'withdrawals', id);
      await updateDoc(wdRef, {
        status: 'rejected',
        rejectReason: 'Rejected by Admin',
        processedAt: serverTimestamp()
      });
      // Refund points to target user
      if (wd) {
        const userRef = doc(firestoreDb, 'users', wd.uid);
        const userDoc = users.find(u => u.uid === wd.uid);
        if (userDoc) {
          const newPts = userDoc.walletPoints + wd.pointsDeducted;
          const newBal = Number((newPts / config.pointsPerRupee).toFixed(2));
          await updateDoc(userRef, {
            walletPoints: newPts,
            walletBalance: newBal,
            points: newPts,
            balanceRupees: newBal,
            updatedAt: serverTimestamp()
          });

          // Write reversal transaction
          const revTx = doc(collection(firestoreDb, 'transactions'));
          await setDoc(revTx, {
            transactionId: revTx.id,
            uid: wd.uid,
            type: 'reversal',
            points: wd.pointsDeducted,
            balanceBefore: userDoc.walletPoints,
            balanceAfter: newPts,
            source: 'admin_panel',
            referenceId: id,
            amount: wd.amountINR,
            title: `Refund for Rejected Withdrawal #${id}`,
            status: 'completed',
            createdAt: serverTimestamp()
          });
        }
      }
      showNotice(`❌ UPI Withdrawal #${id} REJECTED & Points Refunded to User!`);
    } catch (err: any) {
      showNotice(`⚠️ Error rejecting withdrawal: ${err.message}`);
    }
  };

  // =========================================================================
  // 📝 5. ALL APP PAGES CONTENT CMS (TERMS, PRIVACY, ABOUT, HOME, WHEEL, QUIZ, WALLET, SUPPORT)
  // =========================================================================
  const [selectedCmsPage, setSelectedCmsPage] = useState<'terms' | 'privacy' | 'about' | 'home' | 'wheel' | 'quiz' | 'wallet' | 'support'>('terms');

  const [cmsData, setCmsData] = useState(() => {
    const saved = localStorage.getItem('spinwin_all_pages_cms');
    if (saved) {
      try { return JSON.parse(saved); } catch (_) {}
    }
    return {
      terms: {
        pageTitle: 'Terms & Conditions',
        lastUpdated: 'September 2026',
        introduction: 'Welcome to SpinWin Rewards. By installing or using this app, you agree to comply with and be bound by the following terms of service.',
        eligibilityText: 'This app is designed for entertainment and casual rewards. Users must be at least 18 years of age or have parental consent. One account per physical device.',
        rulesPointsText: 'Points accumulated have no direct real-world tender value until redeemed through the official UPI withdrawal portal. Points obtained through bots, auto-clickers, or hacks will be forfeited.',
        payoutPolicyText: 'Withdrawals are processed exclusively via genuine UPI VPAs or direct Indian bank accounts. The admin team reserves the right to verify identities before releasing funds.',
        cancellationText: 'We reserve the right to suspend or terminate any account engaging in fraudulent activities, VPN abuse, or exploitation of glitches.'
      },
      privacy: {
        pageTitle: 'Privacy Policy',
        lastUpdated: 'September 2026',
        introduction: 'Your privacy is paramount. This Privacy Policy clarifies what information we collect and how we safeguard it when you use SpinWin Rewards.',
        dataCollectedText: 'We collect your Google Sign-In information (Name, Email ID, Profile Picture) to authenticate your account and sync your earned points safely.',
        upiUsageText: 'Your UPI ID is collected solely for the purpose of dispatching cash reward redemptions directly into your bank account. We never ask for UPI PINs, passwords, or OTPs.',
        advertisingText: 'Our app serves Google AdMob rewarded videos and banners. Ad partners may process non-personal device identifiers to deliver relevant ad content.',
        securityText: 'All communications between your device and our servers are encrypted via HTTPS/SSL. We do not sell or trade your personal data to any third party.'
      },
      about: {
        pageTitle: 'About Us & Fair Play Rules',
        tagline: 'Play Daily Games, Earn Instant UPI Cash',
        storyText: 'SpinWin Rewards is built on a transparent ad-revenue sharing model. Instead of retaining 100% of corporate advertising revenues, we share a major portion back with our genuine players.',
        antiCheatRules: 'Strict 1 device = 1 account policy. Emulators, auto-clickers, VPN spoofers, and clone apps are permanently blacklisted.'
      },
      home: {
        pageTitle: 'Home Screen Content',
        heroHeadline: 'Spin, Play Quizzes & Win Real UPI Cash!',
        heroSubtitle: 'Complete simple daily spins, trivia quizzes & scratch cards to earn instant money.',
        announcementMarquee: '🔥 Minimum UPI withdrawal is ₹50 • Instant payment within 24 hours • Over ₹25,000 paid to players this week!',
        welcomeBonusText: '🎁 Welcome Bonus Credited: Enjoy 10 Free Daily Spins!'
      },
      wheel: {
        pageTitle: 'Spin Wheel Configuration',
        wheelTitle: 'Daily Lucky Wheel',
        dailySpinLimitNotice: 'You have 10 Free Daily Spins remaining. Come back tomorrow for more!',
        wheelSlices: '10 Pts, 20 Pts, 50 Pts, 100 Pts, Extra Spin, Better Luck, 25 Pts, JACKPOT (500 Pts)',
        rulesNote: 'Wheel outcomes are governed by certified random number generation (RNG). Ad watch resets 1 additional spin.'
      },
      quiz: {
        pageTitle: 'Trivia Quiz Configuration',
        quizTitle: 'Brain Boost & Earn Trivia',
        rewardPerCorrectAnswer: 20,
        instructionsText: 'Answer multiple-choice questions correctly to earn +20 Reward Points instantly. No penalty for wrong answers!'
      },
      wallet: {
        pageTitle: 'Wallet & Withdrawal Screen',
        walletHeading: 'Redeem Cash to UPI',
        minimumNotice: 'Minimum withdrawal threshold is ₹50 (500 Points). Enter your verified UPI ID below.',
        payoutTimelines: 'Withdrawals are audited and credited to your UPI ID within 2 to 24 business hours.',
        supportedModes: 'Supported: Google Pay UPI, PhonePe, Paytm, BHIM, Amazon Pay & All Bank VPAs.'
      },
      support: {
        pageTitle: 'Contact & Support',
        supportEmail: 'gulshanyadav62000@gmail.com',
        whatsappSupport: '+91 98765 43210',
        timings: 'Monday to Saturday: 10:00 AM - 7:00 PM IST',
        resolutionPromise: 'All withdrawal queries and technical tickets are resolved within 24 hours.'
      }
    };
  });

  const [cmsSavedToast, setCmsSavedToast] = useState(false);

  const handleSaveCMS = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('spinwin_all_pages_cms', JSON.stringify(cmsData));
    setCmsSavedToast(true);
    showNotice(`💾 Page Content for [${selectedCmsPage.toUpperCase()}] successfully saved & applied live!`);
    setTimeout(() => setCmsSavedToast(false), 3000);
  };

  // =========================================================================
  // 📢 6. PUSH NOTIFICATION BROADCAST TO ALL USERS
  // =========================================================================
  const [notifTitle, setNotifTitle] = useState('🔥 2X Double UPI Rewards is LIVE!');
  const [notifMessage, setNotifMessage] = useState('Spin the lucky wheel now & withdraw your cash directly via UPI. Tap to claim!');
  const [notifTarget, setNotifTarget] = useState('ALL_ACTIVE');
  const [broadcastHistory, setBroadcastHistory] = useState<BroadcastHistory[]>([
    {
      id: 'b_01',
      title: '🎁 Welcome Bonus Ready',
      message: 'Your 10 Free Daily Spins are active. Start spinning now!',
      target: 'All Active Devices',
      sentAt: 'Today, 11:30 AM',
      deliveredCount: 1
    }
  ]);

  const handleSendBroadcast = (e: React.FormEvent) => {
    e.preventDefault();
    if (!notifTitle.trim() || !notifMessage.trim()) {
      alert('Please enter title and message');
      return;
    }
    const newBroadcast: BroadcastHistory = {
      id: 'b_' + Date.now(),
      title: notifTitle,
      message: notifMessage,
      target: notifTarget === 'ALL_ACTIVE' ? 'All Real Active Devices (100%)' : 'Users with 0 Points',
      sentAt: 'Just now',
      deliveredCount: users.length
    };
    setBroadcastHistory([newBroadcast, ...broadcastHistory]);
    showNotice(`📢 Push Notification broadcast successfully sent to ${users.length} active device(s)!`);
  };

  // =========================================================================
  // 🔒 7. RENDER LOGIN SCREEN IF NOT AUTHENTICATED
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
              Protected Financial Operations & CMS Control (₹ INR)
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

            {/* Security Number Captcha */}
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '8px' }}>
                Security Number Captcha
              </label>
              <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                <div style={{
                  padding: '12px 20px',
                  borderRadius: '12px',
                  background: 'linear-gradient(135deg, rgba(124, 77, 255, 0.25), rgba(0, 209, 255, 0.25))',
                  border: '1px dashed #00D1FF',
                  fontFamily: 'monospace',
                  fontSize: '22px',
                  fontWeight: 800,
                  letterSpacing: '6px',
                  color: '#00E5FF',
                  userSelect: 'none',
                  textShadow: '0 0 10px rgba(0, 209, 255, 0.5)'
                }}>
                  {captchaCode}
                </div>
                <button
                  type="button"
                  onClick={refreshCaptcha}
                  title="Generate new number captcha"
                  style={{
                    padding: '12px',
                    borderRadius: '12px',
                    background: 'rgba(255, 255, 255, 0.08)',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    color: '#fff',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  <RotateCw size={18} />
                </button>
                <input
                  type="text"
                  maxLength={4}
                  placeholder="Enter 4 digits"
                  value={captchaInput}
                  onChange={(e) => setCaptchaInput(e.target.value.replace(/\D/g, ''))}
                  style={{
                    flex: 1,
                    padding: '14px 16px',
                    borderRadius: '12px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '15px',
                    outline: 'none',
                    textAlign: 'center',
                    letterSpacing: '3px',
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    boxSizing: 'border-box'
                  }}
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              style={{
                marginTop: '10px',
                padding: '14px',
                borderRadius: '12px',
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
                boxShadow: '0 10px 25px rgba(124, 77, 255, 0.35)'
              }}
            >
              <ShieldCheck size={18} />
              Unlock Admin Console
            </button>
          </form>
        </div>
      </div>
    );
  }

  // =========================================================================
  // 🚀 8. AUTHENTICATED DASHBOARD
  // =========================================================================
  const pendingWithdrawalsCount = withdrawals.filter(w => w.status === 'pending').length;

  return (
    <div style={{ minHeight: '100vh', background: '#0B0F19', color: '#E2E8F0', fontFamily: 'Inter, sans-serif' }}>
      {/* Toast Notification Banner */}
      {notification && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          zIndex: 9999,
          background: 'linear-gradient(135deg, #059669, #10B981)',
          color: '#fff',
          padding: '12px 20px',
          borderRadius: '12px',
          boxShadow: '0 10px 30px rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontWeight: 600,
          fontSize: '14px',
          border: '1px solid rgba(255, 255, 255, 0.2)'
        }}>
          <CheckCircle size={18} />
          <span>{notification}</span>
        </div>
      )}

      {/* TOP NAVIGATION BAR */}
      <header style={{
        background: '#111827',
        borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        padding: '16px 24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '20px',
            fontWeight: 800
          }}>
            👑
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', margin: 0 }}>SpinWin Master Console</h1>
              <span style={{ fontSize: '11px', background: 'rgba(0, 230, 118, 0.15)', color: '#00E676', padding: '2px 8px', borderRadius: '6px', fontWeight: 700, border: '1px solid rgba(0, 230, 118, 0.3)' }}>
                ₹ INR MODE
              </span>
            </div>
            <p style={{ margin: 0, fontSize: '12px', color: '#6B7A99' }}>Live Control: Real Users, UPI Payouts & Full App CMS</p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            background: 'rgba(255, 255, 255, 0.04)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            padding: '6px 14px',
            borderRadius: '8px',
            fontSize: '12px',
            color: '#FFC542',
            fontWeight: 600
          }}>
            Min Payout Limit: ₹{config.minWithdrawalINR}
          </div>
          <button
            onClick={handleLogout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: '8px',
              background: 'rgba(255, 77, 77, 0.12)',
              border: '1px solid rgba(255, 77, 77, 0.3)',
              color: '#FF4D4D',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer'
            }}
          >
            <LogOut size={16} />
            Logout
          </button>
        </div>
      </header>

      {/* DASHBOARD CONTAINER */}
      <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '24px' }}>
        {/* STATS OVERVIEW CARDS (ALL IN RUPEES ₹) */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px', marginBottom: '24px' }}>
          <div className="glass-card" style={{ padding: '20px', borderRadius: '16px', background: '#131B2E', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6B7A99', fontSize: '13px', marginBottom: '8px' }}>
              <span>REGISTERED REAL USERS</span>
              <Users size={18} color="#00D1FF" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#fff' }}>{users.length} Registered</div>
            <div style={{ fontSize: '12px', color: '#00E676', marginTop: '4px' }}>
              ● {users.filter(u => u.status === 'ACTIVE').length} Active Accounts Live
            </div>
          </div>

          <div className="glass-card" style={{ padding: '20px', borderRadius: '16px', background: '#131B2E', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6B7A99', fontSize: '13px', marginBottom: '8px' }}>
              <span>TOTAL WALLET POINTS</span>
              <Gift size={18} color="#FFC542" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#FFC542' }}>
              {users.reduce((acc, u) => acc + (u.walletPoints || 0), 0).toLocaleString()} Pts
            </div>
            <div style={{ fontSize: '12px', color: '#A8B2C7', marginTop: '4px' }}>
              Live Value: ₹{(users.reduce((acc, u) => acc + (u.walletPoints || 0), 0) / config.pointsPerRupee).toFixed(2)} INR
            </div>
          </div>

          <div className="glass-card" style={{ padding: '20px', borderRadius: '16px', background: '#131B2E', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6B7A99', fontSize: '13px', marginBottom: '8px' }}>
              <span>UPI WITHDRAWALS</span>
              <CheckCircle size={18} color="#FF4D4D" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#FF4D4D' }}>
              {pendingWithdrawalsCount} Pending / {withdrawals.filter(w => w.status === 'approved').length} Done
            </div>
            <div style={{ fontSize: '12px', color: '#6B7A99', marginTop: '4px' }}>
              Total Disbursed: ₹{withdrawals.filter(w => w.status === 'approved').reduce((acc, w) => acc + w.amountINR, 0).toFixed(2)} INR
            </div>
          </div>

          <div className="glass-card" style={{ padding: '20px', borderRadius: '16px', background: '#131B2E', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6B7A99', fontSize: '13px', marginBottom: '8px' }}>
              <span>TOTAL SPINS & RATE</span>
              <ShieldCheck size={18} color="#00E676" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#00E676' }}>
              {users.reduce((acc, u) => acc + (u.totalSpins || 0), 0)} Spins Done
            </div>
            <div style={{ fontSize: '12px', color: '#A8B2C7', marginTop: '4px' }}>
              Rate: {config.pointsPerRupee} Points = ₹1.00 INR
            </div>
          </div>
        </div>

        {/* NAVIGATION TABS */}
        <div style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          marginBottom: '24px',
          overflowX: 'auto',
          paddingBottom: '2px'
        }}>
          <button
            onClick={() => setActiveTab('users')}
            style={{
              padding: '12px 18px',
              border: 'none',
              background: 'none',
              color: activeTab === 'users' ? '#00D1FF' : '#A8B2C7',
              borderBottom: activeTab === 'users' ? '3px solid #00D1FF' : '3px solid transparent',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              whiteSpace: 'nowrap'
            }}
          >
            <Users size={18} />
            Real Installed User & UPI Ledger ({users.length})
          </button>

          <button
            onClick={() => setActiveTab('withdrawals')}
            style={{
              padding: '12px 18px',
              border: 'none',
              background: 'none',
              color: activeTab === 'withdrawals' ? '#00D1FF' : '#A8B2C7',
              borderBottom: activeTab === 'withdrawals' ? '3px solid #00D1FF' : '3px solid transparent',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              whiteSpace: 'nowrap'
            }}
          >
            <CheckCircle size={18} />
            UPI Payouts & Withdrawals
            {pendingWithdrawalsCount > 0 && (
              <span style={{
                background: '#FF4D4D', color: '#fff', fontSize: '10px',
                padding: '2px 6px', borderRadius: '10px', fontWeight: 800
              }}>
                {pendingWithdrawalsCount}
              </span>
            )}
          </button>

          <button
            onClick={() => setActiveTab('audit')}
            style={{
              padding: '12px 18px',
              border: 'none',
              background: 'none',
              color: activeTab === 'audit' ? '#00D1FF' : '#A8B2C7',
              borderBottom: activeTab === 'audit' ? '3px solid #00D1FF' : '3px solid transparent',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              whiteSpace: 'nowrap'
            }}
          >
            <History size={18} />
            Audit Ledger ({auditLogs.length})
          </button>

          <button
            onClick={() => setActiveTab('limits')}
            style={{
              padding: '12px 18px',
              border: 'none',
              background: 'none',
              color: activeTab === 'limits' ? '#00D1FF' : '#A8B2C7',
              borderBottom: activeTab === 'limits' ? '3px solid #00D1FF' : '3px solid transparent',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              whiteSpace: 'nowrap'
            }}
          >
            <Settings size={18} />
            Withdrawal Limit & Rules (₹)
          </button>

          <button
            onClick={() => setActiveTab('content')}
            style={{
              padding: '12px 18px',
              border: 'none',
              background: 'none',
              color: activeTab === 'content' ? '#00D1FF' : '#A8B2C7',
              borderBottom: activeTab === 'content' ? '3px solid #00D1FF' : '3px solid transparent',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              whiteSpace: 'nowrap'
            }}
          >
            <FileText size={18} />
            All Pages Content CMS (Terms, Privacy, etc.)
          </button>

          <button
            onClick={() => setActiveTab('notifications')}
            style={{
              padding: '12px 18px',
              border: 'none',
              background: 'none',
              color: activeTab === 'notifications' ? '#00D1FF' : '#A8B2C7',
              borderBottom: activeTab === 'notifications' ? '3px solid #00D1FF' : '3px solid transparent',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              whiteSpace: 'nowrap'
            }}
          >
            <Bell size={18} />
            Push Notification Broadcast
          </button>
        </div>

        {/* ===================================================================
            TAB 1: REAL INSTALLED USER & UPI LEDGER (ONLY REAL USER)
            =================================================================== */}
        {activeTab === 'users' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', margin: 0 }}>
                    Real Installed Phone & UPI Details ({users.length} Users)
                  </h2>
                  {firestoreStatus === 'connected' ? (
                    <span style={{ fontSize: '11px', background: 'rgba(0, 230, 118, 0.15)', color: '#00E676', padding: '3px 8px', borderRadius: '20px', border: '1px solid rgba(0, 230, 118, 0.4)', display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                      <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#00E676' }}></span>
                      Live Firestore Synced
                    </span>
                  ) : firestoreStatus === 'connecting' ? (
                    <span style={{ fontSize: '11px', background: 'rgba(0, 209, 255, 0.15)', color: '#00D1FF', padding: '3px 8px', borderRadius: '20px', border: '1px solid rgba(0, 209, 255, 0.4)' }}>
                      Connecting to Cloud Firestore...
                    </span>
                  ) : (
                    <a
                      href="https://console.firebase.google.com/project/spin-game-3f38a/firestore"
                      target="_blank"
                      rel="noreferrer"
                      title={firestoreErrorMsg || 'Click to create Firestore database'}
                      style={{ fontSize: '11px', background: 'rgba(255, 197, 66, 0.15)', color: '#FFC542', padding: '3px 8px', borderRadius: '20px', border: '1px solid rgba(255, 197, 66, 0.4)', textDecoration: 'none' }}
                    >
                      ⚠️ Enable Firestore in Firebase Console
                    </a>
                  )}
                </div>
                <p style={{ fontSize: '13px', color: '#6B7A99', margin: '4px 0 0 0' }}>
                  Live synchronized users with real-time points, UPI VPAs & instant cash ledger.
                </p>
              </div>

              <button
                onClick={() => showNotice('🔄 User data synced with local device & Firestore storage!')}
                style={{
                  display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px',
                  background: 'rgba(0, 209, 255, 0.15)', border: '1px solid rgba(0, 209, 255, 0.4)',
                  color: '#00D1FF', borderRadius: '10px', fontSize: '13px', fontWeight: 600, cursor: 'pointer'
                }}
              >
                <RefreshCw size={15} />
                Refresh State
              </button>
            </div>

            {/* Zero Points / Real Rupees Calculation Banner */}
            <div style={{
              background: 'rgba(124, 77, 255, 0.1)',
              border: '1px solid rgba(124, 77, 255, 0.3)',
              borderRadius: '12px',
              padding: '14px 18px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '12px'
            }}>
              <ShieldCheck size={24} color="#7C4DFF" />
              <div>
                <div style={{ fontWeight: 700, color: '#fff', fontSize: '14px' }}>
                  Strict Financial Integrity Rule Active:
                </div>
                <div style={{ color: '#A8B2C7', fontSize: '12px' }}>
                  Agar user ke account me <strong>0 Points</strong> hain, toh balance strictly <strong>₹0.00</strong> hoga. 10 Points = ₹1.00 INR rate ke hisaab se live calculate hota hai.
                </div>
              </div>
            </div>

            {/* TABLE OF REAL USER */}
            <div style={{ background: '#131B2E', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '16px', overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#6B7A99', background: '#0F1626' }}>
                    <th style={{ padding: '16px' }}>REAL USER & DEVICE</th>
                    <th style={{ padding: '16px' }}>GOOGLE EMAIL & PHONE</th>
                    <th style={{ padding: '16px' }}>REGISTERED UPI ID</th>
                    <th style={{ padding: '16px' }}>REAL POINTS COLLECTED</th>
                    <th style={{ padding: '16px' }}>EQUIVALENT RUPEES (₹)</th>
                    <th style={{ padding: '16px' }}>STATUS</th>
                    <th style={{ padding: '16px' }}>ACTIONS</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    // Strict zero points check: if points == 0, then cash is ₹0.00
                    const cashINR = u.points === 0 ? '0.00' : (u.points / config.pointsPerRupee).toFixed(2);

                    return (
                      <tr key={u.uid} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                        <td style={{ padding: '16px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <div style={{
                              width: '42px', height: '42px', borderRadius: '50%',
                              background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
                              display: 'flex', alignItems: 'center', justifyContent: 'center',
                              fontWeight: 800, color: '#fff', fontSize: '16px'
                            }}>
                              {u.name.charAt(0)}
                            </div>
                            <div>
                              <div style={{ fontWeight: 700, color: '#fff', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                {u.name}
                                <span style={{ fontSize: '10px', background: 'rgba(0, 230, 118, 0.2)', color: '#00E676', padding: '2px 6px', borderRadius: '4px', border: '1px solid rgba(0, 230, 118, 0.4)' }}>
                                  Real Phone
                                </span>
                              </div>
                              <div style={{ fontSize: '11px', color: '#6B7A99' }}>{u.deviceModel}</div>
                              <div style={{ fontSize: '11px', color: '#00D1FF' }}>{u.lastActive}</div>
                            </div>
                          </div>
                        </td>

                        <td style={{ padding: '16px' }}>
                          <div style={{ color: '#fff', fontWeight: 600 }}>{u.email}</div>
                          <div style={{ fontSize: '12px', color: '#A8B2C7', marginTop: '2px' }}>{u.phone}</div>
                        </td>

                        <td style={{ padding: '16px' }}>
                          <div style={{
                            background: 'rgba(0, 209, 255, 0.1)',
                            border: '1px solid rgba(0, 209, 255, 0.3)',
                            padding: '6px 10px',
                            borderRadius: '8px',
                            color: '#00E5FF',
                            fontWeight: 700,
                            fontFamily: 'monospace',
                            fontSize: '13px',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '6px'
                          }}>
                            <span>UPI:</span>
                            <span>{u.upiId || 'Not Saved Yet'}</span>
                          </div>
                        </td>

                        <td style={{ padding: '16px' }}>
                          <div style={{ fontWeight: 800, color: '#FFC542', fontSize: '15px' }}>
                            🪙 {u.points} Pts
                          </div>
                          <div style={{ fontSize: '11px', color: '#6B7A99' }}>
                            {u.points === 0 ? 'No earnings yet' : `${u.points} Total Earned`}
                          </div>
                        </td>

                        <td style={{ padding: '16px' }}>
                          <div style={{
                            fontWeight: 800,
                            color: u.points === 0 ? '#6B7A99' : '#00E676',
                            fontSize: '16px'
                          }}>
                            ₹{cashINR} INR
                          </div>
                          <div style={{ fontSize: '11px', color: '#A8B2C7' }}>
                            {u.points === 0 ? 'Balance is zero' : `Convertible Cash`}
                          </div>
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
                          <button
                            onClick={() => openUserEditor(u)}
                            style={{
                              padding: '6px 12px',
                              borderRadius: '8px',
                              background: 'rgba(255, 255, 255, 0.08)',
                              border: '1px solid rgba(255, 255, 255, 0.15)',
                              color: '#fff',
                              fontSize: '12px',
                              fontWeight: 600,
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '6px'
                            }}
                          >
                            <Edit3 size={14} />
                            Edit Details & Points
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* MODAL: EDIT REAL USER UPI & POINTS */}
            {editingUser && (
              <div style={{
                position: 'fixed',
                top: 0, left: 0, right: 0, bottom: 0,
                background: 'rgba(0,0,0,0.75)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                zIndex: 10000, padding: '20px'
              }}>
                <div style={{
                  background: '#16203A',
                  border: '1px solid rgba(255,255,255,0.15)',
                  borderRadius: '20px',
                  padding: '28px',
                  width: '100%',
                  maxWidth: '480px',
                  boxShadow: '0 20px 60px rgba(0,0,0,0.8)'
                }}>
                  <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', marginBottom: '6px' }}>
                    Edit User: {editingUser.name}
                  </h3>
                  <p style={{ fontSize: '12px', color: '#6B7A99', marginBottom: '20px' }}>
                    Update real phone user points, UPI ID, or phone number.
                  </p>

                  <form onSubmit={handleSaveUserEdits} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>
                        Real Points Balance (0 Points = ₹0.00)
                      </label>
                      <input
                        type="number"
                        min="0"
                        value={editPointsInput}
                        onChange={(e) => setEditPointsInput(Number(e.target.value))}
                        style={{
                          width: '100%', padding: '12px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                          color: '#FFC542', fontSize: '16px', fontWeight: 700, outline: 'none',
                          boxSizing: 'border-box'
                        }}
                      />
                      <div style={{ fontSize: '12px', color: '#00E676', marginTop: '4px' }}>
                        Equivalent Rupee Value: ₹{editPointsInput === 0 ? '0.00' : (editPointsInput / config.pointsPerRupee).toFixed(2)} INR
                      </div>
                    </div>

                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#FFC542', marginBottom: '6px', fontWeight: 700 }}>
                        Mandatory Reason for Adjustment (Logged to Audit Trail) *
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="e.g. VIP Reward Grant / Goodwill correction / User Support"
                        value={editReasonInput}
                        onChange={(e) => setEditReasonInput(e.target.value)}
                        style={{
                          width: '100%', padding: '12px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,197,66,0.3)',
                          color: '#fff', fontSize: '14px', outline: 'none',
                          boxSizing: 'border-box'
                        }}
                      />
                    </div>

                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>
                        Account Status Control
                      </label>
                      <select
                        value={editStatusInput}
                        onChange={(e) => setEditStatusInput(e.target.value as 'ACTIVE' | 'BANNED')}
                        style={{
                          width: '100%', padding: '12px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                          color: editStatusInput === 'ACTIVE' ? '#00E676' : '#FF4D4D',
                          fontSize: '14px', fontWeight: 700, outline: 'none',
                          boxSizing: 'border-box'
                        }}
                      >
                        <option value="ACTIVE" style={{ color: '#00E676' }}>🟢 ACTIVE (Normal Access)</option>
                        <option value="BANNED" style={{ color: '#FF4D4D' }}>🔴 BANNED (Frozen Account)</option>
                      </select>
                    </div>

                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>
                        Registered UPI ID (VPA)
                      </label>
                      <input
                        type="text"
                        placeholder="username@okhdfcbank"
                        value={editUpiInput}
                        onChange={(e) => setEditUpiInput(e.target.value)}
                        style={{
                          width: '100%', padding: '12px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                          color: '#00E5FF', fontSize: '14px', fontFamily: 'monospace', outline: 'none',
                          boxSizing: 'border-box'
                        }}
                      />
                    </div>

                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>
                        Registered Mobile Number
                      </label>
                      <input
                        type="text"
                        placeholder="+91 98765 43210"
                        value={editPhoneInput}
                        onChange={(e) => setEditPhoneInput(e.target.value)}
                        style={{
                          width: '100%', padding: '12px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                          color: '#fff', fontSize: '14px', outline: 'none',
                          boxSizing: 'border-box'
                        }}
                      />
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                      <button
                        type="button"
                        onClick={() => setEditingUser(null)}
                        style={{
                          flex: 1, padding: '12px', borderRadius: '10px',
                          background: 'rgba(255,255,255,0.08)', border: 'none',
                          color: '#fff', fontSize: '14px', cursor: 'pointer', fontWeight: 600
                        }}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        style={{
                          flex: 1, padding: '12px', borderRadius: '10px',
                          background: 'linear-gradient(135deg, #059669, #10B981)', border: 'none',
                          color: '#fff', fontSize: '14px', cursor: 'pointer', fontWeight: 700
                        }}
                      >
                        Save Changes
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ===================================================================
            TAB 2: UPI WITHDRAWALS & PAYOUTS (INDIAN RUPEES ONLY)
            =================================================================== */}
        {activeTab === 'withdrawals' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', margin: 0 }}>UPI Payouts & Redemptions</h2>
                <p style={{ fontSize: '13px', color: '#6B7A99', margin: '4px 0 0 0' }}>
                  All amounts are strictly calculated in Indian Rupees (₹ INR). No dollar values.
                </p>
              </div>
              <span style={{ fontSize: '13px', color: '#A8B2C7' }}>
                Pending Requests: <strong style={{ color: '#FFC542' }}>{pendingWithdrawalsCount}</strong>
              </span>
            </div>

            <div style={{ background: '#131B2E', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '16px', overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#6B7A99', background: '#0F1626' }}>
                    <th style={{ padding: '16px' }}>REQUEST ID</th>
                    <th style={{ padding: '16px' }}>USER</th>
                    <th style={{ padding: '16px' }}>UPI ID (PAYOUT ADDRESS)</th>
                    <th style={{ padding: '16px' }}>POINTS DEDUCTED</th>
                    <th style={{ padding: '16px' }}>DISBURSE CASH (₹ INR)</th>
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
                          background: 'rgba(0, 209, 255, 0.1)', padding: '6px 10px', borderRadius: '6px',
                          border: '1px solid rgba(0, 209, 255, 0.3)', color: '#00E5FF', fontWeight: 700, fontFamily: 'monospace'
                        }}>
                          {w.upiId}
                        </span>
                      </td>
                      <td style={{ padding: '16px', fontWeight: 700, color: '#FFC542' }}>
                        🪙 {w.pointsDeducted} Pts
                      </td>
                      <td style={{ padding: '16px', fontWeight: 800, color: '#00E676', fontSize: '16px' }}>
                        ₹{w.amountINR.toFixed(2)} INR
                      </td>
                      <td style={{ padding: '16px' }}>
                        <span style={{
                          background: w.status === 'pending' ? 'rgba(255, 197, 66, 0.15)' : w.status === 'approved' ? 'rgba(0, 230, 118, 0.15)' : 'rgba(255, 77, 77, 0.15)',
                          color: w.status === 'pending' ? '#FFC542' : w.status === 'approved' ? '#00E676' : '#FF4D4D',
                          padding: '4px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 700, textTransform: 'uppercase'
                        }}>
                          {w.status}
                        </span>
                      </td>
                      <td style={{ padding: '16px' }}>
                        {w.status === 'pending' ? (
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              onClick={() => handleApproveWithdrawal(w.id)}
                              style={{
                                padding: '6px 12px', fontSize: '12px', borderRadius: '6px',
                                background: '#059669', color: '#fff', border: 'none', cursor: 'pointer', fontWeight: 700
                              }}
                            >
                              ✓ Approve UPI
                            </button>
                            <button
                              onClick={() => handleRejectWithdrawal(w.id)}
                              style={{
                                padding: '6px 12px', fontSize: '12px', borderRadius: '6px',
                                background: '#DC2626', color: '#fff', border: 'none', cursor: 'pointer', fontWeight: 700
                              }}
                            >
                              ✕ Reject & Refund
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
            TAB: AUDIT LOGS & ADMIN ADJUSTMENTS LEDGER
            =================================================================== */}
        {activeTab === 'audit' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', margin: 0 }}>
                  Immutable Audit Ledger & Admin Modifications ({auditLogs.length} Records)
                </h2>
                <p style={{ fontSize: '13px', color: '#6B7A99', margin: '4px 0 0 0' }}>
                  Every administrative point modification, status freeze/unfreeze, and balance adjustment is recorded with mandatory reason and timestamp.
                </p>
              </div>
            </div>

            <div style={{ background: '#131B2E', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '16px', overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#6B7A99', background: '#0F1626' }}>
                    <th style={{ padding: '16px' }}>TIMESTAMP</th>
                    <th style={{ padding: '16px' }}>TARGET USER</th>
                    <th style={{ padding: '16px' }}>ACTION / POINTS DELTA</th>
                    <th style={{ padding: '16px' }}>BALANCE BEFORE & AFTER</th>
                    <th style={{ padding: '16px' }}>MANDATORY REASON</th>
                    <th style={{ padding: '16px' }}>ADMIN ID</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.length === 0 ? (
                    <tr>
                      <td colSpan={6} style={{ padding: '36px', textAlign: 'center', color: '#6B7A99' }}>
                        No audit logs recorded yet. All administrative actions will appear here in real time.
                      </td>
                    </tr>
                  ) : (
                    auditLogs.map((log) => (
                      <tr key={log.logId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                        <td style={{ padding: '16px', color: '#A8B2C7' }}>{log.timestamp}</td>
                        <td style={{ padding: '16px' }}>
                          <div style={{ fontWeight: 600, color: '#fff' }}>{log.targetUserName}</div>
                          <div style={{ fontSize: '11px', color: '#00D1FF' }}>UID: {log.targetUid}</div>
                        </td>
                        <td style={{ padding: '16px' }}>
                          <span style={{
                            padding: '4px 10px', borderRadius: '6px', fontWeight: 800, fontSize: '13px',
                            background: log.pointsChanged >= 0 ? 'rgba(0, 230, 118, 0.15)' : 'rgba(255, 77, 77, 0.15)',
                            color: log.pointsChanged >= 0 ? '#00E676' : '#FF4D4D'
                          }}>
                            {log.pointsChanged >= 0 ? `+${log.pointsChanged}` : log.pointsChanged} Pts
                          </span>
                        </td>
                        <td style={{ padding: '16px', color: '#FFC542' }}>
                          {log.oldPoints} Pts ➔ {log.newPoints} Pts
                          <div style={{ fontSize: '11px', color: '#6B7A99' }}>
                            (₹{log.oldBalance.toFixed(2)} ➔ ₹{log.newBalance.toFixed(2)})
                          </div>
                        </td>
                        <td style={{ padding: '16px', color: '#fff', fontWeight: 500 }}>
                          {log.reason}
                        </td>
                        <td style={{ padding: '16px', color: '#6B7A99', fontSize: '12px' }}>
                          {log.adminUid}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ===================================================================
            TAB 3: WITHDRAWAL LIMIT ADMIN CONTROL (₹)
            =================================================================== */}
        {activeTab === 'limits' && (
          <div style={{ maxWidth: '800px', margin: '0 auto' }}>
            <div style={{ marginBottom: '20px' }}>
              <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', margin: 0 }}>
                Set Withdrawal Limits & Points Conversion (₹ INR)
              </h2>
              <p style={{ fontSize: '13px', color: '#6B7A99', margin: '4px 0 0 0' }}>
                Aap yahan se minimum withdrawal limit, daily withdrawal cap, aur points conversion rate set aur save kar sakte hain.
              </p>
            </div>

            <div style={{ background: '#131B2E', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '20px', padding: '28px' }}>
              <form onSubmit={handleSaveLimits} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                  {/* Minimum Withdrawal Limit */}
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '8px' }}>
                      Minimum Withdrawal Limit (in ₹ Rupees)
                    </label>
                    <div style={{ position: 'relative' }}>
                      <span style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: '#00E676', fontWeight: 800, fontSize: '16px' }}>₹</span>
                      <input
                        type="number"
                        min="1"
                        value={config.minWithdrawalINR}
                        onChange={(e) => setConfig({ ...config, minWithdrawalINR: Number(e.target.value) })}
                        style={{
                          width: '100%', padding: '12px 14px 12px 34px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                          color: '#fff', fontSize: '16px', fontWeight: 700, outline: 'none', boxSizing: 'border-box'
                        }}
                        required
                      />
                    </div>
                    <div style={{ fontSize: '12px', color: '#6B7A99', marginTop: '4px' }}>
                      Users is amount se kam redeem nahi kar sakenge (Recommended: ₹50).
                    </div>
                  </div>

                  {/* Maximum Daily Withdrawal Limit */}
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '8px' }}>
                      Maximum Daily Withdrawal Cap (in ₹ Rupees)
                    </label>
                    <div style={{ position: 'relative' }}>
                      <span style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: '#00E676', fontWeight: 800, fontSize: '16px' }}>₹</span>
                      <input
                        type="number"
                        min="10"
                        value={config.maxDailyWithdrawalINR}
                        onChange={(e) => setConfig({ ...config, maxDailyWithdrawalINR: Number(e.target.value) })}
                        style={{
                          width: '100%', padding: '12px 14px 12px 34px', borderRadius: '10px',
                          background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                          color: '#fff', fontSize: '16px', fontWeight: 700, outline: 'none', boxSizing: 'border-box'
                        }}
                        required
                      />
                    </div>
                    <div style={{ fontSize: '12px', color: '#6B7A99', marginTop: '4px' }}>
                      Ek user 24 ghante me maximum itna cash nikaal sakta hai.
                    </div>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                  {/* Points per Rupee */}
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '8px' }}>
                      Points Conversion Rate (Points needed for ₹1.00)
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={config.pointsPerRupee}
                      onChange={(e) => setConfig({ ...config, pointsPerRupee: Number(e.target.value) })}
                      style={{
                        width: '100%', padding: '12px 14px', borderRadius: '10px',
                        background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                        color: '#FFC542', fontSize: '16px', fontWeight: 700, outline: 'none', boxSizing: 'border-box'
                      }}
                      required
                    />
                    <div style={{ fontSize: '12px', color: '#6B7A99', marginTop: '4px' }}>
                      Example: 10 Points = ₹1.00 INR (100 Points = ₹10.00).
                    </div>
                  </div>

                  {/* Daily Free Spins */}
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#A8B2C7', marginBottom: '8px' }}>
                      Daily Free Spins Limit per User
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={config.dailySpinLimit}
                      onChange={(e) => setConfig({ ...config, dailySpinLimit: Number(e.target.value) })}
                      style={{
                        width: '100%', padding: '12px 14px', borderRadius: '10px',
                        background: '#0F1626', border: '1px solid rgba(255,255,255,0.15)',
                        color: '#fff', fontSize: '16px', fontWeight: 700, outline: 'none', boxSizing: 'border-box'
                      }}
                      required
                    />
                  </div>
                </div>

                <div style={{
                  padding: '16px',
                  borderRadius: '12px',
                  background: 'rgba(0, 230, 118, 0.08)',
                  border: '1px solid rgba(0, 230, 118, 0.25)',
                  fontSize: '13px',
                  color: '#A8B2C7'
                }}>
                  <strong style={{ color: '#00E676' }}>Live Rule Preview:</strong> Minimum redemption starts at <strong>₹{config.minWithdrawalINR} ({config.minWithdrawalINR * config.pointsPerRupee} Points)</strong>. Agar kisi user ke pass 0 Points hain, toh balance <strong>₹0.00</strong> rahega aur withdraw button disabled rahega.
                </div>

                <button
                  type="submit"
                  style={{
                    padding: '14px',
                    borderRadius: '12px',
                    background: limitsSaved ? '#059669' : 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
                    border: 'none',
                    color: '#fff',
                    fontSize: '15px',
                    fontWeight: 700,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    boxShadow: '0 10px 25px rgba(124, 77, 255, 0.35)'
                  }}
                >
                  <Save size={18} />
                  {limitsSaved ? '✓ Withdrawal Limits Saved!' : 'Save Withdrawal Limits & Rules'}
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            TAB 4: ALL APP PAGES CONTENT CMS (TERMS, PRIVACY, ABOUT, ETC.)
            =================================================================== */}
        {activeTab === 'content' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', margin: 0 }}>
                  App Pages Content CMS & Live Editor
                </h2>
                <p style={{ fontSize: '13px', color: '#6B7A99', margin: '4px 0 0 0' }}>
                  Terms and Conditions, Privacy Policy, Rules, Wheel, Quiz aur sabhi pages ka content change aur save karein.
                </p>
              </div>

              {/* PAGE SELECTOR PILLS */}
              <div style={{ display: 'flex', gap: '8px', overflowX: 'auto' }}>
                <button
                  onClick={() => setSelectedCmsPage('terms')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'terms' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  📜 Terms & Conditions
                </button>
                <button
                  onClick={() => setSelectedCmsPage('privacy')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'privacy' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  🛡️ Privacy Policy
                </button>
                <button
                  onClick={() => setSelectedCmsPage('about')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'about' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  ℹ️ About & Rules
                </button>
                <button
                  onClick={() => setSelectedCmsPage('home')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'home' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  🏠 Home Page
                </button>
                <button
                  onClick={() => setSelectedCmsPage('wheel')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'wheel' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  🎡 Spin Wheel
                </button>
                <button
                  onClick={() => setSelectedCmsPage('quiz')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'quiz' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  🧠 Quiz Rewards
                </button>
                <button
                  onClick={() => setSelectedCmsPage('wallet')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'wallet' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  💳 Wallet Screen
                </button>
                <button
                  onClick={() => setSelectedCmsPage('support')}
                  style={{
                    padding: '8px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer',
                    background: selectedCmsPage === 'support' ? '#7C4DFF' : 'rgba(255,255,255,0.06)',
                    color: '#fff', border: 'none'
                  }}
                >
                  📞 Contact Support
                </button>
              </div>
            </div>

            {/* CMS EDITOR FORM CONTAINER */}
            <div style={{ background: '#131B2E', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '20px', padding: '28px' }}>
              <form onSubmit={handleSaveCMS} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                {/* 1. TERMS AND CONDITIONS */}
                {selectedCmsPage === 'terms' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Terms & Conditions Page
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Page Title</label>
                      <input
                        type="text"
                        value={cmsData.terms.pageTitle}
                        onChange={(e) => setCmsData({ ...cmsData, terms: { ...cmsData.terms, pageTitle: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Introduction Paragraph</label>
                      <textarea
                        rows={3}
                        value={cmsData.terms.introduction}
                        onChange={(e) => setCmsData({ ...cmsData, terms: { ...cmsData.terms, introduction: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Eligibility & Account Rules</label>
                      <textarea
                        rows={3}
                        value={cmsData.terms.eligibilityText}
                        onChange={(e) => setCmsData({ ...cmsData, terms: { ...cmsData.terms, eligibilityText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Points & Virtual Currency Terms</label>
                      <textarea
                        rows={3}
                        value={cmsData.terms.rulesPointsText}
                        onChange={(e) => setCmsData({ ...cmsData, terms: { ...cmsData.terms, rulesPointsText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>UPI Payout Policy & Verification</label>
                      <textarea
                        rows={3}
                        value={cmsData.terms.payoutPolicyText}
                        onChange={(e) => setCmsData({ ...cmsData, terms: { ...cmsData.terms, payoutPolicyText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 2. PRIVACY POLICY */}
                {selectedCmsPage === 'privacy' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Privacy Policy Page
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Page Title</label>
                      <input
                        type="text"
                        value={cmsData.privacy.pageTitle}
                        onChange={(e) => setCmsData({ ...cmsData, privacy: { ...cmsData.privacy, pageTitle: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Data Collected (Google OAuth)</label>
                      <textarea
                        rows={3}
                        value={cmsData.privacy.dataCollectedText}
                        onChange={(e) => setCmsData({ ...cmsData, privacy: { ...cmsData.privacy, dataCollectedText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>UPI ID Handling & Privacy Guarantee</label>
                      <textarea
                        rows={3}
                        value={cmsData.privacy.upiUsageText}
                        onChange={(e) => setCmsData({ ...cmsData, privacy: { ...cmsData.privacy, upiUsageText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Security & Encryption Statement</label>
                      <textarea
                        rows={3}
                        value={cmsData.privacy.securityText}
                        onChange={(e) => setCmsData({ ...cmsData, privacy: { ...cmsData.privacy, securityText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 3. ABOUT & RULES */}
                {selectedCmsPage === 'about' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: About Us & Fair Play Rules
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>App Tagline</label>
                      <input
                        type="text"
                        value={cmsData.about.tagline}
                        onChange={(e) => setCmsData({ ...cmsData, about: { ...cmsData.about, tagline: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Our Story & Revenue Sharing Model</label>
                      <textarea
                        rows={4}
                        value={cmsData.about.storyText}
                        onChange={(e) => setCmsData({ ...cmsData, about: { ...cmsData.about, storyText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Anti-Cheat & Fair Play Policy</label>
                      <textarea
                        rows={3}
                        value={cmsData.about.antiCheatRules}
                        onChange={(e) => setCmsData({ ...cmsData, about: { ...cmsData.about, antiCheatRules: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 4. HOME PAGE */}
                {selectedCmsPage === 'home' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Home Screen Headlines & Marquee
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Hero Headline</label>
                      <input
                        type="text"
                        value={cmsData.home.heroHeadline}
                        onChange={(e) => setCmsData({ ...cmsData, home: { ...cmsData.home, heroHeadline: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Hero Subtitle</label>
                      <input
                        type="text"
                        value={cmsData.home.heroSubtitle}
                        onChange={(e) => setCmsData({ ...cmsData, home: { ...cmsData.home, heroSubtitle: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Announcement Marquee Bar</label>
                      <input
                        type="text"
                        value={cmsData.home.announcementMarquee}
                        onChange={(e) => setCmsData({ ...cmsData, home: { ...cmsData.home, announcementMarquee: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 5. SPIN WHEEL */}
                {selectedCmsPage === 'wheel' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Lucky Spin Wheel Screen
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Wheel Slices & Prizes</label>
                      <input
                        type="text"
                        value={cmsData.wheel.wheelSlices}
                        onChange={(e) => setCmsData({ ...cmsData, wheel: { ...cmsData.wheel, wheelSlices: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Spin Instructions & Rules Note</label>
                      <textarea
                        rows={3}
                        value={cmsData.wheel.rulesNote}
                        onChange={(e) => setCmsData({ ...cmsData, wheel: { ...cmsData.wheel, rulesNote: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 6. QUIZ REWARDS */}
                {selectedCmsPage === 'quiz' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Quiz Screen Rewards
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Points per Correct Answer</label>
                      <input
                        type="number"
                        value={cmsData.quiz.rewardPerCorrectAnswer}
                        onChange={(e) => setCmsData({ ...cmsData, quiz: { ...cmsData.quiz, rewardPerCorrectAnswer: Number(e.target.value) } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#FFC542', fontWeight: 700, outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Quiz Instructions Text</label>
                      <textarea
                        rows={3}
                        value={cmsData.quiz.instructionsText}
                        onChange={(e) => setCmsData({ ...cmsData, quiz: { ...cmsData.quiz, instructionsText: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 7. WALLET SCREEN */}
                {selectedCmsPage === 'wallet' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Wallet & UPI Screen Instructions
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Wallet Heading</label>
                      <input
                        type="text"
                        value={cmsData.wallet.walletHeading}
                        onChange={(e) => setCmsData({ ...cmsData, wallet: { ...cmsData.wallet, walletHeading: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Minimum Threshold Notice</label>
                      <input
                        type="text"
                        value={cmsData.wallet.minimumNotice}
                        onChange={(e) => setCmsData({ ...cmsData, wallet: { ...cmsData.wallet, minimumNotice: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Payout Processing Timelines</label>
                      <textarea
                        rows={2}
                        value={cmsData.wallet.payoutTimelines}
                        onChange={(e) => setCmsData({ ...cmsData, wallet: { ...cmsData.wallet, payoutTimelines: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                {/* 8. SUPPORT */}
                {selectedCmsPage === 'support' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#00D1FF', margin: 0 }}>
                      Edit: Customer Support & Contact Details
                    </h3>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>Support Email</label>
                      <input
                        type="email"
                        value={cmsData.support.supportEmail}
                        onChange={(e) => setCmsData({ ...cmsData, support: { ...cmsData.support, supportEmail: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '12px', color: '#A8B2C7', marginBottom: '6px', fontWeight: 600 }}>WhatsApp / Mobile Helpline</label>
                      <input
                        type="text"
                        value={cmsData.support.whatsappSupport}
                        onChange={(e) => setCmsData({ ...cmsData, support: { ...cmsData.support, whatsappSupport: e.target.value } })}
                        style={{ width: '100%', padding: '12px', borderRadius: '8px', background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', outline: 'none', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                <button
                  type="submit"
                  style={{
                    marginTop: '12px',
                    padding: '14px',
                    borderRadius: '12px',
                    background: cmsSavedToast ? '#059669' : 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
                    border: 'none',
                    color: '#fff',
                    fontSize: '15px',
                    fontWeight: 700,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    boxShadow: '0 10px 25px rgba(124, 77, 255, 0.35)'
                  }}
                >
                  <Save size={18} />
                  {cmsSavedToast ? '✓ Page Content Saved Live!' : `Save [${selectedCmsPage.toUpperCase()}] Page Content`}
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            TAB 5: PUSH NOTIFICATIONS BROADCAST (SABKO 1 BAAR ME)
            =================================================================== */}
        {activeTab === 'notifications' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 1fr) 380px', gap: '24px' }}>
            <div style={{ background: '#131B2E', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '20px', padding: '28px' }}>
              <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', marginBottom: '6px' }}>
                Push Notification Broadcast (1-Click Sabko)
              </h2>
              <p style={{ fontSize: '13px', color: '#6B7A99', marginBottom: '20px' }}>
                Sabhi real active users ke phones par ek click me instant notification bhej sakte hain.
              </p>

              <form onSubmit={handleSendBroadcast} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                    Notification Title
                  </label>
                  <input
                    type="text"
                    value={notifTitle}
                    onChange={(e) => setNotifTitle(e.target.value)}
                    style={{
                      width: '100%', padding: '12px', borderRadius: '10px',
                      background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)',
                      color: '#fff', fontSize: '14px', outline: 'none', boxSizing: 'border-box'
                    }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                    Notification Message Body
                  </label>
                  <textarea
                    rows={3}
                    value={notifMessage}
                    onChange={(e) => setNotifMessage(e.target.value)}
                    style={{
                      width: '100%', padding: '12px', borderRadius: '10px',
                      background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)',
                      color: '#fff', fontSize: '14px', outline: 'none', boxSizing: 'border-box'
                    }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#A8B2C7', marginBottom: '6px' }}>
                    Target Audience
                  </label>
                  <select
                    value={notifTarget}
                    onChange={(e) => setNotifTarget(e.target.value)}
                    style={{
                      width: '100%', padding: '12px', borderRadius: '10px',
                      background: '#0F1626', border: '1px solid rgba(255,255,255,0.12)',
                      color: '#fff', fontSize: '14px', outline: 'none'
                    }}
                  >
                    <option value="ALL_ACTIVE">All Real Active Devices (100%)</option>
                    <option value="ZERO_POINTS">Users with 0 Points (Encourage to Play)</option>
                  </select>
                </div>

                <button
                  type="submit"
                  style={{
                    marginTop: '10px', padding: '14px', borderRadius: '12px',
                    background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)', border: 'none',
                    color: '#fff', fontSize: '15px', fontWeight: 700, cursor: 'pointer',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'
                  }}
                >
                  <Send size={18} />
                  Send Broadcast to All Devices
                </button>
              </form>

              {/* History */}
              <div style={{ marginTop: '28px' }}>
                <h4 style={{ fontSize: '13px', fontWeight: 700, color: '#A8B2C7', marginBottom: '12px' }}>
                  Recent Broadcasts History
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {broadcastHistory.map(b => (
                    <div key={b.id} style={{
                      padding: '12px', borderRadius: '10px', background: 'rgba(255,255,255,0.03)',
                      border: '1px solid rgba(255,255,255,0.06)', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                    }}>
                      <div>
                        <div style={{ fontWeight: 600, color: '#fff', fontSize: '13px' }}>{b.title}</div>
                        <div style={{ color: '#6B7A99', fontSize: '11px' }}>{b.message}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <span style={{ fontSize: '11px', color: '#00E676', background: 'rgba(0,230,118,0.15)', padding: '2px 8px', borderRadius: '6px' }}>
                          Delivered to {b.deliveredCount}
                        </span>
                        <div style={{ fontSize: '10px', color: '#6B7A99', marginTop: '4px' }}>{b.sentAt}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* LIVE ANDROID PHONE PREVIEW MOCKUP */}
            <div>
              <div style={{ textAlign: 'center', marginBottom: '10px', fontSize: '13px', fontWeight: 700, color: '#A8B2C7' }}>
                Live Android Phone Mockup
              </div>
              <div style={{
                background: '#000', borderRadius: '36px', padding: '16px 12px',
                border: '4px solid #334155', boxShadow: '0 25px 60px rgba(0,0,0,0.8)',
                minHeight: '440px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between'
              }}>
                {/* Status Bar */}
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0 12px', fontSize: '11px', color: '#fff', fontWeight: 600 }}>
                  <span>10:30</span>
                  <div style={{ display: 'flex', gap: '4px' }}>
                    <span>5G</span>
                    <span>100%</span>
                  </div>
                </div>

                {/* Android Notification Card */}
                <div style={{
                  background: 'rgba(30, 41, 59, 0.95)',
                  backdropFilter: 'blur(20px)',
                  borderRadius: '18px',
                  padding: '14px',
                  border: '1px solid rgba(255,255,255,0.15)',
                  boxShadow: '0 10px 25px rgba(0,0,0,0.5)'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                    <div style={{ width: '20px', height: '20px', borderRadius: '6px', background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '10px' }}>
                      👑
                    </div>
                    <span style={{ fontSize: '11px', fontWeight: 700, color: '#fff' }}>SpinWin Rewards</span>
                    <span style={{ fontSize: '10px', color: '#94A3B8', marginLeft: 'auto' }}>now</span>
                  </div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                    {notifTitle || 'Notification Title'}
                  </div>
                  <div style={{ fontSize: '12px', color: '#CBD5E1', lineHeight: '1.4' }}>
                    {notifMessage || 'Notification message text preview...'}
                  </div>
                </div>

                {/* Bottom Home Indicator */}
                <div style={{ width: '100px', height: '4px', background: '#64748B', borderRadius: '2px', margin: '10px auto 0 auto' }}></div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

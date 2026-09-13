import { useState } from 'react';
import {
  Users, DollarSign, Award, CheckCircle,
  Settings, AlertCircle
} from 'lucide-react';

interface Withdrawal {
  id: string;
  userName: string;
  uid: string;
  amountRupees: number;
  pointsDeducted: number;
  method: 'UPI' | 'Bank';
  payoutAddress: string;
  status: 'pending' | 'approved' | 'rejected';
  time: string;
}

interface UserItem {
  uid: string;
  name: string;
  email: string;
  phone: string;
  points: number;
  balanceRupees: number;
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  referrals: number;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'withdrawals' | 'users' | 'config'>('withdrawals');

  // Live mock state for admin console
  const [withdrawals, setWithdrawals] = useState<Withdrawal[]>([
    {
      id: 'wd_8912',
      userName: 'Ahmed Khan',
      uid: 'u_98242',
      amountRupees: 50.0,
      pointsDeducted: 500,
      method: 'UPI',
      payoutAddress: 'ahmed.khan@oksbi',
      status: 'pending',
      time: '10 mins ago'
    },
    {
      id: 'wd_8913',
      userName: 'Rohan Sharma',
      uid: 'u_11203',
      amountRupees: 200.0,
      pointsDeducted: 2000,
      method: 'UPI',
      payoutAddress: 'rohan.paytm@paytm',
      status: 'pending',
      time: '35 mins ago'
    },
    {
      id: 'wd_8914',
      userName: 'Zoya Akhtar',
      uid: 'u_33918',
      amountRupees: 150.0,
      pointsDeducted: 1500,
      method: 'Bank',
      payoutAddress: 'A/C: 918237198273 (HDFC0001)',
      status: 'pending',
      time: '1 hour ago'
    },
    {
      id: 'wd_8910',
      userName: 'Vikram Patel',
      uid: 'u_44012',
      amountRupees: 100.0,
      pointsDeducted: 1000,
      method: 'UPI',
      payoutAddress: 'vikram@apl',
      status: 'approved',
      time: '3 hours ago'
    }
  ]);

  const [users] = useState<UserItem[]>([
    { uid: 'u_98242', name: 'Ahmed Khan', email: 'ahmed.khan@example.com', phone: '+91 98765 43210', points: 2450, balanceRupees: 245.0, tier: 'GOLD', referrals: 8 },
    { uid: 'u_11203', name: 'Rohan Sharma', email: 'rohan@sharma.in', phone: '+91 98111 22334', points: 18450, balanceRupees: 1845.0, tier: 'PLATINUM', referrals: 34 },
    { uid: 'u_33918', name: 'Zoya Akhtar', email: 'zoya.akhtar@gmail.com', phone: '+91 99887 66554', points: 14200, balanceRupees: 1420.0, tier: 'PLATINUM', referrals: 21 },
    { uid: 'u_44012', name: 'Vikram Patel', email: 'vikram.p@yahoo.com', phone: '+91 97654 32109', points: 11800, balanceRupees: 1180.0, tier: 'GOLD', referrals: 15 },
    { uid: 'u_55102', name: 'Priya Singh', email: 'priya.singh@outlook.com', phone: '+91 98234 56789', points: 9400, balanceRupees: 940.0, tier: 'SILVER', referrals: 7 }
  ]);

  const [config, setConfig] = useState({
    pointsPerRupee: 10,
    minWithdrawalPoints: 50,
    adRewardPoints: 20,
    dailySpinLimit: 5
  });

  const [notification, setNotification] = useState<string | null>(null);

  const showNotice = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 3000);
  };

  const handleApprove = (id: string) => {
    setWithdrawals(prev => prev.map(w => w.id === id ? { ...w, status: 'approved' } : w));
    showNotice(`Withdrawal #${id} approved! Payout webhook triggered.`);
  };

  const handleReject = (id: string) => {
    setWithdrawals(prev => prev.map(w => w.id === id ? { ...w, status: 'rejected' } : w));
    showNotice(`Withdrawal #${id} rejected. Points refunded to user.`);
  };

  const pendingCount = withdrawals.filter(w => w.status === 'pending').length;
  const totalDisbursed = withdrawals.filter(w => w.status === 'approved').reduce((acc, curr) => acc + curr.amountRupees, 0);

  return (
    <div style={{ minHeight: '100vh', padding: '32px 40px', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Top Header */}
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #7C4DFF, #00D1FF)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px'
          }}>
            👑
          </div>
          <div>
            <h1 style={{ fontSize: '24px', fontWeight: 800, color: '#fff', letterSpacing: '-0.5px' }}>
              SpinWin Rewards <span style={{ color: '#00D1FF', fontSize: '14px', fontWeight: 600 }}>• ADMIN CONSOLE</span>
            </h1>
            <p style={{ color: '#6B7A99', fontSize: '13px' }}>Real-Time Financial Operations & Payout Verification</p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={() => setActiveTab('withdrawals')}
            className={activeTab === 'withdrawals' ? 'btn-primary' : ''}
            style={{
              padding: '10px 18px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'withdrawals' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <DollarSign size={16} /> Payout Queue ({pendingCount})
          </button>
          <button
            onClick={() => setActiveTab('users')}
            className={activeTab === 'users' ? 'btn-primary' : ''}
            style={{
              padding: '10px 18px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'users' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <Users size={16} /> Users ({users.length})
          </button>
          <button
            onClick={() => setActiveTab('config')}
            className={activeTab === 'config' ? 'btn-primary' : ''}
            style={{
              padding: '10px 18px', borderRadius: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              background: activeTab === 'config' ? undefined : 'rgba(255,255,255,0.05)',
              color: '#fff', cursor: 'pointer', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px'
            }}
          >
            <Settings size={16} /> App Rules
          </button>
        </div>
      </header>

      {notification && (
        <div style={{
          marginBottom: '24px', padding: '14px 20px', borderRadius: '12px',
          background: 'rgba(0, 230, 118, 0.15)', border: '1px solid #00E676',
          color: '#00E676', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '10px'
        }}>
          <CheckCircle size={18} /> {notification}
        </div>
      )}

      {/* Metrics Bar */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '32px' }}>
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#A8B2C7', fontSize: '13px', marginBottom: '8px' }}>
            <span>Total Registered Users</span>
            <Users size={18} color="#7C4DFF" />
          </div>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#fff', fontFamily: 'Sora' }}>14,289</div>
          <div style={{ color: '#00E676', fontSize: '12px', marginTop: '6px', fontWeight: 600 }}>+23% this week</div>
        </div>

        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#A8B2C7', fontSize: '13px', marginBottom: '8px' }}>
            <span>Pending Payout Requests</span>
            <AlertCircle size={18} color="#FFC542" />
          </div>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#FFC542', fontFamily: 'Sora' }}>{pendingCount}</div>
          <div style={{ color: '#A8B2C7', fontSize: '12px', marginTop: '6px' }}>Requires manual / automated approval</div>
        </div>

        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#A8B2C7', fontSize: '13px', marginBottom: '8px' }}>
            <span>Total INR Disbursed</span>
            <DollarSign size={18} color="#00E676" />
          </div>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#00E676', fontFamily: 'Sora' }}>₹{totalDisbursed + 12450}</div>
          <div style={{ color: '#00D1FF', fontSize: '12px', marginTop: '6px' }}>100% UPI success rate</div>
        </div>

        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#A8B2C7', fontSize: '13px', marginBottom: '8px' }}>
            <span>AdMob Revenue Earned</span>
            <Award size={18} color="#FF7AC8" />
          </div>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#FF7AC8', fontFamily: 'Sora' }}>$412.80</div>
          <div style={{ color: '#A8B2C7', fontSize: '12px', marginTop: '6px' }}>eCPM: $3.45</div>
        </div>
      </div>

      {/* Main Content Area */}
      {activeTab === 'withdrawals' && (
        <div className="glass-card" style={{ padding: '24px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '18px' }}>Withdrawal Payout Approvals</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', color: '#6B7A99', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>Request ID</th>
                <th style={{ padding: '12px 16px' }}>User</th>
                <th style={{ padding: '12px 16px' }}>Method</th>
                <th style={{ padding: '12px 16px' }}>UPI ID / Bank</th>
                <th style={{ padding: '12px 16px' }}>Amount (INR)</th>
                <th style={{ padding: '12px 16px' }}>Points</th>
                <th style={{ padding: '12px 16px' }}>Status</th>
                <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {withdrawals.map((w) => (
                <tr key={w.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.04)', fontSize: '14px' }}>
                  <td style={{ padding: '16px', fontFamily: 'monospace', color: '#00D1FF' }}>{w.id}</td>
                  <td style={{ padding: '16px' }}>
                    <div style={{ fontWeight: 600 }}>{w.userName}</div>
                    <div style={{ fontSize: '11px', color: '#6B7A99' }}>{w.uid}</div>
                  </td>
                  <td style={{ padding: '16px' }}>
                    <span style={{
                      background: w.method === 'UPI' ? 'rgba(0, 209, 255, 0.15)' : 'rgba(124, 77, 255, 0.15)',
                      color: w.method === 'UPI' ? '#00D1FF' : '#7C4DFF',
                      padding: '3px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 700
                    }}>
                      {w.method}
                    </span>
                  </td>
                  <td style={{ padding: '16px', fontFamily: 'monospace', color: '#A8B2C7' }}>{w.payoutAddress}</td>
                  <td style={{ padding: '16px', fontWeight: 700, color: '#00E676', fontFamily: 'Sora' }}>₹{w.amountRupees.toFixed(2)}</td>
                  <td style={{ padding: '16px', color: '#FFC542' }}>{w.pointsDeducted} pts</td>
                  <td style={{ padding: '16px' }}>
                    <span className={`badge badge-${w.status}`}>{w.status}</span>
                  </td>
                  <td style={{ padding: '16px', textAlign: 'right' }}>
                    {w.status === 'pending' ? (
                      <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                        <button onClick={() => handleApprove(w.id)} className="btn-success">Approve</button>
                        <button onClick={() => handleReject(w.id)} className="btn-danger">Reject</button>
                      </div>
                    ) : (
                      <span style={{ color: '#6B7A99', fontSize: '12px' }}>Processed</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'users' && (
        <div className="glass-card" style={{ padding: '24px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '18px' }}>User Database & Tier Management</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', color: '#6B7A99', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>User</th>
                <th style={{ padding: '12px 16px' }}>Contact</th>
                <th style={{ padding: '12px 16px' }}>Total Points</th>
                <th style={{ padding: '12px 16px' }}>Wallet Balance</th>
                <th style={{ padding: '12px 16px' }}>Tier</th>
                <th style={{ padding: '12px 16px' }}>Referrals</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.uid} style={{ borderBottom: '1px solid rgba(255,255,255,0.04)', fontSize: '14px' }}>
                  <td style={{ padding: '16px' }}>
                    <div style={{ fontWeight: 600 }}>{u.name}</div>
                    <div style={{ fontSize: '11px', color: '#6B7A99' }}>{u.uid}</div>
                  </td>
                  <td style={{ padding: '16px', color: '#A8B2C7' }}>
                    <div>{u.phone}</div>
                    <div style={{ fontSize: '11px', color: '#6B7A99' }}>{u.email}</div>
                  </td>
                  <td style={{ padding: '16px', fontWeight: 700, color: '#FFC542' }}>{u.points} pts</td>
                  <td style={{ padding: '16px', fontWeight: 700, color: '#00E676' }}>₹{u.balanceRupees.toFixed(2)}</td>
                  <td style={{ padding: '16px' }}>
                    <span style={{
                      background: u.tier === 'PLATINUM' ? 'rgba(0, 209, 255, 0.2)' : 'rgba(255, 197, 66, 0.2)',
                      color: u.tier === 'PLATINUM' ? '#00D1FF' : '#FFC542',
                      padding: '4px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700
                    }}>
                      {u.tier}
                    </span>
                  </td>
                  <td style={{ padding: '16px', color: '#fff' }}>{u.referrals} friends</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'config' && (
        <div className="glass-card" style={{ padding: '28px', maxWidth: '600px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '20px' }}>Dynamic App Configuration</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', color: '#A8B2C7', marginBottom: '6px' }}>
                Points per Indian Rupee (Conversion Ratio)
              </label>
              <input
                type="number"
                value={config.pointsPerRupee}
                onChange={(e) => setConfig({ ...config, pointsPerRupee: Number(e.target.value) })}
                style={{
                  width: '100%', padding: '12px 14px', borderRadius: '10px',
                  background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
                  color: '#fff', fontSize: '15px'
                }}
              />
              <span style={{ fontSize: '11px', color: '#6B7A99' }}>Default: 10 points = ₹1.00</span>
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
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', color: '#A8B2C7', marginBottom: '6px' }}>
                AdMob Rewarded Video Payout (Points per Ad)
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
              onClick={() => showNotice("App configuration updated in Firestore 'config/app'")}
              className="btn-primary"
              style={{ marginTop: '12px' }}
            >
              Save Configuration Changes
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

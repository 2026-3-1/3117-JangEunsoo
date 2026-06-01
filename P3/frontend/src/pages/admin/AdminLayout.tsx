import { NavLink } from 'react-router-dom'
import NavBar from '../../components/NavBar'
import type { ReactNode } from 'react'

const tabs = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/users', label: '사용자' },
  { to: '/admin/courses', label: '강의' },
  { to: '/admin/orders', label: '주문/매출' },
  { to: '/admin/reports', label: '신고' },
]

export default function AdminLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-6xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">
          <span className="text-amber-400">관리자</span> · {title}
        </h1>
        <div className="flex gap-1 mb-8 border-b border-gray-800">
          {tabs.map((t) => (
            <NavLink
              key={t.to}
              to={t.to}
              end={t.end}
              className={({ isActive }) =>
                `px-4 py-2 text-sm font-medium -mb-px border-b-2 transition ${
                  isActive
                    ? 'border-amber-400 text-amber-400'
                    : 'border-transparent text-gray-400 hover:text-gray-200'
                }`
              }
            >
              {t.label}
            </NavLink>
          ))}
        </div>
        {children}
      </div>
    </div>
  )
}

import { useCallback, useEffect, useState } from 'react'
import AdminLayout from './AdminLayout'
import { adminApi, type AdminUser } from '../../api/admin'
import { extractMessage } from './adminUtils'
import type { Role } from '../../api/auth'

const roleLabel: Record<Role, string> = { STUDENT: '학생', INSTRUCTOR: '강사', ADMIN: '관리자' }

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [keyword, setKeyword] = useState('')
  const [roleFilter, setRoleFilter] = useState<Role | ''>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setError('')
    adminApi
      .listUsers({ keyword: keyword || undefined, role: roleFilter || undefined, page })
      .then((p) => {
        setUsers(p.content)
        setTotalPages(p.totalPages)
      })
      .catch(() => setError('사용자 목록을 불러오지 못했습니다.'))
  }, [keyword, roleFilter, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const changeRole = async (u: AdminUser, role: Role) => {
    try {
      await adminApi.changeRole(u.id, role)
      load()
    } catch (e: unknown) {
      alert(extractMessage(e))
    }
  }

  const toggleActive = async (u: AdminUser) => {
    try {
      if (u.active) await adminApi.deactivateUser(u.id)
      else await adminApi.activateUser(u.id)
      load()
    } catch (e: unknown) {
      alert(extractMessage(e))
    }
  }

  return (
    <AdminLayout title="사용자 관리">
      <div className="flex gap-2 mb-4">
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setPage(0), load())}
          placeholder="아이디 검색"
          className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200 w-56"
        />
        <select
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value as Role | '')
            setPage(0)
          }}
          className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
        >
          <option value="">전체 역할</option>
          <option value="STUDENT">학생</option>
          <option value="INSTRUCTOR">강사</option>
          <option value="ADMIN">관리자</option>
        </select>
      </div>

      {error && <p className="text-red-400 mb-4">{error}</p>}

      <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-850 text-gray-500 text-left">
            <tr className="border-b border-gray-800">
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">아이디</th>
              <th className="px-4 py-3">역할</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">가입일</th>
              <th className="px-4 py-3 text-right">조치</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className="border-b border-gray-800/60">
                <td className="px-4 py-3 text-gray-500">{u.id}</td>
                <td className="px-4 py-3 text-white">{u.username}</td>
                <td className="px-4 py-3">{roleLabel[u.role]}</td>
                <td className="px-4 py-3">
                  {u.active ? (
                    <span className="text-green-400">활성</span>
                  ) : (
                    <span className="text-red-400">정지</span>
                  )}
                </td>
                <td className="px-4 py-3 text-gray-500">{new Date(u.createdAt).toLocaleDateString()}</td>
                <td className="px-4 py-3 text-right">
                  {u.role !== 'ADMIN' && (
                    <div className="flex gap-2 justify-end">
                      {u.role === 'STUDENT' && (
                        <button
                          onClick={() => changeRole(u, 'INSTRUCTOR')}
                          className="px-2 py-1 text-xs bg-blue-600/80 hover:bg-blue-500 rounded text-white"
                        >
                          강사 승급
                        </button>
                      )}
                      {u.role === 'INSTRUCTOR' && (
                        <button
                          onClick={() => changeRole(u, 'STUDENT')}
                          className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 rounded text-white"
                        >
                          학생 강등
                        </button>
                      )}
                      <button
                        onClick={() => toggleActive(u)}
                        className={`px-2 py-1 text-xs rounded text-white ${
                          u.active ? 'bg-red-600/80 hover:bg-red-500' : 'bg-green-600/80 hover:bg-green-500'
                        }`}
                      >
                        {u.active ? '정지' : '활성화'}
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-600">
                  사용자가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pager page={page} totalPages={totalPages} onChange={setPage} />
    </AdminLayout>
  )
}

export function Pager({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (p: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="flex justify-center gap-2 mt-6">
      <button
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        className="px-3 py-1.5 text-sm bg-gray-800 rounded disabled:opacity-40"
      >
        이전
      </button>
      <span className="px-3 py-1.5 text-sm text-gray-400">
        {page + 1} / {totalPages}
      </span>
      <button
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
        className="px-3 py-1.5 text-sm bg-gray-800 rounded disabled:opacity-40"
      >
        다음
      </button>
    </div>
  )
}

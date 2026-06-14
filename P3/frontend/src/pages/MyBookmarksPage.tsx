import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { deleteBookmark, listMyBookmarks, type Bookmark } from '../api/bookmark'

function formatSeconds(s: number): string {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${sec.toString().padStart(2, '0')}`
}

export default function MyBookmarksPage() {
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([])
  const [error, setError] = useState('')

  const load = async () => {
    try {
      const data = await listMyBookmarks()
      setBookmarks(data)
    } catch {
      setError('북마크를 불러오지 못했습니다.')
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('북마크를 삭제하시겠습니까?')) return
    await deleteBookmark(id)
    load()
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-4xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">내 북마크</h1>

        {error && <p className="text-red-400 mb-4 text-sm">{error}</p>}

        {bookmarks.length === 0 ? (
          <p className="text-gray-500 text-sm">아직 북마크가 없습니다.</p>
        ) : (
          <ul className="space-y-2">
            {bookmarks.map((b) => (
              <li
                key={b.id}
                className="bg-gray-900 border border-gray-800 rounded-2xl p-4 flex items-center justify-between"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-300">
                    {b.courseTitle ?? '강의'}
                    <span className="text-gray-600 mx-2">·</span>
                    <span className="text-white">{b.lectureTitle ?? '렉처'}</span>
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {formatSeconds(b.timeSeconds)} · {new Date(b.createdAt).toLocaleString()}
                  </p>
                  {b.memo && <p className="text-xs text-gray-400 mt-1">{b.memo}</p>}
                </div>
                <div className="flex gap-2 items-center">
                  {b.courseId && (
                    <Link
                      to={`/courses/${b.courseId}`}
                      className="text-xs text-blue-400 hover:text-blue-300"
                    >
                      강의 보기
                    </Link>
                  )}
                  <button
                    onClick={() => handleDelete(b.id)}
                    className="text-xs text-red-400 hover:text-red-300"
                  >
                    삭제
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

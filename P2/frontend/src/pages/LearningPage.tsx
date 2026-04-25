import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCourse, type CourseDetailResponse, type LectureResponse } from '../api/courses'
import { getProgressRate, completeLecture, type ProgressRateResponse } from '../api/progress'
import { getPlaybackPosition, updatePlayback } from '../api/playback'
import {
  createBookmark,
  deleteBookmark,
  listBookmarksByLecture,
  type Bookmark,
} from '../api/bookmark'

function extractYouTubeId(url: string): string {
  const match = url.match(
    /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/
  )
  return match ? match[1] : url
}

function isHtmlVideo(url: string | null | undefined): boolean {
  if (!url) return false
  return /\.(mp4|webm|ogg|m4v)(\?.*)?$/i.test(url)
}

function formatSeconds(s: number): string {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${sec.toString().padStart(2, '0')}`
}

export default function LearningPage() {
  const { id, enrollmentId } = useParams<{ id: string; enrollmentId: string }>()
  const navigate = useNavigate()
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const lastSavedRef = useRef<number>(0)

  const [course, setCourse] = useState<CourseDetailResponse | null>(null)
  const [progress, setProgress] = useState<ProgressRateResponse | null>(null)
  const [completedIds, setCompletedIds] = useState<Set<number>>(new Set())
  const [selectedLecture, setSelectedLecture] = useState<LectureResponse | null>(null)
  const [completing, setCompleting] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([])
  const [bookmarkMemo, setBookmarkMemo] = useState('')
  const [currentTime, setCurrentTime] = useState(0)

  useEffect(() => {
    if (!id || !enrollmentId) return
    const fetchAll = async () => {
      setLoading(true)
      try {
        const [courseData, progressData] = await Promise.all([
          getCourse(Number(id)),
          getProgressRate(Number(enrollmentId)),
        ])
        setCourse(courseData)
        setProgress(progressData)
        if (courseData.sections.length > 0 && courseData.sections[0].lectures.length > 0) {
          setSelectedLecture(courseData.sections[0].lectures[0])
        }
      } catch {
        setError('강의 정보를 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchAll()
  }, [id, enrollmentId])

  useEffect(() => {
    if (!selectedLecture || !enrollmentId) return
    let cancelled = false
    Promise.all([
      getPlaybackPosition(selectedLecture.id, Number(enrollmentId)).catch(() => null),
      listBookmarksByLecture(selectedLecture.id).catch(() => [] as Bookmark[]),
    ]).then(([pos, marks]) => {
      if (cancelled) return
      setBookmarks(marks)
      const seekTo = pos?.currentTimeSeconds ?? 0
      setCurrentTime(seekTo)
      lastSavedRef.current = seekTo
      if (videoRef.current && seekTo > 0) {
        try {
          videoRef.current.currentTime = seekTo
        } catch {
          /* ignore */
        }
      }
    })
    return () => {
      cancelled = true
    }
  }, [selectedLecture?.id, enrollmentId])

  const handleTimeUpdate = () => {
    const v = videoRef.current
    if (!v || !selectedLecture || !enrollmentId) return
    const now = Math.floor(v.currentTime)
    setCurrentTime(now)
    if (Math.abs(now - lastSavedRef.current) >= 10) {
      lastSavedRef.current = now
      updatePlayback(Number(enrollmentId), selectedLecture.id, now).catch(() => undefined)
    }
  }

  const handleComplete = async () => {
    if (!selectedLecture || !enrollmentId) return
    if (completedIds.has(selectedLecture.id)) return
    setCompleting(true)
    try {
      await completeLecture(Number(enrollmentId), selectedLecture.id)
      setCompletedIds((prev) => new Set([...prev, selectedLecture.id]))
      const updated = await getProgressRate(Number(enrollmentId))
      setProgress(updated)
    } catch {
      // ignore
    } finally {
      setCompleting(false)
    }
  }

  const handleAddBookmark = async () => {
    if (!selectedLecture) return
    try {
      const bm = await createBookmark(selectedLecture.id, currentTime, bookmarkMemo || undefined)
      setBookmarks((prev) => [...prev, bm].sort((a, b) => a.timeSeconds - b.timeSeconds))
      setBookmarkMemo('')
    } catch {
      alert('북마크 추가에 실패했습니다.')
    }
  }

  const handleDeleteBookmark = async (bookmarkId: number) => {
    await deleteBookmark(bookmarkId)
    setBookmarks((prev) => prev.filter((b) => b.id !== bookmarkId))
  }

  const handleSeekTo = (seconds: number) => {
    if (videoRef.current) {
      try {
        videoRef.current.currentTime = seconds
        videoRef.current.play().catch(() => undefined)
      } catch {
        /* ignore */
      }
    }
    setCurrentTime(seconds)
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    )
  }

  if (error || !course) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <p className="text-red-400">{error || '강의를 찾을 수 없습니다.'}</p>
      </div>
    )
  }

  const isCompleted = selectedLecture ? completedIds.has(selectedLecture.id) : false
  const useHtmlVideo = isHtmlVideo(selectedLecture?.videoUrl)

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col">
      <header className="flex items-center justify-between px-8 py-4 border-b border-gray-800 shrink-0">
        <button
          onClick={() => navigate(`/courses/${id}`)}
          className="text-sm text-gray-400 hover:text-white transition-colors"
        >
          ← 강의 상세
        </button>
        <div className="text-center">
          <h1 className="text-base font-bold text-white truncate max-w-xs">{course.title}</h1>
          {progress && (
            <p className="text-xs text-gray-400">
              진도율 {progress.progressRate}% ({progress.completedLectures}/{progress.totalLectures})
            </p>
          )}
        </div>
        <button
          onClick={() => navigate('/my/courses')}
          className="text-sm text-gray-400 hover:text-white transition-colors"
        >
          내 수강목록
        </button>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <aside className="w-72 border-r border-gray-800 overflow-y-auto shrink-0">
          <div className="p-4 space-y-4">
            {course.sections.map((section) => (
              <div key={section.id}>
                <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2 px-2">
                  {section.orderNum}. {section.title}
                </h3>
                <ul className="space-y-1">
                  {section.lectures.map((lecture) => {
                    const done = completedIds.has(lecture.id)
                    const active = selectedLecture?.id === lecture.id
                    return (
                      <li key={lecture.id}>
                        <button
                          onClick={() => setSelectedLecture(lecture)}
                          className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors flex items-center gap-2 ${
                            active
                              ? 'bg-blue-600 text-white'
                              : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                          }`}
                        >
                          <span className={done ? 'text-green-400' : 'text-gray-600'}>
                            {done ? '✓' : '○'}
                          </span>
                          <span className="truncate">{lecture.orderNum}. {lecture.title}</span>
                        </button>
                      </li>
                    )
                  })}
                </ul>
              </div>
            ))}
          </div>
        </aside>

        <main className="flex-1 overflow-y-auto p-8 space-y-6">
          {selectedLecture ? (
            <>
              <div className="w-full max-w-3xl space-y-4 mx-auto">
                <h2 className="text-xl font-semibold">{selectedLecture.title}</h2>

                {selectedLecture.videoUrl ? (
                  useHtmlVideo ? (
                    <div className="aspect-video bg-gray-900 rounded-xl overflow-hidden">
                      <video
                        ref={videoRef}
                        key={selectedLecture.id}
                        src={selectedLecture.videoUrl}
                        controls
                        onTimeUpdate={handleTimeUpdate}
                        className="w-full h-full"
                      />
                    </div>
                  ) : (
                    <div className="aspect-video bg-gray-900 rounded-xl overflow-hidden">
                      <iframe
                        key={selectedLecture.videoUrl}
                        src={`https://www.youtube.com/embed/${extractYouTubeId(selectedLecture.videoUrl)}`}
                        className="w-full h-full"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowFullScreen
                      />
                    </div>
                  )
                ) : (
                  <div className="aspect-video bg-gray-900 rounded-xl flex items-center justify-center">
                    <p className="text-gray-500">영상이 없습니다.</p>
                  </div>
                )}

                <div className="flex items-center justify-between">
                  <p className="text-xs text-gray-500">
                    현재 위치: {formatSeconds(currentTime)}
                  </p>
                  <button
                    onClick={handleComplete}
                    disabled={isCompleted || completing}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      isCompleted
                        ? 'bg-green-900 text-green-300 cursor-default'
                        : 'bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 disabled:text-blue-400'
                    }`}
                  >
                    {isCompleted ? '✓ 완료됨' : completing ? '처리 중...' : '완료 처리'}
                  </button>
                </div>

                <div className="bg-gray-900 border border-gray-800 rounded-xl p-4">
                  <h3 className="text-sm font-semibold text-white mb-3">북마크</h3>
                  <div className="flex gap-2 mb-3">
                    <input
                      type="text"
                      value={bookmarkMemo}
                      onChange={(e) => setBookmarkMemo(e.target.value)}
                      placeholder="이 위치 메모 (선택)"
                      className="flex-1 bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm"
                    />
                    <button
                      onClick={handleAddBookmark}
                      className="bg-blue-600 hover:bg-blue-500 text-white px-3 py-1.5 rounded text-sm"
                    >
                      현재 위치 북마크
                    </button>
                  </div>
                  {bookmarks.length === 0 ? (
                    <p className="text-xs text-gray-500">북마크가 없습니다.</p>
                  ) : (
                    <ul className="space-y-1">
                      {bookmarks.map((b) => (
                        <li
                          key={b.id}
                          className="flex items-center justify-between text-sm bg-gray-950 border border-gray-800 rounded px-3 py-2"
                        >
                          <button
                            onClick={() => handleSeekTo(b.timeSeconds)}
                            className="text-blue-400 hover:text-blue-300"
                          >
                            {formatSeconds(b.timeSeconds)}
                          </button>
                          <span className="flex-1 mx-3 text-gray-300 truncate">{b.memo ?? ''}</span>
                          <button
                            onClick={() => handleDeleteBookmark(b.id)}
                            className="text-xs text-red-400 hover:text-red-300"
                          >
                            삭제
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </>
          ) : (
            <p className="text-gray-500">왼쪽에서 강의를 선택하세요.</p>
          )}
        </main>
      </div>
    </div>
  )
}

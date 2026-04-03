import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCourse, type CourseDetailResponse, type LectureResponse } from '../api/courses'
import { getProgressRate, completeLecture, type ProgressRateResponse } from '../api/progress'

function extractYouTubeId(url: string): string {
  const match = url.match(
    /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/
  )
  return match ? match[1] : url
}

export default function LearningPage() {
  const { id, enrollmentId } = useParams<{ id: string; enrollmentId: string }>()
  const navigate = useNavigate()

  const [course, setCourse] = useState<CourseDetailResponse | null>(null)
  const [progress, setProgress] = useState<ProgressRateResponse | null>(null)
  const [completedIds, setCompletedIds] = useState<Set<number>>(new Set())
  const [selectedLecture, setSelectedLecture] = useState<LectureResponse | null>(null)
  const [completing, setCompleting] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

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
        {/* Sidebar */}
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

        {/* Main content */}
        <main className="flex-1 flex flex-col items-center justify-center p-8 space-y-6">
          {selectedLecture ? (
            <>
              <div className="w-full max-w-2xl space-y-4">
                <h2 className="text-xl font-semibold">{selectedLecture.title}</h2>

                {selectedLecture.videoUrl ? (
                  <div className="aspect-video bg-gray-900 rounded-xl overflow-hidden">
                    <iframe
                      key={selectedLecture.videoUrl}
                      src={`https://www.youtube.com/embed/${extractYouTubeId(selectedLecture.videoUrl)}`}
                      className="w-full h-full"
                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                      allowFullScreen
                    />
                  </div>
                ) : (
                  <div className="aspect-video bg-gray-900 rounded-xl flex items-center justify-center">
                    <p className="text-gray-500">영상이 없습니다.</p>
                  </div>
                )}

                <button
                  onClick={handleComplete}
                  disabled={isCompleted || completing}
                  className={`w-full py-3 rounded-xl font-medium transition-colors ${
                    isCompleted
                      ? 'bg-green-900 text-green-300 cursor-default'
                      : 'bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 disabled:text-blue-400'
                  }`}
                >
                  {isCompleted ? '✓ 완료됨' : completing ? '처리 중...' : '완료 처리'}
                </button>
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

import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCourse, type CourseDetailResponse } from '../api/courses'
import { enroll, getMyEnrollments, type EnrollmentResponse } from '../api/enrollments'
import { getReviews, createReview, deleteReview, type ReviewResponse } from '../api/reviews'

const DIFFICULTY_LABEL: Record<string, string> = {
  BEGINNER: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급',
}

export default function CourseDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [course, setCourse] = useState<CourseDetailResponse | null>(null)
  const [enrollment, setEnrollment] = useState<EnrollmentResponse | null>(null)
  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [enrolling, setEnrolling] = useState(false)
  const [error, setError] = useState('')

  const [reviewRating, setReviewRating] = useState(5)
  const [reviewComment, setReviewComment] = useState('')
  const [reviewError, setReviewError] = useState('')
  const [submittingReview, setSubmittingReview] = useState(false)

  useEffect(() => {
    if (!id) return
    const courseId = Number(id)
    const fetchAll = async () => {
      try {
        const [courseData, myEnrollments, reviewData] = await Promise.all([
          getCourse(courseId),
          getMyEnrollments(),
          getReviews(courseId),
        ])
        setCourse(courseData)
        const found = myEnrollments.find((e) => e.courseId === courseData.id) ?? null
        setEnrollment(found)
        setReviews(reviewData)
      } catch {
        setError('강의 정보를 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchAll()
  }, [id])

  const handleEnroll = async () => {
    if (!course) return
    setEnrolling(true)
    try {
      const result = await enroll(course.id)
      setEnrollment(result)
    } catch {
      setError('수강신청에 실패했습니다.')
    } finally {
      setEnrolling(false)
    }
  }

  const handleSubmitReview = async (e: { preventDefault: () => void }) => {
    e.preventDefault()
    if (!course) return
    if (!reviewComment.trim()) {
      setReviewError('리뷰 내용을 입력해주세요.')
      return
    }
    setSubmittingReview(true)
    setReviewError('')
    try {
      const newReview = await createReview(course.id, reviewRating, reviewComment)
      setReviews((prev) => [newReview, ...prev])
      setReviewComment('')
      setReviewRating(5)
    } catch {
      setReviewError('리뷰 작성에 실패했습니다. 이미 리뷰를 작성했거나 수강 중이 아닐 수 있습니다.')
    } finally {
      setSubmittingReview(false)
    }
  }

  const handleDeleteReview = async (reviewId: number) => {
    try {
      await deleteReview(reviewId)
      setReviews((prev) => prev.filter((r) => r.id !== reviewId))
    } catch {
      // ignore
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

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <header className="flex items-center justify-between px-8 py-4 border-b border-gray-800">
        <button
          onClick={() => navigate('/courses')}
          className="text-sm text-gray-400 hover:text-white transition-colors"
        >
          ← 목록으로
        </button>
        <h1 className="text-xl font-bold text-white">DevLearn</h1>
        <div className="w-24" />
      </header>

      <main className="max-w-3xl mx-auto px-6 py-10 space-y-8">
        {/* Course Info */}
        <div className="bg-gray-900 rounded-2xl p-8 space-y-4">
          <div className="space-y-2">
            <div className="flex gap-2 flex-wrap">
              {course.difficulty && (
                <span className={`text-xs px-2 py-1 rounded-full ${
                  course.difficulty === 'BEGINNER' ? 'bg-green-900 text-green-300' :
                  course.difficulty === 'INTERMEDIATE' ? 'bg-yellow-900 text-yellow-300' :
                  'bg-red-900 text-red-300'
                }`}>
                  {DIFFICULTY_LABEL[course.difficulty] ?? course.difficulty}
                </span>
              )}
            </div>
            <h2 className="text-2xl font-bold">{course.title}</h2>
            {course.instructorName && (
              <p className="text-sm text-gray-400">강사: {course.instructorName}</p>
            )}
            {course.description && (
              <p className="text-gray-300 text-sm leading-relaxed">{course.description}</p>
            )}
          </div>

          <hr className="border-gray-800" />

          {enrollment ? (
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-green-400">
                <span>✓</span>
                <span className="text-sm font-medium">수강 중인 강의입니다</span>
              </div>
              <button
                onClick={() => navigate(`/courses/${course.id}/learn/${enrollment.id}`)}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-xl text-sm font-medium transition-colors"
              >
                학습하기
              </button>
            </div>
          ) : (
            <button
              onClick={handleEnroll}
              disabled={enrolling}
              className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 disabled:text-blue-400 rounded-xl font-medium transition-colors"
            >
              {enrolling ? '신청 중...' : '수강신청'}
            </button>
          )}
        </div>

        {/* Curriculum */}
        {course.sections.length > 0 && (
          <div className="bg-gray-900 rounded-2xl p-6 space-y-4">
            <h3 className="text-lg font-semibold">커리큘럼</h3>
            <div className="space-y-4">
              {course.sections.map((section) => (
                <div key={section.id}>
                  <h4 className="text-sm font-semibold text-gray-300 mb-2">
                    {section.orderNum}. {section.title}
                  </h4>
                  <ul className="space-y-1 ml-3">
                    {section.lectures.map((lecture) => (
                      <li key={lecture.id} className="flex items-center gap-2 text-sm text-gray-400 py-1">
                        <span className="text-gray-600">▶</span>
                        {lecture.orderNum}. {lecture.title}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Reviews */}
        <div className="bg-gray-900 rounded-2xl p-6 space-y-4">
          <h3 className="text-lg font-semibold">리뷰 ({reviews.length})</h3>

          {enrollment && (
            <form onSubmit={handleSubmitReview} className="space-y-3 border border-gray-700 rounded-xl p-4">
              <p className="text-sm font-medium text-gray-300">리뷰 작성</p>
              <div className="flex gap-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setReviewRating(star)}
                    className={`text-xl transition-colors ${star <= reviewRating ? 'text-yellow-400' : 'text-gray-600'}`}
                  >
                    ★
                  </button>
                ))}
              </div>
              <textarea
                value={reviewComment}
                onChange={(e) => setReviewComment(e.target.value)}
                placeholder="강의에 대한 리뷰를 작성해주세요."
                rows={3}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:border-blue-500 resize-none"
              />
              {reviewError && <p className="text-red-400 text-xs">{reviewError}</p>}
              <button
                type="submit"
                disabled={submittingReview}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 rounded-lg text-sm font-medium transition-colors"
              >
                {submittingReview ? '등록 중...' : '리뷰 등록'}
              </button>
            </form>
          )}

          {reviews.length === 0 ? (
            <p className="text-gray-500 text-sm">아직 리뷰가 없습니다.</p>
          ) : (
            <ul className="space-y-3">
              {reviews.map((review) => (
                <li key={review.id} className="border border-gray-800 rounded-xl p-4 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-yellow-400 text-sm">{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span>
                    <button
                      onClick={() => handleDeleteReview(review.id)}
                      className="text-xs text-gray-600 hover:text-red-400 transition-colors"
                    >
                      삭제
                    </button>
                  </div>
                  <p className="text-sm text-gray-300">{review.comment}</p>
                  <p className="text-xs text-gray-600">{new Date(review.createdAt).toLocaleDateString('ko-KR')}</p>
                </li>
              ))}
            </ul>
          )}
        </div>
      </main>
    </div>
  )
}

import { useCallback, useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { useAuth } from '../context/useAuth'
import { getCourse, type CourseDetailResponse } from '../api/courses'
import { qnaApi, reportApi, type QnaQuestion } from '../api/qna'

function msg(e: unknown): string {
  if (typeof e === 'object' && e && 'response' in e) {
    const r = (e as { response?: { data?: { message?: string } } }).response
    if (r?.data?.message) return r.data.message
  }
  return '요청을 처리하지 못했습니다.'
}

export default function CourseQnaPage() {
  const { id } = useParams<{ id: string }>()
  const courseId = Number(id)
  const { userId, role } = useAuth()

  const [course, setCourse] = useState<CourseDetailResponse | null>(null)
  const [questions, setQuestions] = useState<QnaQuestion[]>([])
  const [expanded, setExpanded] = useState<QnaQuestion | null>(null)
  const [error, setError] = useState('')

  // 새 질문 폼
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [isPrivate, setIsPrivate] = useState(false)
  const [formError, setFormError] = useState('')

  const canManage = role === 'ADMIN' || (course != null && course.instructorId === userId)

  const loadList = useCallback(() => {
    qnaApi
      .listByCourse(courseId)
      .then((p) => setQuestions(p.content))
      .catch(() => setError('질문 목록을 불러오지 못했습니다.'))
  }, [courseId])

  useEffect(() => {
    getCourse(courseId).then(setCourse).catch(() => {})
    loadList()
  }, [courseId, loadList])

  const submitQuestion = async (e: { preventDefault: () => void }) => {
    e.preventDefault()
    setFormError('')
    if (!title.trim() || !content.trim()) {
      setFormError('제목과 내용을 입력해주세요.')
      return
    }
    try {
      await qnaApi.createQuestion(courseId, { title, content, isPrivate })
      setTitle('')
      setContent('')
      setIsPrivate(false)
      loadList()
    } catch (err) {
      setFormError(msg(err))
    }
  }

  const openQuestion = async (q: QnaQuestion) => {
    try {
      const detail = await qnaApi.getQuestion(q.id)
      setExpanded(detail)
    } catch (err) {
      alert(msg(err))
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <NavBar />
      <main className="max-w-3xl mx-auto px-6 py-10 space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">
            Q&amp;A {course && <span className="text-gray-400 font-normal">· {course.title}</span>}
          </h1>
          <Link to={`/courses/${courseId}`} className="text-sm text-blue-400 hover:text-blue-300">
            ← 강의로 돌아가기
          </Link>
        </div>

        {error && <p className="text-red-400">{error}</p>}

        {/* 질문 작성 (수강생) */}
        <form onSubmit={submitQuestion} className="bg-gray-900 border border-gray-800 rounded-2xl p-5 space-y-3">
          <p className="text-sm font-medium text-gray-300">새 질문 작성</p>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목"
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm"
          />
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="궁금한 내용을 작성해주세요."
            rows={3}
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm resize-none"
          />
          <label className="flex items-center gap-2 text-xs text-gray-400">
            <input type="checkbox" checked={isPrivate} onChange={(e) => setIsPrivate(e.target.checked)} />
            비공개 질문 (강사·관리자만 열람)
          </label>
          {formError && <p className="text-red-400 text-xs">{formError}</p>}
          <button type="submit" className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm font-medium">
            질문 등록
          </button>
        </form>

        {/* 질문 목록 */}
        <div className="space-y-3">
          {questions.length === 0 && <p className="text-gray-500 text-sm">아직 질문이 없습니다.</p>}
          {questions.map((q) => (
            <div key={q.id} className="bg-gray-900 border border-gray-800 rounded-xl p-4">
              <button onClick={() => openQuestion(q)} className="w-full text-left">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-white">
                    {q.isPrivate && <span className="text-gray-500 mr-1">🔒</span>}
                    {q.title}
                  </span>
                  <span className={`text-xs ${q.answered ? 'text-green-400' : 'text-gray-500'}`}>
                    {q.answered ? `답변 ${q.answerCount}` : '미답변'}
                  </span>
                </div>
                <div className="text-xs text-gray-600 mt-1">
                  {q.authorUsername ?? '익명'} · {new Date(q.createdAt).toLocaleDateString()}
                </div>
              </button>

              {expanded?.id === q.id && (
                <QuestionDetail
                  detail={expanded}
                  canManage={canManage}
                  isAuthor={expanded.authorId === userId}
                  onChanged={() => {
                    setExpanded(null)
                    loadList()
                  }}
                  onAnswered={() => openQuestion(q)}
                />
              )}
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}

function QuestionDetail({
  detail,
  canManage,
  isAuthor,
  onChanged,
  onAnswered,
}: {
  detail: QnaQuestion
  canManage: boolean
  isAuthor: boolean
  onChanged: () => void
  onAnswered: () => void
}) {
  const [answer, setAnswer] = useState('')

  const submitAnswer = async () => {
    if (!answer.trim()) return
    try {
      await qnaApi.createAnswer(detail.id, answer)
      setAnswer('')
      onAnswered()
    } catch (e) {
      alert(msg(e))
    }
  }

  const deleteQuestion = async () => {
    if (!confirm('질문을 삭제할까요?')) return
    try {
      await qnaApi.deleteQuestion(detail.id)
      onChanged()
    } catch (e) {
      alert(msg(e))
    }
  }

  const report = async () => {
    const reason = prompt('신고 사유를 입력하세요')
    if (!reason) return
    try {
      await reportApi.create({ targetType: 'QNA_QUESTION', targetId: detail.id, reason })
      alert('신고가 접수되었습니다.')
    } catch (e) {
      alert(msg(e))
    }
  }

  return (
    <div className="mt-3 pt-3 border-t border-gray-800 space-y-3">
      <p className="text-sm text-gray-300 whitespace-pre-wrap">{detail.content}</p>

      <div className="flex gap-3 text-xs">
        {(isAuthor || canManage) && (
          <button onClick={deleteQuestion} className="text-red-400 hover:text-red-300">
            삭제
          </button>
        )}
        {!isAuthor && (
          <button onClick={report} className="text-gray-500 hover:text-gray-300">
            신고
          </button>
        )}
      </div>

      {/* 답변 목록 */}
      {detail.answers && detail.answers.length > 0 && (
        <ul className="space-y-2">
          {detail.answers.map((a) => (
            <li key={a.id} className="bg-gray-800/60 rounded-lg p-3">
              <div className="flex items-center gap-2 text-xs mb-1">
                <span className="text-blue-400 font-medium">{a.authorUsername ?? '강사'}</span>
                <span className="px-1.5 py-0.5 rounded bg-blue-900/60 text-blue-300">
                  {a.authorRole === 'ADMIN' ? '관리자' : '강사'}
                </span>
              </div>
              <p className="text-sm text-gray-200 whitespace-pre-wrap">{a.content}</p>
            </li>
          ))}
        </ul>
      )}

      {/* 답변 작성 (강사/관리자) */}
      {canManage && (
        <div className="space-y-2">
          <textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            placeholder="답변을 작성하세요."
            rows={2}
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm resize-none"
          />
          <button onClick={submitAnswer} className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 rounded-lg text-xs font-medium">
            답변 등록
          </button>
        </div>
      )}
    </div>
  )
}

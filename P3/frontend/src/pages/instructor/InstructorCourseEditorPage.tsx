import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import NavBar from '../../components/NavBar'
import { getCategories, type CategoryResponse } from '../../api/categories'
import {
  instructorApi,
  type InstructorCourse,
  type InstructorLecture,
  type InstructorSection,
} from '../../api/instructor'

const difficulties = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED']

export default function InstructorCourseEditorPage() {
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const isEdit = Boolean(id)
  const courseId = id ? Number(id) : null

  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [course, setCourse] = useState<InstructorCourse | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [difficulty, setDifficulty] = useState<string>('BEGINNER')
  const [categoryId, setCategoryId] = useState<number | ''>('')
  const [price, setPrice] = useState<string>('0')

  const [sections, setSections] = useState<InstructorSection[]>([])
  const [lecturesBySection, setLecturesBySection] = useState<Record<number, InstructorLecture[]>>({})
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getCategories().then(setCategories).catch(() => undefined)
  }, [])

  useEffect(() => {
    if (!courseId) return
    instructorApi.getCourse(courseId).then((c) => {
      setCourse(c)
      setTitle(c.title)
      setDescription(c.description ?? '')
      setDifficulty(c.difficulty ?? 'BEGINNER')
      setCategoryId(c.categoryId)
      setPrice(String(c.price ?? 0))
    })
    refreshSections(courseId)
  }, [courseId])

  const refreshSections = async (cid: number) => {
    const list = await instructorApi.listSections(cid)
    setSections(list)
    const map: Record<number, InstructorLecture[]> = {}
    for (const s of list) {
      map[s.id] = await instructorApi.listLectures(cid, s.id)
    }
    setLecturesBySection(map)
  }

  const handleSaveCourse = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setInfo('')
    if (!categoryId) {
      setError('카테고리를 선택하세요.')
      return
    }
    setSaving(true)
    try {
      if (isEdit && courseId) {
        const updated = await instructorApi.updateCourse(courseId, {
          categoryId: Number(categoryId),
          title,
          description: description || undefined,
          difficulty,
          price: Number(price) || 0,
        })
        setCourse(updated)
        setInfo('강의 정보를 저장했습니다.')
      } else {
        const created = await instructorApi.createCourse({
          categoryId: Number(categoryId),
          title,
          description: description || undefined,
          difficulty,
          price: Number(price) || 0,
        })
        navigate(`/instructor/courses/${created.id}/edit`, { replace: true })
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const handleAddSection = async () => {
    if (!courseId) return
    const title = prompt('섹션 제목')
    if (!title) return
    await instructorApi.createSection(courseId, { title })
    refreshSections(courseId)
  }

  const handleRenameSection = async (s: InstructorSection) => {
    if (!courseId) return
    const newTitle = prompt('섹션 제목', s.title)
    if (!newTitle) return
    await instructorApi.updateSection(courseId, s.id, { title: newTitle, orderNum: s.orderNum ?? undefined })
    refreshSections(courseId)
  }

  const handleDeleteSection = async (s: InstructorSection) => {
    if (!courseId) return
    if (!confirm(`섹션 "${s.title}" 와 포함된 렉처를 모두 삭제할까요?`)) return
    await instructorApi.deleteSection(courseId, s.id)
    refreshSections(courseId)
  }

  const handleAddLecture = async (sectionId: number) => {
    if (!courseId) return
    const title = prompt('렉처 제목')
    if (!title) return
    const videoUrl = prompt('영상 URL (선택)') ?? undefined
    const durationStr = prompt('길이 (초, 선택)') ?? ''
    const durationSeconds = durationStr ? Number(durationStr) : undefined
    await instructorApi.createLecture(courseId, sectionId, {
      title,
      videoUrl: videoUrl || undefined,
      durationSeconds,
    })
    refreshSections(courseId)
  }

  const handleEditLecture = async (sectionId: number, l: InstructorLecture) => {
    if (!courseId) return
    const newTitle = prompt('렉처 제목', l.title)
    if (!newTitle) return
    const newUrl = prompt('영상 URL', l.videoUrl ?? '') ?? undefined
    const newDur = prompt('길이 (초)', String(l.durationSeconds ?? '')) ?? ''
    await instructorApi.updateLecture(courseId, sectionId, l.id, {
      title: newTitle,
      videoUrl: newUrl || undefined,
      orderNum: l.orderNum ?? undefined,
      durationSeconds: newDur ? Number(newDur) : undefined,
    })
    refreshSections(courseId)
  }

  const handleDeleteLecture = async (sectionId: number, l: InstructorLecture) => {
    if (!courseId) return
    if (!confirm(`렉처 "${l.title}" 를 삭제할까요?`)) return
    await instructorApi.deleteLecture(courseId, sectionId, l.id)
    refreshSections(courseId)
  }

  const handlePublish = async () => {
    if (!courseId) return
    setError('')
    try {
      const updated = await instructorApi.publishCourse(courseId)
      setCourse(updated)
      setInfo('강의를 발행했습니다.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '발행에 실패했습니다. 섹션·렉처가 1개 이상 필요합니다.')
    }
  }

  const handleArchive = async () => {
    if (!courseId) return
    if (!confirm('강의를 보관 상태로 전환하면 학생이 더 이상 볼 수 없습니다. 진행할까요?')) return
    try {
      await instructorApi.archiveCourse(courseId)
      const c = await instructorApi.getCourse(courseId)
      setCourse(c)
      setInfo('강의를 보관 처리했습니다.')
    } catch {
      setError('보관 처리에 실패했습니다.')
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-5xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">
          {isEdit ? '강의 편집' : '새 강의 만들기'}
          {course && (
            <span className="ml-3 text-xs bg-gray-800 text-gray-300 px-2 py-1 rounded">
              {course.publishStatus}
            </span>
          )}
        </h1>

        <form onSubmit={handleSaveCourse} className="bg-gray-900 border border-gray-800 rounded-2xl p-6 space-y-4">
          <div>
            <label className="block text-sm text-gray-400 mb-1">카테고리</label>
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
              className="bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
              required
            >
              <option value="">선택하세요</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm text-gray-400 mb-1">제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-400 mb-1">설명</label>
            <textarea
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-400 mb-1">난이도</label>
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
              >
                {difficulties.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-400 mb-1">가격 (원, 0 = 무료)</label>
              <input
                type="number"
                min={0}
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
              />
            </div>
          </div>

          {error && <p className="text-red-400 text-sm">{error}</p>}
          {info && <p className="text-emerald-400 text-sm">{info}</p>}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={saving}
              className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded text-sm"
            >
              {saving ? '저장 중...' : '저장'}
            </button>
            {isEdit && course?.publishStatus === 'DRAFT' && (
              <button
                type="button"
                onClick={handlePublish}
                className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded text-sm"
              >
                발행
              </button>
            )}
            {isEdit && course?.publishStatus === 'PUBLISHED' && (
              <button
                type="button"
                onClick={handleArchive}
                className="bg-yellow-700 hover:bg-yellow-600 text-white px-4 py-2 rounded text-sm"
              >
                보관
              </button>
            )}
          </div>
        </form>

        {isEdit && courseId && (
          <div className="mt-8">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-white">커리큘럼</h2>
              <button
                onClick={handleAddSection}
                className="bg-gray-800 hover:bg-gray-700 text-white px-3 py-1.5 rounded text-sm"
              >
                + 섹션 추가
              </button>
            </div>

            {sections.length === 0 ? (
              <p className="text-gray-500 text-sm">섹션이 없습니다. 위에서 섹션을 추가하세요.</p>
            ) : (
              <div className="space-y-4">
                {sections.map((s) => (
                  <div key={s.id} className="bg-gray-900 border border-gray-800 rounded-2xl p-4">
                    <div className="flex items-center justify-between mb-3">
                      <div>
                        <span className="text-xs text-gray-500">#{s.orderNum ?? '-'}</span>
                        <h3 className="text-white font-medium ml-2 inline">{s.title}</h3>
                      </div>
                      <div className="space-x-2 text-sm">
                        <button onClick={() => handleAddLecture(s.id)} className="text-blue-400 hover:text-blue-300">
                          + 렉처
                        </button>
                        <button onClick={() => handleRenameSection(s)} className="text-gray-300 hover:text-white">
                          이름 변경
                        </button>
                        <button onClick={() => handleDeleteSection(s)} className="text-red-400 hover:text-red-300">
                          삭제
                        </button>
                      </div>
                    </div>
                    {(lecturesBySection[s.id] ?? []).length === 0 ? (
                      <p className="text-xs text-gray-600 ml-1">렉처가 없습니다.</p>
                    ) : (
                      <ul className="space-y-1">
                        {(lecturesBySection[s.id] ?? []).map((l) => (
                          <li
                            key={l.id}
                            className="flex items-center justify-between bg-gray-950 border border-gray-800 rounded px-3 py-2 text-sm"
                          >
                            <div className="flex items-center gap-2">
                              <span className="text-gray-500 text-xs">#{l.orderNum ?? '-'}</span>
                              <span className="text-white">{l.title}</span>
                              {l.durationSeconds != null && (
                                <span className="text-gray-500 text-xs">({l.durationSeconds}초)</span>
                              )}
                            </div>
                            <div className="space-x-2">
                              <button onClick={() => handleEditLecture(s.id, l)} className="text-blue-400 hover:text-blue-300">
                                편집
                              </button>
                              <button onClick={() => handleDeleteLecture(s.id, l)} className="text-red-400 hover:text-red-300">
                                삭제
                              </button>
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

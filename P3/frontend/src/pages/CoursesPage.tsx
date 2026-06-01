import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCourses, type CourseResponse } from '../api/courses'
import { getCategories, type CategoryResponse } from '../api/categories'
import NavBar from '../components/NavBar'

const DIFFICULTIES = [
  { label: '전체', value: '' },
  { label: '초급', value: 'BEGINNER' },
  { label: '중급', value: 'INTERMEDIATE' },
  { label: '고급', value: 'ADVANCED' },
]

const DIFFICULTY_LABEL: Record<string, string> = {
  BEGINNER: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급',
}

export default function CoursesPage() {
  const navigate = useNavigate()
  const [courses, setCourses] = useState<CourseResponse[]>([])
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedCategory, setSelectedCategory] = useState<number | undefined>(undefined)
  const [selectedDifficulty, setSelectedDifficulty] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  useEffect(() => {
    getCategories().then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    const fetchCourses = async () => {
      setLoading(true)
      setError('')
      try {
        const res = await getCourses({
          categoryId: selectedCategory,
          difficulty: selectedDifficulty || undefined,
          keyword: keyword || undefined,
          page,
        })
        setCourses(res.content)
        setTotalPages(res.totalPages)
      } catch {
        setError('강의 목록을 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchCourses()
  }, [page, selectedCategory, selectedDifficulty, keyword])

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setPage(0)
    setKeyword(searchInput)
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <NavBar />

      <main className="max-w-5xl mx-auto px-6 py-8">
        <h2 className="text-2xl font-semibold mb-6">강의 목록</h2>

        <form onSubmit={handleSearch} className="mb-6 flex gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="강의 제목 검색..."
            className="flex-1 bg-gray-900 border border-gray-700 rounded-lg px-4 py-2 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:border-blue-500"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm font-medium transition-colors"
          >
            검색
          </button>
          {(keyword || selectedCategory !== undefined || selectedDifficulty) && (
            <button
              type="button"
              onClick={() => {
                setSearchInput('')
                setKeyword('')
                setSelectedCategory(undefined)
                setSelectedDifficulty('')
                setPage(0)
              }}
              className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition-colors"
            >
              초기화
            </button>
          )}
        </form>

        <div className="mb-6 space-y-3">
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => { setPage(0); setSelectedCategory(undefined) }}
              className={`px-3 py-1 rounded-full text-sm transition-colors ${
                selectedCategory === undefined
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
              }`}
            >
              전체
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => { setPage(0); setSelectedCategory(cat.id) }}
                className={`px-3 py-1 rounded-full text-sm transition-colors ${
                  selectedCategory === cat.id
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>

          <div className="flex gap-2">
            {DIFFICULTIES.map((d) => (
              <button
                key={d.value}
                onClick={() => { setPage(0); setSelectedDifficulty(d.value) }}
                className={`px-3 py-1 rounded-full text-sm transition-colors ${
                  selectedDifficulty === d.value
                    ? 'bg-purple-600 text-white'
                    : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                }`}
              >
                {d.label}
              </button>
            ))}
          </div>
        </div>

        {loading && <p className="text-gray-400 text-center py-20">불러오는 중...</p>}
        {error && <p className="text-red-400 text-center py-20">{error}</p>}
        {!loading && !error && courses.length === 0 && (
          <p className="text-gray-500 text-center py-20">등록된 강의가 없습니다.</p>
        )}

        <ul className="space-y-3">
          {courses.map((course) => (
            <li
              key={course.id}
              className="flex items-center justify-between bg-gray-900 hover:bg-gray-800 transition-colors rounded-xl px-6 py-4"
            >
              <div className="flex-1 min-w-0">
                <p
                  className="font-medium text-white truncate cursor-pointer hover:text-blue-300"
                  onClick={() => navigate(`/courses/${course.id}`)}
                >
                  {course.title}
                </p>
                {course.description && (
                  <p className="text-sm text-gray-500 mt-1 truncate">{course.description}</p>
                )}
                <div className="flex gap-3 mt-1 items-center">
                  {course.instructorId ? (
                    <Link
                      to={`/instructors/${course.instructorId}`}
                      className="text-xs text-blue-400 hover:text-blue-300"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {course.instructorName ?? '강사'}
                    </Link>
                  ) : course.instructorName ? (
                    <span className="text-xs text-gray-500">{course.instructorName}</span>
                  ) : null}
                  <span className="text-xs text-gray-400">
                    {course.price === 0 ? '무료' : `${course.price.toLocaleString()}원`}
                  </span>
                </div>
              </div>
              <div className="flex gap-2 ml-4 shrink-0 items-center">
                {course.difficulty && (
                  <span className={`text-xs px-2 py-1 rounded-full ${
                    course.difficulty === 'BEGINNER' ? 'bg-green-900 text-green-300' :
                    course.difficulty === 'INTERMEDIATE' ? 'bg-yellow-900 text-yellow-300' :
                    'bg-red-900 text-red-300'
                  }`}>
                    {DIFFICULTY_LABEL[course.difficulty] ?? course.difficulty}
                  </span>
                )}
                <span className="text-xs text-gray-500 self-center">
                  {categories.find(c => c.id === course.categoryId)?.name ?? `#${course.categoryId}`}
                </span>
              </div>
            </li>
          ))}
        </ul>

        {totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-8">
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                onClick={() => setPage(i)}
                className={`w-8 h-8 rounded-lg text-sm ${
                  i === page
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                }`}
              >
                {i + 1}
              </button>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { instructorApi, type PublicInstructorProfile } from '../api/instructor'

export default function InstructorPublicProfilePage() {
  const { userId } = useParams<{ userId: string }>()
  const [profile, setProfile] = useState<PublicInstructorProfile | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!userId) return
    instructorApi
      .publicProfile(Number(userId))
      .then(setProfile)
      .catch((err: unknown) => {
        const status = (err as { response?: { status?: number } })?.response?.status
        if (status === 404) setError('해당 강사를 찾을 수 없습니다.')
        else setError('강사 프로필을 불러오지 못했습니다.')
      })
  }, [userId])

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-5xl mx-auto px-6 py-10">
        {error && (
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 text-center text-gray-400">
            {error}
          </div>
        )}

        {profile && (
          <>
            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 flex gap-6 items-start">
              <div className="w-20 h-20 rounded-full bg-gray-800 overflow-hidden flex items-center justify-center text-gray-500 text-xl">
                {profile.profileImageUrl ? (
                  <img src={profile.profileImageUrl} alt={profile.displayName} className="w-full h-full object-cover" />
                ) : (
                  profile.displayName.charAt(0).toUpperCase()
                )}
              </div>
              <div className="flex-1">
                <h1 className="text-2xl font-bold text-white">{profile.displayName}</h1>
                <p className="text-sm text-gray-500 mt-1">@{profile.username}</p>
                {profile.careerYears != null && (
                  <p className="text-sm text-gray-400 mt-2">경력 {profile.careerYears}년</p>
                )}
                {profile.bio && <p className="text-sm text-gray-300 mt-3 whitespace-pre-line">{profile.bio}</p>}
                <div className="flex gap-6 mt-4 text-sm">
                  <span className="text-gray-400">
                    수강생 <span className="text-white font-medium">{profile.totalEnrollments.toLocaleString()}</span>
                  </span>
                  <span className="text-gray-400">
                    리뷰 <span className="text-white font-medium">{profile.totalReviews.toLocaleString()}</span>
                  </span>
                  <span className="text-gray-400">
                    평점 <span className="text-white font-medium">{profile.averageRating.toFixed(1)}</span>
                  </span>
                </div>
              </div>
            </div>

            <h2 className="text-lg font-semibold text-white mt-10 mb-4">개설 강의</h2>
            {profile.courses.length === 0 ? (
              <p className="text-sm text-gray-500">아직 발행된 강의가 없습니다.</p>
            ) : (
              <ul className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {profile.courses.map((c) => (
                  <li key={c.id} className="bg-gray-900 border border-gray-800 rounded-2xl p-5 hover:border-gray-700 transition">
                    <Link to={`/courses/${c.id}`} className="block">
                      <h3 className="text-white font-medium">{c.title}</h3>
                      {c.description && (
                        <p className="text-sm text-gray-500 mt-1 line-clamp-2">{c.description}</p>
                      )}
                      <div className="flex justify-between items-center mt-3 text-xs text-gray-400">
                        <span>{c.difficulty ?? '-'}</span>
                        <span>
                          {c.price === 0 ? '무료' : `${c.price.toLocaleString()}원`}
                        </span>
                      </div>
                      <div className="flex justify-between items-center mt-2 text-xs text-gray-500">
                        <span>수강생 {c.enrollmentCount}</span>
                        <span>★ {c.averageRating.toFixed(1)}</span>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </div>
  )
}

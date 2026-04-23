import { useEffect, useState, type FormEvent } from 'react'
import NavBar from '../../components/NavBar'
import { instructorApi, type InstructorProfile } from '../../api/instructor'

export default function InstructorProfileEditPage() {
  const [profile, setProfile] = useState<InstructorProfile | null>(null)
  const [displayName, setDisplayName] = useState('')
  const [bio, setBio] = useState('')
  const [careerYears, setCareerYears] = useState<string>('')
  const [profileImageUrl, setProfileImageUrl] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    instructorApi
      .myProfile()
      .then((p) => {
        setProfile(p)
        setDisplayName(p.displayName)
        setBio(p.bio ?? '')
        setCareerYears(p.careerYears != null ? String(p.careerYears) : '')
        setProfileImageUrl(p.profileImageUrl ?? '')
      })
      .catch(() => setError('프로필을 불러오지 못했습니다.'))
  }, [])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setInfo('')
    setSaving(true)
    try {
      const updated = await instructorApi.updateMyProfile({
        displayName,
        bio: bio || undefined,
        careerYears: careerYears ? Number(careerYears) : undefined,
        profileImageUrl: profileImageUrl || undefined,
      })
      setProfile(updated)
      setInfo('저장되었습니다.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-2xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">강사 프로필 편집</h1>

        <form onSubmit={handleSubmit} className="bg-gray-900 border border-gray-800 rounded-2xl p-6 space-y-4">
          <div>
            <label className="block text-sm text-gray-400 mb-1">표시 이름</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              maxLength={50}
              required
              className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">경력 (년)</label>
            <input
              type="number"
              min={0}
              value={careerYears}
              onChange={(e) => setCareerYears(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">프로필 이미지 URL</label>
            <input
              type="text"
              value={profileImageUrl}
              onChange={(e) => setProfileImageUrl(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">소개</label>
            <textarea
              rows={5}
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 text-white rounded px-3 py-2 text-sm"
            />
          </div>

          {error && <p className="text-red-400 text-sm">{error}</p>}
          {info && <p className="text-emerald-400 text-sm">{info}</p>}

          <button
            type="submit"
            disabled={saving}
            className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded text-sm"
          >
            {saving ? '저장 중...' : '저장'}
          </button>
        </form>

        {profile && (
          <p className="mt-4 text-xs text-gray-500">
            공개 프로필 URL: <code className="text-gray-300">/instructors/{profile.userId}</code>
          </p>
        )}
      </div>
    </div>
  )
}

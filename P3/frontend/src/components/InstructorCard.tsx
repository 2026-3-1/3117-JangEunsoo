import { Link } from 'react-router-dom'

interface Props {
  instructorId: number | null | undefined
  instructorName: string | null | undefined
}

export default function InstructorCard({ instructorId, instructorName }: Props) {
  if (!instructorId && !instructorName) return null
  if (!instructorId) {
    return <span className="text-gray-400 text-sm">{instructorName}</span>
  }
  return (
    <Link
      to={`/instructors/${instructorId}`}
      className="text-blue-400 hover:text-blue-300 text-sm"
    >
      {instructorName ?? '강사 프로필'}
    </Link>
  )
}

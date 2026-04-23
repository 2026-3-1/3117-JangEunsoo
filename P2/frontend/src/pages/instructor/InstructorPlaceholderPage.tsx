interface Props {
  title: string
}

export default function InstructorPlaceholderPage({ title }: Props) {
  return (
    <div className="min-h-screen bg-gray-950 text-gray-300 px-6 py-12">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-2xl font-bold text-white">{title}</h1>
        <p className="mt-3 text-sm text-gray-500">준비 중입니다.</p>
      </div>
    </div>
  )
}

import api from './index'

export interface CategoryResponse {
  id: number
  name: string
}

export const getCategories = async (): Promise<CategoryResponse[]> => {
  const { data } = await api.get('/categories')
  return data.data
}

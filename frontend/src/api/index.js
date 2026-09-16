import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const chuteApi = {
  list: (params) => http.get('/chutes', { params }),
  create: (data) => http.post('/chutes', data),
  update: (id, data) => http.put(`/chutes/${id}`, data)
}

export const batchApi = {
  list: (params) => http.get('/batches', { params }),
  create: (data) => http.post('/batches', data),
  advance: (id, action) => http.post(`/batches/${id}/advance`, null, { params: { action } })
}

export const planApi = {
  list: (params) => http.get('/plans', { params }),
  create: (data) => http.post('/plans', data),
  depart: (id) => http.post(`/plans/${id}/depart`)
}

export const exceptionApi = {
  list: (params) => http.get('/exceptions', { params }),
  create: (data) => http.post('/exceptions', data),
  resolve: (id) => http.post(`/exceptions/${id}/resolve`)
}

export default http

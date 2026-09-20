import axios from 'axios'

const http = axios.create({
  baseURL: '/api'
})

export function describeError(error) {
  const status = error.response?.status
  const data = error.response?.data
  if (typeof data === 'string' && data) {
    return data
  }
  if (Array.isArray(data?.errors)) {
    return data.errors.map(e => e.defaultMessage || e.field).join(', ')
  }
  if (data?.message) {
    return data.message
  }
  if (status === 409) {
    return 'Conflict: the listing was changed by someone else or already exists. Refresh and try again.'
  }
  if (status === 403) {
    return 'You are not allowed to perform this action.'
  }
  if (status === 404) {
    return 'Resource not found.'
  }
  return error.message || 'Something went wrong.'
}

export async function fetchProducts({ search, category, page, size }) {
  const params = { page, size }
  if (search) params.search = search
  if (category) params.category = category
  const { data } = await http.get('/products', { params })
  return data
}

export async function fetchCategories() {
  const { data } = await http.get('/products/categories')
  return data
}

export async function fetchProduct(id) {
  const { data } = await http.get(`/products/${id}`)
  return data
}

export async function fetchProductListings(id, page = 0, size = 20) {
  const { data } = await http.get(`/products/${id}/listings`, { params: { page, size } })
  return data
}

export async function fetchSellers() {
  const { data } = await http.get('/sellers')
  return data
}

export async function fetchSellerListings(sellerId, page = 0, size = 50) {
  const { data } = await http.get('/seller/listings', {
    headers: { 'X-Seller-Id': sellerId },
    params: { page, size }
  })
  return data
}

export async function createListing(sellerId, payload) {
  const { data } = await http.post('/seller/listings', payload, {
    headers: { 'X-Seller-Id': sellerId }
  })
  return data
}

export async function updateListing(sellerId, listingId, payload) {
  const { data } = await http.patch(`/seller/listings/${listingId}`, payload, {
    headers: { 'X-Seller-Id': sellerId }
  })
  return data
}

export async function stopListing(sellerId, listingId) {
  await http.delete(`/seller/listings/${listingId}`, {
    headers: { 'X-Seller-Id': sellerId }
  })
}

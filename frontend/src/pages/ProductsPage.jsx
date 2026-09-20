import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchCategories, fetchProducts, describeError } from '../api.js'
import Pagination from '../components/Pagination.jsx'
import { Loading, ErrorState, EmptyState } from '../components/States.jsx'

const PAGE_SIZE = 9

export default function ProductsPage() {
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [categories, setCategories] = useState([])
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    fetchCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    fetchProducts({ search, category, page, size: PAGE_SIZE })
      .then(data => {
        if (active) {
          setResult(data)
          setError(null)
        }
      })
      .catch(err => {
        if (active) setError(describeError(err))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [search, category, page, reloadKey])

  const products = result?.content ?? []

  return (
    <div>
      <div className="page-head">
        <h1>Construction materials marketplace</h1>
        <p className="subtitle">Compare prices, stock and minimum orders from multiple sellers.</p>
      </div>

      <div className="filters">
        <input
          className="input search-input"
          type="search"
          placeholder="Search products by name…"
          value={searchInput}
          onChange={e => setSearchInput(e.target.value)}
        />
        <select
          className="input"
          value={category}
          onChange={e => {
            setCategory(e.target.value)
            setPage(0)
          }}
        >
          <option value="">All categories</option>
          {categories.map(c => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {loading && <Loading />}
      {!loading && error && <ErrorState message={error} onRetry={() => setReloadKey(k => k + 1)} />}
      {!loading && !error && products.length === 0 && (
        <EmptyState>No products match your search.</EmptyState>
      )}

      {!loading && !error && products.length > 0 && (
        <>
          <div className="product-grid">
            {products.map(product => (
              <Link key={product.id} to={`/products/${product.id}`} className="card product-card">
                <div className="card-top">
                  <span className="chip">{product.category}</span>
                  <span className="unit">per {product.unit}</span>
                </div>
                <h3>{product.name}</h3>
                <p className="description">{product.description}</p>
                <div className="card-bottom">
                  {product.activeSellerCount > 0 ? (
                    <>
                      <span className="price">
                        From ₹{Number(product.lowestPrice).toLocaleString('en-IN')}
                      </span>
                      <span className="sellers-count">
                        {product.activeSellerCount} seller{product.activeSellerCount === 1 ? '' : 's'}
                      </span>
                    </>
                  ) : (
                    <span className="badge badge-bad">No active offers</span>
                  )}
                </div>
              </Link>
            ))}
          </div>
          <Pagination page={result.number} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

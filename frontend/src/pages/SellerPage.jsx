import { useEffect, useMemo, useState } from 'react'
import {
  createListing,
  describeError,
  fetchProducts,
  fetchSellerListings,
  fetchSellers,
  stopListing,
  updateListing
} from '../api.js'
import { ErrorState, Loading, EmptyState } from '../components/States.jsx'

const STORAGE_KEY = 'bajrix-mock-seller-id'

export default function SellerPage() {
  const [sellers, setSellers] = useState([])
  const [sellerId, setSellerId] = useState(() => localStorage.getItem(STORAGE_KEY) || '')
  const [listings, setListings] = useState(null)
  const [products, setProducts] = useState([])
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [editing, setEditing] = useState(null)

  useEffect(() => {
    fetchSellers()
      .then(data => {
        setSellers(data)
        setSellerId(current => {
          if (current && data.some(s => String(s.id) === String(current))) return current
          const firstApproved = data.find(s => s.status === 'APPROVED') || data[0]
          return firstApproved ? String(firstApproved.id) : ''
        })
      })
      .catch(err => setError(describeError(err)))
  }, [])

  useEffect(() => {
    if (!sellerId) return
    localStorage.setItem(STORAGE_KEY, sellerId)
    setListings(null)
    setError(null)
    fetchSellerListings(sellerId)
      .then(setListings)
      .catch(err => setError(describeError(err)))
  }, [sellerId])

  useEffect(() => {
    fetchProducts({ page: 0, size: 100 })
      .then(data => setProducts(data.content))
      .catch(() => setProducts([]))
  }, [])

  const seller = useMemo(() => sellers.find(s => String(s.id) === String(sellerId)), [sellers, sellerId])

  const reload = () => {
    if (!sellerId) return
    fetchSellerListings(sellerId)
      .then(setListings)
      .catch(err => setError(describeError(err)))
  }

  const handleStop = async listing => {
    setNotice(null)
    try {
      await stopListing(sellerId, listing.id)
      setNotice(`Stopped selling "${listing.productName}".`)
      reload()
    } catch (err) {
      setError(describeError(err))
    }
  }

  const handleResume = async listing => {
    setNotice(null)
    try {
      await updateListing(sellerId, listing.id, { status: 'ACTIVE', version: listing.version })
      setNotice(`Resumed selling "${listing.productName}".`)
      reload()
    } catch (err) {
      setError(describeError(err))
      if (err.response?.status === 409) reload()
    }
  }

  if (sellers.length === 0 && !error) {
    return <Loading />
  }

  return (
    <div>
      <div className="page-head">
        <h1>Seller dashboard</h1>
        <p className="subtitle">Manage your listings: price, stock, minimum order and availability.</p>
      </div>

      <div className="filters">
        <label className="seller-select">
          <span>Acting as (mocked identity):</span>
          <select className="input" value={sellerId} onChange={e => setSellerId(e.target.value)}>
            {sellers.map(s => (
              <option key={s.id} value={s.id}>
                {s.name} — {s.status}
              </option>
            ))}
          </select>
        </label>
      </div>

      {seller && seller.status !== 'APPROVED' && (
        <div className="banner banner-warn">
          This seller account is {seller.status}. Only APPROVED sellers can create or modify listings.
        </div>
      )}

      {notice && <div className="banner banner-ok">{notice}</div>}
      {error && <ErrorState message={error} onRetry={reload} />}

      {!error && (
        <div className="toolbar">
          <button className="btn btn-primary" onClick={() => setShowAddForm(v => !v)}>
            {showAddForm ? 'Close' : 'Add listing'}
          </button>
        </div>
      )}

      {showAddForm && sellerId && (
        <AddListingForm
          products={products}
          existingProductIds={(listings?.content || []).map(l => l.productId)}
          sellerId={sellerId}
          onCreated={name => {
            setNotice(`Listing created for "${name}".`)
            setShowAddForm(false)
            reload()
          }}
          onFormError={setError}
        />
      )}

      {!error && sellerId && !listings && <Loading />}
      {!error && sellerId && listings && listings.content.length === 0 && (
        <EmptyState>You have no listings yet. Add your first listing above.</EmptyState>
      )}

      {!error && listings && listings.content.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Product</th>
                <th className="right">Price</th>
                <th className="right">Stock</th>
                <th className="right">MOQ</th>
                <th>Status</th>
                <th className="right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {listings.content.map(listing => (
                <tr key={listing.id} className={listing.status === 'STOPPED' ? 'row-stopped' : ''}>
                  <td>{listing.productName}</td>
                  <td className="right price-cell">₹{Number(listing.price).toLocaleString('en-IN')}</td>
                  <td className="right">{listing.stockQuantity}</td>
                  <td className="right">{listing.minimumOrderQuantity}</td>
                  <td>
                    <span className={listing.status === 'ACTIVE' ? 'badge badge-ok' : 'badge badge-bad'}>
                      {listing.status}
                    </span>
                  </td>
                  <td className="right actions">
                    <button className="btn btn-small" onClick={() => setEditing(listing)}>
                      Edit
                    </button>
                    {listing.status === 'ACTIVE' ? (
                      <button className="btn btn-small btn-danger" onClick={() => handleStop(listing)}>
                        Stop selling
                      </button>
                    ) : (
                      <button className="btn btn-small" onClick={() => handleResume(listing)}>
                        Resume
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <EditListingModal
          listing={editing}
          sellerId={sellerId}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setNotice(`Listing for "${editing.productName}" updated.`)
            setEditing(null)
            reload()
          }}
          onConflict={() => {
            setEditing(null)
            setError('This listing was modified elsewhere. It has been reloaded — please try again.')
            reload()
          }}
        />
      )}
    </div>
  )
}

function AddListingForm({ products, existingProductIds, sellerId, onCreated, onFormError }) {
  const availableProducts = products.filter(p => !existingProductIds.includes(p.id))
  const [productId, setProductId] = useState(availableProducts[0]?.id || '')
  const [price, setPrice] = useState('')
  const [stock, setStock] = useState('')
  const [moq, setMoq] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const validate = () => {
    const errs = {}
    if (!productId) errs.productId = 'Select a product.'
    if (!price || Number(price) <= 0) errs.price = 'Price must be greater than 0.'
    if (stock === '' || Number(stock) < 0 || !Number.isInteger(Number(stock))) errs.stock = 'Stock must be 0 or more.'
    if (!moq || Number(moq) < 1 || !Number.isInteger(Number(moq))) errs.moq = 'MOQ must be at least 1.'
    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async e => {
    e.preventDefault()
    onFormError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      await createListing(sellerId, {
        productId: Number(productId),
        price: Number(price),
        stockQuantity: Number(stock),
        minimumOrderQuantity: Number(moq)
      })
      const product = products.find(p => String(p.id) === String(productId))
      onCreated(product?.name || 'product')
    } catch (err) {
      onFormError(describeError(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (availableProducts.length === 0) {
    return <div className="banner banner-warn">You already have a listing for every product.</div>
  }

  return (
    <form className="card form" onSubmit={handleSubmit}>
      <h2>New listing</h2>
      <div className="form-grid">
        <label>
          Product
          <select className="input" value={productId} onChange={e => setProductId(e.target.value)}>
            {availableProducts.map(p => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.category})
              </option>
            ))}
          </select>
          {fieldErrors.productId && <span className="field-error">{fieldErrors.productId}</span>}
        </label>
        <label>
          Price (₹)
          <input className="input" type="number" step="0.01" min="0.01" value={price} onChange={e => setPrice(e.target.value)} />
          {fieldErrors.price && <span className="field-error">{fieldErrors.price}</span>}
        </label>
        <label>
          Stock quantity
          <input className="input" type="number" min="0" step="1" value={stock} onChange={e => setStock(e.target.value)} />
          {fieldErrors.stock && <span className="field-error">{fieldErrors.stock}</span>}
        </label>
        <label>
          Minimum order quantity
          <input className="input" type="number" min="1" step="1" value={moq} onChange={e => setMoq(e.target.value)} />
          {fieldErrors.moq && <span className="field-error">{fieldErrors.moq}</span>}
        </label>
      </div>
      <div className="form-actions">
        <button className="btn btn-primary" type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create listing'}
        </button>
      </div>
    </form>
  )
}

function EditListingModal({ listing, sellerId, onClose, onSaved, onConflict }) {
  const [price, setPrice] = useState(String(listing.price))
  const [stock, setStock] = useState(String(listing.stockQuantity))
  const [moq, setMoq] = useState(String(listing.minimumOrderQuantity))
  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const validate = () => {
    const errs = {}
    if (!price || Number(price) <= 0) errs.price = 'Price must be greater than 0.'
    if (stock === '' || Number(stock) < 0 || !Number.isInteger(Number(stock))) errs.stock = 'Stock must be 0 or more.'
    if (!moq || Number(moq) < 1 || !Number.isInteger(Number(moq))) errs.moq = 'MOQ must be at least 1.'
    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async e => {
    e.preventDefault()
    setFormError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      await updateListing(sellerId, listing.id, {
        price: Number(price),
        stockQuantity: Number(stock),
        minimumOrderQuantity: Number(moq),
        version: listing.version
      })
      onSaved()
    } catch (err) {
      if (err.response?.status === 409) {
        onConflict()
      } else {
        setFormError(describeError(err))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="card form modal" onClick={e => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>Edit listing — {listing.productName}</h2>
        {formError && <div className="banner banner-bad">{formError}</div>}
        <div className="form-grid">
          <label>
            Price (₹)
            <input className="input" type="number" step="0.01" min="0.01" value={price} onChange={e => setPrice(e.target.value)} />
            {fieldErrors.price && <span className="field-error">{fieldErrors.price}</span>}
          </label>
          <label>
            Stock quantity
            <input className="input" type="number" min="0" step="1" value={stock} onChange={e => setStock(e.target.value)} />
            {fieldErrors.stock && <span className="field-error">{fieldErrors.stock}</span>}
          </label>
          <label>
            Minimum order quantity
            <input className="input" type="number" min="1" step="1" value={moq} onChange={e => setMoq(e.target.value)} />
            {fieldErrors.moq && <span className="field-error">{fieldErrors.moq}</span>}
          </label>
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </div>
  )
}

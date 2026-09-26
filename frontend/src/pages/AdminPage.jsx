import { useEffect, useState } from 'react'
import {
  describeError,
  fetchAdminDashboard,
  fetchAdminSellers,
  fetchAdminSellerListings
} from '../api.js'
import { ErrorState, Loading, EmptyState } from '../components/States.jsx'
import Pagination from '../components/Pagination.jsx'

const statusBadgeClass = status => {
  if (status === 'APPROVED' || status === 'ACTIVE') return 'badge badge-ok'
  if (status === 'PENDING') return 'badge badge-warn'
  return 'badge badge-bad'
}

export default function AdminPage() {
  const [dashboard, setDashboard] = useState(null)
  const [sellers, setSellers] = useState(null)
  const [error, setError] = useState(null)
  const [selectedSellerId, setSelectedSellerId] = useState(null)
  const [listings, setListings] = useState(null)
  const [listingsError, setListingsError] = useState(null)
  const [page, setPage] = useState(0)

  const loadOverview = () => {
    setError(null)
    Promise.all([fetchAdminDashboard(), fetchAdminSellers()])
      .then(([dashData, sellersData]) => {
        setDashboard(dashData)
        setSellers(sellersData)
      })
      .catch(err => setError(describeError(err)))
  }

  useEffect(() => {
    loadOverview()
  }, [])

  const selectedSeller = sellers?.find(s => s.id === selectedSellerId) || null

  const loadListings = (sellerId, pageNumber) => {
    setListings(null)
    setListingsError(null)
    fetchAdminSellerListings(sellerId, pageNumber)
      .then(setListings)
      .catch(err => setListingsError(describeError(err)))
  }

  const handleSelectSeller = sellerId => {
    setSelectedSellerId(sellerId)
    setPage(0)
    loadListings(sellerId, 0)
  }

  const handlePageChange = nextPage => {
    if (!selectedSellerId) return
    setPage(nextPage)
    loadListings(selectedSellerId, nextPage)
  }

  if (error) {
    return (
      <div>
        <div className="page-head">
          <h1>Admin portal</h1>
          <p className="subtitle">Marketplace overview for the BajriX team.</p>
        </div>
        <ErrorState message={error} onRetry={loadOverview} />
      </div>
    )
  }

  if (!dashboard || !sellers) {
    return <Loading />
  }

  return (
    <div>
      <div className="page-head">
        <h1>Admin portal</h1>
        <p className="subtitle">Marketplace overview for the BajriX team.</p>
      </div>

      <div className="admin-summary">
        <div className="card admin-summary-card">
          <div className="admin-summary-label">Total sellers</div>
          <div className="admin-summary-value">{dashboard.totalSellers}</div>
        </div>
        <div className="card admin-summary-card">
          <div className="admin-summary-label">Listings under review</div>
          <div className="admin-summary-value">{dashboard.listingsUnderReview}</div>
        </div>
      </div>

      <h2 className="admin-section-title">Sellers</h2>

      {sellers.length === 0 ? (
        <EmptyState>No sellers yet.</EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Status</th>
                <th className="right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {sellers.map(seller => (
                <tr key={seller.id}>
                  <td>{seller.id}</td>
                  <td>{seller.name}</td>
                  <td>
                    <span className={statusBadgeClass(seller.status)}>{seller.status}</span>
                  </td>
                  <td className="right">
                    <button
                      className="btn btn-small"
                      onClick={() => handleSelectSeller(seller.id)}
                      disabled={selectedSellerId === seller.id}
                    >
                      {selectedSellerId === seller.id ? 'Viewing' : 'View listings'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedSeller && (
        <>
          <h2 className="admin-section-title">
            Listings — {selectedSeller.name}
          </h2>

          {listingsError && (
            <ErrorState message={listingsError} onRetry={() => loadListings(selectedSellerId, page)} />
          )}

          {!listingsError && !listings && <Loading />}

          {!listingsError && listings && listings.content.length === 0 && (
            <EmptyState>This seller has no listings.</EmptyState>
          )}

          {!listingsError && listings && listings.content.length > 0 && (
            <>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th className="right">Price</th>
                      <th className="right">Stock</th>
                      <th className="right">MOQ</th>
                      <th>Status</th>
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
                          <span className={statusBadgeClass(listing.status)}>{listing.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={page} totalPages={listings.totalPages} onChange={handlePageChange} />
            </>
          )}
        </>
      )}
    </div>
  )
}

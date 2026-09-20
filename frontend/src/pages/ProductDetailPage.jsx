import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchProduct, fetchProductListings, describeError } from '../api.js'
import { availabilityOf } from '../availability.js'
import AvailabilityBadge from '../components/AvailabilityBadge.jsx'
import { Loading, ErrorState, EmptyState } from '../components/States.jsx'

export default function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [listings, setListings] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    Promise.all([fetchProduct(id), fetchProductListings(id)])
      .then(([p, l]) => {
        if (active) {
          setProduct(p)
          setListings(l)
          setError(null)
        }
      })
      .catch(err => {
        if (active) setError(describeError(err))
      })
    return () => {
      active = false
    }
  }, [id])

  if (error) {
    return (
      <ErrorState message={error} />
    )
  }
  if (!product || !listings) {
    return <Loading />
  }

  return (
    <div>
      <Link to="/products" className="back-link">
        ← Back to products
      </Link>
      <div className="page-head">
        <div className="card-top">
          <span className="chip">{product.category}</span>
          <span className="unit">Sold per {product.unit}</span>
        </div>
        <h1>{product.name}</h1>
        <p className="subtitle">{product.description}</p>
      </div>

      <h2>Seller offers</h2>
      {listings.content.length === 0 ? (
        <EmptyState>No seller is currently offering this product.</EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Seller</th>
                <th className="right">Price</th>
                <th className="right">Stock</th>
                <th className="right">MOQ</th>
                <th>Availability</th>
              </tr>
            </thead>
            <tbody>
              {listings.content.map(listing => {
                const availability = availabilityOf(listing)
                return (
                  <tr key={listing.id}>
                    <td>
                      <div className="seller-name">{listing.sellerName}</div>
                      <div className="seller-status">{listing.sellerStatus}</div>
                    </td>
                    <td className="right price-cell">₹{Number(listing.price).toLocaleString('en-IN')}</td>
                    <td className="right">{listing.stockQuantity}</td>
                    <td className="right">{listing.minimumOrderQuantity}</td>
                    <td>
                      <AvailabilityBadge code={availability.key} />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export const AVAILABILITY = {
  READY: { label: 'Ready to order', tone: 'ok' },
  LOW_STOCK: { label: 'Low stock', tone: 'warn' },
  BELOW_MOQ: { label: 'Below minimum order', tone: 'warn' },
  OUT_OF_STOCK: { label: 'Out of stock', tone: 'bad' },
  STOPPED: { label: 'Not being sold', tone: 'bad' },
  SELLER_UNAVAILABLE: { label: 'Seller unavailable', tone: 'bad' }
}

export function listingAvailability(listing) {
  if (listing.status === 'STOPPED') {
    return 'STOPPED'
  }
  if (listing.sellerStatus && listing.sellerStatus !== 'APPROVED') {
    return 'SELLER_UNAVAILABLE'
  }
  if (listing.stockQuantity === 0) {
    return 'OUT_OF_STOCK'
  }
  if (listing.stockQuantity < listing.minimumOrderQuantity) {
    return 'BELOW_MOQ'
  }
  if (listing.stockQuantity < listing.minimumOrderQuantity * 5) {
    return 'LOW_STOCK'
  }
  return 'READY'
}

export function availabilityOf(listing) {
  const key = listingAvailability(listing)
  return { key, ...AVAILABILITY[key] }
}

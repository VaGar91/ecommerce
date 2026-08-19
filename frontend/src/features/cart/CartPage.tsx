import { useState } from "react";
import { Link } from "react-router";

import type { CartItem } from "./cartStore";
import { useCart } from "./useCart";

export function CartPage() {
  const { items, totalQuantity, setQuantity, removeProduct, clearCart } = useCart();
  const [confirmingClear, setConfirmingClear] = useState(false);

  if (items.length === 0) {
    return <EmptyCart />;
  }

  const subtotal = items.reduce((total, item) => total + item.price * item.quantity, 0);

  return (
    <section className="cart-page">
      <div className="page-heading cart-heading">
        <div>
          <p className="eyebrow">Cart</p>
          <h1>Review your selection</h1>
          <p className="lede">Adjust quantities before the backend verifies your order.</p>
        </div>
        <button className="button button-quiet clear-cart-button" type="button" onClick={() => setConfirmingClear(true)}>
          Clear cart
        </button>
      </div>

      <div className="cart-layout">
        <div className="cart-lines" role="list" aria-label="Cart items">
          {items.map((item) => (
            <CartLine
              key={item.productId}
              item={item}
              setQuantity={setQuantity}
              removeProduct={removeProduct}
            />
          ))}
        </div>

        <aside className="order-summary" aria-labelledby="order-summary-title">
          <p className="eyebrow">Order estimate</p>
          <h2 id="order-summary-title">Order summary</h2>
          <dl>
            <div>
              <dt>Items</dt>
              <dd>{totalQuantity.toLocaleString()}</dd>
            </div>
            <div className="summary-total">
              <dt>Subtotal</dt>
              <dd>{formatPrice(subtotal)}</dd>
            </div>
          </dl>
          <p className="summary-note">
            Current prices and stock are revalidated by the backend when you place the order.
          </p>
          <Link className="button button-primary checkout-button" to="/checkout">
            Continue to checkout
          </Link>
          <Link className="continue-shopping" to="/catalog">Continue shopping</Link>
        </aside>
      </div>

      {confirmingClear && (
        <div className="dialog-backdrop" role="presentation">
          <section
            className="dialog-panel"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="clear-cart-title"
            aria-describedby="clear-cart-description"
          >
            <p className="eyebrow">Remove all items</p>
            <h2 id="clear-cart-title">Clear your cart?</h2>
            <p id="clear-cart-description">This removes every selected product from this browser.</p>
            <div className="form-actions">
              <button className="button button-secondary" type="button" onClick={() => setConfirmingClear(false)} autoFocus>
                Keep items
              </button>
              <button
                className="button button-danger"
                type="button"
                onClick={() => {
                  clearCart();
                  setConfirmingClear(false);
                }}
              >
                Clear cart
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

type CartLineProps = {
  item: CartItem;
  setQuantity: (productId: string, quantity: number) => void;
  removeProduct: (productId: string) => void;
};

function CartLine({ item, setQuantity, removeProduct }: CartLineProps) {
  return (
    <article className="cart-line" role="listitem">
      <div className="cart-line-monogram" aria-hidden="true">{item.name.charAt(0).toUpperCase()}</div>
      <div className="cart-line-product">
        <small>{item.sku}</small>
        <h2>{item.name}</h2>
        <span>{formatPrice(item.price)} each</span>
      </div>
      <div className="cart-line-quantity">
        <span>Quantity</span>
        <div className="quantity-control" role="group" aria-label={`Quantity for ${item.name}`}>
          <button
            type="button"
            onClick={() => setQuantity(item.productId, item.quantity - 1)}
            disabled={item.quantity === 1}
            aria-label={`Decrease ${item.name} quantity`}
          >
            −
          </button>
          <output aria-label={`${item.name} quantity`}>{item.quantity}</output>
          <button
            type="button"
            onClick={() => setQuantity(item.productId, item.quantity + 1)}
            disabled={item.quantity >= item.availableStock}
            aria-label={`Increase ${item.name} quantity`}
          >
            +
          </button>
        </div>
        <small>{item.quantity >= item.availableStock ? "Stock limit reached" : `${item.availableStock} available`}</small>
      </div>
      <div className="cart-line-total">
        <small>Line total</small>
        <strong>{formatPrice(item.price * item.quantity)}</strong>
      </div>
      <button
        className="text-button text-button-danger cart-remove"
        type="button"
        onClick={() => removeProduct(item.productId)}
        aria-label={`Remove ${item.name} from cart`}
      >
        Remove
      </button>
    </article>
  );
}

function EmptyCart() {
  return (
    <section className="cart-empty">
      <p className="eyebrow">Cart</p>
      <h1>Your cart is empty</h1>
      <p>Browse the catalog to choose products for your order.</p>
      <Link className="button-link" to="/catalog">Browse products</Link>
    </section>
  );
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price);
}

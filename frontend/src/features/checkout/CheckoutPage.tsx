import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link } from "react-router";

import { ApiError } from "../../shared/api/http";
import { useCart } from "../cart/useCart";
import { purchaseOrder, type Order } from "./api/orders";

type PaymentOutcome = "approved" | "declined";

export function CheckoutPage() {
  const queryClient = useQueryClient();
  const { items, totalQuantity, clearCart } = useCart();
  const [paymentOutcome, setPaymentOutcome] = useState<PaymentOutcome>("approved");
  const purchaseMutation = useMutation({
    mutationFn: purchaseOrder,
    onSuccess: () => {
      clearCart();
      void queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });

  if (purchaseMutation.data) {
    return <OrderConfirmation order={purchaseMutation.data} />;
  }

  if (items.length === 0) {
    return <EmptyCheckout />;
  }

  const estimatedSubtotal = items.reduce((total, item) => total + item.price * item.quantity, 0);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    purchaseMutation.mutate({
      items: items.map((item) => ({ productId: item.productId, quantity: item.quantity })),
      paymentToken: paymentOutcome === "declined" ? "tok_declined" : "tok_approved",
    });
  };

  return (
    <section className="checkout-page">
      <div className="page-heading checkout-heading">
        <div>
          <p className="eyebrow">Checkout</p>
          <h1>Complete your purchase</h1>
          <p className="lede">Confirm the order and exercise the fake payment gateway.</p>
        </div>
      </div>

      <form className="checkout-layout" onSubmit={submit}>
        <div className="checkout-main">
          <section className="checkout-card" aria-labelledby="payment-title">
            <p className="eyebrow">Step 1</p>
            <h2 id="payment-title">Fake payment result</h2>
            <p>No payment details are collected. Choose the gateway outcome used for this demonstration.</p>

            <div className="payment-options">
              <label className={paymentOutcome === "approved" ? "payment-option selected" : "payment-option"}>
                <input
                  type="radio"
                  name="paymentOutcome"
                  value="approved"
                  checked={paymentOutcome === "approved"}
                  onChange={() => {
                    setPaymentOutcome("approved");
                    purchaseMutation.reset();
                  }}
                />
                <span>
                  <strong>Approve payment</strong>
                  <small>Uses the deterministic token <code>tok_approved</code>.</small>
                </span>
              </label>
              <label className={paymentOutcome === "declined" ? "payment-option selected" : "payment-option"}>
                <input
                  type="radio"
                  name="paymentOutcome"
                  value="declined"
                  checked={paymentOutcome === "declined"}
                  onChange={() => {
                    setPaymentOutcome("declined");
                    purchaseMutation.reset();
                  }}
                />
                <span>
                  <strong>Simulate a decline</strong>
                  <small>Verifies that inventory and the order roll back together.</small>
                </span>
              </label>
            </div>
          </section>

          <section className="checkout-card" aria-labelledby="review-title">
            <p className="eyebrow">Step 2</p>
            <h2 id="review-title">Review order</h2>
            <div className="checkout-lines" role="list" aria-label="Products being purchased">
              {items.map((item) => (
                <div className="checkout-line" role="listitem" key={item.productId}>
                  <div>
                    <strong>{item.name}</strong>
                    <small>{item.sku} · Quantity {item.quantity}</small>
                  </div>
                  <span>{formatDecimal(item.price * item.quantity)}</span>
                </div>
              ))}
            </div>
          </section>

          {purchaseMutation.isError && <CheckoutError error={purchaseMutation.error} />}
        </div>

        <aside className="checkout-summary" aria-labelledby="checkout-summary-title">
          <p className="eyebrow">Final step</p>
          <h2 id="checkout-summary-title">Place order</h2>
          <dl>
            <div><dt>Items</dt><dd>{totalQuantity.toLocaleString()}</dd></div>
            <div className="summary-total"><dt>Estimated total (USD)</dt><dd>{formatDecimal(estimatedSubtotal)}</dd></div>
          </dl>
          <p>The backend locks products, revalidates stock and uses current prices before charging the fake gateway.</p>
          <button className="button button-primary checkout-submit" type="submit" disabled={purchaseMutation.isPending}>
            {purchaseMutation.isPending ? "Placing order…" : "Place order"}
          </button>
          <Link className="continue-shopping" to="/cart">Return to cart</Link>
        </aside>
      </form>
    </section>
  );
}

function CheckoutError({ error }: { error: Error }) {
  const { title, detail, advice } = checkoutErrorContent(error);

  return (
    <section className="checkout-error" role="alert" aria-labelledby="checkout-error-title">
      <strong id="checkout-error-title">{title}</strong>
      <p>{detail}</p>
      <small>{advice}</small>
    </section>
  );
}

function OrderConfirmation({ order }: { order: Order }) {
  return (
    <section className="confirmation-page" aria-labelledby="order-confirmed-title" role="status">
      <div className="confirmation-mark" aria-hidden="true">✓</div>
      <p className="eyebrow">Payment approved</p>
      <h1 id="order-confirmed-title">Order confirmed</h1>
      <p className="lede">The transactional purchase completed and inventory has been updated.</p>

      <div className="confirmation-details">
        <dl>
          <div><dt>Order ID</dt><dd><code>{order.id}</code></dd></div>
          <div><dt>Status</dt><dd><span className="paid-status">{order.status}</span></dd></div>
          <div><dt>Payment reference</dt><dd><code>{order.paymentReference}</code></dd></div>
          <div><dt>Created</dt><dd>{formatDate(order.createdAt)}</dd></div>
        </dl>

        <div className="confirmation-lines" role="list" aria-label="Purchased products">
          {order.items.map((item) => (
            <div className="confirmation-line" role="listitem" key={item.productId}>
              <div>
                <strong>{item.productName}</strong>
                <small>{item.sku} · {item.quantity} × {formatMoney(item.unitPrice, order.currency)}</small>
              </div>
              <span>{formatMoney(item.lineTotal, order.currency)}</span>
            </div>
          ))}
          <div className="confirmation-total">
            <strong>Total</strong>
            <strong>{formatMoney(order.total, order.currency)}</strong>
          </div>
        </div>
      </div>

      <Link className="button-link" to="/catalog">Continue shopping</Link>
    </section>
  );
}

function EmptyCheckout() {
  return (
    <section className="checkout-empty">
      <p className="eyebrow">Checkout</p>
      <h1>There is nothing to purchase</h1>
      <p>Add products to your cart before starting checkout.</p>
      <Link className="button-link" to="/catalog">Browse products</Link>
    </section>
  );
}

function checkoutErrorContent(error: Error) {
  if (error instanceof ApiError) {
    if (error.status === 402) {
      return {
        title: "Payment declined",
        detail: error.message,
        advice: "Choose the approved fake payment result and try again. Your cart and inventory are unchanged.",
      };
    }
    if (error.status === 409) {
      return {
        title: "Stock changed",
        detail: error.message,
        advice: "Return to the cart, adjust quantities and try again. No payment or inventory change was committed.",
      };
    }
    if (error.status === 404) {
      return {
        title: "Product unavailable",
        detail: error.message,
        advice: "Return to the cart and remove the unavailable product.",
      };
    }
    return {
      title: error.problem.title ?? "Order could not be placed",
      detail: error.message,
      advice: "Review the cart and try again.",
    };
  }

  return {
    title: "Order could not be placed",
    detail: "The server could not be reached.",
    advice: "Your cart is unchanged. Check the connection and try again.",
  };
}

function formatDecimal(value: number): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatMoney(value: number, currency: string): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(value);
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

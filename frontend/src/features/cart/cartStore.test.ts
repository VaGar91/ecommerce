import type { Product } from "../../shared/api/products";
import { CART_STORAGE_KEY, cartReducer, loadCart } from "./cartStore";

const product: Product = {
  id: "0f6eb16e-65ed-4854-8a71-0ad8d1036aac",
  name: "Mechanical Keyboard",
  sku: "KEY-001",
  description: "Tactile mechanical keyboard",
  category: "Accessories",
  price: 49.9,
  stock: 2,
  weightKg: 0.85,
};

afterEach(() => {
  window.localStorage.clear();
});

describe("cartStore", () => {
  it("adds products and never exceeds the stock snapshot", () => {
    let cart = cartReducer([], { type: "add", product });
    cart = cartReducer(cart, { type: "add", product });
    cart = cartReducer(cart, { type: "add", product });

    expect(cart).toHaveLength(1);
    expect(cart[0].quantity).toBe(2);
  });

  it("removes a line when its quantity reaches zero", () => {
    const cart = cartReducer(
      cartReducer([], { type: "add", product }),
      { type: "set-quantity", productId: product.id, quantity: 0 },
    );

    expect(cart).toEqual([]);
  });

  it("ignores malformed persisted cart data", () => {
    window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([
      { productId: product.id, quantity: "many" },
      {
        productId: product.id,
        name: product.name,
        sku: product.sku,
        price: product.price,
        availableStock: product.stock,
        quantity: 1,
      },
    ]));

    expect(loadCart()).toHaveLength(1);
    expect(loadCart()[0].sku).toBe("KEY-001");
  });
});

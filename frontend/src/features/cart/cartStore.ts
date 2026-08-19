import { createContext } from "react";

import type { Product } from "../../shared/api/products";

export const CART_STORAGE_KEY = "gila-commerce.cart.v1";

export type CartItem = {
  productId: string;
  name: string;
  sku: string;
  price: number;
  availableStock: number;
  quantity: number;
};

export type CartContextValue = {
  items: CartItem[];
  totalQuantity: number;
  addProduct: (product: Product) => void;
  setQuantity: (productId: string, quantity: number) => void;
  removeProduct: (productId: string) => void;
  clearCart: () => void;
  quantityFor: (productId: string) => number;
};

export const CartContext = createContext<CartContextValue | undefined>(undefined);

type CartAction =
  | { type: "add"; product: Product }
  | { type: "set-quantity"; productId: string; quantity: number }
  | { type: "remove"; productId: string }
  | { type: "clear" };

export function cartReducer(items: CartItem[], action: CartAction): CartItem[] {
  switch (action.type) {
    case "add":
      return addProduct(items, action.product);
    case "set-quantity":
      return setItemQuantity(items, action.productId, action.quantity);
    case "remove":
      return items.filter((item) => item.productId !== action.productId);
    case "clear":
      return [];
  }
}

export function loadCart(): CartItem[] {
  try {
    const storedCart: unknown = JSON.parse(window.localStorage.getItem(CART_STORAGE_KEY) ?? "[]");
    return Array.isArray(storedCart) ? storedCart.filter(isCartItem) : [];
  } catch {
    return [];
  }
}

function addProduct(items: CartItem[], product: Product): CartItem[] {
  if (product.stock <= 0) {
    return items;
  }

  const existing = items.find((item) => item.productId === product.id);
  const nextQuantity = Math.min((existing?.quantity ?? 0) + 1, product.stock);
  const updatedItem: CartItem = {
    productId: product.id,
    name: product.name,
    sku: product.sku,
    price: product.price,
    availableStock: product.stock,
    quantity: nextQuantity,
  };

  if (!existing) {
    return [...items, updatedItem];
  }

  return items.map((item) => item.productId === product.id ? updatedItem : item);
}

function setItemQuantity(items: CartItem[], productId: string, quantity: number): CartItem[] {
  if (!Number.isInteger(quantity) || quantity <= 0) {
    return items.filter((item) => item.productId !== productId);
  }

  return items.map((item) => item.productId === productId
    ? { ...item, quantity: Math.min(quantity, item.availableStock) }
    : item);
}

function isCartItem(value: unknown): value is CartItem {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const item = value as Record<string, unknown>;
  return typeof item.productId === "string"
    && item.productId.trim().length > 0
    && typeof item.name === "string"
    && item.name.trim().length > 0
    && typeof item.sku === "string"
    && item.sku.trim().length > 0
    && typeof item.price === "number"
    && Number.isFinite(item.price)
    && item.price >= 0
    && typeof item.availableStock === "number"
    && Number.isInteger(item.availableStock)
    && item.availableStock >= 0
    && typeof item.quantity === "number"
    && Number.isInteger(item.quantity)
    && item.quantity > 0
    && item.quantity <= item.availableStock;
}

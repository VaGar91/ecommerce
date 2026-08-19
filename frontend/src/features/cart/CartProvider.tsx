import { useEffect, useMemo, useReducer, type ReactNode } from "react";

import {
  CART_STORAGE_KEY,
  CartContext,
  cartReducer,
  loadCart,
  type CartContextValue,
} from "./cartStore";

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, dispatch] = useReducer(cartReducer, undefined, loadCart);

  useEffect(() => {
    try {
      window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
    } catch {
      // The cart still works in memory when browser storage is unavailable.
    }
  }, [items]);

  const value = useMemo<CartContextValue>(() => ({
    items,
    totalQuantity: items.reduce((total, item) => total + item.quantity, 0),
    addProduct: (product) => dispatch({ type: "add", product }),
    setQuantity: (productId: string, quantity: number) =>
      dispatch({ type: "set-quantity", productId, quantity }),
    removeProduct: (productId: string) => dispatch({ type: "remove", productId }),
    clearCart: () => dispatch({ type: "clear" }),
    quantityFor: (productId: string) =>
      items.find((item) => item.productId === productId)?.quantity ?? 0,
  }), [items]);

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
}

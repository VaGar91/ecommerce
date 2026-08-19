import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";

import { CartPage } from "./CartPage";
import { CartProvider } from "./CartProvider";
import { CART_STORAGE_KEY, type CartItem } from "./cartStore";

const keyboard: CartItem = {
  productId: "0f6eb16e-65ed-4854-8a71-0ad8d1036aac",
  name: "Mechanical Keyboard",
  sku: "KEY-001",
  price: 49.9,
  availableStock: 2,
  quantity: 1,
};

const cable: CartItem = {
  productId: "3ef8cf18-f727-4d97-8f39-2c3d8c62cb86",
  name: "USB Cable",
  sku: "CAB-002",
  price: 12.5,
  availableStock: 5,
  quantity: 2,
};

afterEach(() => {
  window.localStorage.clear();
});

describe("CartPage", () => {
  it("links an empty cart back to the catalog", () => {
    renderCart();

    expect(screen.getByRole("heading", { name: "Your cart is empty" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Browse products" })).toHaveAttribute("href", "/catalog");
  });

  it("updates quantities, totals and persisted cart state", async () => {
    seedCart([keyboard, cable]);
    renderCart();
    const summary = screen.getByRole("complementary", { name: "Order summary" });

    expect(within(summary).getByText("74.90")).toBeInTheDocument();
    expect(within(summary).getByRole("link", { name: "Continue to checkout" })).toHaveAttribute("href", "/checkout");

    fireEvent.click(screen.getByRole("button", { name: "Increase Mechanical Keyboard quantity" }));

    expect(screen.getByLabelText("Mechanical Keyboard quantity")).toHaveTextContent("2");
    expect(screen.getByRole("button", { name: "Increase Mechanical Keyboard quantity" })).toBeDisabled();
    expect(within(summary).getByText("124.80")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Decrease USB Cable quantity" }));
    expect(screen.getByLabelText("USB Cable quantity")).toHaveTextContent("1");
    expect(within(summary).getByText("112.30")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Remove USB Cable from cart" }));
    expect(screen.queryByRole("heading", { name: "USB Cable" })).not.toBeInTheDocument();
    expect(within(summary).getByText("99.80")).toBeInTheDocument();

    await waitFor(() => {
      const storedCart = JSON.parse(window.localStorage.getItem(CART_STORAGE_KEY) ?? "[]") as CartItem[];
      expect(storedCart).toHaveLength(1);
      expect(storedCart[0]?.quantity).toBe(2);
    });
  });

  it("requires confirmation before clearing every item", async () => {
    seedCart([keyboard, cable]);
    renderCart();

    fireEvent.click(screen.getByRole("button", { name: "Clear cart" }));
    let confirmation = screen.getByRole("alertdialog");
    fireEvent.click(within(confirmation).getByRole("button", { name: "Keep items" }));
    expect(screen.getByRole("heading", { name: "Mechanical Keyboard" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Clear cart" }));
    confirmation = screen.getByRole("alertdialog");
    fireEvent.click(within(confirmation).getByRole("button", { name: "Clear cart" }));

    expect(await screen.findByRole("heading", { name: "Your cart is empty" })).toBeInTheDocument();
    await waitFor(() => expect(window.localStorage.getItem(CART_STORAGE_KEY)).toBe("[]"));
  });
});

function renderCart() {
  return render(
    <CartProvider>
      <MemoryRouter>
        <CartPage />
      </MemoryRouter>
    </CartProvider>,
  );
}

function seedCart(items: CartItem[]) {
  window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
}

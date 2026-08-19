import { render, screen } from "@testing-library/react";

import { App } from "./App";

describe("App", () => {
  it("renders the catalog route and primary navigation", async () => {
    window.history.pushState({}, "", "/catalog");

    render(<App />);

    expect(await screen.findByRole("heading", { name: "Find the right product" })).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Primary navigation" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Products" })).toHaveAttribute("href", "/admin/products");
  });
});

import { useQuery } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { useSearchParams } from "react-router";

import { ApiError } from "../../shared/api/http";
import { listProducts, type Product } from "../../shared/api/products";
import { useCart } from "../cart/useCart";

const PAGE_SIZE = 12;

export function CatalogPage() {
  const [searchParameters, setSearchParameters] = useSearchParams();
  const [cartNotice, setCartNotice] = useState<string>();
  const { addProduct, quantityFor } = useCart();
  const query = searchParameters.get("query")?.trim() ?? "";
  const category = searchParameters.get("category")?.trim() ?? "";
  const page = parsePage(searchParameters.get("page"));
  const productsQuery = useQuery({
    queryKey: ["products", "catalog", { query, category, page }],
    queryFn: () => listProducts({ query, category, page, size: PAGE_SIZE }),
    placeholderData: (previousData) => previousData,
  });

  const search = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const nextQuery = String(form.get("query") ?? "").trim();
    const nextCategory = String(form.get("category") ?? "").trim();
    const nextParameters = new URLSearchParams();

    if (nextQuery) {
      nextParameters.set("query", nextQuery);
    }
    if (nextCategory) {
      nextParameters.set("category", nextCategory);
    }

    setSearchParameters(nextParameters);
  };

  const clearSearch = () => setSearchParameters(new URLSearchParams());

  const changePage = (nextPage: number) => {
    const nextParameters = new URLSearchParams(searchParameters);
    if (nextPage > 0) {
      nextParameters.set("page", String(nextPage));
    } else {
      nextParameters.delete("page");
    }
    setSearchParameters(nextParameters);
  };

  const addToCart = (product: Product) => {
    addProduct(product);
    const nextQuantity = Math.min(quantityFor(product.id) + 1, product.stock);
    setCartNotice(`${product.name} added to cart. Quantity: ${nextQuantity}.`);
  };

  const products = productsQuery.data?.content ?? [];
  const totalPages = productsQuery.data?.totalPages ?? 0;

  return (
    <section className="catalog-page">
      <div className="catalog-hero">
        <p className="eyebrow">Storefront</p>
        <h1>Find the right product</h1>
        <p className="lede">Search the live catalog and build your order with current inventory.</p>

        <form
          key={`${query}:${category}`}
          className="catalog-search"
          onSubmit={search}
          role="search"
        >
          <label className="field catalog-query">
            <span>Search products</span>
            <input
              name="query"
              type="search"
              defaultValue={query}
              placeholder="Name, SKU or description"
              maxLength={200}
            />
          </label>
          <label className="field">
            <span>Category</span>
            <input
              name="category"
              defaultValue={category}
              placeholder="All categories"
              maxLength={100}
            />
          </label>
          <button className="button button-primary" type="submit">Search</button>
          {(query || category) && (
            <button className="button button-quiet" type="button" onClick={clearSearch}>Clear</button>
          )}
        </form>
      </div>

      {cartNotice && <div className="alert alert-success cart-notice" role="status">{cartNotice}</div>}

      <div className="catalog-results" aria-busy={productsQuery.isFetching}>
        {productsQuery.isPending && <CatalogState message="Loading products…" />}
        {productsQuery.isError && (
          <CatalogError error={productsQuery.error} onRetry={() => productsQuery.refetch()} />
        )}
        {productsQuery.isSuccess && products.length === 0 && (
          <CatalogState
            title={query || category ? "No products match this search." : "The catalog is empty."}
            message={query || category ? "Try a broader term or clear the category filter." : "Products will appear here after they are created or imported."}
          />
        )}
        {productsQuery.isSuccess && products.length > 0 && (
          <>
            <div className="results-heading">
              <div>
                <p className="eyebrow">Catalog results</p>
                <h2>{productsQuery.data.totalElements.toLocaleString()} products</h2>
              </div>
              {productsQuery.isFetching && <span role="status">Refreshing…</span>}
            </div>

            <div className="product-grid">
              {products.map((product) => {
                const cartQuantity = quantityFor(product.id);
                const stockLimitReached = cartQuantity >= product.stock;

                return (
                  <article className="product-card" key={product.id}>
                    <div className="product-card-topline">
                      <span>{product.category}</span>
                      <StockStatus stock={product.stock} />
                    </div>
                    <div className="product-monogram" aria-hidden="true">{product.name.charAt(0).toUpperCase()}</div>
                    <div className="product-card-body">
                      <small>{product.sku}</small>
                      <h3>{product.name}</h3>
                      <p>{product.description}</p>
                    </div>
                    <div className="product-card-footer">
                      <div className="product-price">
                        <small>Price (USD)</small>
                        <strong>{formatPrice(product.price)}</strong>
                      </div>
                      <button
                        className="button button-primary button-small"
                        type="button"
                        onClick={() => addToCart(product)}
                        disabled={product.stock === 0 || stockLimitReached}
                      >
                        {cartButtonLabel(product.stock, cartQuantity)}
                      </button>
                    </div>
                    {cartQuantity > 0 && (
                      <small className="cart-quantity">{cartQuantity} currently in cart</small>
                    )}
                  </article>
                );
              })}
            </div>

            <div className="catalog-pagination" aria-label="Catalog pagination">
              <p>Page {page + 1} of {totalPages}</p>
              <div>
                <button
                  className="button button-secondary button-small"
                  type="button"
                  onClick={() => changePage(page - 1)}
                  disabled={page === 0 || productsQuery.isFetching}
                >
                  Previous
                </button>
                <button
                  className="button button-secondary button-small"
                  type="button"
                  onClick={() => changePage(page + 1)}
                  disabled={page + 1 >= totalPages || productsQuery.isFetching}
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
}

function StockStatus({ stock }: { stock: number }) {
  if (stock === 0) {
    return <span className="stock-status stock-out">Out of stock</span>;
  }
  if (stock <= 5) {
    return <span className="stock-status stock-low">Only {stock} left</span>;
  }
  return <span className="stock-status">In stock</span>;
}

function CatalogState({ title, message }: { title?: string; message: string }) {
  return (
    <div className="catalog-state" role={title ? undefined : "status"}>
      {title && <strong>{title}</strong>}
      <p>{message}</p>
    </div>
  );
}

function CatalogError({ error, onRetry }: { error: Error; onRetry: () => void }) {
  const message = error instanceof ApiError
    ? error.message
    : "The server could not be reached. Please try again.";

  return (
    <div className="catalog-state" role="alert">
      <strong>Products could not be loaded.</strong>
      <p>{message}</p>
      <button className="button button-secondary button-small" type="button" onClick={onRetry}>Try again</button>
    </div>
  );
}

function parsePage(value: string | null): number {
  const page = Number(value);
  return Number.isInteger(page) && page >= 0 ? page : 0;
}

function cartButtonLabel(stock: number, cartQuantity: number): string {
  if (stock === 0) {
    return "Out of stock";
  }
  if (cartQuantity >= stock) {
    return "Stock limit reached";
  }
  return cartQuantity > 0 ? "Add another" : "Add to cart";
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price);
}
